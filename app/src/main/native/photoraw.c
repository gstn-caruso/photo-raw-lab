/* Stable, owned buffers at the Java boundary; all version-dependent layouts stay here. */
#include <libraw/libraw.h>
#include <lensfun.h>
#include <lcms2.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>

void pr_free(void *p) { free(p); }
void pr_lf_free(void *p) { lf_free(p); }

libraw_processed_image_t *pr_decode(const char *path, float temperature, float tint, int *error) {
    libraw_data_t *raw=libraw_init(0);
    if(!raw){*error=-100007;return NULL;}
    *error=libraw_open_file(raw,path);
    libraw_processed_image_t *image=NULL;
    if(*error) goto done;
    raw->params.output_bps=16;
    raw->params.output_color=8; /* Rec.2020 primaries; avoid an intermediate sRGB gamut clip. */
    raw->params.use_camera_matrix=3;
    raw->params.gamm[0]=1; raw->params.gamm[1]=1;
    raw->params.no_auto_bright=1; raw->params.bright=1;
    raw->params.use_camera_wb=1;
    if(temperature!=0 || tint!=0) {
        float gains[4]={expf(temperature*.006f),expf(-tint*.004f),expf(-temperature*.006f),expf(-tint*.004f)};
        for(int c=0;c<4;c++) raw->params.user_mul[c]=(raw->color.cam_mul[c]>0?raw->color.cam_mul[c]:1)*gains[c];
        raw->params.use_camera_wb=0;
    }
    *error=libraw_unpack(raw); if(*error) goto done;
    *error=libraw_dcraw_process(raw); if(*error) goto done;
    image=libraw_dcraw_make_mem_image(raw,error);
    if(image && (image->type!=LIBRAW_IMAGE_BITMAP || image->bits!=16 || image->colors!=3)) {
        libraw_dcraw_clear_mem(image); image=NULL; *error=-1;
    }
done: libraw_close(raw); return image;
}
int pr_width(const libraw_processed_image_t *p){return p->width;}
int pr_height(const libraw_processed_image_t *p){return p->height;}
void pr_image_free(libraw_processed_image_t *p){libraw_dcraw_clear_mem(p);}

static cmsHPROFILE linear_profile(int rec2020) {
    cmsCIExyY white={.3127,.3290,1};
    cmsCIExyYTRIPLE primaries=rec2020 ? (cmsCIExyYTRIPLE){{.708,.292,1},{.170,.797,1},{.131,.046,1}}
        : (cmsCIExyYTRIPLE){{.640,.330,1},{.300,.600,1},{.150,.060,1}};
    cmsToneCurve *curve=cmsBuildGamma(NULL,1);
    cmsToneCurve *curves[3]={curve,curve,curve};
    cmsHPROFILE profile=cmsCreateRGBProfile(&white,&primaries,curves);
    cmsFreeToneCurve(curve);return profile;
}
int pr_to_working(const libraw_processed_image_t *p,float *out) {
    const unsigned short *data=(const unsigned short *)p->data;
    size_t count=(size_t)p->width*p->height*3;
    if(p->data_size!=count*sizeof(unsigned short))return -1;
    for(size_t i=0;i<count;i++)out[i]=data[i]/65535.0f;
    return 0;
}
int pr_to_srgb(const float *in, unsigned short *out,int count) {
    cmsHPROFILE from=linear_profile(1),to=cmsCreate_sRGBProfile();
    cmsHTRANSFORM t=cmsCreateTransform(from,TYPE_RGB_FLT,to,TYPE_RGB_16,INTENT_RELATIVE_COLORIMETRIC,cmsFLAGS_NOOPTIMIZE);
    if(t) cmsDoTransform(t,in,out,count);
    if(t) cmsDeleteTransform(t);cmsCloseProfile(from);cmsCloseProfile(to);return t?0:-1;
}

/* Strings are Lensfun-owned allocations released with pr_lf_free. */
char *pr_catalog(const char *camera, const char *lens, int adapted) {
    lfDatabase *db=lf_db_new();
    if(lf_db_load(db)!=LF_NO_ERROR){lf_db_destroy(db);return NULL;}
    const lfCamera **cameras=NULL;const lfLens **found=NULL;
    const lfCamera *cam=NULL;
    if(camera && *camera) {cameras=lf_db_find_cameras_ext(db,NULL,camera,0);if(cameras && cameras[0] && !cameras[1])cam=cameras[0];}
    char *xml=NULL;
    if((camera && *camera && !adapted) || (lens && *lens)) {
        /* Unknown camera cannot establish compatibility. */
        if(cam || adapted) {
            found=lf_db_find_lenses_hd(db,adapted?NULL:cam,NULL,lens && *lens?lens:NULL,0);
            if(found) xml=lf_db_save(NULL,NULL,found);
        }
    } else xml=lf_db_save(lf_db_get_mounts(db),lf_db_get_cameras(db),lf_db_get_lenses(db));
    if(found)lf_free(found);if(cameras)lf_free(cameras);lf_db_destroy(db);return xml;
}
float pr_camera_crop(const char *camera) {
    lfDatabase *db=lf_db_new();lf_db_load(db);
    const lfCamera **cams=lf_db_find_cameras_ext(db,NULL,camera,0);
    float crop=cams && cams[0] && !cams[1]?cams[0]->CropFactor:1;
    if(cams)lf_free(cams);lf_db_destroy(db);return crop;
}

typedef struct {lfDatabase *db; lfModifier *mod;int flags;} pr_optics;
void *pr_optics_new(const char *xml,float crop,int width,int height,float focal,float aperture,int requested) {
    pr_optics *o=calloc(1,sizeof(*o));if(!o)return NULL;
    o->db=lf_db_new();
    if(lf_db_load_data(o->db,"saved calibration",xml,strlen(xml))!=LF_NO_ERROR)goto fail;
    const lfLens *const *lenses=lf_db_get_lenses(o->db);
    if(!lenses || !lenses[0] || lenses[1])goto fail;
    const lfLens *lens=lenses[0];
    o->mod=lf_modifier_new(lens,crop,width,height);
    int flags=0;
    if(focal>0) {
        if(requested&1)flags|=LF_MODIFY_DISTORTION;
        if(requested&2)flags|=LF_MODIFY_TCA;
        if((requested&4) && aperture>0)flags|=LF_MODIFY_VIGNETTING;
    }
    o->flags=lf_modifier_initialize(o->mod,lens,LF_PF_F32,focal,aperture,1000,1,LF_RECTILINEAR,flags,0);
    return o;
fail: lf_db_destroy(o->db);free(o);return NULL;
}
int pr_optics_flags(pr_optics *o){return ((o->flags&LF_MODIFY_DISTORTION)?1:0)|((o->flags&LF_MODIFY_TCA)?2:0)|((o->flags&LF_MODIFY_VIGNETTING)?4:0);}
void pr_optics_row(pr_optics *o,int y,int width,float *map) {
    for(int x=0;x<width;x++)for(int c=0;c<3;c++){map[x*6+c*2]=x;map[x*6+c*2+1]=y;}
    lf_modifier_apply_subpixel_geometry_distortion(o->mod,0,y,width,1,map);
}
void pr_optics_color(pr_optics *o,float *rgb,int width,int height) {
    lf_modifier_apply_color_modification(o->mod,rgb,0,0,width,height,LF_CR_3(RED,GREEN,BLUE),width*3*sizeof(float));
}
void pr_optics_free(pr_optics *o){lf_modifier_destroy(o->mod);lf_db_destroy(o->db);free(o);}

int pr_raw_flip(const libraw_data_t *raw){return raw->sizes.flip;}
void pr_bitmap_info(const libraw_processed_image_t *p,int *info){
    info[0]=p->type;info[1]=p->height;info[2]=p->width;info[3]=p->colors;info[4]=p->bits;info[5]=p->data_size;
}
const void *pr_bitmap_data(const libraw_processed_image_t *p){return p->data;}

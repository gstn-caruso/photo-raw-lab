package photorawlab.domain.edit;

/** Owned float32 interleaved RGB pixels in linear Rec.2020. */
public record LinearImage(int width, int height, float[] pixels) {
    public LinearImage {
        if (width <= 0 || height <= 0 || (long) width * height * 3 != pixels.length)
            throw new IllegalArgumentException("Dimensiones inválidas");
    }
}

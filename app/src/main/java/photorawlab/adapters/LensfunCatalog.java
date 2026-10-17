package photorawlab.adapters;

import static java.lang.foreign.FunctionDescriptor.*;
import static java.lang.foreign.ValueLayout.*;

import org.w3c.dom.*;
import org.xml.sax.InputSource;

import photorawlab.domain.edit.*;

import java.io.*;
import java.lang.foreign.*;
import java.util.*;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public final class LensfunCatalog implements LensCatalog {
    private final List<LensProfile> all;

    public LensfunCatalog() throws IOException {
        all = query("", "", true);
    }

    public List<String> manufacturers() {
        return all.stream().map(LensProfile::maker).distinct().sorted().toList();
    }

    public List<LensProfile> search(String maker, String text, String camera, boolean adapted)
            throws IOException {
        List<LensProfile> source = adapted ? all : query(camera, "", false);
        if (!adapted && camera.isBlank()) return List.of();
        String[] words = text.toLowerCase(Locale.ROOT).split("\\s+");
        return source.stream()
                .filter(p -> maker.isBlank() || p.maker().equalsIgnoreCase(maker))
                .filter(
                        p ->
                                Arrays.stream(words)
                                        .allMatch(
                                                w ->
                                                        p.model()
                                                                .toLowerCase(Locale.ROOT)
                                                                .contains(w)))
                .toList();
    }

    public List<LensProfile> automatic(String camera, String lens) throws IOException {
        if (camera.isBlank() || lens.isBlank()) return List.of();
        return query(camera, lens, false);
    }

    public double cameraCrop(String camera) throws IOException {
        if (camera.isBlank()) return 1;
        try (Arena a = Arena.ofConfined()) {
            return (float)
                    NativeBridge.call(
                            "pr_camera_crop", of(JAVA_FLOAT, ADDRESS), a.allocateFrom(camera));
        }
    }

    public Availability available(LensSettings s) throws IOException {
        if (s.profile() == null || s.focal() == 0) return new Availability(false, false, false);
        try (Optics o =
                new Optics(
                        new LensSettings(
                                s.mode(),
                                s.profile(),
                                true,
                                true,
                                true,
                                s.focal(),
                                s.aperture(),
                                s.cameraCrop(),
                                s.adapted()),
                        100,
                        100)) {
            int flags = o.flags();
            return new Availability((flags & 1) != 0, (flags & 2) != 0, (flags & 4) != 0);
        }
    }

    private static List<LensProfile> query(String camera, String lens, boolean adapted)
            throws IOException {
        try (Arena a = Arena.ofConfined()) {
            MemorySegment p =
                    (MemorySegment)
                            NativeBridge.call(
                                    "pr_catalog",
                                    of(ADDRESS, ADDRESS, ADDRESS, JAVA_INT),
                                    a.allocateFrom(camera),
                                    a.allocateFrom(lens),
                                    adapted ? 1 : 0);
            try {
                return parse(NativeBridge.string(p));
            } finally {
                NativeBridge.release("pr_lf_free", p);
            }
        }
    }

    static List<LensProfile> parse(String xml) throws IOException {
        if (xml.isBlank()) return List.of();
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            Document document =
                    factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            var lenses = document.getElementsByTagName("lens");
            List<LensProfile> result = new ArrayList<>();
            Map<String, Integer> variants = new HashMap<>();
            for (int i = 0; i < lenses.getLength(); i++) {
                Element lens = (Element) lenses.item(i);
                String maker = text(lens, "maker"), model = text(lens, "model");
                double crop = Double.parseDouble(text(lens, "cropfactor"));
                StringWriter writer = new StringWriter();
                var t = TransformerFactory.newInstance().newTransformer();
                t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                t.transform(new DOMSource(lens), new StreamResult(writer));
                String snapshot = "<lensdatabase version=\"1\">" + writer + "</lensdatabase>";
                String variant =
                        text(lens, "mount")
                                + " · crop "
                                + crop
                                + " · "
                                + "calibración "
                                + variants.merge(maker + model, 1, Integer::sum);
                result.add(new LensProfile(maker, model, variant, snapshot, crop));
            }
            return List.copyOf(result);
        } catch (Exception e) {
            throw new IOException("Base Lensfun inválida", e);
        }
    }

    private static String text(Element e, String tag) {
        return e.getElementsByTagName(tag).item(0).getTextContent();
    }
}

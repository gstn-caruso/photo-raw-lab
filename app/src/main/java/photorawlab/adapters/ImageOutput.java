package photorawlab.adapters;

import photorawlab.domain.edit.LinearImage;

import java.awt.color.*;
import java.awt.image.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.function.BooleanSupplier;

import javax.imageio.*;
import javax.imageio.plugins.tiff.*;

public final class ImageOutput {
    public enum Format {
        JPEG,
        TIFF
    }

    public static BufferedImage display(LinearImage input) throws IOException {
        short[] rgb = LinearRawDecoder.srgb(input);
        BufferedImage image =
                new BufferedImage(input.width(), input.height(), BufferedImage.TYPE_INT_RGB);
        int[] pixels = new int[input.width() * input.height()];
        for (int i = 0; i < pixels.length; i++)
            pixels[i] =
                    ((Short.toUnsignedInt(rgb[i * 3]) + 128) / 257 << 16)
                            | ((Short.toUnsignedInt(rgb[i * 3 + 1]) + 128) / 257 << 8)
                            | (Short.toUnsignedInt(rgb[i * 3 + 2]) + 128) / 257;
        image.setRGB(0, 0, input.width(), input.height(), pixels, 0, input.width());
        return image;
    }

    public Path write(
            LinearImage input,
            Path directory,
            String stem,
            Format format,
            BooleanSupplier cancelled)
            throws IOException {
        Files.createDirectories(directory);
        Path temporary = Files.createTempFile(directory, ".photoraw-export-", ".tmp");
        try {
            if (cancelled.getAsBoolean()) throw new IOException("Exportación cancelada");
            BufferedImage image;
            if (format == Format.JPEG) image = display(input);
            else {
                var color =
                        new ComponentColorModel(
                                ColorSpace.getInstance(ColorSpace.CS_sRGB),
                                new int[] {16, 16, 16},
                                false,
                                false,
                                java.awt.Transparency.OPAQUE,
                                DataBuffer.TYPE_USHORT);
                var raster =
                        Raster.createInterleavedRaster(
                                new DataBufferUShort(
                                        LinearRawDecoder.srgb(input), input.pixels().length),
                                input.width(),
                                input.height(),
                                input.width() * 3,
                                3,
                                new int[] {0, 1, 2},
                                null);
                image = new BufferedImage(color, raster, false, null);
            }
            var writers =
                    ImageIO.getImageWritersByFormatName(format == Format.JPEG ? "JPEG" : "TIFF");
            if (!writers.hasNext()) throw new IOException("No hay encoder " + format);
            var writer = writers.next();
            try (var out = ImageIO.createImageOutputStream(temporary.toFile())) {
                writer.setOutput(out);
                var param = writer.getDefaultWriteParam();
                if (format == Format.JPEG) {
                    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    param.setCompressionQuality(.95f);
                }
                var metadata =
                        writer.getDefaultImageMetadata(
                                ImageTypeSpecifier.createFromRenderedImage(image), param);
                if (format == Format.TIFF) {
                    var tags = TIFFDirectory.createFromMetadata(metadata);
                    byte[] icc = ICC_Profile.getInstance(ColorSpace.CS_sRGB).getData();
                    tags.addTIFFField(
                            new TIFFField(
                                    BaselineTIFFTagSet.getInstance()
                                            .getTag(BaselineTIFFTagSet.TAG_ICC_PROFILE),
                                    TIFFTag.TIFF_UNDEFINED,
                                    icc.length,
                                    icc));
                    metadata = tags.getAsMetadata();
                }
                if (format == Format.JPEG) {
                    String nativeFormat = "javax_imageio_jpeg_image_1.0";
                    var tree =
                            (javax.imageio.metadata.IIOMetadataNode)
                                    metadata.getAsTree(nativeFormat);
                    var jfif =
                            (javax.imageio.metadata.IIOMetadataNode)
                                    tree.getElementsByTagName("app0JFIF").item(0);
                    if (jfif != null) {
                        var icc = new javax.imageio.metadata.IIOMetadataNode("app2ICC");
                        icc.setUserObject(ICC_Profile.getInstance(ColorSpace.CS_sRGB));
                        jfif.appendChild(icc);
                        metadata.setFromTree(nativeFormat, tree);
                    }
                }
                writer.write(null, new IIOImage(image, null, metadata), param);
                out.flush();
            } finally {
                writer.dispose();
            }
            try (var file =
                    java.nio.channels.FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                file.force(true);
            }
            String extension = format == Format.JPEG ? ".jpg" : ".tif";
            for (int n = 0; ; n++) {
                if (cancelled.getAsBoolean()) throw new IOException("Exportación cancelada");
                Path target = directory.resolve(stem + (n == 0 ? "" : "-" + n) + extension);
                try {
                    Files.createLink(target, temporary);
                    return target;
                } catch (FileAlreadyExistsException collision) {
                    /* try a free name */
                }
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}

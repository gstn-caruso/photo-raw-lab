package photorawlab.app;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

final class JpegOrientation {
    private JpegOrientation() { }

    static int flip(byte[] jpeg, int cameraFlip) {
        ByteBuffer bytes = ByteBuffer.wrap(jpeg);
        if (jpeg.length < 2 || Short.toUnsignedInt(bytes.getShort(0)) != 0xffd8) {
            return cameraFlip;
        }
        for (int offset = 2; offset + 4 <= jpeg.length;) {
            int marker = Short.toUnsignedInt(bytes.getShort(offset));
            if (marker == 0xffda || marker == 0xffd9 || (marker & 0xff00) != 0xff00) {
                return cameraFlip;
            }
            int length = Short.toUnsignedInt(bytes.getShort(offset + 2));
            if (length < 2 || offset + 2L + length > jpeg.length) {
                return cameraFlip;
            }
            if (marker == 0xffe1 && length >= 16 && bytes.getInt(offset + 4) == 0x45786966
                    && bytes.getShort(offset + 8) == 0) {
                return exifFlip(ByteBuffer.wrap(jpeg, offset + 10, length - 8).slice(), cameraFlip);
            }
            offset += 2 + length;
        }
        return cameraFlip;
    }

    private static int exifFlip(ByteBuffer tiff, int cameraFlip) {
        int order = Short.toUnsignedInt(tiff.getShort(0));
        if (order != 0x4949 && order != 0x4d4d) {
            return cameraFlip;
        }
        tiff.order(order == 0x4949 ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
        long directory = Integer.toUnsignedLong(tiff.getInt(4));
        if (tiff.getShort(2) != 42 || directory > tiff.limit() - 2) {
            return cameraFlip;
        }
        int count = Short.toUnsignedInt(tiff.getShort((int) directory));
        for (int index = 0; index < count; index++) {
            long entry = directory + 2 + 12L * index;
            if (entry + 12 > tiff.limit()) {
                return cameraFlip;
            }
            int offset = (int) entry;
            if (Short.toUnsignedInt(tiff.getShort(offset)) == 0x0112 && tiff.getShort(offset + 2) == 3
                    && tiff.getInt(offset + 4) == 1) {
                int orientation = Short.toUnsignedInt(tiff.getShort(offset + 8));
                return switch (orientation) {
                    case 1 -> 0;
                    case 2 -> 1;
                    case 3 -> 3;
                    case 4 -> 2;
                    case 5 -> 4;
                    case 6 -> 6;
                    case 7 -> 7;
                    case 8 -> 5;
                    default -> cameraFlip;
                };
            }
        }
        return cameraFlip;
    }
}

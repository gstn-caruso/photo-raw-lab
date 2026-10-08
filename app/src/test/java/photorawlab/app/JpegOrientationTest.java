package photorawlab.app;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JpegOrientationTest {
    @Test
    void prefersJpegExifOrientationToCameraOrientationInBothByteOrders() {
        int[] flips = {0, 0, 1, 3, 2, 4, 6, 7, 5};
        for (ByteOrder order : new ByteOrder[] {ByteOrder.BIG_ENDIAN, ByteOrder.LITTLE_ENDIAN}) {
            for (int orientation = 1; orientation <= 8; orientation++) {
                assertEquals(flips[orientation], JpegOrientation.flip(jpeg(order, orientation), 3));
            }
        }
    }

    @Test
    void usesCameraOrientationWhenExifIsAbsentInvalidOrTruncated() {
        assertEquals(6, JpegOrientation.flip(new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xd9}, 6));
        assertEquals(6, JpegOrientation.flip(jpeg(ByteOrder.BIG_ENDIAN, 9), 6));
        byte[] malformed = jpeg(ByteOrder.BIG_ENDIAN, 1);
        ByteBuffer.wrap(malformed).putInt(16, Integer.MAX_VALUE);
        assertEquals(6, JpegOrientation.flip(malformed, 6));
        for (int size = 0; size < malformed.length; size++) {
            assertEquals(6, JpegOrientation.flip(java.util.Arrays.copyOf(malformed, size), 6));
        }
    }

    static byte[] jpeg(ByteOrder order, int orientation) {
        var bytes = ByteBuffer.allocate(40);
        bytes.putShort((short) 0xffd8).putShort((short) 0xffe1).putShort((short) 34);
        bytes.put(new byte[] {'E', 'x', 'i', 'f', 0, 0});
        bytes.order(order).putShort((short) (order == ByteOrder.BIG_ENDIAN ? 0x4d4d : 0x4949));
        bytes.putShort((short) 42).putInt(8).putShort((short) 1);
        bytes.putShort((short) 0x0112).putShort((short) 3).putInt(1).putShort((short) orientation).putShort((short) 0).putInt(0);
        bytes.order(ByteOrder.BIG_ENDIAN).putShort((short) 0xffd9);
        return bytes.array();
    }
}

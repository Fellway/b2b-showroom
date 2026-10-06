package pl.wizytowka.catalog.adapter.out.image;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pl.wizytowka.catalog.application.port.out.ImageInspector.InspectedImage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class MagicBytesImageInspectorTest {

    private final MagicBytesImageInspector inspector = new MagicBytesImageInspector();

    @Test
    void detectsJpegWithDimensions() throws IOException {
        assertThat(inspector.inspect(image("jpg", 40, 30)))
                .contains(new InspectedImage("image/jpeg", "jpg", 40, 30));
    }

    @Test
    void detectsPngWithDimensions() throws IOException {
        assertThat(inspector.inspect(image("png", 7, 9)))
                .contains(new InspectedImage("image/png", "png", 7, 9));
    }

    @Test
    void readsWebpLossyDimensions() {
        byte[] webp = riff("VP8 ", 30);
        webp[23] = (byte) 0x9D;
        webp[24] = 0x01;
        webp[25] = 0x2A;
        putLe16(webp, 26, 1600);
        putLe16(webp, 28, 2000);

        assertThat(inspector.inspect(webp)).contains(new InspectedImage("image/webp", "webp", 1600, 2000));
    }

    @Test
    void readsWebpLosslessDimensions() {
        byte[] webp = riff("VP8L", 30);
        webp[20] = 0x2F;
        int bits = (800 - 1) | ((600 - 1) << 14);
        putLe16(webp, 21, bits & 0xFFFF);
        putLe16(webp, 23, bits >>> 16);

        assertThat(inspector.inspect(webp)).contains(new InspectedImage("image/webp", "webp", 800, 600));
    }

    @Test
    void readsWebpExtendedDimensions() {
        byte[] webp = riff("VP8X", 30);
        putLe24(webp, 24, 3000 - 1);
        putLe24(webp, 27, 4000 - 1);

        assertThat(inspector.inspect(webp)).contains(new InspectedImage("image/webp", "webp", 3000, 4000));
    }

    @Test
    void rejectsWebpExceedingMaximumDimension() {
        byte[] webp = riff("VP8X", 30);
        putLe24(webp, 24, MagicBytesImageInspector.MAX_DIMENSION);
        putLe24(webp, 27, 99);

        assertThat(inspector.inspect(webp)).isEmpty();
    }

    @Test
    void rejectsWebpWithBrokenFrameHeader() {
        assertThat(inspector.inspect(riff("VP8 ", 30))).isEmpty();
        assertThat(inspector.inspect(riff("ABCD", 30))).isEmpty();
    }

    @Test
    void detectsAvifWithoutDimensions() {
        byte[] avif = new byte[32];
        System.arraycopy(ascii("ftypavif"), 0, avif, 4, 8);

        assertThat(inspector.inspect(avif)).contains(new InspectedImage("image/avif", "avif", null, null));
    }

    @Test
    void rejectsJpegSignatureWithCorruptedBody() {
        byte[] fake = new byte[64];
        fake[0] = (byte) 0xFF;
        fake[1] = (byte) 0xD8;
        fake[2] = (byte) 0xFF;

        assertThat(inspector.inspect(fake)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "<html><script>alert(1)</script></html>",
            "<?xml version=\"1.0\"?><svg xmlns=\"http://www.w3.org/2000/svg\"/>",
            "%PDF-1.7\n%âãÏÓ\n1 0 obj",
            "GIF89a................",
            "short"
    })
    void rejectsNonImageContent(String content) {
        assertThat(inspector.inspect(content.getBytes(StandardCharsets.UTF_8))).isEmpty();
    }

    @Test
    void rejectsNullAndEmpty() {
        assertThat(inspector.inspect(null)).isEmpty();
        assertThat(inspector.inspect(new byte[0])).isEmpty();
    }

    private static byte[] image(String format, int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertThat(ImageIO.write(image, format, out)).isTrue();
        return out.toByteArray();
    }

    private static byte[] riff(String chunk, int length) {
        byte[] bytes = new byte[length];
        System.arraycopy(ascii("RIFF"), 0, bytes, 0, 4);
        System.arraycopy(ascii("WEBP"), 0, bytes, 8, 4);
        System.arraycopy(ascii(chunk), 0, bytes, 12, 4);
        return bytes;
    }

    private static byte[] ascii(String value) {
        return value.getBytes(StandardCharsets.US_ASCII);
    }

    private static void putLe16(byte[] b, int offset, int value) {
        b[offset] = (byte) value;
        b[offset + 1] = (byte) (value >>> 8);
    }

    private static void putLe24(byte[] b, int offset, int value) {
        putLe16(b, offset, value);
        b[offset + 2] = (byte) (value >>> 16);
    }
}

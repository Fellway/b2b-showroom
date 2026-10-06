package pl.wizytowka.catalog.adapter.out.image;

import org.springframework.stereotype.Component;
import pl.wizytowka.catalog.application.port.out.ImageInspector;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;

/**
 * Rozpoznaje JPEG, PNG, WebP i AVIF wyłącznie po sygnaturze pliku. Wymiary czyta z nagłówków
 * (ImageIO dla JPEG/PNG bez dekodowania bitmapy, ręczne parsowanie chunków VP8/VP8L/VP8X dla WebP),
 * więc złośliwie duże obrazy ("decompression bomb") nie są rozpakowywane do pamięci.
 */
@Component
class MagicBytesImageInspector implements ImageInspector {

    /** Obrazy o większym boku traktujemy jako niepoprawne — katalog nie potrzebuje takich rozdzielczości. */
    static final int MAX_DIMENSION = 20_000;

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

    @Override
    public Optional<InspectedImage> inspect(byte[] content) {
        if (content == null || content.length < 12) {
            return Optional.empty();
        }
        if (startsWith(content, JPEG)) {
            return withImageIoDimensions(content, "image/jpeg", "jpg");
        }
        if (startsWith(content, PNG)) {
            return withImageIoDimensions(content, "image/png", "png");
        }
        if (ascii(content, 0, "RIFF") && ascii(content, 8, "WEBP")) {
            return webp(content);
        }
        if (ascii(content, 4, "ftyp") && (ascii(content, 8, "avif") || ascii(content, 8, "avis"))) {
            return Optional.of(new InspectedImage("image/avif", "avif", null, null));
        }
        return Optional.empty();
    }

    private static Optional<InspectedImage> withImageIoDimensions(byte[] content, String contentType, String extension) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            if (input == null) {
                return Optional.empty();
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                return Optional.empty();
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                return sized(contentType, extension, reader.getWidth(0), reader.getHeight(0));
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException e) {
            // sygnatura się zgadza, ale nagłówek jest uszkodzony — nie przyjmujemy pliku
            return Optional.empty();
        }
    }

    private static Optional<InspectedImage> webp(byte[] content) {
        if (content.length < 30) {
            return Optional.empty();
        }
        int width;
        int height;
        if (ascii(content, 12, "VP8 ")) {
            // ramka kluczowa VP8: 3 bajty tagu, kod startu 9D 01 2A, potem 14-bitowe wymiary LE
            if ((content[23] & 0xFF) != 0x9D || (content[24] & 0xFF) != 0x01 || (content[25] & 0xFF) != 0x2A) {
                return Optional.empty();
            }
            width = le16(content, 26) & 0x3FFF;
            height = le16(content, 28) & 0x3FFF;
        } else if (ascii(content, 12, "VP8L")) {
            if ((content[20] & 0xFF) != 0x2F) {
                return Optional.empty();
            }
            int bits = le32(content, 21);
            width = (bits & 0x3FFF) + 1;
            height = ((bits >>> 14) & 0x3FFF) + 1;
        } else if (ascii(content, 12, "VP8X")) {
            width = le24(content, 24) + 1;
            height = le24(content, 27) + 1;
        } else {
            return Optional.empty();
        }
        return sized("image/webp", "webp", width, height);
    }

    private static Optional<InspectedImage> sized(String contentType, String extension, int width, int height) {
        if (width <= 0 || height <= 0 || width > MAX_DIMENSION || height > MAX_DIMENSION) {
            return Optional.empty();
        }
        return Optional.of(new InspectedImage(contentType, extension, width, height));
    }

    private static boolean startsWith(byte[] content, byte[] signature) {
        return content.length >= signature.length
                && Arrays.equals(content, 0, signature.length, signature, 0, signature.length);
    }

    private static boolean ascii(byte[] content, int offset, String expected) {
        byte[] bytes = expected.getBytes(StandardCharsets.US_ASCII);
        return content.length >= offset + bytes.length
                && Arrays.equals(content, offset, offset + bytes.length, bytes, 0, bytes.length);
    }

    private static int le16(byte[] b, int offset) {
        return (b[offset] & 0xFF) | (b[offset + 1] & 0xFF) << 8;
    }

    private static int le24(byte[] b, int offset) {
        return le16(b, offset) | (b[offset + 2] & 0xFF) << 16;
    }

    private static int le32(byte[] b, int offset) {
        return le24(b, offset) | (b[offset + 3] & 0xFF) << 24;
    }
}

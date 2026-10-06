package pl.wizytowka.linesheet.adapter.out.pdf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.wizytowka.shared.application.port.out.FileStorage;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Iterator;
import java.util.Optional;

/**
 * Miniatury zdjęć produktów osadzane w PDF jako {@code data:} URI (JPEG). Obraz jest dekodowany z podpróbkowaniem,
 * więc nawet duże zdjęcie nigdy nie trafia do pamięci w pełnej rozdzielczości. Każdy problem (brak pliku, AVIF,
 * uszkodzony obraz) daje pusty wynik — szablon pokazuje wtedy neutralny placeholder zamiast przerywać generowanie.
 */
final class ProductThumbnails {

    /** Wymiar miniatury w pikselach: ok. 2x rozmiar wyświetlania (14 x 17,5 mm) dla ostrego wydruku. */
    static final int WIDTH = 120;
    static final int HEIGHT = 150;

    private static final Logger log = LoggerFactory.getLogger(ProductThumbnails.class);
    private static final int MAX_SOURCE_DIMENSION = 20_000;
    private static final long MAX_SOURCE_BYTES = 15L * 1024 * 1024;
    private static final float JPEG_QUALITY = 0.85f;

    static {
        // wtyczki ImageIO (WebP z TwelveMonkeys) muszą być widoczne także przy uruchomieniu z fat jara
        ImageIO.scanForPlugins();
    }

    private final FileStorage fileStorage;

    ProductThumbnails(FileStorage fileStorage) {
        this.fileStorage = fileStorage;
    }

    Optional<String> dataUri(String storageKey) {
        if (storageKey == null) {
            return Optional.empty();
        }
        try {
            Optional<FileStorage.StoredFile> stored = fileStorage.open(storageKey);
            if (stored.isEmpty()) {
                return Optional.empty();
            }
            byte[] source;
            try (FileStorage.StoredFile file = stored.get(); InputStream content = file.content()) {
                source = content.readNBytes((int) MAX_SOURCE_BYTES + 1);
            }
            if (source.length > MAX_SOURCE_BYTES) {
                return Optional.empty();
            }
            return decode(source).map(ProductThumbnails::fitOnCanvas).map(ProductThumbnails::toJpegDataUri);
        } catch (IOException | RuntimeException e) {
            log.warn("Cannot create PDF thumbnail for {}: {}", storageKey, e.toString());
            return Optional.empty();
        }
    }

    private static Optional<BufferedImage> decode(byte[] source) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(source))) {
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
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > MAX_SOURCE_DIMENSION || height > MAX_SOURCE_DIMENSION) {
                    return Optional.empty();
                }
                int subsampling = Math.max(1, Math.min(width / WIDTH, height / HEIGHT) / 2);
                ImageReadParam param = reader.getDefaultReadParam();
                param.setSourceSubsampling(subsampling, subsampling, 0, 0);
                return Optional.ofNullable(reader.read(0, param));
            } finally {
                reader.dispose();
            }
        }
    }

    /** Dopasowanie "contain" na białym tle — zdjęcie produktu nigdy nie jest przycinane. */
    private static BufferedImage fitOnCanvas(BufferedImage source) {
        double scale = Math.min((double) WIDTH / source.getWidth(), (double) HEIGHT / source.getHeight());
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        BufferedImage canvas = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = canvas.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, WIDTH, HEIGHT);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, (WIDTH - width) / 2, (HEIGHT - height) / 2, width, height, null);
        } finally {
            graphics.dispose();
        }
        return canvas;
    }

    private static String toJpegDataUri(BufferedImage image) {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (MemoryCacheImageOutputStream output = new MemoryCacheImageOutputStream(bytes)) {
            writer.setOutput(output);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_QUALITY);
            writer.write(null, new IIOImage(image, null, null), param);
        } catch (IOException e) {
            throw new IllegalStateException("JPEG encoding failed", e);
        } finally {
            writer.dispose();
        }
        return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(bytes.toByteArray());
    }
}

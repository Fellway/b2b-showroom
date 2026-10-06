package pl.wizytowka.linesheet.adapter.out.pdf;

import com.sun.net.httpserver.HttpServer;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.wizytowka.contact.application.view.SalesRepresentativeContact;
import pl.wizytowka.linesheet.application.view.LineSheetDocument;
import pl.wizytowka.linesheet.domain.LineSheetItem;
import pl.wizytowka.linesheet.domain.Recipient;
import pl.wizytowka.shared.application.port.out.FileStorage;
import pl.wizytowka.shared.domain.CatalogLanguage;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlLineSheetPdfRendererTest {

    private static final Logger log = LoggerFactory.getLogger(HtmlLineSheetPdfRendererTest.class);
    private static final Instant CREATED = Instant.parse("2026-10-06T08:30:00Z");
    private static final Path SAMPLES = Path.of("target", "pdf-samples");
    /** Najmniejszy poprawny WebP (bezstratny, 1x1) — TwelveMonkeys potrafi WebP tylko czytać. */
    private static final byte[] WEBP_1X1 = Base64.getDecoder().decode("UklGRhoAAABXRUJQVlA4TA0AAAAvAAAAEAcQERGIiP4HAA==");

    private final InMemoryFileStorage storage = new InMemoryFileStorage();
    private final BrandProperties brand = new BrandProperties("Atelier Północ", "Atelier Północ sp. z o.o.",
            List.of("ul. Lniana 7", "00-001 Warszawa"), "atelier.example", "b2b@atelier.example", "+48 22 000 00 00",
            "#7A4E2D", null, ZoneId.of("Europe/Warsaw"));
    private final HtmlLineSheetPdfRenderer renderer = new HtmlLineSheetPdfRenderer(storage, brand);

    @Test
    void rendersPolishDocumentWithAllSectionsAndTotals() throws IOException {
        storage.put("products/a/navy.jpg", jpeg(1600, 2000, new Color(30, 40, 70)));
        storage.put("products/b/white.png", png(800, 1000, new Color(235, 230, 220)));
        storage.put("products/c/green.webp", WEBP_1X1);
        storage.put("products/d/red.avif", avifLike());
        List<LineSheetItem> items = new ArrayList<>();
        String[][] products = {
                {"LS-001", "Koszula lniana z kołnierzykiem", "Granatowy", "products/a/navy.jpg"},
                {"TR-100", "Spodnie chino żakardowe", "Écru", "products/b/white.png"},
                {"KN-210", "Sweter z wełny merynosa", "Butelkowa zieleń", "products/c/green.webp"},
                {"DR-330", "Sukienka midi z wiskozy", "Czerwień", "products/d/red.avif"},
                {"JK-410", "Kurtka pikowana", "Czarny", "products/missing.jpg"},
        };
        String[] sizes = {"S", "M", "L", "XL", "XXL"};
        int position = 1;
        for (String[] product : products) {
            for (String size : sizes) {
                Integer quantity = position % 4 == 0 ? null : position;
                items.add(item(position++, product[0], product[1], product[2], size, product[3], "149.00", "349.99", quantity));
            }
        }
        LineSheetDocument document = document(CatalogLanguage.PL, items,
                new Recipient("Butik Żółć s.c.", "Jadwiga Łęcka", "zakupy@butik.example", "+48 600 100 200", "PL5213000000"),
                "Prosimy o informację o terminach dostaw na sezon wiosenny.");

        byte[] pdf = renderer.render(document);
        save("line-sheet-pl.pdf", pdf);

        try (PDDocument loaded = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(loaded);
            assertThat(squash(text)).contains("ZESTAWIENIEOFERTOWE", "KLIENT", "OPIEKUNHANDLOWY");
            assertThat(text).contains("LS-2026-000042", "Koszula lniana z kołnierzykiem",
                    "Butik Żółć s.c.", "Jadwiga Łęcka", "PL5213000000", "Anna Nowak", "anna@atelier.example",
                    "terminach dostaw", "5901234123457", "LS001-S", "Pozycje", "Łączna ilość", "Wartość hurtowa netto",
                    "dotyczy pozycji z podaną ilością", "Ceny hurtowe są cenami netto", "Atelier Północ sp. z o.o.",
                    "Strona 1 z");
            assertThat(text).contains("6 października 2026");
            assertThat(normalizeSpaces(text)).contains("149,00 zł", "349,99 zł");
            assertThat(loaded.getDocumentInformation().getTitle()).isEqualTo("Zestawienie ofertowe LS-2026-000042");
            assertThat(loaded.getDocumentInformation().getAuthor()).isEqualTo("Atelier Północ");
            // JPEG, PNG i WebP mają miniatury (identyczne obrazy są współdzielone); AVIF i brakujący plik -> placeholder
            assertThat(distinctImages(loaded)).isEqualTo(3);
        }
    }

    @Test
    void rendersEveryLanguageWithLocalizedLabelsAndGlyphs() throws IOException {
        Map<CatalogLanguage, List<String>> expected = Map.of(
                CatalogLanguage.EN, List.of("LINESHEET", "Wholesalevalue(net)", "SALESREPRESENTATIVE", "Page1of"),
                CatalogLanguage.CS, List.of("NABÍDKOVÝPŘEHLED", "VelkoobchodníhodnotabezDPH", "OBCHODNÍZÁSTUPCE", "Strana1z"),
                CatalogLanguage.UK, List.of("КОМЕРЦІЙНАПРОПОЗИЦІЯ", "ОптовавартістьбезПДВ", "МЕНЕДЖЕРЗПРОДАЖУ", "Сторінка1з"),
                CatalogLanguage.PL, List.of("ZESTAWIENIEOFERTOWE", "Wartośćhurtowanetto", "OPIEKUNHANDLOWY", "Strona1z"));
        for (var entry : expected.entrySet()) {
            String name = switch (entry.getKey()) {
                case UK -> "Лляна сорочка «Ґанок» — їжак";
                case CS -> "Lněná košile – řůžová ďábelská";
                case EN -> "Linen shirt";
                case PL -> "Koszula źdźbło";
            };
            LineSheetDocument document = document(entry.getKey(),
                    List.of(item(1, "LS-001", name, "Navy", "M", null, "149.00", "349.99", 3)), null, null);

            byte[] pdf = renderer.render(document);
            if (entry.getKey() == CatalogLanguage.UK) {
                save("line-sheet-uk.pdf", pdf);
            }

            try (PDDocument loaded = Loader.loadPDF(pdf)) {
                String text = new PDFTextStripper().getText(loaded);
                assertThat(squash(text)).as(entry.getKey().code()).contains(entry.getValue());
                assertThat(text).contains(name);
            }
        }
    }

    @Test
    void longDocumentPaginatesWithRepeatedHeader() throws IOException {
        storage.put("products/shared.jpg", jpeg(1200, 1500, new Color(120, 90, 60)));
        List<LineSheetItem> items = IntStream.rangeClosed(1, 200)
                .mapToObj(i -> item(i, "ST-" + i, "Produkt numer " + i, "Kolor " + (i % 7), "M", "products/shared.jpg",
                        "99.90", "229.00", 2))
                .toList();
        LineSheetDocument document = document(CatalogLanguage.PL, items, null, null);

        long started = System.nanoTime();
        byte[] pdf = renderer.render(document);
        long millis = (System.nanoTime() - started) / 1_000_000;
        log.info("200-item line sheet rendered in {} ms, {} KB", millis, pdf.length / 1024);

        try (PDDocument loaded = Loader.loadPDF(pdf)) {
            assertThat(loaded.getNumberOfPages()).isGreaterThan(5);
            PDFTextStripper stripper = new PDFTextStripper();
            for (int page = 1; page <= loaded.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String pageText = stripper.getText(loaded);
                if (pageText.contains("Produkt numer")) {
                    assertThat(squash(pageText)).as("page %d header", page).contains("EAN", "ROZMIAR");
                }
                assertThat(pageText).contains("Strona " + page + " z " + loaded.getNumberOfPages());
            }
            String all = new PDFTextStripper().getText(loaded);
            assertThat(all).contains("Produkt numer 1", "Produkt numer 200");
            assertThat(normalizeSpaces(all)).contains("39 960,00 zł");
        }
        assertThat(millis).isLessThan(20_000);
        assertThat(pdf.length).as("one image embedded once per page, not per row").isLessThan(3 * 1024 * 1024);
    }

    @Test
    void userInputIsEscapedAndNoExternalResourceIsFetched() throws IOException {
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requests.incrementAndGet();
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        server.start();
        try {
            String evilUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/x";
            String company = "<img src=\"" + evilUrl + "\">";
            String message = "<link rel=\"stylesheet\" href=\"" + evilUrl + "\"/><b>bold</b> & co";
            LineSheetDocument document = document(CatalogLanguage.EN,
                    List.of(item(1, "LS-001", "<script>alert(1)</script>", "Navy", "M", null, "10.00", "20.00", null)),
                    new Recipient(company, null, null, null, null), message);

            byte[] pdf = renderer.render(document);

            try (PDDocument loaded = Loader.loadPDF(pdf)) {
                String text = new PDFTextStripper().getText(loaded);
                assertThat(text).contains("<img src=", "<script>alert(1)</script>", "<b>bold</b> & co");
                assertThat(distinctImages(loaded)).isZero();
            }
            assertThat(requests).hasValue(0);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void documentWithoutOptionalBlocksAndQuantitiesStillRenders() throws IOException {
        LineSheetDocument document = new LineSheetDocument(UUID.randomUUID(), "LS-2026-000043", CatalogLanguage.PL, "PLN",
                null, null, CREATED, List.of(item(1, "LS-001", "Koszula", "Biały", "M", null, "10.00", "20.00", null)), null);

        byte[] pdf = new HtmlLineSheetPdfRenderer(storage, BrandProperties.placeholder()).render(document);

        try (PDDocument loaded = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(loaded);
            assertThat(squash(text)).contains("BRANDNAME", "Pozycje").doesNotContain("Łącznailość", "KLIENT", "OPIEKUNHANDLOWY");
        }
    }

    // ------------------------------------------------------------------ dane testowe

    private static LineSheetDocument document(CatalogLanguage language, List<LineSheetItem> items, Recipient recipient,
                                              String message) {
        return new LineSheetDocument(UUID.randomUUID(), "LS-2026-000042", language, "PLN", recipient, message, CREATED,
                items, new SalesRepresentativeContact(UUID.randomUUID(), "Anna Nowak", "anna@atelier.example", "+48 600 000 000"));
    }

    private static LineSheetItem item(int position, String styleCode, String name, String color, String size,
                                      String imageKey, String wholesale, String retail, Integer quantity) {
        String ean = position == 1 ? "5901234123457" : String.format("%013d", 5_900_000_000_000L + position);
        return new LineSheetItem(position, UUID.randomUUID(), UUID.randomUUID(), styleCode, name, color, size,
                styleCode.replace("-", "") + "-" + size, ean, new BigDecimal(wholesale), new BigDecimal(retail),
                imageKey, quantity);
    }

    private static byte[] jpeg(int width, int height, Color color) throws IOException {
        return encode(image(width, height, color), "jpeg");
    }

    private static byte[] png(int width, int height, Color color) throws IOException {
        return encode(image(width, height, color), "png");
    }

    private static BufferedImage image(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(244, 242, 238));
        graphics.fillRect(0, 0, width, height);
        graphics.setColor(color);
        graphics.fillRoundRect(width / 5, height / 8, width * 3 / 5, height * 3 / 4, width / 10, width / 10);
        graphics.dispose();
        return image;
    }

    private static byte[] encode(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, format, out);
        return out.toByteArray();
    }

    /** Nagłówek kontenera AVIF bez dekodera w JDK — ma zostać zastąpiony placeholderem. */
    private static byte[] avifLike() {
        byte[] bytes = new byte[64];
        byte[] header = {0, 0, 0, 0x1c, 'f', 't', 'y', 'p', 'a', 'v', 'i', 'f'};
        System.arraycopy(header, 0, bytes, 0, header.length);
        return bytes;
    }

    /** Liczba różnych obrazów osadzonych w dokumencie (po skrócie surowego strumienia). */
    private static int distinctImages(PDDocument document) throws IOException {
        java.util.Set<String> digests = new java.util.HashSet<>();
        for (PDPage page : document.getPages()) {
            PDResources resources = page.getResources();
            for (COSName name : resources.getXObjectNames()) {
                if (resources.getXObject(name) instanceof PDImageXObject image) {
                    try (InputStream raw = image.getCOSObject().createRawInputStream()) {
                        digests.add(java.util.HexFormat.of().formatHex(sha256(raw.readAllBytes())));
                    }
                }
            }
        }
        return digests.size();
    }

    private static byte[] sha256(byte[] bytes) {
        try {
            return java.security.MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Nagłówki składane wersalikami z rozstrzeleniem są ekstrahowane litera po literze — porównujemy bez odstępów. */
    private static String squash(String text) {
        return text.replaceAll("\\s+", "");
    }

    private static String normalizeSpaces(String text) {
        return text.replace(' ', ' ').replace(' ', ' ');
    }

    private static void save(String name, byte[] pdf) throws IOException {
        Files.createDirectories(SAMPLES);
        Files.write(SAMPLES.resolve(name), pdf);
    }

    private static final class InMemoryFileStorage implements FileStorage {

        private final Map<String, byte[]> files = new HashMap<>();

        void put(String key, byte[] content) {
            files.put(key, content);
        }

        @Override
        public void store(String key, InputStream content, long sizeBytes, String contentType) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<StoredFile> open(String key) {
            return Optional.ofNullable(files.get(key))
                    .map(bytes -> new StoredFile(new ByteArrayInputStream(bytes), bytes.length, "application/octet-stream"));
        }

        @Override
        public void delete(String key) {
            files.remove(key);
        }
    }
}

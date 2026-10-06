package pl.wizytowka.linesheet.adapter.out.pdf;

import com.openhtmltopdf.extend.FSStream;
import com.openhtmltopdf.extend.FSStreamFactory;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import pl.wizytowka.contact.application.view.SalesRepresentativeContact;
import pl.wizytowka.linesheet.application.port.out.LineSheetDocumentRenderer;
import pl.wizytowka.linesheet.application.view.LineSheetDocument;
import pl.wizytowka.linesheet.domain.LineSheetItem;
import pl.wizytowka.linesheet.domain.Recipient;
import pl.wizytowka.shared.application.port.out.FileStorage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Zestawienie ofertowe jako PDF: szablon Thymeleaf (XHTML + CSS stronicowania) renderowany przez OpenHTMLtoPDF.
 * <p>
 * Bezpieczeństwo: dane klienta trafiają do szablonu wyłącznie przez {@code th:text} (escapowanie), a renderer
 * nie pobiera żadnych zasobów zewnętrznych — dozwolone są tylko {@code data:} URI (miniatury, logo) i fonty
 * wczytane z classpath przy starcie.
 */
@Component
class HtmlLineSheetPdfRenderer implements LineSheetDocumentRenderer {

    private static final Logger log = LoggerFactory.getLogger(HtmlLineSheetPdfRenderer.class);
    private static final String TEMPLATE = "line-sheet";
    private static final String FONT_FAMILY = "Noto Sans";
    private static final List<String> BLOCKED_PROTOCOLS = List.of("http", "https", "file", "jar", "ftp", "classpath");

    private final TemplateEngine templates;
    private final ProductThumbnails thumbnails;
    private final BrandProperties brand;
    private final String logoDataUri;
    private final byte[] regularFont;
    private final byte[] semiBoldFont;

    HtmlLineSheetPdfRenderer(FileStorage fileStorage, BrandProperties brand) {
        this.brand = brand;
        this.thumbnails = new ProductThumbnails(fileStorage);
        this.templates = templateEngine();
        this.regularFont = classpathBytes("pdf/fonts/NotoSans-Regular.ttf");
        this.semiBoldFont = classpathBytes("pdf/fonts/NotoSans-SemiBold.ttf");
        this.logoDataUri = brand.logo() == null ? null : logoDataUri(brand.logo());
    }

    @Override
    public byte[] render(LineSheetDocument document) {
        long started = System.nanoTime();
        Locale locale = document.language().toLocale();
        Context context = new Context(locale, Map.of("doc", model(document, locale)));
        String html = templates.process(TEMPLATE, context);
        byte[] pdf = toPdf(html);
        log.debug("Rendered line sheet {} ({} items, {} KB) in {} ms", document.referenceNumber(),
                document.items().size(), pdf.length / 1024, (System.nanoTime() - started) / 1_000_000);
        return pdf;
    }

    // ------------------------------------------------------------------ model

    private LineSheetPdfModel model(LineSheetDocument document, Locale locale) {
        NumberFormat money = NumberFormat.getCurrencyInstance(locale);
        money.setCurrency(Currency.getInstance(document.currency()));
        NumberFormat integer = NumberFormat.getIntegerInstance(locale);
        DateTimeFormatter date = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.SHORT)
                .withLocale(locale)
                .withZone(brand.timeZone());

        Map<String, Optional<String>> thumbnailCache = new HashMap<>();
        List<LineSheetPdfModel.Row> rows = document.items().stream()
                .map(item -> new LineSheetPdfModel.Row(
                        thumbnailCache.computeIfAbsent(Objects.requireNonNullElse(item.imageStorageKey(), ""),
                                key -> key.isEmpty() ? Optional.empty() : thumbnails.dataUri(key)).orElse(null),
                        item.productName(),
                        item.styleCode(),
                        item.colorName(),
                        item.sizeCode(),
                        item.ean(),
                        item.sku(),
                        item.quantity() == null ? "–" : integer.format(item.quantity()),
                        money.format(item.wholesalePrice()),
                        money.format(item.retailPrice())))
                .toList();

        return new LineSheetPdfModel(
                locale.toLanguageTag(),
                new LineSheetPdfModel.Brand(brand.name(), logoDataUri, brand.accentColor(), footerParts()),
                document.referenceNumber(),
                date.format(document.createdAt()),
                clientFields(document.recipient()),
                contact(document.salesRepresentative()),
                document.message(),
                rows,
                totals(document.items(), money, integer));
    }

    private static List<LineSheetPdfModel.Field> clientFields(Recipient recipient) {
        if (recipient == null) {
            return List.of();
        }
        List<LineSheetPdfModel.Field> fields = new ArrayList<>();
        addField(fields, "client.company", recipient.companyName());
        addField(fields, "client.contact", recipient.contactName());
        addField(fields, "client.email", recipient.email());
        addField(fields, "client.phone", recipient.phone());
        addField(fields, "client.taxId", recipient.taxId());
        return List.copyOf(fields);
    }

    private static void addField(List<LineSheetPdfModel.Field> fields, String labelKey, String value) {
        if (value != null && !value.isBlank()) {
            fields.add(new LineSheetPdfModel.Field(labelKey, value));
        }
    }

    private static LineSheetPdfModel.Contact contact(SalesRepresentativeContact representative) {
        return representative == null ? null
                : new LineSheetPdfModel.Contact(representative.fullName(), representative.email(), representative.phone());
    }

    private static LineSheetPdfModel.Totals totals(List<LineSheetItem> items, NumberFormat money, NumberFormat integer) {
        List<LineSheetItem> withQuantity = items.stream().filter(item -> item.quantity() != null).toList();
        if (withQuantity.isEmpty()) {
            return new LineSheetPdfModel.Totals(items.size(), null, null, false);
        }
        long quantity = withQuantity.stream().mapToLong(LineSheetItem::quantity).sum();
        BigDecimal value = withQuantity.stream().map(LineSheetItem::wholesaleTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new LineSheetPdfModel.Totals(items.size(), integer.format(quantity), money.format(value),
                withQuantity.size() < items.size());
    }

    private List<String> footerParts() {
        return Stream.concat(
                        Stream.of(brand.legalName() == null ? brand.name() : brand.legalName()),
                        Stream.concat(brand.addressLines().stream(), Stream.of(brand.website(), brand.email(), brand.phone())))
                .filter(part -> part != null && !part.isBlank())
                .toList();
    }

    // ------------------------------------------------------------------ renderowanie

    private byte[] toPdf(String html) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(64 * 1024);
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        builder.usePdfUaAccessbility(true);
        builder.withHtmlContent(html, null);
        builder.useFont(() -> new java.io.ByteArrayInputStream(regularFont), FONT_FAMILY, 400,
                BaseRendererBuilder.FontStyle.NORMAL, true);
        builder.useFont(() -> new java.io.ByteArrayInputStream(semiBoldFont), FONT_FAMILY, 600,
                BaseRendererBuilder.FontStyle.NORMAL, true);
        // tylko osadzone data: URI — żadnych żądań sieciowych ani odczytu plików wskazanych w dokumencie
        builder.useUriResolver((baseUri, uri) -> uri != null && uri.startsWith("data:") ? uri : null);
        FSStreamFactory denyAll = new DenyingStreamFactory();
        builder.useHttpStreamImplementation(denyAll);
        builder.useProtocolsStreamImplementation(denyAll, BLOCKED_PROTOCOLS.toArray(String[]::new));
        builder.toStream(out);
        try {
            builder.run();
        } catch (IOException e) {
            throw new UncheckedIOException("PDF rendering failed", e);
        }
        return out.toByteArray();
    }

    private static TemplateEngine templateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver(HtmlLineSheetPdfRenderer.class.getClassLoader());
        resolver.setPrefix("pdf/templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(true);
        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);
        engine.setMessageResolver(new PdfMessages());
        return engine;
    }

    private static byte[] classpathBytes(String location) {
        try (InputStream in = HtmlLineSheetPdfRenderer.class.getClassLoader().getResourceAsStream(location)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource: " + location);
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + location, e);
        }
    }

    private static String logoDataUri(String location) {
        String mediaType = location.toLowerCase(Locale.ROOT).endsWith(".png") ? "image/png" : "image/jpeg";
        return "data:" + mediaType + ";base64," + Base64.getEncoder().encodeToString(classpathBytes(location));
    }

    /** Fabryka strumieni odrzucająca każde żądanie zasobu — renderer pracuje wyłącznie na treści osadzonej. */
    private static final class DenyingStreamFactory implements FSStreamFactory {

        @Override
        public FSStream getUrl(String url) {
            log.warn("Blocked external resource request while rendering PDF: {}", url);
            return new FSStream() {
                @Override
                public InputStream getStream() {
                    return null;
                }

                @Override
                public java.io.Reader getReader() {
                    return null;
                }
            };
        }
    }
}

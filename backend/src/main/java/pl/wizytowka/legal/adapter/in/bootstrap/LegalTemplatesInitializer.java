package pl.wizytowka.legal.adapter.in.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import pl.wizytowka.legal.application.port.in.ManageLegalDocumentsUseCase;
import pl.wizytowka.legal.domain.LegalDocumentType;
import pl.wizytowka.legal.domain.LegalText;
import pl.wizytowka.shared.domain.CatalogLanguage;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

/**
 * Przy pustej bazie publikuje wzory regulaminu i polityki prywatności (wersja 1) z
 * {@code classpath:legal/templates/<rodzaj>.<język>.md} — showroom nigdy nie startuje bez dokumentów, do których
 * prowadzą zgody w rejestracji. Wzory zawierają pola do uzupełnienia i wymagają weryfikacji prawnej przed produkcją;
 * marka zastępuje je w panelu nową wersją. Wyłączenie: {@code showroom.legal.seed-templates=false}.
 *
 * <p>Format pliku: pierwsza linia {@code # Tytuł}, dalej treść (Markdown). {@code {brand}} = nazwa marki.</p>
 */
@Component
class LegalTemplatesInitializer {

    private static final Logger log = LoggerFactory.getLogger(LegalTemplatesInitializer.class);

    private final ManageLegalDocumentsUseCase documents;
    private final boolean enabled;
    private final String brand;

    LegalTemplatesInitializer(ManageLegalDocumentsUseCase documents,
                              @Value("${showroom.legal.seed-templates:true}") boolean enabled,
                              @Value("${showroom.brand.name:B2B Showroom}") String brand) {
        this.documents = documents;
        this.enabled = enabled;
        this.brand = brand;
    }

    @EventListener(ApplicationReadyEvent.class)
    void publishTemplates() {
        if (!enabled) {
            return;
        }
        for (LegalDocumentType type : LegalDocumentType.values()) {
            if (documents.publishTemplateIfEmpty(type, load(type, brand))) {
                log.warn("Published template {} (version 1) — it contains placeholders and requires legal review "
                        + "before production", type);
            }
        }
    }

    static Map<CatalogLanguage, LegalText> load(LegalDocumentType type, String brand) {
        Map<CatalogLanguage, LegalText> texts = new EnumMap<>(CatalogLanguage.class);
        for (CatalogLanguage language : CatalogLanguage.values()) {
            String path = "legal/templates/%s.%s.md".formatted(type.slug(), language.code());
            texts.put(language, parse(read(path).replace("{brand}", brand), path));
        }
        return texts;
    }

    private static LegalText parse(String content, String path) {
        String normalized = content.replace("\r\n", "\n").strip();
        int newline = normalized.indexOf('\n');
        if (!normalized.startsWith("# ") || newline < 0) {
            throw new IllegalStateException("Legal template " + path + " must start with '# Title'");
        }
        return new LegalText(normalized.substring(2, newline).strip(), normalized.substring(newline + 1).strip(), null);
    }

    private static String read(String path) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Missing legal template " + path, e);
        }
    }
}

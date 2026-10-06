package pl.wizytowka.linesheet.adapter.out.pdf;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.ZoneId;
import java.util.List;

/**
 * Dane marki drukowane w zestawieniu PDF. Do czasu przygotowania identyfikacji wizualnej (Etap 4)
 * wartości domyślne są neutralnymi placeholderami.
 *
 * @param name         nazwa marki (wordmark w nagłówku, autor dokumentu)
 * @param legalName    nazwa firmy w stopce (opcjonalna)
 * @param addressLines linie adresu w stopce
 * @param accentColor  kolor akcentu w formacie #RRGGBB
 * @param logo         opcjonalne logo PNG/JPEG z classpath (np. {@code pdf/brand/logo.png}); brak = wordmark z nazwy
 * @param timeZone     strefa czasowa daty wygenerowania drukowanej w dokumencie
 */
@Validated
@ConfigurationProperties("showroom.brand")
public record BrandProperties(@DefaultValue("Brand Name") @NotBlank String name,
                              String legalName,
                              @DefaultValue List<String> addressLines,
                              String website,
                              String email,
                              String phone,
                              @DefaultValue("#1F2A37") @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String accentColor,
                              @Pattern(regexp = "^[A-Za-z0-9_./-]+\\.(png|jpe?g)$") String logo,
                              @DefaultValue("Europe/Warsaw") @NotNull ZoneId timeZone) {

    public BrandProperties {
        // puste zmienne środowiskowe (np. SHOWROOM_BRAND_EMAIL=) traktujemy jak brak wartości
        legalName = blankToNull(legalName);
        website = blankToNull(website);
        email = blankToNull(email);
        phone = blankToNull(phone);
        logo = blankToNull(logo);
        addressLines = addressLines == null ? List.of()
                : addressLines.stream().map(BrandProperties::blankToNull).filter(java.util.Objects::nonNull).toList();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    /** Placeholder używany w testach i przed konfiguracją marki. */
    public static BrandProperties placeholder() {
        return new BrandProperties("Brand Name", null, List.of(), null, null, null, "#1F2A37", null,
                ZoneId.of("Europe/Warsaw"));
    }
}

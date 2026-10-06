package pl.wizytowka.catalog.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import pl.wizytowka.catalog.application.port.in.StatusTransition;
import pl.wizytowka.catalog.domain.CategoryId;
import pl.wizytowka.catalog.domain.ProductDetails;
import pl.wizytowka.catalog.domain.ProductTranslation;
import pl.wizytowka.shared.domain.InvalidValueException;
import pl.wizytowka.shared.domain.Pricing;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Żądania panelu administracyjnego dotyczące produktów. Walidacja reguł biznesowych pozostaje w domenie. */
public final class AdminProductRequests {

    private static final int MAX_LIST = 500;

    private AdminProductRequests() {
    }

    /**
     * Dane podstawowe produktu (POST i PUT). Przy PUT pole {@code version} jest wymagane
     * (wartość z ostatniego odczytu produktu) — chroni przed nadpisaniem cudzych zmian.
     */
    public record ProductRequest(@NotNull UUID categoryId,
                                 @NotBlank @Size(max = 160) String slug,
                                 @NotBlank @Size(max = 40) String styleCode,
                                 @NotNull BigDecimal wholesalePrice,
                                 @NotNull BigDecimal retailPrice,
                                 @Pattern(regexp = "^[A-Z]{3}$", message = "must be an ISO 4217 currency code") String currency,
                                 boolean featured,
                                 @PositiveOrZero int sortOrder,
                                 @NotEmpty @Size(max = 4) Map<String, @NotNull @Valid TranslationRequest> translations,
                                 @PositiveOrZero Long version) {

        public ProductDetails toDetails() {
            Pricing pricing = new Pricing(wholesalePrice, retailPrice, resolvedCurrency());
            return new ProductDetails(CategoryId.of(categoryId), slug, styleCode, pricing, featured, sortOrder,
                    LanguageMaps.toTranslations(translations, TranslationRequest::toDomain));
        }

        public long requiredVersion() {
            if (version == null) {
                throw new InvalidValueException("request.version-required", "Field 'version' is required for updates");
            }
            return version;
        }

        private Currency resolvedCurrency() {
            String code = currency == null ? "PLN" : currency;
            try {
                return Currency.getInstance(code);
            } catch (IllegalArgumentException e) {
                throw new InvalidValueException("pricing.currency", "Unknown currency: " + code);
            }
        }
    }

    public record TranslationRequest(@Size(max = 200) String name,
                                     @Size(max = 10_000) String description,
                                     @Size(max = 500) String materialComposition,
                                     @Size(max = 2_000) String careInstructions) {

        ProductTranslation toDomain() {
            return new ProductTranslation(name, description, materialComposition, careInstructions);
        }
    }

    public record StatusChangeRequest(@NotNull StatusTransition transition, @NotNull @PositiveOrZero Long version) {
    }

    public record AddColorRequest(@NotNull UUID colorId) {
    }

    public record ColorOrderRequest(@NotNull @Size(max = MAX_LIST) List<@NotNull UUID> colorIds) {
    }

    public record VariantRequest(@NotNull UUID colorId,
                                 @NotNull UUID sizeId,
                                 @NotBlank @Size(max = 64) String sku,
                                 @NotBlank @Size(max = 14) String ean) {
    }

    public record VariantUpdateRequest(@NotBlank @Size(max = 64) String sku,
                                       @NotBlank @Size(max = 14) String ean,
                                       @NotNull Boolean active) {
    }

    public record ImageOrderRequest(@NotNull @Size(max = MAX_LIST) List<@NotNull UUID> imageIds) {
    }

    /** @param colorId {@code null} odpina zdjęcie od koloru (zdjęcie ogólne) */
    public record ImageColorRequest(UUID colorId) {
    }
}

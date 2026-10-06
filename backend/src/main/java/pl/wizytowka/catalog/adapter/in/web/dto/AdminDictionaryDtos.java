package pl.wizytowka.catalog.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import pl.wizytowka.catalog.application.view.AdminCategoryView;
import pl.wizytowka.catalog.application.view.AdminColorView;
import pl.wizytowka.catalog.application.view.AdminSizeView;
import pl.wizytowka.catalog.domain.CategoryDetails;
import pl.wizytowka.catalog.domain.CategoryId;
import pl.wizytowka.catalog.domain.CategoryTranslation;
import pl.wizytowka.catalog.domain.ColorDetails;
import pl.wizytowka.shared.domain.InvalidValueException;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/** Żądania i odpowiedzi słowników katalogu: kategorie, kolory, rozmiary. Pole {@code version} wymagane przy PUT. */
public final class AdminDictionaryDtos {

    private AdminDictionaryDtos() {
    }

    static long requireVersion(Long version) {
        if (version == null) {
            throw new InvalidValueException("request.version-required", "Field 'version' is required for updates");
        }
        return version;
    }

    // ------------------------------------------------------------------ kategorie

    /** @param visible domyślnie {@code true} */
    public record CategoryRequest(UUID parentId,
                                  @NotBlank @Size(max = 120) String slug,
                                  @PositiveOrZero int sortOrder,
                                  Boolean visible,
                                  @NotEmpty @Size(max = 4) Map<String, @NotNull @Valid CategoryTranslationRequest> translations,
                                  @PositiveOrZero Long version) {

        public CategoryDetails toDetails() {
            return new CategoryDetails(parentId == null ? null : CategoryId.of(parentId), slug, sortOrder,
                    visible == null || visible,
                    LanguageMaps.toTranslations(translations, CategoryTranslationRequest::toDomain));
        }

        public long requiredVersion() {
            return requireVersion(version);
        }
    }

    public record CategoryTranslationRequest(@Size(max = 120) String name, @Size(max = 5_000) String description) {

        CategoryTranslation toDomain() {
            return new CategoryTranslation(name, description);
        }
    }

    public record CategoryResponse(UUID id, UUID parentId, String slug, int sortOrder, boolean visible,
                                   Map<String, CategoryTranslationResponse> translations, long productCount, long version) {

        public static CategoryResponse of(AdminCategoryView view) {
            return new CategoryResponse(view.id(), view.parentId(), view.slug(), view.sortOrder(), view.visible(),
                    LanguageMaps.toCodes(view.translations(), t -> new CategoryTranslationResponse(t.name(), t.description())),
                    view.productCount(), view.version());
        }
    }

    public record CategoryTranslationResponse(String name, String description) {
    }

    // ------------------------------------------------------------------ kolory

    /** @param names nazwa koloru per kod języka; "pl" wymagane */
    public record ColorRequest(@NotBlank @Size(max = 40) String code,
                               @Size(max = 7) String hex,
                               @PositiveOrZero int sortOrder,
                               @NotEmpty @Size(max = 4) Map<String, @NotBlank @Size(max = 80) String> names,
                               @PositiveOrZero Long version) {

        public ColorDetails toDetails() {
            return new ColorDetails(code, hex, sortOrder, LanguageMaps.toTranslations(names, Function.identity()));
        }

        public long requiredVersion() {
            return requireVersion(version);
        }
    }

    public record ColorResponse(UUID id, String code, String hex, int sortOrder, Map<String, String> names,
                                long productCount, long version) {

        public static ColorResponse of(AdminColorView view) {
            return new ColorResponse(view.id(), view.code(), view.hex(), view.sortOrder(),
                    LanguageMaps.toCodes(view.names(), Function.identity()), view.productCount(), view.version());
        }
    }

    // ------------------------------------------------------------------ rozmiary

    public record SizeRequest(@NotBlank @Size(max = 16) String code,
                              @PositiveOrZero int sortOrder,
                              @PositiveOrZero Long version) {

        public long requiredVersion() {
            return requireVersion(version);
        }
    }

    public record SizeResponse(UUID id, String code, int sortOrder, long variantCount, long version) {

        public static SizeResponse of(AdminSizeView view) {
            return new SizeResponse(view.id(), view.code(), view.sortOrder(), view.variantCount(), view.version());
        }
    }
}

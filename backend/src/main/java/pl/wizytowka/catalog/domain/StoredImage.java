package pl.wizytowka.catalog.domain;

import pl.wizytowka.shared.domain.InvalidValueException;

import java.util.Objects;
import java.util.Set;

/** Metadane pliku już zapisanego w storage. Klucz generuje serwer — nigdy nie pochodzi z nazwy pliku użytkownika. */
public record StoredImage(String storageKey, String contentType, Integer widthPx, Integer heightPx, Long sizeBytes) {

    public static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/avif");

    public StoredImage {
        Objects.requireNonNull(storageKey, "storageKey");
        if (storageKey.isBlank() || storageKey.length() > 512 || storageKey.startsWith("/")
                || storageKey.contains("..") || storageKey.contains("\\")) {
            throw new InvalidValueException("catalog.image.storage-key", "Unsafe storage key");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new InvalidValueException("catalog.image.content-type", "Unsupported image type: " + contentType);
        }
        if ((widthPx != null && widthPx <= 0) || (heightPx != null && heightPx <= 0)) {
            throw new InvalidValueException("catalog.image.dimensions", "Image dimensions must be positive");
        }
    }
}

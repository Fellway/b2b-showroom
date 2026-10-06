package pl.wizytowka.catalog.application.view;

import java.util.UUID;

/** @param colorId {@code null} dla zdjęcia ogólnego produktu */
public record ImageView(UUID id, UUID colorId, String storageKey, Integer widthPx, Integer heightPx) {
}

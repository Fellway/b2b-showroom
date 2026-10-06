package pl.wizytowka.catalog.application.port.out;

import java.util.Optional;

/**
 * Rozpoznaje format obrazu po zawartości (magic bytes), a nie po nagłówku Content-Type czy rozszerzeniu
 * przysłanym przez klienta, i odczytuje wymiary bez dekodowania całej bitmapy.
 */
public interface ImageInspector {

    Optional<InspectedImage> inspect(byte[] content);

    /** @param widthPx/heightPx {@code null}, gdy format nie pozwala tanio odczytać wymiarów */
    record InspectedImage(String contentType, String fileExtension, Integer widthPx, Integer heightPx) {
    }
}

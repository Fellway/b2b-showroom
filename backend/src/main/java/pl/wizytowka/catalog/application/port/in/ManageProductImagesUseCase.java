package pl.wizytowka.catalog.application.port.in;

import pl.wizytowka.catalog.domain.ColorId;
import pl.wizytowka.catalog.domain.ImageId;
import pl.wizytowka.catalog.domain.ProductId;

import java.util.List;

public interface ManageProductImagesUseCase {

    /** Maksymalny rozmiar pojedynczego zdjęcia. */
    long MAX_IMAGE_BYTES = 10L * 1024 * 1024;

    /**
     * Format jest rozpoznawany po zawartości pliku; deklarowany typ MIME klienta jest ignorowany.
     * @param colorId kolor, którego dotyczy zdjęcie, lub {@code null} dla zdjęcia ogólnego
     */
    ImageId upload(ProductId productId, ColorId colorId, byte[] content);

    void remove(ProductId productId, ImageId imageId);

    void reorder(ProductId productId, List<ImageId> ordered);

    void assignColor(ProductId productId, ImageId imageId, ColorId colorId);
}

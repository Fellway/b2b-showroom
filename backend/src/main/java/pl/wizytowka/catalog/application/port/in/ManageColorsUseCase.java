package pl.wizytowka.catalog.application.port.in;

import pl.wizytowka.catalog.domain.ColorDetails;
import pl.wizytowka.catalog.domain.ColorId;

public interface ManageColorsUseCase {

    ColorId create(ColorDetails details);

    void update(ColorId id, long expectedVersion, ColorDetails details);

    /** Dozwolone tylko dla koloru nieużywanego w żadnym produkcie. */
    void delete(ColorId id);
}

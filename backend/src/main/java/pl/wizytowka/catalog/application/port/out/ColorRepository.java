package pl.wizytowka.catalog.application.port.out;

import pl.wizytowka.catalog.domain.Color;
import pl.wizytowka.catalog.domain.ColorId;

import java.util.Optional;

public interface ColorRepository {

    Optional<Color> findById(ColorId id);

    /** @throws pl.wizytowka.shared.domain.ConcurrentModificationException przy niezgodności wersji */
    void save(Color color);

    void delete(Color color);

    boolean existsById(ColorId id);

    boolean existsByCode(String code, ColorId excluded);
}

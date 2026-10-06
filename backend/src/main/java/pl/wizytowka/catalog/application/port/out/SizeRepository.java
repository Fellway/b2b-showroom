package pl.wizytowka.catalog.application.port.out;

import pl.wizytowka.catalog.domain.Size;
import pl.wizytowka.catalog.domain.SizeId;

import java.util.Optional;

public interface SizeRepository {

    Optional<Size> findById(SizeId id);

    /** @throws pl.wizytowka.shared.domain.ConcurrentModificationException przy niezgodności wersji */
    void save(Size size);

    void delete(Size size);

    boolean existsById(SizeId id);

    boolean existsByCode(String code, SizeId excluded);
}

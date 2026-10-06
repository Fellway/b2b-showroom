package pl.wizytowka.catalog.application.port.in;

import pl.wizytowka.catalog.domain.SizeId;

public interface ManageSizesUseCase {

    SizeId create(String code, int sortOrder);

    void update(SizeId id, long expectedVersion, String code, int sortOrder);

    /** Dozwolone tylko dla rozmiaru nieużywanego w żadnym wariancie. */
    void delete(SizeId id);
}

package pl.wizytowka.catalog.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.wizytowka.catalog.application.port.in.ManageSizesUseCase;
import pl.wizytowka.catalog.application.port.out.ProductRepository;
import pl.wizytowka.catalog.application.port.out.SizeRepository;
import pl.wizytowka.catalog.domain.Size;
import pl.wizytowka.catalog.domain.SizeId;
import pl.wizytowka.shared.application.Versions;
import pl.wizytowka.shared.domain.ResourceConflictException;
import pl.wizytowka.shared.domain.ResourceNotFoundException;

@Service
@Transactional
class SizeManagementService implements ManageSizesUseCase {

    private final SizeRepository sizes;
    private final ProductRepository products;

    SizeManagementService(SizeRepository sizes, ProductRepository products) {
        this.sizes = sizes;
        this.products = products;
    }

    @Override
    public SizeId create(String code, int sortOrder) {
        Size size = Size.create(code, sortOrder);
        requireCodeFree(size.code(), null);
        sizes.save(size);
        return size.id();
    }

    @Override
    public void update(SizeId id, long expectedVersion, String code, int sortOrder) {
        Size size = load(id);
        Versions.require(expectedVersion, size.version());
        size.update(code, sortOrder);
        requireCodeFree(size.code(), id);
        sizes.save(size);
    }

    @Override
    public void delete(SizeId id) {
        Size size = load(id);
        if (products.existsBySize(id)) {
            throw new ResourceConflictException("catalog.size.in-use", "Size is used by product variants");
        }
        sizes.delete(size);
    }

    private void requireCodeFree(String code, SizeId self) {
        if (sizes.existsByCode(code, self)) {
            throw new ResourceConflictException("catalog.size.code-taken", "Size code is already used");
        }
    }

    private Size load(SizeId id) {
        return sizes.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("catalog.size.not-found", "Size not found"));
    }
}

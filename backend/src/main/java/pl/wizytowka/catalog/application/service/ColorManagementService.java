package pl.wizytowka.catalog.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.wizytowka.catalog.application.port.in.ManageColorsUseCase;
import pl.wizytowka.catalog.application.port.out.ColorRepository;
import pl.wizytowka.catalog.application.port.out.ProductRepository;
import pl.wizytowka.catalog.domain.Color;
import pl.wizytowka.catalog.domain.ColorDetails;
import pl.wizytowka.catalog.domain.ColorId;
import pl.wizytowka.shared.application.Versions;
import pl.wizytowka.shared.domain.ResourceConflictException;
import pl.wizytowka.shared.domain.ResourceNotFoundException;

@Service
@Transactional
class ColorManagementService implements ManageColorsUseCase {

    private final ColorRepository colors;
    private final ProductRepository products;

    ColorManagementService(ColorRepository colors, ProductRepository products) {
        this.colors = colors;
        this.products = products;
    }

    @Override
    public ColorId create(ColorDetails details) {
        requireCodeFree(details.code(), null);
        Color color = Color.create(details);
        colors.save(color);
        return color.id();
    }

    @Override
    public void update(ColorId id, long expectedVersion, ColorDetails details) {
        Color color = load(id);
        Versions.require(expectedVersion, color.version());
        requireCodeFree(details.code(), id);
        color.update(details);
        colors.save(color);
    }

    @Override
    public void delete(ColorId id) {
        Color color = load(id);
        if (products.existsByColor(id)) {
            throw new ResourceConflictException("catalog.color.in-use", "Color is used by products");
        }
        colors.delete(color);
    }

    private void requireCodeFree(String code, ColorId self) {
        if (colors.existsByCode(code, self)) {
            throw new ResourceConflictException("catalog.color.code-taken", "Color code is already used");
        }
    }

    private Color load(ColorId id) {
        return colors.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("catalog.color.not-found", "Color not found"));
    }
}

package pl.wizytowka.catalog.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.wizytowka.catalog.application.port.in.ManageCategoriesUseCase;
import pl.wizytowka.catalog.application.port.out.CategoryRepository;
import pl.wizytowka.catalog.application.port.out.ProductRepository;
import pl.wizytowka.catalog.domain.Category;
import pl.wizytowka.catalog.domain.CategoryDetails;
import pl.wizytowka.catalog.domain.CategoryId;
import pl.wizytowka.shared.application.Versions;
import pl.wizytowka.shared.domain.InvalidValueException;
import pl.wizytowka.shared.domain.ResourceConflictException;
import pl.wizytowka.shared.domain.ResourceNotFoundException;

import java.util.HashSet;
import java.util.Set;

@Service
@Transactional
class CategoryManagementService implements ManageCategoriesUseCase {

    private final CategoryRepository categories;
    private final ProductRepository products;

    CategoryManagementService(CategoryRepository categories, ProductRepository products) {
        this.categories = categories;
        this.products = products;
    }

    @Override
    public CategoryId create(CategoryDetails details) {
        validate(details, null);
        Category category = Category.create(details);
        categories.save(category);
        return category.id();
    }

    @Override
    public void update(CategoryId id, long expectedVersion, CategoryDetails details) {
        Category category = load(id);
        Versions.require(expectedVersion, category.version());
        validate(details, id);
        requireNoCycle(id, details.parentId());
        category.update(details);
        categories.save(category);
    }

    @Override
    public void delete(CategoryId id) {
        Category category = load(id);
        if (categories.hasChildren(id) || products.existsByCategory(id)) {
            throw new ResourceConflictException("catalog.category.in-use",
                    "Category has subcategories or products; move them first");
        }
        categories.delete(category);
    }

    private void validate(CategoryDetails details, CategoryId self) {
        if (details.parentId() != null && !categories.existsById(details.parentId())) {
            throw new ResourceNotFoundException("catalog.category.parent-not-found", "Parent category not found");
        }
        if (categories.existsBySlug(details.slug(), self)) {
            throw new ResourceConflictException("catalog.category.slug-taken", "Slug is already used by another category");
        }
    }

    /** Nowy rodzic nie może być kategorią przenoszoną ani żadnym z jej potomków. */
    private void requireNoCycle(CategoryId moved, CategoryId newParent) {
        Set<CategoryId> visited = new HashSet<>();
        for (CategoryId ancestor = newParent; ancestor != null; ancestor = load(ancestor).parentId()) {
            if (ancestor.equals(moved) || !visited.add(ancestor)) {
                throw new InvalidValueException("catalog.category.cycle",
                        "Category cannot be moved under itself or its descendant");
            }
        }
    }

    private Category load(CategoryId id) {
        return categories.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("catalog.category.not-found", "Category not found"));
    }
}

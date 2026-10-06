package pl.wizytowka.catalog.adapter.in.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.wizytowka.catalog.adapter.in.web.dto.AdminDictionaryDtos.CategoryRequest;
import pl.wizytowka.catalog.adapter.in.web.dto.AdminDictionaryDtos.CategoryResponse;
import pl.wizytowka.catalog.adapter.in.web.dto.CommonResponses.IdResponse;
import pl.wizytowka.catalog.application.port.in.AdminCatalogQueryUseCase;
import pl.wizytowka.catalog.application.port.in.ManageCategoriesUseCase;
import pl.wizytowka.catalog.domain.CategoryId;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/categories")
class AdminCategoryController {

    private final ManageCategoriesUseCase categories;
    private final AdminCatalogQueryUseCase queries;

    AdminCategoryController(ManageCategoriesUseCase categories, AdminCatalogQueryUseCase queries) {
        this.categories = categories;
        this.queries = queries;
    }

    @GetMapping
    List<CategoryResponse> list() {
        return queries.categories().stream().map(CategoryResponse::of).toList();
    }

    @PostMapping
    ResponseEntity<IdResponse> create(@Valid @RequestBody CategoryRequest request) {
        return Created.at(categories.create(request.toDetails()).value());
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void update(@PathVariable UUID id, @Valid @RequestBody CategoryRequest request) {
        categories.update(CategoryId.of(id), request.requiredVersion(), request.toDetails());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) {
        categories.delete(CategoryId.of(id));
    }
}

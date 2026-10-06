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
import pl.wizytowka.catalog.adapter.in.web.dto.AdminDictionaryDtos.SizeRequest;
import pl.wizytowka.catalog.adapter.in.web.dto.AdminDictionaryDtos.SizeResponse;
import pl.wizytowka.catalog.adapter.in.web.dto.CommonResponses.IdResponse;
import pl.wizytowka.catalog.application.port.in.AdminCatalogQueryUseCase;
import pl.wizytowka.catalog.application.port.in.ManageSizesUseCase;
import pl.wizytowka.catalog.domain.SizeId;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/sizes")
class AdminSizeController {

    private final ManageSizesUseCase sizes;
    private final AdminCatalogQueryUseCase queries;

    AdminSizeController(ManageSizesUseCase sizes, AdminCatalogQueryUseCase queries) {
        this.sizes = sizes;
        this.queries = queries;
    }

    @GetMapping
    List<SizeResponse> list() {
        return queries.sizes().stream().map(SizeResponse::of).toList();
    }

    @PostMapping
    ResponseEntity<IdResponse> create(@Valid @RequestBody SizeRequest request) {
        return Created.at(sizes.create(request.code(), request.sortOrder()).value());
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void update(@PathVariable UUID id, @Valid @RequestBody SizeRequest request) {
        sizes.update(SizeId.of(id), request.requiredVersion(), request.code(), request.sortOrder());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) {
        sizes.delete(SizeId.of(id));
    }
}

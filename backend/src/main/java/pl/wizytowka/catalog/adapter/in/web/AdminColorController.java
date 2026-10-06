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
import pl.wizytowka.catalog.adapter.in.web.dto.AdminDictionaryDtos.ColorRequest;
import pl.wizytowka.catalog.adapter.in.web.dto.AdminDictionaryDtos.ColorResponse;
import pl.wizytowka.catalog.adapter.in.web.dto.CommonResponses.IdResponse;
import pl.wizytowka.catalog.application.port.in.AdminCatalogQueryUseCase;
import pl.wizytowka.catalog.application.port.in.ManageColorsUseCase;
import pl.wizytowka.catalog.domain.ColorId;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/colors")
class AdminColorController {

    private final ManageColorsUseCase colors;
    private final AdminCatalogQueryUseCase queries;

    AdminColorController(ManageColorsUseCase colors, AdminCatalogQueryUseCase queries) {
        this.colors = colors;
        this.queries = queries;
    }

    @GetMapping
    List<ColorResponse> list() {
        return queries.colors().stream().map(ColorResponse::of).toList();
    }

    @PostMapping
    ResponseEntity<IdResponse> create(@Valid @RequestBody ColorRequest request) {
        return Created.at(colors.create(request.toDetails()).value());
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void update(@PathVariable UUID id, @Valid @RequestBody ColorRequest request) {
        colors.update(ColorId.of(id), request.requiredVersion(), request.toDetails());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) {
        colors.delete(ColorId.of(id));
    }
}

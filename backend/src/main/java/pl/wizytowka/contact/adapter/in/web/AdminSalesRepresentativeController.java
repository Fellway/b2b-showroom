package pl.wizytowka.contact.adapter.in.web;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pl.wizytowka.contact.application.port.in.ManageSalesRepresentativesUseCase;
import pl.wizytowka.contact.application.port.in.SalesRepresentativeQuery;
import pl.wizytowka.contact.application.view.AdminSalesRepresentativeView;
import pl.wizytowka.contact.domain.SalesRepresentativeId;
import pl.wizytowka.shared.domain.InvalidValueException;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/sales-representatives")
class AdminSalesRepresentativeController {

    private final ManageSalesRepresentativesUseCase manage;
    private final SalesRepresentativeQuery query;

    AdminSalesRepresentativeController(ManageSalesRepresentativesUseCase manage, SalesRepresentativeQuery query) {
        this.manage = manage;
        this.query = query;
    }

    @GetMapping
    List<AdminSalesRepresentativeView> list() {
        return query.allRepresentatives();
    }

    @GetMapping("/{id}")
    AdminSalesRepresentativeView get(@PathVariable UUID id) {
        return query.representative(SalesRepresentativeId.of(id));
    }

    @PostMapping
    ResponseEntity<AdminSalesRepresentativeView> create(@Valid @RequestBody SalesRepresentativeRequest request) {
        SalesRepresentativeId id = manage.create(request.toDetails());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(id.value()).toUri();
        return ResponseEntity.created(location).body(query.representative(id));
    }

    @PutMapping("/{id}")
    AdminSalesRepresentativeView update(@PathVariable UUID id, @Valid @RequestBody SalesRepresentativeRequest request) {
        if (request.version() == null) {
            throw new InvalidValueException("request.version-required", "Field 'version' is required for updates");
        }
        SalesRepresentativeId representativeId = SalesRepresentativeId.of(id);
        manage.update(representativeId, request.version(), request.toDetails());
        return query.representative(representativeId);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        manage.delete(SalesRepresentativeId.of(id));
        return ResponseEntity.noContent().build();
    }
}

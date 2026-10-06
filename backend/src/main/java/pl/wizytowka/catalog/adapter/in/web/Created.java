package pl.wizytowka.catalog.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pl.wizytowka.catalog.adapter.in.web.dto.CommonResponses.IdResponse;

import java.net.URI;
import java.util.UUID;

/** Odpowiedź 201 Created z nagłówkiem Location wskazującym nowy zasób względem bieżącego URI kolekcji. */
final class Created {

    private Created() {
    }

    static ResponseEntity<IdResponse> at(UUID id) {
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().path("/{id}").buildAndExpand(id).toUri();
        return ResponseEntity.created(location).body(new IdResponse(id));
    }
}

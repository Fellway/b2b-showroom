package pl.wizytowka.contact.adapter.in.web;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.wizytowka.contact.application.port.in.SalesRepresentativeQuery;
import pl.wizytowka.contact.application.view.SalesRepresentativeContact;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/v1/contact")
class PublicContactController {

    private final SalesRepresentativeQuery query;

    PublicContactController(SalesRepresentativeQuery query) {
        this.query = query;
    }

    @GetMapping("/representatives")
    ResponseEntity<List<SalesRepresentativeContact>> representatives() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
                .body(query.activeRepresentatives());
    }
}

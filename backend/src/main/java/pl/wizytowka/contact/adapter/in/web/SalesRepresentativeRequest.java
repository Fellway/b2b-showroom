package pl.wizytowka.contact.adapter.in.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import pl.wizytowka.contact.domain.SalesRepresentativeDetails;

/** @param version wymagana przy aktualizacji (wartość z ostatniego odczytu) */
record SalesRepresentativeRequest(@NotBlank @Size(max = 120) String fullName,
                                  @NotBlank @Email @Size(max = 254) String email,
                                  @Size(max = 32) String phone,
                                  boolean active,
                                  @PositiveOrZero int sortOrder,
                                  Long version) {

    SalesRepresentativeDetails toDetails() {
        return new SalesRepresentativeDetails(fullName, email, phone, active, sortOrder);
    }
}

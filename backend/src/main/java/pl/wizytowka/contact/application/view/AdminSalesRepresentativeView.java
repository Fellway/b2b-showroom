package pl.wizytowka.contact.application.view;

import java.util.UUID;

public record AdminSalesRepresentativeView(UUID id, String fullName, String email, String phone, boolean active,
                                           int sortOrder, long version) {
}

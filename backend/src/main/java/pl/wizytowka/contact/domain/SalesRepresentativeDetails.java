package pl.wizytowka.contact.domain;

import pl.wizytowka.shared.domain.Guard;
import pl.wizytowka.shared.domain.InvalidValueException;

import java.util.regex.Pattern;

/**
 * Edytowalne dane opiekuna handlowego.
 * @param phone numer w dowolnym czytelnym formacie (cyfry, spacje, +, -, nawiasy), np. "+48 600 100 200"
 */
public record SalesRepresentativeDetails(String fullName, String email, String phone, boolean active, int sortOrder) {

    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9 ()./-]{5,32}$");

    public SalesRepresentativeDetails {
        fullName = Guard.requiredText(fullName, 120, "contact.representative.full-name");
        email = Guard.email(email, "contact.representative.email");
        phone = Guard.optionalText(phone, 32, "contact.representative.phone");
        if (phone != null && (!PHONE.matcher(phone).matches() || phone.chars().filter(Character::isDigit).count() < 5)) {
            throw new InvalidValueException("contact.representative.phone", "Invalid phone number");
        }
        if (sortOrder < 0) {
            throw new InvalidValueException("contact.representative.sort-order", "Sort order must not be negative");
        }
    }
}

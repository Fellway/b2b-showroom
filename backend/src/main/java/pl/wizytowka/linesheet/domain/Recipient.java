package pl.wizytowka.linesheet.domain;

import pl.wizytowka.shared.domain.Guard;

/** Dane kontaktowe klienta hurtowego podane przy generowaniu zestawienia (wszystkie opcjonalne). */
public record Recipient(String companyName, String contactName, String email, String phone, String taxId) {

    public Recipient {
        companyName = Guard.optionalText(companyName, 200, "linesheet.recipient.company-name");
        contactName = Guard.optionalText(contactName, 120, "linesheet.recipient.contact-name");
        email = email == null || email.isBlank() ? null : Guard.email(email, "linesheet.recipient.email");
        phone = Guard.optionalText(phone, 32, "linesheet.recipient.phone");
        taxId = Guard.optionalText(taxId, 32, "linesheet.recipient.tax-id");
    }

    public static Recipient anonymous() {
        return new Recipient(null, null, null, null, null);
    }

    public boolean hasEmail() {
        return email != null;
    }

    /** Czy klient podał jakiekolwiek dane osobowe/firmowe (wymaga to zgody na przetwarzanie). */
    public boolean hasAnyData() {
        return companyName != null || contactName != null || email != null || phone != null || taxId != null;
    }
}

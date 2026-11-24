package pl.wizytowka.linesheet.domain;

import pl.wizytowka.shared.domain.CatalogLanguage;
import pl.wizytowka.shared.domain.Pricing;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class LineSheetFixtures {

    public static final Instant NOW = Instant.parse("2026-10-05T12:00:00Z");

    private LineSheetFixtures() {
    }

    public static SelectedVariant selection(String sku, Integer quantity) {
        return new SelectedVariant(UUID.randomUUID(), UUID.randomUUID(), "LS-001", "Koszula lniana", "Granatowy", "M",
                sku, "5901234123457", Pricing.pln("149.00", "349.99"), "products/ls-001/hero.webp", quantity);
    }

    public static LineSheet anonymous(SelectedVariant... selections) {
        return LineSheet.create("LS-2026-000001", CatalogLanguage.PL, Recipient.anonymous(), null, false,
                null, null, null, List.of(selections), NOW);
    }

    public static LineSheet withContact(SelectedVariant... selections) {
        return LineSheet.create("LS-2026-000002", CatalogLanguage.EN,
                new Recipient("Boutique s.r.o.", "Jana", "Buyer@Boutique.cz", null, "CZ12345678"),
                "Please confirm delivery dates", true, null, null, UUID.fromString("01900000-0000-7000-8000-000000000001"),
                List.of(selections), NOW);
    }
}

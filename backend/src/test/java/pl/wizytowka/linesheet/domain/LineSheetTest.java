package pl.wizytowka.linesheet.domain;

import org.junit.jupiter.api.Test;
import pl.wizytowka.shared.domain.BusinessRuleViolationException;
import pl.wizytowka.shared.domain.CatalogLanguage;
import pl.wizytowka.shared.domain.InvalidValueException;
import pl.wizytowka.shared.domain.Pricing;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pl.wizytowka.linesheet.domain.LineSheetFixtures.NOW;
import static pl.wizytowka.linesheet.domain.LineSheetFixtures.selection;

class LineSheetTest {

    @Test
    void createsSnapshotItemsInSelectionOrder() {
        LineSheet sheet = LineSheetFixtures.withContact(selection("A-1", 12), selection("A-2", null));

        assertThat(sheet.items()).extracting(LineSheetItem::position, LineSheetItem::sku).containsExactly(
                org.assertj.core.groups.Tuple.tuple(1, "A-1"), org.assertj.core.groups.Tuple.tuple(2, "A-2"));
        assertThat(sheet.items().getFirst().wholesaleTotal()).isEqualByComparingTo("1788.00");
        assertThat(sheet.items().get(1).wholesaleTotal()).isNull();
        assertThat(sheet.currency()).isEqualTo(Currency.getInstance("PLN"));
        assertThat(sheet.recipient().email()).isEqualTo("buyer@boutique.cz");
        assertThat(sheet.privacyAcceptedAt()).isEqualTo(NOW);
        assertThat(sheet.publicId().version()).as("publiczny identyfikator losowy (v4)").isEqualTo(4);
        assertThat(sheet.id().value().version()).isEqualTo(7);
    }

    @Test
    void formatsReferenceNumber() {
        assertThat(LineSheet.formatReference(2026, 42)).isEqualTo("LS-2026-000042");
        assertThat(LineSheet.formatReference(2026, 1_234_567)).isEqualTo("LS-2026-1234567");
    }

    @Test
    void requiresConsentWhenContactDataProvided() {
        assertThatThrownBy(() -> LineSheet.create("LS-2026-000001", CatalogLanguage.PL,
                new Recipient("Firma", null, null, null, null), null, false, null, null, null,
                List.of(selection("A-1", null)), NOW))
                .isInstanceOf(InvalidValueException.class)
                .hasFieldOrPropertyWithValue("code", "linesheet.privacy.consent-required");
    }

    @Test
    void rejectsEmptyDuplicateTooManyAndMixedCurrencyItems() {
        SelectedVariant one = selection("A-1", null);
        assertThatThrownBy(LineSheetFixtures::anonymous).hasFieldOrPropertyWithValue("code", "linesheet.items.empty");
        assertThatThrownBy(() -> LineSheetFixtures.anonymous(one, one))
                .hasFieldOrPropertyWithValue("code", "linesheet.items.duplicate");
        SelectedVariant[] tooMany = IntStream.rangeClosed(0, LineSheet.MAX_ITEMS)
                .mapToObj(i -> selection("S-" + i, null)).toArray(SelectedVariant[]::new);
        assertThatThrownBy(() -> LineSheetFixtures.anonymous(tooMany))
                .hasFieldOrPropertyWithValue("code", "linesheet.items.too-many");
        SelectedVariant eur = new SelectedVariant(UUID.randomUUID(), UUID.randomUUID(), "X", "X", "X", "M", "E-1",
                "5901234123457", new Pricing(BigDecimal.TEN, BigDecimal.TEN, Currency.getInstance("EUR")), null, null);
        assertThatThrownBy(() -> LineSheetFixtures.anonymous(one, eur))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void contactEmailIsOnlyContactDataAndStillRequiresConsent() {
        assertThatThrownBy(() -> LineSheet.create("LS-2026-000001", CatalogLanguage.PL,
                new Recipient(null, null, "buyer@boutique.cz", null, null), null, false, null, null, null,
                List.of(selection("A-1", null)), NOW))
                .hasFieldOrPropertyWithValue("code", "linesheet.privacy.consent-required");

        LineSheet anonymous = LineSheetFixtures.anonymous(selection("A-1", null));
        assertThat(anonymous.privacyAcceptedAt()).as("bez danych kontaktowych zgoda nie jest potrzebna").isNull();
    }

    @Test
    void rejectsNonPositiveQuantity() {
        assertThatThrownBy(() -> selection("A-1", 0)).isInstanceOf(InvalidValueException.class);
    }
}

package pl.wizytowka.identity.domain;

import org.junit.jupiter.api.Test;
import pl.wizytowka.shared.domain.BusinessRuleViolationException;
import pl.wizytowka.shared.domain.CatalogLanguage;
import pl.wizytowka.shared.domain.InvalidValueException;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BuyerAccountTest {

    static final Instant NOW = Instant.parse("2026-11-24T10:00:00Z");
    static final AdminUserId REVIEWER = AdminUserId.newId();

    static BuyerAccount registered() {
        return BuyerAccount.register("Jana@Boutique.CZ", "{fake}secret", "Boutique s.r.o.", TaxId.of("CZ", "12345678"),
                "Jana Nováková", "+420 777 100 200", CatalogLanguage.CS, true, true, NOW);
    }

    @Test
    void registrationRequiresBothConsentsAndStartsWaitingForEmail() {
        BuyerAccount account = registered();

        assertThat(account.status()).isEqualTo(BuyerStatus.PENDING_EMAIL);
        assertThat(account.email()).isEqualTo("jana@boutique.cz");
        assertThat(account.termsAcceptedAt()).isEqualTo(NOW);
        assertThat(account.privacyAcceptedAt()).isEqualTo(NOW);
        assertThat(account.isActive()).isFalse();

        assertThatThrownBy(() -> BuyerAccount.register("a@b.pl", "{fake}x", "Firma", TaxId.of("PL", "1234563218"),
                "Jan", "600100200", CatalogLanguage.PL, true, false, NOW))
                .isInstanceOf(InvalidValueException.class)
                .hasFieldOrPropertyWithValue("code", "identity.buyer.consent-required");
    }

    @Test
    void validatesContactData() {
        assertThatThrownBy(() -> BuyerAccount.register("a@b.pl", "{fake}x", " ", TaxId.of("PL", "1234563218"),
                "Jan", "600100200", CatalogLanguage.PL, true, true, NOW))
                .hasFieldOrPropertyWithValue("code", "identity.buyer.company-name");
        assertThatThrownBy(() -> BuyerAccount.register("a@b.pl", "{fake}x", "Firma", TaxId.of("PL", "1234563218"),
                "Jan", "call me", CatalogLanguage.PL, true, true, NOW))
                .hasFieldOrPropertyWithValue("code", "identity.buyer.phone");
        assertThatThrownBy(() -> BuyerAccount.register("not-an-email", "{fake}x", "Firma", TaxId.of("PL", "1234563218"),
                "Jan", "600100200", CatalogLanguage.PL, true, true, NOW))
                .hasFieldOrPropertyWithValue("code", "identity.email");
    }

    @Test
    void followsApprovalLifecycle() {
        BuyerAccount account = registered();
        assertThatThrownBy(() -> account.approve(REVIEWER, NOW))
                .as("bez potwierdzonego adresu nie da się zaakceptować")
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasFieldOrPropertyWithValue("code", "identity.buyer.email-not-verified");

        assertThat(account.verifyEmail(NOW)).isTrue();
        assertThat(account.verifyEmail(NOW.plusSeconds(5))).as("ponowne potwierdzenie niczego nie zmienia").isFalse();
        assertThat(account.status()).isEqualTo(BuyerStatus.PENDING_APPROVAL);
        assertThat(account.emailVerifiedAt()).isEqualTo(NOW);

        account.reject(REVIEWER, "Detal, nie hurt", NOW);
        assertThat(account.status()).isEqualTo(BuyerStatus.REJECTED);
        assertThat(account.reviewNote()).isEqualTo("Detal, nie hurt");

        account.approve(REVIEWER, NOW.plus(Duration.ofDays(1)));
        assertThat(account.isActive()).as("odrzucony wniosek można jeszcze zaakceptować").isTrue();
        assertThat(account.reviewedBy()).isEqualTo(REVIEWER);
        assertThat(account.reviewNote()).isNull();

        account.suspend(REVIEWER, "Zaległe płatności", NOW);
        assertThat(account.status()).isEqualTo(BuyerStatus.SUSPENDED);
        assertThatThrownBy(() -> account.approve(REVIEWER, NOW))
                .hasFieldOrPropertyWithValue("code", "identity.buyer.invalid-transition");
        account.reactivate(REVIEWER, NOW);
        assertThat(account.isActive()).isTrue();
        assertThatThrownBy(() -> account.reject(REVIEWER, null, NOW))
                .hasFieldOrPropertyWithValue("code", "identity.buyer.invalid-transition");
    }

    @Test
    void locksAfterRepeatedFailuresAndNewPasswordUnlocks() {
        BuyerAccount account = registered();
        LockoutPolicy policy = new LockoutPolicy(3, Duration.ofMinutes(15));

        account.recordFailedLogin(NOW, policy);
        account.recordFailedLogin(NOW, policy);
        assertThat(account.isLocked(NOW)).isFalse();
        account.recordFailedLogin(NOW, policy);
        assertThat(account.isLocked(NOW)).isTrue();
        assertThat(account.isLocked(NOW.plus(Duration.ofMinutes(16)))).isFalse();

        account.changePasswordHash("{fake}new");
        assertThat(account.isLocked(NOW)).isFalse();
    }

    @Test
    void buyerUpdatesOnlyContactDataAndLanguage() {
        BuyerAccount account = registered();

        account.updateProfile("Petra Svobodová", "+420 777 999 000", CatalogLanguage.EN);

        assertThat(account.contactName()).isEqualTo("Petra Svobodová");
        assertThat(account.phone()).isEqualTo("+420 777 999 000");
        assertThat(account.language()).isEqualTo(CatalogLanguage.EN);
        assertThat(account.companyName()).isEqualTo("Boutique s.r.o.");
    }
}

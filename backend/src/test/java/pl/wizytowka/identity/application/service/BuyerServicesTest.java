package pl.wizytowka.identity.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.wizytowka.identity.application.port.in.AccountNotActiveException;
import pl.wizytowka.identity.application.port.in.AuthenticationFailedException;
import pl.wizytowka.identity.application.port.in.BuyerContactQuery.BuyerContact;
import pl.wizytowka.identity.application.port.in.BuyerSelfServiceUseCase;
import pl.wizytowka.identity.application.port.in.BuyerSession;
import pl.wizytowka.identity.application.port.in.RegisterBuyerCommand;
import pl.wizytowka.identity.domain.AdminRole;
import pl.wizytowka.identity.domain.AdminUser;
import pl.wizytowka.identity.domain.BuyerAccount;
import pl.wizytowka.identity.domain.BuyerAccountDeleted;
import pl.wizytowka.identity.domain.BuyerActionPurpose;
import pl.wizytowka.identity.domain.BuyerConsent;
import pl.wizytowka.identity.domain.BuyerStatus;
import pl.wizytowka.identity.domain.LockoutPolicy;
import pl.wizytowka.legal.domain.LegalDocumentType;
import pl.wizytowka.shared.domain.CatalogLanguage;
import pl.wizytowka.shared.domain.ConcurrentModificationException;
import pl.wizytowka.shared.domain.InvalidValueException;
import pl.wizytowka.shared.domain.ResourceConflictException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/** Rejestracja, potwierdzenie adresu, reset hasła, logowanie i decyzje marki — bez Springa i bazy. */
class BuyerServicesTest {

    static final Instant START = Instant.parse("2026-11-24T10:00:00Z");
    static final String PASSWORD = "correct horse battery";

    final IdentityFakes.BuyerAccounts accounts = new IdentityFakes.BuyerAccounts();
    final IdentityFakes.BuyerSessions sessions = new IdentityFakes.BuyerSessions();
    final IdentityFakes.ActionTokens actionTokens = new IdentityFakes.ActionTokens();
    final IdentityFakes.Users staff = new IdentityFakes.Users();
    final IdentityFakes.RecordingMailer mailer = new IdentityFakes.RecordingMailer();
    final IdentityFakes.DeferredHooks hooks = new IdentityFakes.DeferredHooks();
    final IdentityFakes.Consents consents = new IdentityFakes.Consents();
    final IdentityFakes.Legal legalDocuments = new IdentityFakes.Legal();
    final IdentityFakes.Events events = new IdentityFakes.Events();
    final AtomicReference<Instant> now = new AtomicReference<>(START);

    BuyerRegistrationService registration;
    BuyerAuthenticationService authentication;
    BuyerAdministrationService administration;
    BuyerLegalService legal;
    AdminUser reviewer;

    @BeforeEach
    void setUp() {
        Clock clock = new Clock() {
            @Override
            public ZoneOffset getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(java.time.ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return now.get();
            }
        };
        BuyerPolicy policy = new BuyerPolicy(new LockoutPolicy(3, Duration.ofMinutes(15)), Duration.ofDays(30),
                Duration.ofHours(48), Duration.ofHours(1));
        IdentityFakes.Hasher hasher = new IdentityFakes.Hasher();
        IdentityFakes.SequentialTokens tokens = new IdentityFakes.SequentialTokens();
        legalDocuments.publish(LegalDocumentType.TERMS, true);
        legalDocuments.publish(LegalDocumentType.PRIVACY_POLICY, false);
        legal = new BuyerLegalService(accounts, consents, legalDocuments, clock);
        BuyerAccountRemoval removal = new BuyerAccountRemoval(accounts, events);
        registration = new BuyerRegistrationService(accounts, actionTokens, sessions, staff, hasher, tokens, mailer,
                hooks, policy, legal, clock);
        authentication = new BuyerAuthenticationService(accounts, sessions, hasher, new IdentityFakes.Issuer(), tokens,
                policy, legal, removal, mailer, hooks, clock);
        administration = new BuyerAdministrationService(accounts, sessions, mailer, hooks, legal, removal, clock);
        reviewer = IdentityFakes.user("editor@brand.pl", AdminRole.EDITOR, "editor-password-1");
        staff.save(reviewer);
    }

    RegisterBuyerCommand command(String email) {
        return new RegisterBuyerCommand(email, PASSWORD, "Boutique s.r.o.", "CZ", "CZ12345678", "Jana Nováková",
                "+420 777 100 200", CatalogLanguage.CS, true, true, legalDocuments.current(LegalDocumentType.TERMS),
                legalDocuments.current(LegalDocumentType.PRIVACY_POLICY));
    }

    UUID terms() {
        return legalDocuments.current(LegalDocumentType.TERMS);
    }

    UUID privacy() {
        return legalDocuments.current(LegalDocumentType.PRIVACY_POLICY);
    }

    BuyerAccount registerAndVerify() {
        registration.register(command("jana@boutique.cz"));
        hooks.commit();
        registration.verifyEmail(mailer.lastToken("verify"));
        hooks.commit();
        return accounts.only();
    }

    BuyerAccount activeBuyer() {
        BuyerAccount account = registerAndVerify();
        administration.approve(reviewer.id(), account.id(), account.version());
        hooks.commit();
        return account;
    }

    // ------------------------------------------------------------------ rejestracja

    @Test
    void registrationSendsVerificationLinkOnlyAfterCommit() {
        registration.register(command("Jana@Boutique.cz"));

        BuyerAccount account = accounts.only();
        assertThat(account.status()).isEqualTo(BuyerStatus.PENDING_EMAIL);
        assertThat(account.passwordHash()).isEqualTo("{fake}" + PASSWORD);
        assertThat(mailer.sent).as("e-mail dopiero po commicie").isEmpty();

        hooks.commit();

        assertThat(mailer.sent).singleElement().satisfies(mail -> {
            assertThat(mail.kind()).isEqualTo("verify");
            assertThat(mail.to()).isEqualTo("jana@boutique.cz");
            assertThat(mail.language()).isEqualTo(CatalogLanguage.CS);
            assertThat(mail.token()).isNotBlank();
        });
        assertThat(actionTokens.byHash.values()).singleElement()
                .satisfies(token -> assertThat(token.tokenHash()).isNotEqualTo(mailer.lastToken("verify")));
    }

    @Test
    void registrationOnExistingEmailLooksTheSameButNotifiesOwner() {
        registration.register(command("jana@boutique.cz"));
        hooks.commit();
        mailer.sent.clear();

        registration.register(command("JANA@boutique.cz"));
        hooks.commit();

        assertThat(accounts.store).hasSize(1);
        assertThat(mailer.kinds()).containsExactly("exists");
    }

    @Test
    void registrationValidatesPasswordTaxIdAndConsents() {
        assertThatThrownBy(() -> registration.register(new RegisterBuyerCommand("a@b.pl", "short", "Firma", "PL",
                "1234563218", "Jan", "600100200", CatalogLanguage.PL, true, true, terms(), privacy())))
                .hasFieldOrPropertyWithValue("code", "identity.password.policy");
        assertThatThrownBy(() -> registration.register(new RegisterBuyerCommand("a@b.pl", PASSWORD, "Firma", "PL",
                "1234563219", "Jan", "600100200", CatalogLanguage.PL, true, true, terms(), privacy())))
                .hasFieldOrPropertyWithValue("code", "identity.buyer.tax-id");
        assertThatThrownBy(() -> registration.register(new RegisterBuyerCommand("a@b.pl", PASSWORD, "Firma", "PL",
                "1234563218", "Jan", "600100200", CatalogLanguage.PL, false, true, terms(), privacy())))
                .hasFieldOrPropertyWithValue("code", "identity.buyer.consent-required");
        assertThat(accounts.store).isEmpty();
    }

    @Test
    void verificationMovesAccountToApprovalQueueAndNotifiesStaffOnce() {
        BuyerAccount account = registerAndVerify();

        assertThat(account.status()).isEqualTo(BuyerStatus.PENDING_APPROVAL);
        assertThat(mailer.kinds()).containsExactly("verify", "staff");
        assertThat(administration.countPendingApproval()).isEqualTo(1);

        String usedToken = mailer.lastToken("verify");
        assertThatThrownBy(() -> registration.verifyEmail(usedToken))
                .as("token jednorazowy")
                .isInstanceOf(InvalidValueException.class)
                .hasFieldOrPropertyWithValue("code", "identity.buyer.token-invalid");
    }

    @Test
    void verificationLinkExpiresAndResendReplacesIt() {
        registration.register(command("jana@boutique.cz"));
        hooks.commit();
        String first = mailer.lastToken("verify");

        now.set(START.plus(Duration.ofMinutes(1)));
        registration.resendVerification("jana@boutique.cz");
        hooks.commit();
        assertThat(mailer.kinds()).as("ponowna wysyłka nie częściej niż co 2 minuty").containsExactly("verify");

        now.set(START.plus(Duration.ofMinutes(3)));
        registration.resendVerification("jana@boutique.cz");
        hooks.commit();
        String second = mailer.lastToken("verify");
        assertThat(second).isNotEqualTo(first);
        assertThatThrownBy(() -> registration.verifyEmail(first)).as("nowy link unieważnia poprzedni")
                .hasFieldOrPropertyWithValue("code", "identity.buyer.token-invalid");

        now.set(START.plus(Duration.ofHours(49)));
        assertThatThrownBy(() -> registration.verifyEmail(second)).as("link wygasa po 48 h")
                .hasFieldOrPropertyWithValue("code", "identity.buyer.token-invalid");
    }

    @Test
    void unknownEmailRequestsAreSilent() {
        registration.resendVerification("nobody@example.com");
        registration.requestPasswordReset("nobody@example.com");
        registration.requestPasswordReset("not an email");
        hooks.commit();

        assertThat(mailer.sent).isEmpty();
        assertThatThrownBy(() -> registration.verifyEmail("x".repeat(500)))
                .hasFieldOrPropertyWithValue("code", "identity.buyer.token-invalid");
    }

    // ------------------------------------------------------------------ logowanie i sesje

    @Test
    void onlyActiveAccountCanLogInAndStatusIsRevealedOnlyWithCorrectPassword() {
        registerAndVerify();

        assertThatThrownBy(() -> authentication.login("jana@boutique.cz", "wrong password!!"))
                .isInstanceOf(AuthenticationFailedException.class);
        assertThatThrownBy(() -> authentication.login("jana@boutique.cz", PASSWORD))
                .isInstanceOf(AccountNotActiveException.class)
                .satisfies(e -> assertThat(((AccountNotActiveException) e).status()).isEqualTo(BuyerStatus.PENDING_APPROVAL));
        assertThatThrownBy(() -> authentication.login("nobody@example.com", PASSWORD))
                .isInstanceOf(AuthenticationFailedException.class);
        assertThat(sessions.byHash).isEmpty();
    }

    @Test
    void approvedBuyerLogsInRotatesSessionAndIsLockedAfterFailures() {
        BuyerAccount account = activeBuyer();
        assertThat(mailer.kinds()).endsWith("approved");

        BuyerSession session = authentication.login("Jana@Boutique.cz", PASSWORD);
        assertThat(session.accessToken().value()).isEqualTo("buyer-access-" + account.id());
        assertThat(session.buyer().status()).isEqualTo(BuyerStatus.ACTIVE);
        assertThat(session.refreshTokenExpiresAt()).isEqualTo(START.plus(Duration.ofDays(30)));

        BuyerSession rotated = authentication.refresh(session.refreshToken());
        assertThat(rotated.refreshToken()).isNotEqualTo(session.refreshToken());
        assertThatThrownBy(() -> authentication.refresh(session.refreshToken()))
                .as("ponowne użycie zużytego tokenu unieważnia rodzinę")
                .isInstanceOf(AuthenticationFailedException.class);
        assertThatThrownBy(() -> authentication.refresh(rotated.refreshToken()))
                .isInstanceOf(AuthenticationFailedException.class);

        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> authentication.login("jana@boutique.cz", "wrong password!!"))
                    .isInstanceOf(AuthenticationFailedException.class);
        }
        assertThatThrownBy(() -> authentication.login("jana@boutique.cz", PASSWORD))
                .as("zablokowane konto nie loguje się nawet poprawnym hasłem")
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void suspensionEndsSessionsAndRefreshFails() {
        BuyerAccount account = activeBuyer();
        BuyerSession session = authentication.login("jana@boutique.cz", PASSWORD);

        administration.suspend(reviewer.id(), account.id(), account.version(), "Zaległe płatności");

        assertThat(sessions.activeCount()).isZero();
        assertThatThrownBy(() -> authentication.refresh(session.refreshToken()))
                .isInstanceOf(AuthenticationFailedException.class);
        assertThat(administration.activeBuyer(account.id().value())).isEmpty();

        administration.reactivate(reviewer.id(), account.id(), account.version());
        assertThat(administration.activeBuyer(account.id().value())).get()
                .extracting(BuyerContact::taxId).isEqualTo("CZ 12345678");
    }

    @Test
    void passwordResetChangesPasswordEndsSessionsAndIsSingleUse() {
        activeBuyer();
        BuyerSession session = authentication.login("jana@boutique.cz", PASSWORD);

        registration.requestPasswordReset("jana@boutique.cz");
        hooks.commit();
        String token = mailer.lastToken("reset");

        assertThatThrownBy(() -> registration.resetPassword(token, "short"))
                .hasFieldOrPropertyWithValue("code", "identity.password.policy");
        registration.resetPassword(token, "a brand new passphrase");

        assertThat(sessions.activeCount()).isZero();
        assertThatThrownBy(() -> authentication.refresh(session.refreshToken())).isInstanceOf(AuthenticationFailedException.class);
        assertThat(authentication.login("jana@boutique.cz", "a brand new passphrase").buyer().email())
                .isEqualTo("jana@boutique.cz");
        assertThatThrownBy(() -> registration.resetPassword(token, "another passphrase!"))
                .hasFieldOrPropertyWithValue("code", "identity.buyer.token-invalid");

        now.set(START.plus(Duration.ofMinutes(5)));
        registration.requestPasswordReset("jana@boutique.cz");
        hooks.commit();
        String late = mailer.lastToken("reset");
        now.set(START.plus(Duration.ofHours(2)));
        assertThatThrownBy(() -> registration.resetPassword(late, "another passphrase!"))
                .as("link resetu ważny 1 h").hasFieldOrPropertyWithValue("code", "identity.buyer.token-invalid");
    }

    @Test
    void passwordResetLinkAlsoConfirmsUnverifiedEmail() {
        registration.register(command("jana@boutique.cz"));
        hooks.commit();
        registration.requestPasswordReset("jana@boutique.cz");
        hooks.commit();

        registration.resetPassword(mailer.lastToken("reset"), "a brand new passphrase");
        hooks.commit();

        assertThat(accounts.only().status()).isEqualTo(BuyerStatus.PENDING_APPROVAL);
        assertThat(mailer.kinds()).contains("staff");
        assertThat(actionTokens.byHash.values())
                .filteredOn(t -> t.purpose() == BuyerActionPurpose.PASSWORD_RESET)
                .allSatisfy(t -> assertThat(t.usedAt()).isNotNull());
    }

    @Test
    void changePasswordRequiresCurrentPasswordAndOpensFreshSession() {
        BuyerAccount account = activeBuyer();
        BuyerSession old = authentication.login("jana@boutique.cz", PASSWORD);

        assertThatThrownBy(() -> authentication.changePassword(account.id(), "not my password", "a brand new passphrase"))
                .hasFieldOrPropertyWithValue("code", "identity.buyer.current-password-invalid");

        BuyerSession fresh = authentication.changePassword(account.id(), PASSWORD, "a brand new passphrase");

        assertThatThrownBy(() -> authentication.refresh(old.refreshToken())).isInstanceOf(AuthenticationFailedException.class);
        assertThat(authentication.refresh(fresh.refreshToken()).buyer().id()).isEqualTo(account.id().value());
    }

    @Test
    void decisionsRequireCurrentVersionAndRejectionNotifiesBuyer() {
        BuyerAccount account = registerAndVerify();

        assertThatThrownBy(() -> administration.reject(reviewer.id(), account.id(), account.version() + 1, null))
                .isInstanceOf(ConcurrentModificationException.class);
        administration.reject(reviewer.id(), account.id(), account.version(), "Klient detaliczny");
        hooks.commit();

        assertThat(mailer.kinds()).endsWith("rejected");
        assertThat(administration.get(account.id()).reviewNote()).isEqualTo("Klient detaliczny");
        assertThat(administration.get(account.id()).reviewedBy()).isEqualTo(reviewer.id().value());

        administration.delete(account.id());
        assertThat(accounts.store).isEmpty();
        assertThat(events.published).as("dane osobowe znikają też z zestawień")
                .containsExactly(new BuyerAccountDeleted(account.id().value()));
    }

    // ------------------------------------------------------------------ dokumenty prawne i zgody (D25)

    @Test
    void registrationRecordsAcceptedVersionsInConsentRegistry() {
        BuyerAccount account = registerAndVerify();

        assertThat(consents.findByBuyer(account.id()))
                .extracting(BuyerConsent::documentType, BuyerConsent::versionNumber, BuyerConsent::language,
                        BuyerConsent::context)
                .containsExactlyInAnyOrder(
                        tuple(LegalDocumentType.TERMS, 1, CatalogLanguage.CS, BuyerConsent.Context.REGISTRATION),
                        tuple(LegalDocumentType.PRIVACY_POLICY, 1, CatalogLanguage.CS, BuyerConsent.Context.REGISTRATION));
        assertThat(legal.status(account.id()).pending()).isEmpty();
        assertThat(legal.mustAcceptTerms(account.id().value())).isFalse();
    }

    @Test
    void registrationWithOutdatedDocumentVersionIsRejectedBeforeAnythingIsStored() {
        UUID seenTerms = terms();
        legalDocuments.publish(LegalDocumentType.TERMS, true);

        assertThatThrownBy(() -> registration.register(new RegisterBuyerCommand("jana@boutique.cz", PASSWORD,
                "Boutique s.r.o.", "CZ", "CZ12345678", "Jana Nováková", "+420 777 100 200", CatalogLanguage.CS,
                true, true, seenTerms, privacy())))
                .isInstanceOf(ResourceConflictException.class)
                .hasFieldOrPropertyWithValue("code", "identity.buyer.legal-version-outdated");
        assertThatThrownBy(() -> registration.register(new RegisterBuyerCommand("jana@boutique.cz", PASSWORD,
                "Boutique s.r.o.", "CZ", "CZ12345678", "Jana Nováková", "+420 777 100 200", CatalogLanguage.CS,
                true, true, null, null)))
                .as("brak wersji, gdy dokumenty są opublikowane")
                .hasFieldOrPropertyWithValue("code", "identity.buyer.legal-version-outdated");
        assertThat(accounts.store).isEmpty();
        assertThat(consents.store).isEmpty();
    }

    @Test
    void newTermsVersionBlocksPricesUntilAcceptedButEditorialFixDoesNot() {
        BuyerAccount account = activeBuyer();

        legalDocuments.publish(LegalDocumentType.TERMS, false);
        assertThat(legal.mustAcceptTerms(account.id().value())).as("poprawka redakcyjna").isFalse();

        UUID v3 = legalDocuments.publish(LegalDocumentType.TERMS, true);
        assertThat(legal.mustAcceptTerms(account.id().value())).isTrue();
        assertThat(legal.status(account.id()).pending()).singleElement().satisfies(pending -> {
            assertThat(pending.type()).isEqualTo(LegalDocumentType.TERMS);
            assertThat(pending.versionId()).isEqualTo(v3);
            assertThat(pending.versionNumber()).isEqualTo(3);
            assertThat(pending.blocking()).isTrue();
            assertThat(pending.firstTime()).isFalse();
        });

        now.set(START.plus(Duration.ofDays(2)));
        legal.accept(account.id(), List.of(v3, v3), CatalogLanguage.EN);
        legal.accept(account.id(), List.of(v3), CatalogLanguage.EN);

        assertThat(legal.mustAcceptTerms(account.id().value())).isFalse();
        assertThat(legal.status(account.id()).history()).first().satisfies(latest -> {
            assertThat(latest.versionNumber()).isEqualTo(3);
            assertThat(latest.language()).isEqualTo(CatalogLanguage.EN);
            assertThat(latest.context()).isEqualTo(BuyerConsent.Context.UPDATE);
            assertThat(latest.acceptedAt()).isEqualTo(START.plus(Duration.ofDays(2)));
        });
        assertThat(consents.store).as("ponowna akceptacja tej samej wersji nic nie dopisuje").hasSize(3);
    }

    @Test
    void newPrivacyPolicyIsOnlyANoticeAndOldVersionsCannotBeAccepted() {
        BuyerAccount account = activeBuyer();
        UUID oldPrivacy = privacy();
        UUID newPrivacy = legalDocuments.publish(LegalDocumentType.PRIVACY_POLICY, true);

        assertThat(legal.mustAcceptTerms(account.id().value())).isFalse();
        assertThat(legal.status(account.id()).pending()).singleElement().satisfies(pending -> {
            assertThat(pending.type()).isEqualTo(LegalDocumentType.PRIVACY_POLICY);
            assertThat(pending.blocking()).isFalse();
        });
        assertThatThrownBy(() -> legal.accept(account.id(), List.of(oldPrivacy), CatalogLanguage.PL))
                .hasFieldOrPropertyWithValue("code", "identity.buyer.legal-version-outdated");
        assertThatThrownBy(() -> legal.accept(account.id(), List.of(UUID.randomUUID()), CatalogLanguage.PL))
                .hasFieldOrPropertyWithValue("code", "identity.buyer.legal-version-unknown");

        legal.accept(account.id(), List.of(newPrivacy), CatalogLanguage.PL);
        assertThat(legal.status(account.id()).pending()).isEmpty();
    }

    @Test
    void accountWithoutAnyConsentMustAcceptCurrentTerms() {
        BuyerAccount account = activeBuyer();
        consents.store.clear();

        assertThat(legal.mustAcceptTerms(account.id().value())).isTrue();
        assertThat(legal.status(account.id()).pending())
                .allSatisfy(pending -> assertThat(pending.firstTime()).isTrue());
    }

    // ------------------------------------------------------------------ prawa z RODO (D25)

    @Test
    void exportContainsAccountDataAndConsentHistory() {
        BuyerAccount account = activeBuyer();
        authentication.login("jana@boutique.cz", PASSWORD);

        BuyerSelfServiceUseCase.BuyerDataExport export = authentication.export(account.id());

        assertThat(export.account().email()).isEqualTo("jana@boutique.cz");
        assertThat(export.account().taxId()).isEqualTo("12345678");
        assertThat(export.emailVerifiedAt()).isEqualTo(START);
        assertThat(export.lastLoginAt()).isEqualTo(START);
        assertThat(export.consents()).hasSize(2);
    }

    @Test
    void buyerDeletesOwnAccountOnlyWithCorrectPassword() {
        BuyerAccount account = activeBuyer();
        authentication.login("jana@boutique.cz", PASSWORD);
        mailer.sent.clear();

        assertThatThrownBy(() -> authentication.deleteAccount(account.id(), "not my password"))
                .hasFieldOrPropertyWithValue("code", "identity.buyer.current-password-invalid");
        assertThat(accounts.store).hasSize(1);
        assertThat(accounts.only().failedLoginAttempts()).as("liczy się jak nieudane logowanie").isEqualTo(1);

        authentication.deleteAccount(account.id(), PASSWORD);
        assertThat(accounts.store).isEmpty();
        assertThat(events.published).containsExactly(new BuyerAccountDeleted(account.id().value()));
        assertThat(mailer.sent).as("potwierdzenie dopiero po commicie").isEmpty();
        hooks.commit();
        assertThat(mailer.sent).singleElement().satisfies(mail -> {
            assertThat(mail.kind()).isEqualTo("deleted");
            assertThat(mail.to()).isEqualTo("jana@boutique.cz");
            assertThat(mail.language()).isEqualTo(CatalogLanguage.CS);
        });
    }
}

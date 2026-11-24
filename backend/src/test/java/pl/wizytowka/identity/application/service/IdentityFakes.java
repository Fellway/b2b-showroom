package pl.wizytowka.identity.application.service;

import pl.wizytowka.identity.application.port.in.AccessToken;
import pl.wizytowka.identity.application.port.in.BuyerFilter;
import pl.wizytowka.identity.application.port.out.AccessTokenIssuer;
import pl.wizytowka.identity.application.port.out.BuyerAccountRepository;
import pl.wizytowka.identity.application.port.out.BuyerActionTokenRepository;
import pl.wizytowka.identity.application.port.out.BuyerMailer;
import pl.wizytowka.identity.application.port.out.BuyerRefreshTokenRepository;
import pl.wizytowka.identity.application.port.out.AdminUserRepository;
import pl.wizytowka.identity.application.port.out.PasswordHasher;
import pl.wizytowka.identity.application.port.out.RefreshTokenRepository;
import pl.wizytowka.identity.application.port.out.SecureTokenGenerator;
import pl.wizytowka.identity.domain.AdminRole;
import pl.wizytowka.identity.domain.AdminUser;
import pl.wizytowka.identity.domain.AdminUserId;
import pl.wizytowka.identity.domain.BuyerAccount;
import pl.wizytowka.identity.domain.BuyerAccountId;
import pl.wizytowka.identity.domain.BuyerActionPurpose;
import pl.wizytowka.identity.domain.BuyerActionToken;
import pl.wizytowka.identity.domain.BuyerStatus;
import pl.wizytowka.identity.domain.RefreshToken;
import pl.wizytowka.shared.application.PageQuery;
import pl.wizytowka.shared.application.PageResult;
import pl.wizytowka.shared.application.port.out.TransactionalHooks;
import pl.wizytowka.shared.domain.CatalogLanguage;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/** Proste implementacje portów wyjściowych w pamięci, do testów serwisów bez Springa i bazy. */
final class IdentityFakes {

    private IdentityFakes() {
    }

    static final class Users implements AdminUserRepository {
        final Map<AdminUserId, AdminUser> store = new LinkedHashMap<>();

        @Override
        public Optional<AdminUser> findById(AdminUserId id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Optional<AdminUser> findByEmail(String normalizedEmail) {
            return store.values().stream().filter(u -> u.email().equals(normalizedEmail)).findFirst();
        }

        @Override
        public List<AdminUser> findAll() {
            return store.values().stream().sorted(Comparator.comparing(AdminUser::email)).toList();
        }

        @Override
        public void save(AdminUser user) {
            store.put(user.id(), user);
        }

        @Override
        public boolean existsByEmail(String normalizedEmail) {
            return findByEmail(normalizedEmail).isPresent();
        }

        @Override
        public boolean existsAny() {
            return !store.isEmpty();
        }

        @Override
        public long countEnabledAdmins() {
            return store.values().stream().filter(AdminUser::isEnabledAdmin).count();
        }
    }

    static final class Tokens implements RefreshTokenRepository {
        final Map<String, RefreshToken> byHash = new LinkedHashMap<>();

        @Override
        public Optional<RefreshToken> findByHashForUpdate(String tokenHash) {
            return Optional.ofNullable(byHash.get(tokenHash));
        }

        @Override
        public void save(RefreshToken token) {
            byHash.put(token.tokenHash(), token);
        }

        @Override
        public void revokeFamily(UUID familyId, Instant now) {
            byHash.values().stream().filter(t -> t.familyId().equals(familyId)).forEach(t -> t.revoke(now));
        }

        @Override
        public void revokeAllOfUser(AdminUserId adminUserId, Instant now) {
            byHash.values().stream().filter(t -> t.subjectId().equals(adminUserId.value())).forEach(t -> t.revoke(now));
        }

        long activeCount() {
            return byHash.values().stream().filter(t -> !t.isSpent()).count();
        }
    }

    static final class Hasher implements PasswordHasher {
        final AtomicInteger matchCalls = new AtomicInteger();

        @Override
        public String hash(String rawPassword) {
            return "{fake}" + rawPassword;
        }

        @Override
        public boolean matches(String rawPassword, String passwordHash) {
            matchCalls.incrementAndGet();
            return passwordHash.equals("{fake}" + rawPassword);
        }
    }

    static final class Issuer implements AccessTokenIssuer {
        @Override
        public AccessToken issue(AdminUser user, Instant now) {
            return new AccessToken("access-" + user.email(), now.plus(Duration.ofMinutes(15)));
        }

        @Override
        public AccessToken issue(BuyerAccount buyer, Instant now) {
            return new AccessToken("buyer-access-" + buyer.id(), now.plus(Duration.ofMinutes(15)));
        }
    }

    // ------------------------------------------------------------------ konta kupców (D23)

    static final class BuyerAccounts implements BuyerAccountRepository {
        final Map<BuyerAccountId, BuyerAccount> store = new LinkedHashMap<>();

        @Override
        public Optional<BuyerAccount> findById(BuyerAccountId id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Optional<BuyerAccount> findByEmail(String normalizedEmail) {
            return store.values().stream().filter(a -> a.email().equals(normalizedEmail)).findFirst();
        }

        @Override
        public boolean existsByEmail(String normalizedEmail) {
            return findByEmail(normalizedEmail).isPresent();
        }

        @Override
        public void save(BuyerAccount account) {
            store.put(account.id(), account);
        }

        @Override
        public void delete(BuyerAccountId id) {
            store.remove(id);
        }

        @Override
        public PageResult<BuyerAccount> find(BuyerFilter filter, PageQuery page) {
            List<BuyerAccount> matching = store.values().stream()
                    .filter(a -> filter.status() == null || a.status() == filter.status())
                    .toList();
            return new PageResult<>(matching, page.page(), page.size(), matching.size());
        }

        @Override
        public long countByStatus(BuyerStatus status) {
            return store.values().stream().filter(a -> a.status() == status).count();
        }

        BuyerAccount only() {
            assert store.size() == 1 : "expected exactly one buyer account";
            return store.values().iterator().next();
        }
    }

    static final class BuyerSessions implements BuyerRefreshTokenRepository {
        final Map<String, RefreshToken> byHash = new LinkedHashMap<>();

        @Override
        public Optional<RefreshToken> findByHashForUpdate(String tokenHash) {
            return Optional.ofNullable(byHash.get(tokenHash));
        }

        @Override
        public void save(RefreshToken token) {
            byHash.put(token.tokenHash(), token);
        }

        @Override
        public void revokeFamily(UUID familyId, Instant now) {
            byHash.values().stream().filter(t -> t.familyId().equals(familyId)).forEach(t -> t.revoke(now));
        }

        @Override
        public void revokeAllOfBuyer(BuyerAccountId buyerAccountId, Instant now) {
            byHash.values().stream().filter(t -> t.subjectId().equals(buyerAccountId.value())).forEach(t -> t.revoke(now));
        }

        long activeCount() {
            return byHash.values().stream().filter(t -> !t.isSpent()).count();
        }
    }

    static final class ActionTokens implements BuyerActionTokenRepository {
        final Map<String, BuyerActionToken> byHash = new LinkedHashMap<>();

        @Override
        public Optional<BuyerActionToken> findByHashForUpdate(String tokenHash) {
            return Optional.ofNullable(byHash.get(tokenHash));
        }

        @Override
        public void save(BuyerActionToken token) {
            byHash.put(token.tokenHash(), token);
        }

        @Override
        public void invalidateUnused(BuyerAccountId buyerAccountId, BuyerActionPurpose purpose, Instant now) {
            byHash.values().stream()
                    .filter(t -> t.buyerAccountId().equals(buyerAccountId) && t.purpose() == purpose && t.usedAt() == null)
                    .forEach(t -> t.markUsed(now));
        }

        @Override
        public Optional<Instant> lastIssuedAt(BuyerAccountId buyerAccountId, BuyerActionPurpose purpose) {
            return byHash.values().stream()
                    .filter(t -> t.buyerAccountId().equals(buyerAccountId) && t.purpose() == purpose)
                    .map(BuyerActionToken::createdAt)
                    .max(Comparator.naturalOrder());
        }
    }

    /** Zapamiętuje wysłane e-maile (rodzaj, adresat, surowy token z linku). */
    static final class RecordingMailer implements BuyerMailer {
        record Sent(String kind, String to, CatalogLanguage language, String token) {
        }

        final List<Sent> sent = new ArrayList<>();

        @Override
        public void sendEmailVerification(String to, CatalogLanguage language, String rawToken) {
            sent.add(new Sent("verify", to, language, rawToken));
        }

        @Override
        public void sendAccountAlreadyExists(String to, CatalogLanguage language) {
            sent.add(new Sent("exists", to, language, null));
        }

        @Override
        public void sendAccountApproved(String to, CatalogLanguage language) {
            sent.add(new Sent("approved", to, language, null));
        }

        @Override
        public void sendAccountRejected(String to, CatalogLanguage language) {
            sent.add(new Sent("rejected", to, language, null));
        }

        @Override
        public void sendPasswordReset(String to, CatalogLanguage language, String rawToken) {
            sent.add(new Sent("reset", to, language, rawToken));
        }

        @Override
        public void notifyStaffAboutPendingBuyer(List<String> staffEmails) {
            staffEmails.forEach(to -> sent.add(new Sent("staff", to, CatalogLanguage.PL, null)));
        }

        List<String> kinds() {
            return sent.stream().map(Sent::kind).toList();
        }

        String lastToken(String kind) {
            return sent.stream().filter(s -> s.kind().equals(kind)).reduce((a, b) -> b).orElseThrow().token();
        }
    }

    /** Akcje „po commicie” wykonywane dopiero na żądanie testu (jak w transakcji). */
    static final class DeferredHooks implements TransactionalHooks {
        private final List<Runnable> onCommit = new ArrayList<>();

        @Override
        public void afterCommit(Runnable action) {
            onCommit.add(action);
        }

        @Override
        public void afterRollback(Runnable action) {
        }

        void commit() {
            List<Runnable> actions = List.copyOf(onCommit);
            onCommit.clear();
            actions.forEach(Runnable::run);
        }

        void rollback() {
            onCommit.clear();
        }
    }

    static final class SequentialTokens implements SecureTokenGenerator {
        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public String generate() {
            return "refresh-" + counter.incrementAndGet();
        }
    }

    static AdminUser user(String email, AdminRole role, String password) {
        return AdminUser.create(email, email, role, "{fake}" + password);
    }
}

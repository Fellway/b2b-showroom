package pl.wizytowka.identity.application.service;

import pl.wizytowka.identity.application.port.in.AccessToken;
import pl.wizytowka.identity.application.port.out.AccessTokenIssuer;
import pl.wizytowka.identity.application.port.out.AdminUserRepository;
import pl.wizytowka.identity.application.port.out.PasswordHasher;
import pl.wizytowka.identity.application.port.out.RefreshTokenRepository;
import pl.wizytowka.identity.application.port.out.SecureTokenGenerator;
import pl.wizytowka.identity.domain.AdminRole;
import pl.wizytowka.identity.domain.AdminUser;
import pl.wizytowka.identity.domain.AdminUserId;
import pl.wizytowka.identity.domain.RefreshToken;

import java.time.Duration;
import java.time.Instant;
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
            byHash.values().stream().filter(t -> t.adminUserId().equals(adminUserId)).forEach(t -> t.revoke(now));
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

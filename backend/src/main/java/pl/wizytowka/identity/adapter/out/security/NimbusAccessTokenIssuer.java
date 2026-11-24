package pl.wizytowka.identity.adapter.out.security;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;
import pl.wizytowka.config.security.JwtProperties;
import pl.wizytowka.identity.application.port.in.AccessToken;
import pl.wizytowka.identity.application.port.out.AccessTokenIssuer;
import pl.wizytowka.identity.domain.AdminUser;
import pl.wizytowka.identity.domain.BuyerAccount;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Access tokeny panelu i kupców podpisuje ten sam klucz; rodzaj konta wyznacza rola w claimie {@code roles}
 * ({@code ADMIN}/{@code EDITOR} albo {@code BUYER}) — reguły w SecurityConfig nie dopuszczają tokenu kupca do panelu
 * i odwrotnie. Claim {@code typ} ułatwia diagnostykę.
 */
@Component
class NimbusAccessTokenIssuer implements AccessTokenIssuer {

    static final String BUYER_ROLE = "BUYER";

    private final JwtEncoder encoder;
    private final JwtProperties properties;

    NimbusAccessTokenIssuer(JwtEncoder encoder, JwtProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    @Override
    public AccessToken issue(AdminUser user, Instant now) {
        return encode(JwtClaimsSet.builder()
                .subject(user.id().value().toString())
                .claim("typ", "admin")
                .claim("email", user.email())
                .claim("name", user.displayName())
                .claim("roles", List.of(user.role().name())), now);
    }

    @Override
    public AccessToken issue(BuyerAccount buyer, Instant now) {
        return encode(JwtClaimsSet.builder()
                .subject(buyer.id().value().toString())
                .claim("typ", "buyer")
                .claim("roles", List.of(BUYER_ROLE)), now);
    }

    private AccessToken encode(JwtClaimsSet.Builder claims, Instant now) {
        Instant expiresAt = now.plus(properties.accessTokenTtl());
        JwtClaimsSet claimsSet = claims
                .issuer(properties.issuer())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claimsSet)).getTokenValue();
        return new AccessToken(token, expiresAt);
    }
}

package pl.wizytowka.identity.adapter.out.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.stereotype.Component;
import pl.wizytowka.identity.application.port.out.PasswordHasher;

import java.util.Map;

/**
 * Hashowanie haseł: PBKDF2-HMAC-SHA256, 600 000 iteracji (rekomendacja OWASP), sól 16 B.
 * Wybrane zamiast bcrypt, bo bcrypt obcina hasła po 72 bajtach, a polityka dopuszcza 128 znaków;
 * Argon2 wymagałby dodatkowej zależności (BouncyCastle). Prefiks {id} pozwala później zmienić algorytm
 * bez unieważniania istniejących hashy.
 */
@Component
class SpringPasswordHasher implements PasswordHasher {

    private static final String PBKDF2_ID = "pbkdf2";

    private final PasswordEncoder encoder;

    SpringPasswordHasher() {
        Pbkdf2PasswordEncoder pbkdf2 = new Pbkdf2PasswordEncoder("", 16, 600_000,
                Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256);
        // base64 zamiast hex: wynik mieści się w kolumnie password_hash varchar(100)
        pbkdf2.setEncodeHashAsBase64(true);
        this.encoder = new DelegatingPasswordEncoder(PBKDF2_ID,
                Map.of(PBKDF2_ID, pbkdf2, "bcrypt", new BCryptPasswordEncoder()));
    }

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        try {
            return encoder.matches(rawPassword, passwordHash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}

package pl.wizytowka.shared.adapter.in.web.media;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import pl.wizytowka.shared.application.port.in.PublicMediaQuery;
import pl.wizytowka.shared.application.port.in.PublicMediaQuery.MediaFile;

import java.time.Duration;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Publiczne serwowanie zdjęć produktów. Udostępnia WYŁĄCZNIE klucze z prefiksem {@code products/}
 * (sprawdzane tu i w {@link PublicMediaQuery}) — pozostałe pliki, np. PDF-y zestawień z danymi klientów,
 * są niedostępne tą drogą.
 * Klucze są unikalne dla każdego uploadu, więc odpowiedź może być cache'owana bezterminowo.
 */
@RestController
class MediaController {

    /** Segmenty ze znaków bezpiecznych, rozdzielone '/' lub '.'; wyklucza "..", "//", ukryte pliki i backslash. */
    private static final Pattern SAFE_KEY = Pattern.compile("^products/[A-Za-z0-9_-]+(?:[/.][A-Za-z0-9_-]+)*$");
    private static final int MAX_KEY_LENGTH = 512;
    private static final CacheControl CACHE = CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable();

    private final PublicMediaQuery media;

    MediaController(PublicMediaQuery media) {
        this.media = media;
    }

    @GetMapping("/api/v1/media/{*key}")
    ResponseEntity<Resource> media(@PathVariable String key) {
        String normalized = key.startsWith("/") ? key.substring(1) : key;
        if (normalized.length() > MAX_KEY_LENGTH || !SAFE_KEY.matcher(normalized).matches()) {
            return ResponseEntity.notFound().build();
        }
        Optional<MediaFile> file = media.open(normalized);
        if (file.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        MediaFile stored = file.get();
        // ResourceHttpMessageConverter zamyka strumień po zapisaniu odpowiedzi
        return ResponseEntity.ok()
                .cacheControl(CACHE)
                .contentType(MediaType.parseMediaType(stored.contentType()))
                .contentLength(stored.sizeBytes())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().build().toString())
                .body(new InputStreamResource(stored.content()));
    }
}

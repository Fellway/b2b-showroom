package pl.wizytowka.shared.application.port.in;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * Odczyt publicznie dostępnych plików (zdjęć produktów). Pliki prywatne — np. PDF-y zestawień
 * z danymi klientów — nigdy nie są zwracane tą drogą.
 */
public interface PublicMediaQuery {

    /** @return plik lub pusty wynik, gdy nie istnieje albo klucz nie wskazuje publicznego zasobu */
    Optional<MediaFile> open(String key);

    /** Strumień musi zostać zamknięty przez wywołującego. */
    record MediaFile(InputStream content, long sizeBytes, String contentType) implements AutoCloseable {

        @Override
        public void close() throws IOException {
            content.close();
        }
    }
}

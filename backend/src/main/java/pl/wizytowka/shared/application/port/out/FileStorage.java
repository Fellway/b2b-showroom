package pl.wizytowka.shared.application.port.out;

import java.io.InputStream;
import java.util.Optional;

/**
 * Magazyn plików binarnych (zdjęcia produktów, wygenerowane PDF-y). Implementacje: dysk lokalny, S3.
 * Klucze są zawsze generowane przez serwer.
 */
public interface FileStorage {

    void store(String key, InputStream content, long sizeBytes, String contentType);

    Optional<StoredFile> open(String key);

    void delete(String key);

    /** Strumień musi zostać zamknięty przez wywołującego. */
    record StoredFile(InputStream content, long sizeBytes, String contentType) implements AutoCloseable {

        @Override
        public void close() throws java.io.IOException {
            content.close();
        }
    }
}

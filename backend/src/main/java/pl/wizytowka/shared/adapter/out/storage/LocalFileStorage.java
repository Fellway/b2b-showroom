package pl.wizytowka.shared.adapter.out.storage;

import org.springframework.stereotype.Component;
import pl.wizytowka.shared.application.port.out.FileStorage;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Optional;

/**
 * Magazyn plików na dysku lokalnym (dev / pojedyncza instancja). Adapter S3 zastąpi go bez zmian w aplikacji.
 * Każdy klucz jest rozwiązywany względem katalogu bazowego i odrzucany, jeśli wychodzi poza niego (path traversal).
 */
@Component
class LocalFileStorage implements FileStorage {

    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "jpg", "image/jpeg", "png", "image/png", "webp", "image/webp", "avif", "image/avif", "pdf", "application/pdf");

    private final Path root;

    LocalFileStorage(StorageProperties properties) {
        this.root = properties.localRoot().toAbsolutePath().normalize();
    }

    @Override
    public void store(String key, InputStream content, long sizeBytes, String contentType) {
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Path temp = Files.createTempFile(target.getParent(), ".upload-", ".tmp");
            try {
                Files.copy(content, temp, StandardCopyOption.REPLACE_EXISTING);
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot store file " + key, e);
        }
    }

    @Override
    public Optional<StoredFile> open(String key) {
        Path file = resolve(key);
        try {
            InputStream stream = Files.newInputStream(file);
            return Optional.of(new StoredFile(stream, Files.size(file), contentTypeOf(key)));
        } catch (NoSuchFileException e) {
            return Optional.empty();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read file " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot delete file " + key, e);
        }
    }

    private Path resolve(String key) {
        if (key == null || key.isBlank() || key.contains("\\") || key.contains("\0")) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        Path resolved = root.resolve(key).normalize();
        if (!resolved.startsWith(root) || resolved.equals(root)) {
            throw new IllegalArgumentException("Storage key escapes storage root");
        }
        return resolved;
    }

    private static String contentTypeOf(String key) {
        String extension = key.substring(key.lastIndexOf('.') + 1).toLowerCase(java.util.Locale.ROOT);
        return CONTENT_TYPES.getOrDefault(extension, "application/octet-stream");
    }
}

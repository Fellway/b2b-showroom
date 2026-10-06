package pl.wizytowka.catalog.application.service;

import pl.wizytowka.shared.application.port.out.FileStorage;
import pl.wizytowka.shared.application.port.out.TransactionalHooks;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Magazyn plików w pamięci + hooki transakcyjne, które wykonują akcje dopiero na żądanie testu. */
final class RecordingFileStorage implements FileStorage, TransactionalHooks {

    final Map<String, byte[]> files = new HashMap<>();
    final List<Runnable> pendingAfterCommit = new ArrayList<>();
    final List<Runnable> pendingAfterRollback = new ArrayList<>();

    @Override
    public void store(String key, InputStream content, long sizeBytes, String contentType) {
        try {
            files.put(key, content.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Optional<StoredFile> open(String key) {
        return Optional.ofNullable(files.get(key))
                .map(bytes -> new StoredFile(new java.io.ByteArrayInputStream(bytes), bytes.length, "application/octet-stream"));
    }

    @Override
    public void delete(String key) {
        files.remove(key);
    }

    @Override
    public void afterCommit(Runnable action) {
        pendingAfterCommit.add(action);
    }

    @Override
    public void afterRollback(Runnable action) {
        pendingAfterRollback.add(action);
    }

    void commit() {
        pendingAfterCommit.forEach(Runnable::run);
        pendingAfterCommit.clear();
        pendingAfterRollback.clear();
    }

    void rollback() {
        pendingAfterRollback.forEach(Runnable::run);
        pendingAfterCommit.clear();
        pendingAfterRollback.clear();
    }
}

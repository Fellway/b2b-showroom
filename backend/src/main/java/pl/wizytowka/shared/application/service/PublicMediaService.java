package pl.wizytowka.shared.application.service;

import org.springframework.stereotype.Service;
import pl.wizytowka.shared.application.port.in.PublicMediaQuery;
import pl.wizytowka.shared.application.port.out.FileStorage;

import java.util.Optional;

@Service
class PublicMediaService implements PublicMediaQuery {

    /** Jedyny publiczny obszar magazynu plików. */
    static final String PUBLIC_PREFIX = "products/";

    private final FileStorage storage;

    PublicMediaService(FileStorage storage) {
        this.storage = storage;
    }

    @Override
    public Optional<MediaFile> open(String key) {
        if (key == null || !key.startsWith(PUBLIC_PREFIX) || key.contains("..")) {
            return Optional.empty();
        }
        return storage.open(key).map(file -> new MediaFile(file.content(), file.sizeBytes(), file.contentType()));
    }
}

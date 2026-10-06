package pl.wizytowka.linesheet.application.view;

import java.io.IOException;
import java.io.InputStream;

/** Plik PDF do pobrania; strumień zamyka wywołujący. */
public record LineSheetPdf(InputStream content, long sizeBytes, String fileName) implements AutoCloseable {

    @Override
    public void close() throws IOException {
        content.close();
    }
}

package pl.wizytowka.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import pl.wizytowka.linesheet.application.port.out.LineSheetDocumentRenderer;
import pl.wizytowka.linesheet.application.port.out.LineSheetMailer;
import pl.wizytowka.linesheet.application.view.LineSheetDocument;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Zastępcze adaptery generowania PDF i wysyłki e-mail dla testów pełnego kontekstu.
 * Prawdziwe adaptery dochodzą w Etapie 3; wtedy testy integracyjne mogą przejść na nie (z GreenMail).
 */
@TestConfiguration(proxyBeanMethods = false)
public class LineSheetOutputFakesConfiguration {

    public static final byte[] FAKE_PDF = "%PDF-1.7\n% fake line sheet\n%%EOF".getBytes(StandardCharsets.US_ASCII);

    @Bean
    LineSheetDocumentRenderer fakeLineSheetRenderer() {
        return document -> FAKE_PDF.clone();
    }

    @Bean
    RecordingLineSheetMailer fakeLineSheetMailer() {
        return new RecordingLineSheetMailer();
    }

    public static class RecordingLineSheetMailer implements LineSheetMailer {

        private final List<LineSheetDocument> sent = new CopyOnWriteArrayList<>();

        @Override
        public void send(LineSheetDocument document, byte[] pdf) {
            sent.add(document);
        }

        public List<LineSheetDocument> sent() {
            return List.copyOf(sent);
        }
    }
}

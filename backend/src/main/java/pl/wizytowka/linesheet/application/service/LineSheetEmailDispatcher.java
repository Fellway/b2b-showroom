package pl.wizytowka.linesheet.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pl.wizytowka.analytics.application.port.in.RecordAnalyticsEventsUseCase;
import pl.wizytowka.analytics.application.port.in.ServerEvent;
import pl.wizytowka.analytics.domain.AnalyticsEventType;
import pl.wizytowka.linesheet.application.port.out.LineSheetMailer;
import pl.wizytowka.linesheet.application.view.LineSheetDocument;
import pl.wizytowka.linesheet.domain.LineSheetId;

import java.util.Map;

/**
 * Wysyła zestawienie poza transakcją biznesową (po jej zatwierdzeniu) i zapisuje wynik w nowej transakcji.
 * Etap 3 przeniesie wysyłkę do kolejki asynchronicznej z ponowieniami.
 */
@Service
class LineSheetEmailDispatcher {

    private static final Logger log = LoggerFactory.getLogger(LineSheetEmailDispatcher.class);

    private final LineSheetMailer mailer;
    private final LineSheetDeliveryRecorder recorder;
    private final RecordAnalyticsEventsUseCase analytics;

    LineSheetEmailDispatcher(LineSheetMailer mailer, LineSheetDeliveryRecorder recorder, RecordAnalyticsEventsUseCase analytics) {
        this.mailer = mailer;
        this.recorder = recorder;
        this.analytics = analytics;
    }

    void deliver(LineSheetId id, LineSheetDocument document, byte[] pdf) {
        try {
            mailer.send(document, pdf);
        } catch (RuntimeException e) {
            log.warn("Sending line sheet {} failed", document.referenceNumber(), e);
            recorder.recordFailure(id, e.getClass().getSimpleName() + ": " + e.getMessage());
            return;
        }
        recorder.recordSuccess(id);
        analytics.recordServerEvent(new ServerEvent(AnalyticsEventType.LINE_SHEET_EMAILED, null, document.language(),
                null, null, id.value(), Map.of()));
    }
}

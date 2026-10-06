package pl.wizytowka.linesheet.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pl.wizytowka.linesheet.application.port.out.LineSheetRepository;
import pl.wizytowka.linesheet.domain.LineSheet;
import pl.wizytowka.linesheet.domain.LineSheetId;
import pl.wizytowka.shared.domain.ResourceNotFoundException;

import java.time.Clock;

/** Zapis wyniku wysyłki w osobnej transakcji (wywoływany po zatwierdzeniu transakcji tworzącej). */
@Service
class LineSheetDeliveryRecorder {

    private final LineSheetRepository lineSheets;
    private final Clock clock;

    LineSheetDeliveryRecorder(LineSheetRepository lineSheets, Clock clock) {
        this.lineSheets = lineSheets;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(LineSheetId id) {
        LineSheet sheet = load(id);
        sheet.markEmailSent(clock.instant());
        lineSheets.save(sheet);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(LineSheetId id, String error) {
        LineSheet sheet = load(id);
        sheet.markEmailFailed(error);
        lineSheets.save(sheet);
    }

    private LineSheet load(LineSheetId id) {
        return lineSheets.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("linesheet.not-found", "Line sheet not found"));
    }
}

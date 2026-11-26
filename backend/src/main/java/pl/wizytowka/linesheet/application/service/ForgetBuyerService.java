package pl.wizytowka.linesheet.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pl.wizytowka.linesheet.application.port.in.ForgetBuyerUseCase;
import pl.wizytowka.linesheet.application.port.out.LineSheetRepository;
import pl.wizytowka.shared.application.port.out.FileStorage;
import pl.wizytowka.shared.application.port.out.TransactionalHooks;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
class ForgetBuyerService implements ForgetBuyerUseCase {

    private final LineSheetRepository lineSheets;
    private final FileStorage fileStorage;
    private final TransactionalHooks transactionalHooks;

    ForgetBuyerService(LineSheetRepository lineSheets, FileStorage fileStorage, TransactionalHooks transactionalHooks) {
        this.lineSheets = lineSheets;
        this.fileStorage = fileStorage;
        this.transactionalHooks = transactionalHooks;
    }

    /** Wywoływane w transakcji usunięcia konta — pliki PDF znikają dopiero po jej zatwierdzeniu. */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int forgetBuyer(UUID buyerAccountId) {
        Objects.requireNonNull(buyerAccountId, "buyerAccountId");
        List<String> pdfKeys = lineSheets.anonymizeOfBuyer(buyerAccountId);
        transactionalHooks.afterCommit(() -> pdfKeys.forEach(fileStorage::delete));
        return pdfKeys.size();
    }
}

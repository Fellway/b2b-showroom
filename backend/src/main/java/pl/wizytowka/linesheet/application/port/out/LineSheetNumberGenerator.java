package pl.wizytowka.linesheet.application.port.out;

/** Kolejne numery zestawień; bezpieczne przy współbieżności (luki są dopuszczalne). */
public interface LineSheetNumberGenerator {

    long nextSequenceNumber();
}

package pl.wizytowka.shared.application;

import pl.wizytowka.shared.domain.ConcurrentModificationException;

public final class Versions {

    private Versions() {
    }

    /** Odrzuca zmianę wykonaną na nieaktualnej wersji zasobu (ktoś inny zapisał go w międzyczasie). */
    public static void require(long expected, long actual) {
        if (expected != actual) {
            throw new ConcurrentModificationException("data.concurrent-modification",
                    "The resource was modified by someone else; reload and try again");
        }
    }
}

package pl.wizytowka.identity.application.port.out;

public interface SecureTokenGenerator {

    /** Losowa wartość o 256 bitach entropii, zakodowana base64url bez paddingu. */
    String generate();
}

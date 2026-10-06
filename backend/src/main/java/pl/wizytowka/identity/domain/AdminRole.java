package pl.wizytowka.identity.domain;

public enum AdminRole {
    /** Pełny dostęp, łącznie z zarządzaniem kontami administratorów. */
    ADMIN,
    /** Zarządzanie katalogiem i treściami, bez dostępu do kont. */
    EDITOR
}

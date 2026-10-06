package pl.wizytowka.analytics.domain;

public enum AnalyticsEventType {
    PAGE_VIEW(true),
    PRODUCT_VIEW(true),
    SELECTION_ADDED(true),
    SELECTION_REMOVED(true),
    LINE_SHEET_GENERATED(false),
    CONTACT_CLICKED(true);

    private final boolean clientReportable;

    AnalyticsEventType(boolean clientReportable) {
        this.clientReportable = clientReportable;
    }

    /** Czy zdarzenie może zgłosić przeglądarka. Pozostałe rejestruje wyłącznie serwer (nie da się ich sfałszować z zewnątrz). */
    public boolean isClientReportable() {
        return clientReportable;
    }
}

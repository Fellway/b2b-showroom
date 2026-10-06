-- =====================================================================
-- Rezygnacja z wysyłki e-mail zestawień (decyzja produktowa, Etap 3)
--
-- Aplikacja nie wysyła żadnych wiadomości: klient pobiera PDF, a z opiekunem handlowym
-- kontaktuje się przez widoczny e-mail / telefon. Adres e-mail klienta zostaje w line_sheet
-- wyłącznie jako opcjonalna dana kontaktowa (zgoda RODO nadal wymagana — ck_line_sheet_privacy).
-- =====================================================================

-- ---------------------------------------------------------------------
-- ZESTAWIENIA — usunięcie stanu wysyłki e-mail
-- ---------------------------------------------------------------------
drop index if exists ix_line_sheet_email_pending;

alter table line_sheet
    drop constraint ck_line_sheet_email_status,
    drop constraint ck_line_sheet_email_required,
    drop constraint ck_line_sheet_email_attempts,
    drop column email_status,
    drop column email_attempts,
    drop column email_sent_at,
    drop column email_last_error;

-- ---------------------------------------------------------------------
-- ANALITYKA — typ zdarzenia LINE_SHEET_EMAILED przestaje istnieć
-- ---------------------------------------------------------------------
delete from analytics_event where event_type = 'LINE_SHEET_EMAILED';

alter table analytics_event drop constraint ck_analytics_event_type;
alter table analytics_event add constraint ck_analytics_event_type check (event_type in (
    'PAGE_VIEW', 'PRODUCT_VIEW', 'SELECTION_ADDED', 'SELECTION_REMOVED',
    'LINE_SHEET_GENERATED', 'CONTACT_CLICKED'));

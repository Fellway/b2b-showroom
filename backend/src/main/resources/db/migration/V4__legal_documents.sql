-- =====================================================================
-- Dokumenty prawne i rejestr zgód kupców (D25).
-- Regulamin i polityka prywatności są wersjonowane: marka edytuje szkic w panelu
-- i publikuje go jako kolejną wersję. Opublikowana treść jest niezmienna, a rejestr
-- zgód wskazuje konkretną wersję — da się wykazać, co kupiec zaakceptował (art. 7 ust. 1 RODO).
-- =====================================================================

create table legal_document_version (
    id                   uuid         primary key,
    document_type        varchar(20)  not null,
    status               varchar(12)  not null,
    version_number       integer,                       -- nadawany przy publikacji
    acceptance_required  boolean      not null default false,
    published_at         timestamptz,
    published_by         uuid references admin_user (id) on delete set null,
    created_at           timestamptz  not null,
    updated_at           timestamptz  not null,
    version              bigint       not null default 0,
    constraint ck_legal_document_type check (document_type in ('TERMS', 'PRIVACY_POLICY')),
    constraint ck_legal_document_status check (status in ('DRAFT', 'PUBLISHED')),
    constraint ck_legal_document_published check (
        (status = 'DRAFT' and version_number is null and published_at is null)
        or (status = 'PUBLISHED' and version_number >= 1 and published_at is not null)),
    constraint ck_legal_document_acceptance check (document_type = 'TERMS' or not acceptance_required),
    constraint uq_legal_document_number unique (document_type, version_number)
);
-- najwyżej jeden szkic na rodzaj dokumentu
create unique index uq_legal_document_draft on legal_document_version (document_type) where status = 'DRAFT';

-- Treść wersji w językach interfejsu (Markdown w ograniczonym zakresie, renderowany bez HTML-a).
create table legal_document_text (
    version_id      uuid          not null references legal_document_version (id) on delete cascade,
    language        varchar(2)    not null,
    title           varchar(200)  not null,
    body            text          not null,
    change_summary  varchar(1000),
    primary key (version_id, language),
    constraint ck_legal_document_text_language check (language in ('pl', 'en', 'cs', 'uk')),
    constraint ck_legal_document_text_body check (length(body) <= 100000)
);

-- ---------------------------------------------------------------------
-- REJESTR ZGÓD KUPCÓW — tylko dopisywany. Bez adresu IP i User-Agenta (zasada projektu).
-- context: REGISTRATION (formularz rejestracji) | UPDATE (akceptacja nowej wersji po zalogowaniu).
-- Usunięcie konta usuwa też jego wpisy (prawo do usunięcia danych).
-- ---------------------------------------------------------------------
create table buyer_consent (
    id                    uuid         primary key,
    buyer_account_id      uuid         not null references buyer_account (id) on delete cascade,
    document_type         varchar(20)  not null,
    document_version_id   uuid         not null references legal_document_version (id),
    version_number        integer      not null,
    language              varchar(2)   not null,
    context               varchar(16)  not null,
    accepted_at           timestamptz  not null,
    constraint ck_buyer_consent_type check (document_type in ('TERMS', 'PRIVACY_POLICY')),
    constraint ck_buyer_consent_language check (language in ('pl', 'en', 'cs', 'uk')),
    constraint ck_buyer_consent_context check (context in ('REGISTRATION', 'UPDATE')),
    constraint uq_buyer_consent_version unique (buyer_account_id, document_version_id)
);
create index ix_buyer_consent_account on buyer_consent (buyer_account_id, document_type, version_number desc);

-- Same daty akceptacji (bez wersji dokumentu) zastępuje rejestr. Istniejące konta (wyłącznie dane
-- deweloperskie — produkcji jeszcze nie ma) zaakceptują obowiązującą wersję przy następnym logowaniu.
alter table buyer_account drop column terms_accepted_at;
alter table buyer_account drop column privacy_accepted_at;

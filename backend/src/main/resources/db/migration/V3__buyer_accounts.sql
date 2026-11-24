-- =====================================================================
-- Konta kupców (D23): ceny, EAN i zestawienia PDF tylko dla zalogowanych,
-- zaakceptowanych przez markę kupców. Konta kupców są niezależne od kont panelu.
-- =====================================================================

-- ---------------------------------------------------------------------
-- KONTA KUPCÓW
-- Cykl: PENDING_EMAIL → (potwierdzenie adresu) → PENDING_APPROVAL → (marka) → ACTIVE | REJECTED;
-- ACTIVE ⇄ SUSPENDED. Hasło wyłącznie jako hash (jak w admin_user).
-- ---------------------------------------------------------------------
create table buyer_account (
    id                     uuid         primary key,
    email                  varchar(254) not null,
    password_hash          varchar(100) not null,
    company_name           varchar(200) not null,
    country_code           varchar(2)   not null,
    tax_id                 varchar(32)  not null,      -- NIP / numer VAT bez prefiksu kraju
    contact_name           varchar(120) not null,
    phone                  varchar(32)  not null,
    language               varchar(2)   not null,      -- język e-maili i interfejsu
    status                 varchar(20)  not null,
    terms_accepted_at      timestamptz  not null,
    privacy_accepted_at    timestamptz  not null,
    email_verified_at      timestamptz,
    reviewed_at            timestamptz,
    reviewed_by            uuid references admin_user (id) on delete set null,
    review_note            varchar(500),               -- notatka wewnętrzna marki, nigdy nie trafia do kupca
    failed_login_attempts  integer      not null default 0,
    locked_until           timestamptz,
    last_login_at          timestamptz,
    created_at             timestamptz  not null,
    updated_at             timestamptz  not null,
    version                bigint       not null default 0,
    constraint ck_buyer_account_email_lowercase check (email = lower(email)),
    constraint ck_buyer_account_country check (country_code ~ '^[A-Z]{2}$'),
    constraint ck_buyer_account_language check (language in ('pl', 'en', 'cs', 'uk')),
    constraint ck_buyer_account_status check (status in
        ('PENDING_EMAIL', 'PENDING_APPROVAL', 'ACTIVE', 'REJECTED', 'SUSPENDED')),
    constraint ck_buyer_account_verified check (status = 'PENDING_EMAIL' or email_verified_at is not null),
    constraint ck_buyer_account_failed_attempts check (failed_login_attempts >= 0)
);
create unique index uq_buyer_account_email on buyer_account (email);
create index ix_buyer_account_status_created on buyer_account (status, created_at desc);

-- Sesje kupców: ta sama mechanika co admin_refresh_token (hash SHA-256, rodziny rotacji).
create table buyer_refresh_token (
    id                uuid        primary key,
    buyer_account_id  uuid        not null references buyer_account (id) on delete cascade,
    family_id         uuid        not null,
    token_hash        varchar(64) not null,
    issued_at         timestamptz not null,
    expires_at        timestamptz not null,
    used_at           timestamptz,
    revoked_at        timestamptz,
    constraint uq_buyer_refresh_token_hash unique (token_hash),
    constraint ck_buyer_refresh_token_hash check (token_hash ~ '^[0-9a-f]{64}$'),
    constraint ck_buyer_refresh_token_expiry check (expires_at > issued_at)
);
create index ix_buyer_refresh_token_family on buyer_refresh_token (family_id);
create index ix_buyer_refresh_token_account on buyer_refresh_token (buyer_account_id);

-- Jednorazowe tokeny z e-maili (potwierdzenie adresu, reset hasła) — wyłącznie hash, krótka ważność.
create table buyer_action_token (
    id                uuid        primary key,
    buyer_account_id  uuid        not null references buyer_account (id) on delete cascade,
    purpose           varchar(24) not null,
    token_hash        varchar(64) not null,
    created_at        timestamptz not null,
    expires_at        timestamptz not null,
    used_at           timestamptz,
    constraint uq_buyer_action_token_hash unique (token_hash),
    constraint ck_buyer_action_token_hash check (token_hash ~ '^[0-9a-f]{64}$'),
    constraint ck_buyer_action_token_purpose check (purpose in ('EMAIL_VERIFICATION', 'PASSWORD_RESET')),
    constraint ck_buyer_action_token_expiry check (expires_at > created_at)
);
create index ix_buyer_action_token_account on buyer_action_token (buyer_account_id, purpose, created_at desc);

-- ---------------------------------------------------------------------
-- ZESTAWIENIA — właściciel. Historyczne zestawienia (sprzed kont) i zestawienia
-- usuniętych kont zostają z buyer_account_id = NULL (snapshot danych kontaktowych zostaje).
-- ---------------------------------------------------------------------
alter table line_sheet add column buyer_account_id uuid references buyer_account (id) on delete set null;
create index ix_line_sheet_buyer on line_sheet (buyer_account_id, created_at desc) where buyer_account_id is not null;

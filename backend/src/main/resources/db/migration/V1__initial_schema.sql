-- =====================================================================
-- B2B Showroom — schemat początkowy
--
-- Konwencje:
--   * identyfikatory: uuid nadawane przez domenę (UUIDv7 — uporządkowane czasowo, przyjazne dla indeksów B-tree)
--   * języki: kody ISO 639-1 ('pl','en','cs','uk'); 'pl' jest językiem bazowym (fallback)
--   * kwoty: numeric(12,2) + kod waluty ISO 4217
--   * czas: timestamptz (aplikacja zapisuje w UTC)
--   * enumy jako varchar + CHECK (łatwiejsza ewolucja niż typy ENUM PostgreSQL)
--   * FK do product_color są DEFERRABLE i bez kaskad: dzieci agregatu produktu usuwa jawnie aplikacja
--     (agregat), a baza sprawdza spójność przy commicie — kolejność operacji w transakcji nie ma znaczenia
-- =====================================================================

-- ---------------------------------------------------------------------
-- KATEGORIE
-- ---------------------------------------------------------------------
create table category (
    id          uuid         primary key,
    parent_id   uuid references category (id) on delete restrict,
    slug        varchar(120) not null,
    sort_order  integer      not null default 0,
    visible     boolean      not null default true,
    created_at  timestamptz  not null,
    updated_at  timestamptz  not null,
    version     bigint       not null default 0,
    constraint uq_category_slug unique (slug),
    constraint ck_category_slug check (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
    constraint ck_category_not_own_parent check (parent_id is null or parent_id <> id)
);
create index ix_category_parent on category (parent_id);

create table category_translation (
    category_id uuid         not null references category (id) on delete cascade,
    locale      varchar(2)   not null,
    name        varchar(120) not null,
    description text,
    primary key (category_id, locale),
    constraint ck_category_translation_locale check (locale in ('pl', 'en', 'cs', 'uk'))
);

-- ---------------------------------------------------------------------
-- SŁOWNIKI: KOLORY I ROZMIARY
-- ---------------------------------------------------------------------
create table color (
    id          uuid        primary key,
    code        varchar(40) not null,              -- wewnętrzny kod koloru, np. 'BLK', 'NAVY-02'
    hex_value   varchar(7),                        -- próbka do UI, np. '#1C2340'
    sort_order  integer     not null default 0,
    created_at  timestamptz not null,
    updated_at  timestamptz not null,
    version     bigint      not null default 0,
    constraint uq_color_code unique (code),
    constraint ck_color_hex check (hex_value is null or hex_value ~ '^#[0-9A-F]{6}$')
);

create table color_translation (
    color_id uuid        not null references color (id) on delete cascade,
    locale   varchar(2)  not null,
    name     varchar(80) not null,
    primary key (color_id, locale),
    constraint ck_color_translation_locale check (locale in ('pl', 'en', 'cs', 'uk'))
);

-- Oznaczenia rozmiarów (XS, M, 38, ONE SIZE) są międzynarodowe, więc bez tłumaczeń.
create table size (
    id          uuid        primary key,
    code        varchar(16) not null,
    sort_order  integer     not null default 0,   -- kolejność na rozmiarówce: XS < S < M ...
    created_at  timestamptz not null,
    updated_at  timestamptz not null,
    version     bigint      not null default 0,
    constraint uq_size_code unique (code)
);

-- ---------------------------------------------------------------------
-- PRODUKTY (agregat: product + translation + color + variant + image)
-- ---------------------------------------------------------------------
create table product (
    id               uuid           primary key,
    category_id      uuid           not null references category (id) on delete restrict,
    slug             varchar(160)   not null,
    style_code       varchar(40)    not null,       -- numer modelu / indeks producenta
    status           varchar(16)    not null default 'DRAFT',
    wholesale_price  numeric(12, 2) not null,       -- cena hurtowa netto
    retail_price     numeric(12, 2) not null,       -- sugerowana cena detaliczna brutto (MSRP/RRP)
    currency         varchar(3)     not null default 'PLN',
    featured         boolean        not null default false,
    sort_order       integer        not null default 0,
    published_at     timestamptz,
    created_at       timestamptz    not null,
    updated_at       timestamptz    not null,
    version          bigint         not null default 0,
    constraint uq_product_slug unique (slug),
    constraint uq_product_style_code unique (style_code),
    constraint ck_product_slug check (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
    constraint ck_product_status check (status in ('DRAFT', 'PUBLISHED', 'HIDDEN', 'ARCHIVED')),
    constraint ck_product_wholesale_price check (wholesale_price > 0),
    constraint ck_product_retail_price check (retail_price > 0),
    constraint ck_product_currency check (currency ~ '^[A-Z]{3}$'),
    constraint ck_product_published_at check (status <> 'PUBLISHED' or published_at is not null)
);
create index ix_product_category on product (category_id);
-- Publiczny katalog odpytuje wyłącznie opublikowane produkty — indeks częściowy.
create index ix_product_published_listing on product (category_id, sort_order, id) where status = 'PUBLISHED';

create table product_translation (
    product_id            uuid         not null references product (id) on delete cascade,
    locale                varchar(2)   not null,
    name                  varchar(200) not null,
    description           text,
    material_composition  varchar(500),           -- np. '95% bawełna organiczna, 5% elastan'
    care_instructions     text,
    primary key (product_id, locale),
    constraint ck_product_translation_locale check (locale in ('pl', 'en', 'cs', 'uk'))
);

-- Poziom "Kolor" w hierarchii Produkt -> Kolor -> Rozmiar. Kolor identyfikowany w obrębie produktu przez color_id.
create table product_color (
    product_id  uuid    not null references product (id) on delete cascade,
    color_id    uuid    not null references color (id) on delete restrict,
    sort_order  integer not null default 0,
    primary key (product_id, color_id)
);
create index ix_product_color_color on product_color (color_id);

-- Poziom "Rozmiar" -> SKU/EAN. Jeden wiersz = jedna fizyczna pozycja handlowa.
create table product_variant (
    id          uuid        primary key,
    product_id  uuid        not null references product (id) on delete cascade,
    color_id    uuid        not null,
    size_id     uuid        not null references size (id) on delete restrict,
    sku         varchar(64) not null,
    ean         varchar(14) not null,       -- GTIN-8/12/13/14, suma kontrolna walidowana w domenie
    active      boolean     not null default true,
    created_at  timestamptz not null,
    updated_at  timestamptz not null,
    constraint fk_variant_product_color foreign key (product_id, color_id)
        references product_color (product_id, color_id) deferrable initially deferred,
    constraint uq_variant_color_size unique (product_id, color_id, size_id),
    constraint uq_variant_sku unique (sku),
    constraint uq_variant_ean unique (ean),
    constraint ck_variant_ean check (ean ~ '^([0-9]{8}|[0-9]{12,14})$')
);
create index ix_variant_size on product_variant (size_id);

create table product_image (
    id            uuid         primary key,
    product_id    uuid         not null references product (id) on delete cascade,
    color_id      uuid,                          -- null = zdjęcie ogólne produktu (np. lookbook)
    storage_key   varchar(512) not null,         -- klucz w S3 / ścieżka względna w local storage
    content_type  varchar(100) not null,
    width_px      integer,
    height_px     integer,
    size_bytes    bigint,
    sort_order    integer      not null default 0,   -- najniższy = zdjęcie główne
    created_at    timestamptz  not null,
    constraint uq_product_image_storage_key unique (storage_key),
    -- gwarancja, że zdjęcie koloru dotyczy koloru TEGO produktu
    constraint fk_product_image_color foreign key (product_id, color_id)
        references product_color (product_id, color_id) deferrable initially deferred,
    constraint ck_product_image_content_type check (content_type in ('image/jpeg', 'image/png', 'image/webp', 'image/avif')),
    constraint ck_product_image_dimensions check ((width_px is null or width_px > 0) and (height_px is null or height_px > 0))
);
create index ix_product_image_product on product_image (product_id, sort_order);

-- ---------------------------------------------------------------------
-- OPIEKUNOWIE HANDLOWI
-- ---------------------------------------------------------------------
create table sales_representative (
    id          uuid         primary key,
    full_name   varchar(120) not null,
    email       varchar(254) not null,
    phone       varchar(32),
    active      boolean      not null default true,
    sort_order  integer      not null default 0,
    created_at  timestamptz  not null,
    updated_at  timestamptz  not null,
    version     bigint       not null default 0,
    constraint ck_sales_representative_email_lowercase check (email = lower(email))
);
create unique index uq_sales_representative_email on sales_representative (email);

-- ---------------------------------------------------------------------
-- ZESTAWIENIA OFERTOWE (LINE SHEETS) — wygenerowane PDF-y
-- Pozycje przechowują SNAPSHOT danych z chwili generowania: zmiana ceny
-- czy usunięcie produktu nie może zmienić historii wysłanej oferty.
-- ---------------------------------------------------------------------
create sequence line_sheet_number_seq;

create table line_sheet (
    id                       uuid         primary key,
    public_id                uuid         not null,          -- losowy (v4) identyfikator do linków — nie ujawnia czasu utworzenia
    reference_number         varchar(24)  not null,          -- np. 'LS-2026-000042'
    locale                   varchar(2)   not null,
    currency                 varchar(3)   not null,
    company_name             varchar(200),
    contact_name             varchar(120),
    email                    varchar(254),
    phone                    varchar(32),
    tax_id                   varchar(32),                    -- NIP / VAT-UE
    message                  varchar(2000),
    privacy_accepted_at      timestamptz,
    sales_representative_id  uuid references sales_representative (id) on delete set null,
    visitor_id               uuid,                           -- anonimowe ID przeglądarki (jeśli klient wyraził zgodę)
    pdf_storage_key          varchar(512),
    pdf_generated_at         timestamptz,
    email_status             varchar(16)  not null default 'NOT_REQUESTED',
    email_attempts           integer      not null default 0,
    email_sent_at            timestamptz,
    email_last_error         varchar(500),
    created_at               timestamptz  not null,
    version                  bigint       not null default 0,
    constraint uq_line_sheet_public_id unique (public_id),
    constraint uq_line_sheet_reference unique (reference_number),
    constraint ck_line_sheet_locale check (locale in ('pl', 'en', 'cs', 'uk')),
    constraint ck_line_sheet_currency check (currency ~ '^[A-Z]{3}$'),
    constraint ck_line_sheet_email_status check (email_status in ('NOT_REQUESTED', 'PENDING', 'SENT', 'FAILED')),
    constraint ck_line_sheet_email_required check (email_status = 'NOT_REQUESTED' or email is not null),
    constraint ck_line_sheet_privacy check (email is null or privacy_accepted_at is not null),
    constraint ck_line_sheet_email_attempts check (email_attempts >= 0)
);
create index ix_line_sheet_created on line_sheet (created_at desc);
create index ix_line_sheet_email_pending on line_sheet (created_at) where email_status in ('PENDING', 'FAILED');

create table line_sheet_item (
    id                uuid           primary key,
    line_sheet_id     uuid           not null references line_sheet (id) on delete cascade,
    position          integer        not null,
    -- miękkie powiązania z katalogiem (NULL po usunięciu produktu, snapshot zostaje)
    product_id        uuid references product (id) on delete set null,
    variant_id        uuid references product_variant (id) on delete set null,
    -- snapshot
    style_code        varchar(40)    not null,
    product_name      varchar(200)   not null,
    color_name        varchar(80)    not null,
    size_code         varchar(16)    not null,
    sku               varchar(64)    not null,
    ean               varchar(14)    not null,
    wholesale_price   numeric(12, 2) not null,
    retail_price      numeric(12, 2) not null,
    image_storage_key varchar(512),
    quantity          integer,                    -- opcjonalna ilość orientacyjna
    constraint uq_line_sheet_item_position unique (line_sheet_id, position),
    constraint ck_line_sheet_item_quantity check (quantity is null or quantity > 0),
    constraint ck_line_sheet_item_prices check (wholesale_price > 0 and retail_price > 0)
);
create index ix_line_sheet_item_product on line_sheet_item (product_id);
create index ix_line_sheet_item_variant on line_sheet_item (variant_id);

-- ---------------------------------------------------------------------
-- ANALITYKA — append-only log zdarzeń
-- Brak FK do katalogu celowo: log musi przetrwać usunięcie produktu
-- i nie może blokować zapisów w katalogu. Nie zapisujemy adresów IP (RODO).
-- ---------------------------------------------------------------------
create table analytics_event (
    id             bigint generated by default as identity primary key,
    occurred_at    timestamptz  not null,
    event_type     varchar(32)  not null,
    visitor_id     uuid,
    session_id     uuid,
    locale         varchar(2),
    product_id     uuid,
    variant_id     uuid,
    line_sheet_id  uuid,
    path           varchar(512),
    referrer_host  varchar(255),
    properties     jsonb        not null default '{}'::jsonb,
    constraint ck_analytics_event_type check (event_type in (
        'PAGE_VIEW', 'PRODUCT_VIEW', 'SELECTION_ADDED', 'SELECTION_REMOVED',
        'LINE_SHEET_GENERATED', 'LINE_SHEET_EMAILED', 'CONTACT_CLICKED')),
    constraint ck_analytics_event_locale check (locale is null or locale in ('pl', 'en', 'cs', 'uk'))
);
-- BRIN: tani indeks dla kolumny rosnącej z czasem (zapytania zakresowe po dacie)
create index ix_analytics_event_occurred_brin on analytics_event using brin (occurred_at);
create index ix_analytics_event_type_time on analytics_event (event_type, occurred_at);
create index ix_analytics_event_product on analytics_event (product_id, event_type, occurred_at) where product_id is not null;

-- ---------------------------------------------------------------------
-- ADMINISTRATORZY I SESJE (JWT access token + rotowany refresh token)
-- ---------------------------------------------------------------------
create table admin_user (
    id                     uuid         primary key,
    email                  varchar(254) not null,
    password_hash          varchar(100) not null,   -- DelegatingPasswordEncoder z prefiksem {id}
    display_name           varchar(120) not null,
    role                   varchar(16)  not null,
    enabled                boolean      not null default true,
    failed_login_attempts  integer      not null default 0,
    locked_until           timestamptz,
    last_login_at          timestamptz,
    created_at             timestamptz  not null,
    updated_at             timestamptz  not null,
    version                bigint       not null default 0,
    constraint ck_admin_user_role check (role in ('ADMIN', 'EDITOR')),
    constraint ck_admin_user_email_lowercase check (email = lower(email)),
    constraint ck_admin_user_failed_attempts check (failed_login_attempts >= 0)
);
create unique index uq_admin_user_email on admin_user (email);

-- Refresh tokeny przechowujemy wyłącznie jako hash SHA-256. family_id grupuje kolejne rotacje
-- jednej sesji: ponowne użycie już zrotowanego tokenu unieważnia całą rodzinę (wykrycie kradzieży).
create table admin_refresh_token (
    id             uuid        primary key,
    admin_user_id  uuid        not null references admin_user (id) on delete cascade,
    family_id      uuid        not null,
    token_hash     varchar(64) not null,
    issued_at      timestamptz not null,
    expires_at     timestamptz not null,
    used_at        timestamptz,
    revoked_at     timestamptz,
    constraint uq_admin_refresh_token_hash unique (token_hash),
    constraint ck_admin_refresh_token_hash check (token_hash ~ '^[0-9a-f]{64}$'),
    constraint ck_admin_refresh_token_expiry check (expires_at > issued_at)
);
create index ix_admin_refresh_token_family on admin_refresh_token (family_id);
create index ix_admin_refresh_token_user on admin_refresh_token (admin_user_id);

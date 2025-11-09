CREATE TABLE country (
    id BIGSERIAL PRIMARY KEY,
    iso3 VARCHAR(3) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL
);

CREATE TABLE agreement (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    rvc_threshold NUMERIC(5,2)
);

CREATE TABLE agreement_party (
    agreement_id BIGINT NOT NULL REFERENCES agreement(id) ON DELETE CASCADE,
    country_iso3 VARCHAR(3) NOT NULL REFERENCES country(iso3),
    PRIMARY KEY (agreement_id, country_iso3)
);

CREATE TABLE hs_product (
    id BIGSERIAL PRIMARY KEY,
    destination_iso3 VARCHAR(3) NOT NULL REFERENCES country(iso3),
    hs_version VARCHAR(20) NOT NULL,
    hs_code VARCHAR(12) NOT NULL,
    hs_label VARCHAR(255) NOT NULL,
    CONSTRAINT uq_hs_product UNIQUE (destination_iso3, hs_version, hs_code)
);

CREATE TABLE tariff_rate (
    id BIGSERIAL PRIMARY KEY,
    importer_iso3 VARCHAR(3) NOT NULL REFERENCES country(iso3),
    origin_iso3 VARCHAR(3) REFERENCES country(iso3),
    hs_product_id BIGINT NOT NULL REFERENCES hs_product(id) ON DELETE CASCADE,
    basis VARCHAR(4) NOT NULL CHECK (basis IN ('MFN', 'PREF')),
    agreement_id BIGINT REFERENCES agreement(id) ON DELETE SET NULL,
    ad_valorem_rate NUMERIC(9,6),
    is_non_ad_valorem BOOLEAN NOT NULL DEFAULT FALSE,
    non_ad_valorem_text TEXT,
    source_ref TEXT,
    CONSTRAINT chk_tariff_basis_mfn_origin CHECK ((basis = 'MFN' AND origin_iso3 IS NULL) OR basis = 'PREF'),
    CONSTRAINT chk_tariff_basis_pref_origin CHECK (basis = 'MFN' OR (basis = 'PREF' AND origin_iso3 IS NOT NULL)),
    CONSTRAINT chk_tariff_basis_agreement CHECK (
        (basis = 'MFN' AND agreement_id IS NULL)
        OR (basis = 'PREF' AND agreement_id IS NOT NULL)
    )
);

CREATE INDEX idx_tariff_lookup_iso3 ON tariff_rate (importer_iso3, hs_product_id);
CREATE INDEX idx_pref_lookup_iso3 ON tariff_rate (importer_iso3, origin_iso3, hs_product_id);

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    about_me TEXT,
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    password VARCHAR(255) NOT NULL,
    admin BOOLEAN NOT NULL DEFAULT FALSE
);

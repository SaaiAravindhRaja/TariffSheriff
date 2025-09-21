CREATE TABLE country (
    id BIGSERIAL PRIMARY KEY,
    iso2 CHAR(2) NOT NULL UNIQUE,
    iso3 CHAR(3) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL
);

CREATE TABLE agreement (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('in_force', 'signed', 'inactive')),
    entered_into_force DATE
);

CREATE TABLE agreement_party (
    agreement_id BIGINT NOT NULL REFERENCES agreement(id) ON DELETE CASCADE,
    country_id BIGINT NOT NULL REFERENCES country(id) ON DELETE CASCADE,
    PRIMARY KEY (agreement_id, country_id)
);

CREATE TABLE hs_product (
    id BIGSERIAL PRIMARY KEY,
    destination_id BIGINT NOT NULL REFERENCES country(id) ON DELETE CASCADE,
    hs_version VARCHAR(20) NOT NULL,
    hs_code VARCHAR(10) NOT NULL,
    hs_label VARCHAR(255) NOT NULL,
    CONSTRAINT uq_hs_product UNIQUE (destination_id, hs_version, hs_code)
);

CREATE TABLE tariff_rate (
    id BIGSERIAL PRIMARY KEY,
    importer_id BIGINT NOT NULL REFERENCES country(id) ON DELETE CASCADE,
    origin_id BIGINT REFERENCES country(id) ON DELETE SET NULL,
    hs_product_id BIGINT NOT NULL REFERENCES hs_product(id) ON DELETE CASCADE,
    basis VARCHAR(4) NOT NULL CHECK (basis IN ('MFN', 'PREF')),
    agreement_id BIGINT REFERENCES agreement(id) ON DELETE SET NULL,
    rate_type VARCHAR(10) NOT NULL CHECK (rate_type IN ('ad_valorem', 'specific', 'compound')),
    ad_valorem_rate NUMERIC(9,6),
    specific_amount NUMERIC(19,4),
    specific_unit VARCHAR(32),
    valid_from DATE NOT NULL,
    valid_to DATE,
    source_ref TEXT,
    CONSTRAINT chk_tariff_validity CHECK (valid_to IS NULL OR valid_to >= valid_from),
    CONSTRAINT chk_tariff_basis_agreement CHECK (
        (basis = 'MFN' AND agreement_id IS NULL) OR
        (basis = 'PREF' AND agreement_id IS NOT NULL)
    )
);

CREATE INDEX idx_tariff_lookup ON tariff_rate (importer_id, hs_product_id, valid_from DESC);
CREATE INDEX idx_pref_lookup ON tariff_rate (importer_id, origin_id, hs_product_id, valid_from DESC);

CREATE TABLE vat (
    importer_id BIGINT PRIMARY KEY REFERENCES country(id) ON DELETE CASCADE,
    standard_rate NUMERIC(6,4) NOT NULL CHECK (standard_rate >= 0)
);

CREATE TABLE roo_rule (
    id BIGSERIAL PRIMARY KEY,
    agreement_id BIGINT NOT NULL REFERENCES agreement(id) ON DELETE CASCADE,
    hs_product_id BIGINT NOT NULL REFERENCES hs_product(id) ON DELETE CASCADE,
    method VARCHAR(50) NOT NULL,
    threshold VARCHAR(50) NOT NULL,
    requires_certificate BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX uq_roo_rule_agreement_product ON roo_rule (agreement_id, hs_product_id);

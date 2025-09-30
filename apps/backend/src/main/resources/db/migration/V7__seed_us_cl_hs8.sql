-- Seed additional country and HS product data for US/Chile scenario
-- Includes 8-digit HS codes that may share numbers across destinations

-- Countries
INSERT INTO country (id, iso2, iso3, name)
VALUES
    (4, 'US', 'USA', 'United States of America'),
    (5, 'CL', 'CHL', 'Republic of Chile')
ON CONFLICT (id) DO NOTHING;

INSERT INTO country (iso2, iso3, name)
VALUES
    ('JP', 'JPN', 'Japan'),
    ('DE', 'DEU', 'Germany')
ON CONFLICT (iso2) DO NOTHING;

-- Agreement between US and Chile with 30% RVC threshold
INSERT INTO agreement (id, name, type, rvc_threshold, status, entered_into_force)
VALUES (2, 'Chile-United States FTA', 'FTA', 30.00, 'in_force', DATE '2004-01-01')
ON CONFLICT (id) DO NOTHING;

-- HS products (reuse destination-specific catalog)
INSERT INTO hs_product (id, destination_id, hs_version, hs_code, hs_label)
VALUES
    (10, 4, '2017', '87038010', 'EV passenger cars, US market specific'),
    (11, 4, '2017', '87038020', 'EV passenger cars with extended range, US'),
    (12, 5, '2017', '87038010', 'EV passenger cars, Chile market'),
    (13, 5, '2017', '87038020', 'EV passenger cars with battery swap capability, Chile')
ON CONFLICT (id) DO NOTHING;

-- MFN rates (baseline)
INSERT INTO tariff_rate (
    id, importer_id, origin_id, hs_product_id, basis, agreement_id,
    rate_type, ad_valorem_rate, specific_amount, specific_unit,
    valid_from, valid_to, source_ref)
VALUES
    -- US baseline
    (20, 4, NULL, 10, 'MFN', NULL, 'ad_valorem', 0.055, NULL, NULL,
        DATE '2017-01-01', NULL, 'MFN baseline for US EV passenger cars (8-digit)'),
    (21, 4, NULL, 11, 'MFN', NULL, 'ad_valorem', 0.060, NULL, NULL,
        DATE '2017-01-01', NULL, 'MFN baseline for US EV long-range models'),
    -- Chile baseline
    (22, 5, NULL, 12, 'MFN', NULL, 'ad_valorem', 0.080, NULL, NULL,
        DATE '2017-01-01', NULL, 'MFN baseline for Chile EV passenger cars'),
    (23, 5, NULL, 13, 'MFN', NULL, 'ad_valorem', 0.085, NULL, NULL,
        DATE '2017-01-01', NULL, 'MFN baseline for Chile EV battery swap models')
ON CONFLICT (id) DO NOTHING;

-- Preferential rates under US-Chile agreement (zero duty when RVC >= 30%)
INSERT INTO tariff_rate (
    id, importer_id, origin_id, hs_product_id, basis, agreement_id,
    rate_type, ad_valorem_rate, specific_amount, specific_unit,
    valid_from, valid_to, source_ref)
VALUES
    (24, 4, 5, 10, 'PREF', 2, 'ad_valorem', 0.000, NULL, NULL,
        DATE '2017-01-01', NULL, 'Preferential rate for Chile-origin EVs entering US'),
    (25, 5, 4, 12, 'PREF', 2, 'ad_valorem', 0.000, NULL, NULL,
        DATE '2017-01-01', NULL, 'Preferential rate for US-origin EVs entering Chile'),
    (26, 4, 5, 11, 'PREF', 2, 'ad_valorem', 0.000, NULL, NULL,
        DATE '2017-01-01', NULL, 'Preferential rate for Chile-origin EV long-range models entering US'),
    (27, 5, 4, 13, 'PREF', 2, 'ad_valorem', 0.000, NULL, NULL,
        DATE '2017-01-01', NULL, 'Preferential rate for US-origin EV battery swap models entering Chile')
ON CONFLICT (id) DO NOTHING;

-- Additional MFN-only products sharing codes with different labels
INSERT INTO hs_product (destination_id, hs_version, hs_code, hs_label)
SELECT dest, version, code, label FROM (
    VALUES
        (1, '2022', '85076010', 'Lithium-ion battery packs (EU)'),
        (1, '2022', '85076020', 'Lithium-ion battery modules (EU)'),
        (4, '2022', '85076010', 'Lithium-ion battery packs (US)'),
        (4, '2022', '85076020', 'Lithium-ion battery modules (US)')
) AS data(dest, version, code, label)
ON CONFLICT (destination_id, hs_version, hs_code) DO NOTHING;

-- MFN rates for the battery products (example values)
INSERT INTO tariff_rate (
    importer_id, origin_id, hs_product_id, basis, agreement_id,
    rate_type, ad_valorem_rate, specific_amount, specific_unit,
    valid_from, valid_to, source_ref)
SELECT importer, origin, product_id, 'MFN', NULL, 'ad_valorem', rate, NULL, NULL,
       DATE '2019-01-01', NULL, source
FROM (
    VALUES
        (4, NULL::BIGINT, (SELECT id FROM hs_product WHERE destination_id = 4 AND hs_code = '85076010' AND hs_version = '2022'), 0.025, 'MFN for US lithium-ion battery packs'),
        (4, NULL::BIGINT, (SELECT id FROM hs_product WHERE destination_id = 4 AND hs_code = '85076020' AND hs_version = '2022'), 0.030, 'MFN for US lithium-ion battery modules'),
        (1, NULL::BIGINT, (SELECT id FROM hs_product WHERE destination_id = 1 AND hs_code = '85076010' AND hs_version = '2022'), 0.020, 'MFN for EU lithium-ion battery packs'),
        (1, NULL::BIGINT, (SELECT id FROM hs_product WHERE destination_id = 1 AND hs_code = '85076020' AND hs_version = '2022'), 0.028, 'MFN for EU lithium-ion battery modules')
) AS rates(importer, origin, product_id, rate, source)
ON CONFLICT DO NOTHING;

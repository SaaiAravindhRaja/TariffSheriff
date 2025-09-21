INSERT INTO country (id, iso2, iso3, name) VALUES
    (1, 'EU', 'EUR', 'European Union'),
    (2, 'KR', 'KOR', 'Republic of Korea'),
    (3, 'CN', 'CHN', 'China');

SELECT setval('country_id_seq', 3, true);

INSERT INTO agreement (id, name, type, status, entered_into_force) VALUES
    (1, 'EU–Korea FTA', 'FTA', 'in_force', DATE '2011-07-01');

SELECT setval('agreement_id_seq', 1, true);

INSERT INTO agreement_party (agreement_id, country_id) VALUES
    (1, 1),
    (1, 2);

INSERT INTO hs_product (id, destination_id, hs_version, hs_code, hs_label) VALUES
    (1, 1, '2022', '870380', 'EV passenger cars');

SELECT setval('hs_product_id_seq', 1, true);

INSERT INTO vat (importer_id, standard_rate) VALUES
    (1, 0.2000);

INSERT INTO tariff_rate (
    id,
    importer_id,
    origin_id,
    hs_product_id,
    basis,
    agreement_id,
    rate_type,
    ad_valorem_rate,
    specific_amount,
    specific_unit,
    valid_from,
    valid_to,
    source_ref
) VALUES
    (
        1,
        1,
        NULL,
        1,
        'MFN',
        NULL,
        'ad_valorem',
        0.1000,
        NULL,
        NULL,
        DATE '2022-01-01',
        NULL,
        'Mock MFN baseline for EV passenger cars'
    ),
    (
        2,
        1,
        2,
        1,
        'PREF',
        1,
        'ad_valorem',
        0.0000,
        NULL,
        NULL,
        DATE '2022-01-01',
        NULL,
        'Mock preferential rate under EU–Korea FTA'
    );

SELECT setval('tariff_rate_id_seq', 2, true);

INSERT INTO roo_rule (id, agreement_id, hs_product_id, method, threshold, requires_certificate) VALUES
    (1, 1, 1, 'RVC', '>=40%', TRUE);

SELECT setval('roo_rule_id_seq', 1, true);

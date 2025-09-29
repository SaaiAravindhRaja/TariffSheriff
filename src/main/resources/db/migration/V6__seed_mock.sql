INSERT INTO country (id, iso2, iso3, name) VALUES
    (4, 'US', 'USA', 'United States of America'),
    (5, 'CL', 'CHL', 'Chile');

INSERT INTO agreement (id, name, type, status, entered_into_force, rvc) VALUES
    (2, 'Chile-United States FTA', 'FTA', 'in_force', DATE '2004-01-01', 30);

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
        3,
        4,
        NULL,
        1,
        'MFN',
        NULL,
        'ad_valorem',
        0.06,
        NULL,
        NULL,
        DATE '2004-01-01',
        NULL,
        'Mock MFN baseline for EV passenger cars under Chile-United States FTA'
    ),
    (
        4,
        5,
        NULL,
        1,
        'MFN',
        NULL,
        'ad_valorem',
        0.06,
        NULL,
        NULL,
        DATE '2004-01-01',
        NULL,
        'Mock MFN baseline for EV passenger cars under Chile-United States FTA'
    ),
    (
        5,
        4,
        5,
        1,
        'PREF',
        2,
        'ad_valorem',
        0.0000,
        NULL,
        NULL,
        DATE '2004-01-01',
        NULL,
        'Mock preferential rate under Chile-United States FTA'
    ),
    (
        6,
        5,
        4,
        1,
        'PREF',
        2,
        'ad_valorem',
        0.0000,
        NULL,
        NULL,
        DATE '2004-01-01',
        NULL,
        'Mock preferential rate under Chile-United States FTA'
    );
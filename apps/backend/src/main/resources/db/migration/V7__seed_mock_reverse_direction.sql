-- Seed reverse-direction data so that importer = KR and origin = EU also returns rates

-- Ensure HS product for KR as importer (destination KR = id 2)
INSERT INTO hs_product (destination_id, hs_version, hs_code, hs_label)
VALUES (2, '2022', '870380', 'EV passenger cars')
ON CONFLICT (destination_id, hs_version, hs_code) DO NOTHING;

-- MFN baseline for KR importer on EV passenger cars
INSERT INTO tariff_rate (
    importer_id, origin_id, hs_product_id, basis, agreement_id,
    rate_type, ad_valorem_rate, specific_amount, specific_unit,
    valid_from, valid_to, source_ref
)
SELECT 2, NULL, hp.id, 'MFN', NULL,
       'ad_valorem', 0.1000, NULL, NULL,
       DATE '2022-01-01', NULL, 'Mock MFN baseline for EV passenger cars (KR importer)'
FROM hs_product hp
WHERE hp.destination_id = 2 AND hp.hs_version = '2022' AND hp.hs_code = '870380'
  AND NOT EXISTS (
    SELECT 1 FROM tariff_rate tr
    WHERE tr.importer_id = 2 AND tr.origin_id IS NULL AND tr.hs_product_id = hp.id AND tr.basis = 'MFN'
  );

-- Preferential rate under EU–Korea FTA for KR importer (origin EU = id 1)
INSERT INTO tariff_rate (
    importer_id, origin_id, hs_product_id, basis, agreement_id,
    rate_type, ad_valorem_rate, specific_amount, specific_unit,
    valid_from, valid_to, source_ref
)
SELECT 2, 1, hp.id, 'PREF', 1,
       'ad_valorem', 0.0000, NULL, NULL,
       DATE '2022-01-01', NULL, 'Mock preferential rate under EU–Korea FTA (KR importer)'
FROM hs_product hp
WHERE hp.destination_id = 2 AND hp.hs_version = '2022' AND hp.hs_code = '870380'
  AND NOT EXISTS (
    SELECT 1 FROM tariff_rate tr
    WHERE tr.importer_id = 2 AND tr.origin_id = 1 AND tr.hs_product_id = hp.id AND tr.basis = 'PREF'
  );



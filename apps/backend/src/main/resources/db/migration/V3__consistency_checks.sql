ALTER TABLE tariff_rate
    ADD CONSTRAINT chk_tariff_basis_mfn_origin
    CHECK ((basis = 'MFN' AND origin_id IS NULL) OR basis = 'PREF');

ALTER TABLE tariff_rate
    ADD CONSTRAINT chk_tariff_basis_pref_origin
    CHECK (basis = 'MFN' OR (basis = 'PREF' AND origin_id IS NOT NULL));

ALTER TABLE tariff_rate
    DROP CONSTRAINT IF EXISTS chk_tariff_basis_agreement,
    ADD CONSTRAINT chk_tariff_basis_agreement
    CHECK (
        (basis = 'MFN' AND agreement_id IS NULL)
        OR (basis = 'PREF' AND agreement_id IS NOT NULL)
    );

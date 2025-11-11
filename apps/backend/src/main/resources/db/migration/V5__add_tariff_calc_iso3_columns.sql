-- Ensure ISO3 columns exist on tariff_calculation
-- This migration is safe to run repeatedly due to IF NOT EXISTS

ALTER TABLE tariff_calculation
    ADD COLUMN IF NOT EXISTS importer_iso3 VARCHAR(3),
    ADD COLUMN IF NOT EXISTS origin_iso3 VARCHAR(3);

-- Helpful indexes
CREATE INDEX IF NOT EXISTS idx_tariff_calc_importer_iso3 ON tariff_calculation (importer_iso3);
CREATE INDEX IF NOT EXISTS idx_tariff_calc_origin_iso3 ON tariff_calculation (origin_iso3);


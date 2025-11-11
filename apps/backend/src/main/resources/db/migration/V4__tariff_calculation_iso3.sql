-- Truncate mock data and switch tariff_calculation to ISO3 columns
TRUNCATE TABLE tariff_calculation;

-- Drop legacy ISO2 columns if they exist
ALTER TABLE tariff_calculation
    DROP COLUMN IF EXISTS importer_iso2,
    DROP COLUMN IF EXISTS origin_iso2;

-- Add ISO3 columns if they do not exist
ALTER TABLE tariff_calculation
    ADD COLUMN IF NOT EXISTS importer_iso3 VARCHAR(3),
    ADD COLUMN IF NOT EXISTS origin_iso3 VARCHAR(3);

-- Optional: indexes to speed up queries by country
CREATE INDEX IF NOT EXISTS idx_tariff_calc_importer_iso3 ON tariff_calculation (importer_iso3);
CREATE INDEX IF NOT EXISTS idx_tariff_calc_origin_iso3 ON tariff_calculation (origin_iso3);


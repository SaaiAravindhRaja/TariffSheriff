CREATE TABLE tariff_calculation (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(255),
    notes TEXT,
    hs_code VARCHAR(32),
    importer_iso2 VARCHAR(2),
    origin_iso2 VARCHAR(2),

    mfn_rate NUMERIC(18,6),
    pref_rate NUMERIC(18,6),
    rvc NUMERIC(18,6),
    agreement_id BIGINT,
    quantity INTEGER,
    total_value NUMERIC(18,2),
    material_cost NUMERIC(18,2),
    labour_cost NUMERIC(18,2),
    overhead_cost NUMERIC(18,2),
    profit NUMERIC(18,2),
    other_costs NUMERIC(18,2),
    fob NUMERIC(18,2),
    non_origin_value NUMERIC(18,2),

    rvc_computed NUMERIC(18,6),
    rate_used VARCHAR(8),
    applied_rate NUMERIC(18,6),
    total_tariff NUMERIC(18,2),

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tariff_calc_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_tariff_calc_user_created ON tariff_calculation(user_id, created_at);
CREATE INDEX idx_tariff_calc_user_id ON tariff_calculation(user_id, id);

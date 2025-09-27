-- Add RVC (regional value content) threshold to agreements
-- Stored as percentage with two decimals, e.g., 40.00

ALTER TABLE agreement
ADD COLUMN IF NOT EXISTS rvc_threshold NUMERIC(5,2);



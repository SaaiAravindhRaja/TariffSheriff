-- Update mock agreement to include RVC threshold
UPDATE agreement
SET rvc_threshold = 40.00
WHERE id = 1;



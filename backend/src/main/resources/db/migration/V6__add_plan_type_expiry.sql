-- V6__add_plan_expiry.sql
ALTER TABLE plans ADD COLUMN duration_days INTEGER;          -- NULL = no expiry
UPDATE plans SET duration_days = 30 WHERE code = 'PILOT';

ALTER TABLE plans RENAME COLUMN monthly_price_pence TO monthly_price_pounds;
ALTER TABLE plans ALTER COLUMN monthly_price_pounds SET DATA TYPE NUMERIC(10,2);

ALTER TABLE companies ADD COLUMN plan_started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE companies ADD COLUMN plan_expires_at TIMESTAMP;  -- NULL = never expires
ALTER TABLE companies ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'TRIAL';
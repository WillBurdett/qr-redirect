-- V7__set_plan_pricing.sql
UPDATE plans SET monthly_price_pounds = 0,   max_active_codes = 5   WHERE code = 'PILOT';
UPDATE plans SET monthly_price_pounds = 29,  max_active_codes = 10  WHERE code = 'STARTER';
UPDATE plans SET monthly_price_pounds = 59, max_active_codes = 40  WHERE code = 'BRANCH';
UPDATE plans SET monthly_price_pounds = 99, max_active_codes = 100 WHERE code = 'AGENCY';
-- CUSTOM stays NULL: quoted per client
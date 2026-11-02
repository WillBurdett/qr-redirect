CREATE TABLE plans (
                       code             VARCHAR(32) PRIMARY KEY,
                       monthly_price_pence INTEGER,          -- NULL for custom (quoted per client)
                       max_active_codes INTEGER              -- NULL = no fixed limit / negotiated
);

INSERT INTO plans (code, monthly_price_pence, max_active_codes) VALUES
                                                                    ('PILOT',   NULL, NULL),
                                                                    ('STARTER', NULL, NULL),
                                                                    ('BRANCH',  NULL, NULL),
                                                                    ('AGENCY',  NULL, NULL),
                                                                    ('CUSTOM',  NULL, NULL);

ALTER TABLE companies ADD COLUMN plan_type VARCHAR(32) NOT NULL DEFAULT 'PILOT';
ALTER TABLE companies
    ADD CONSTRAINT fk_companies_plan FOREIGN KEY (plan_type) REFERENCES plans (code);
-- NextGen Digital Banking Platform — V3 Add Customer Number
-- Adds public/business identifier CUST-XXXXXXXX to cust_profiles

ALTER TABLE cust_profiles
    ADD COLUMN IF NOT EXISTS customer_number VARCHAR(20);

-- Backfill any existing records with unique public customer numbers if not present
DO $$
DECLARE
    rec RECORD;
    v_chars TEXT := '23456789ABCDEFGHJKLMNPQRSTUVWXYZ';
    v_random_str TEXT;
    i INT;
    v_candidate TEXT;
BEGIN
    FOR rec IN SELECT customer_id FROM cust_profiles WHERE customer_number IS NULL LOOP
        LOOP
            v_random_str := '';
            FOR i IN 1..8 LOOP
                v_random_str := v_random_str || substr(v_chars, floor(random() * length(v_chars) + 1)::int, 1);
            END LOOP;
            v_candidate := 'CUST-' || v_random_str;

            -- Check for collision
            IF NOT EXISTS (SELECT 1 FROM cust_profiles WHERE customer_number = v_candidate) THEN
                UPDATE cust_profiles SET customer_number = v_candidate WHERE customer_id = rec.customer_id;
                EXIT;
            END IF;
        END LOOP;
    END LOOP;
END $$;

-- Enforce NOT NULL and UNIQUE constraint
ALTER TABLE cust_profiles
    ALTER COLUMN customer_number SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_cust_profiles_customer_number ON cust_profiles(customer_number);

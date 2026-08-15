-- NextGen Digital Banking Platform — V4 Auth OTPs Table
-- Stores cryptographic hashes for customer and transaction OTP verifications

CREATE TABLE IF NOT EXISTS auth_otps (
    otp_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(100) NOT NULL,
    purpose VARCHAR(50) NOT NULL,
    otp_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    is_used BOOLEAN NOT NULL DEFAULT FALSE,
    used_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_auth_otps_lookup ON auth_otps(identifier, purpose, is_used);

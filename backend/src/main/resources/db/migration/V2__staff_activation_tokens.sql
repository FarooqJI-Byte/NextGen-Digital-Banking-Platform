-- V2: Staff Activation Tokens Schema
-- Supports secure one-time tokenized employee onboarding

CREATE TABLE IF NOT EXISTS auth_activation_tokens (
    token_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth_users(user_id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    is_used BOOLEAN NOT NULL DEFAULT false,
    used_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_auth_activation_tokens_hash ON auth_activation_tokens(token_hash);
CREATE INDEX idx_auth_activation_tokens_user_id ON auth_activation_tokens(user_id);

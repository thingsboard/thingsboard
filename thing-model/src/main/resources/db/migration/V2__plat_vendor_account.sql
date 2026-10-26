CREATE TABLE IF NOT EXISTS plat_vendor_account (
    tenant_id UUID PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    password_cipher TEXT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

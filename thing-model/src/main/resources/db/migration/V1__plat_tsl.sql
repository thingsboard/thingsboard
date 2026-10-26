CREATE TABLE IF NOT EXISTS plat_product (
    tenant_id UUID NOT NULL,
    product_id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    model VARCHAR(64),
    category_id VARCHAR(64),
    vendor VARCHAR(128),
    auth_type VARCHAR(32) NOT NULL DEFAULT 'ACCESS_TOKEN',
    protocol VARCHAR(32) NOT NULL DEFAULT 'MQTT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_plat_product_tenant ON plat_product (tenant_id);

CREATE TABLE IF NOT EXISTS plat_product_tsl (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL,
    product_id VARCHAR(64) NOT NULL REFERENCES plat_product (product_id) ON DELETE CASCADE,
    version VARCHAR(32) NOT NULL,
    checksum VARCHAR(64) NOT NULL,
    strict_unknown BOOLEAN NOT NULL DEFAULT FALSE,
    tsl_json JSONB NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'DEPRECATED')),
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (product_id, version)
);

CREATE INDEX IF NOT EXISTS idx_plat_product_tsl_product ON plat_product_tsl (product_id, created_at DESC);

CREATE TABLE IF NOT EXISTS plat_tsl_property (
    product_id VARCHAR(64) NOT NULL,
    tsl_version VARCHAR(32) NOT NULL,
    property_key VARCHAR(50) NOT NULL,
    name VARCHAR(128),
    data_type VARCHAR(32),
    mode VARCHAR(8),
    unit VARCHAR(32),
    min_value DOUBLE PRECISION,
    max_value DOUBLE PRECISION,
    step_value DOUBLE PRECISION,
    required BOOLEAN NOT NULL DEFAULT FALSE,
    mapping_json JSONB,
    column_name VARCHAR(64),
    sort_order INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (product_id, tsl_version, property_key),
    FOREIGN KEY (product_id, tsl_version)
        REFERENCES plat_product_tsl (product_id, version) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS plat_tsl_event (
    product_id VARCHAR(64) NOT NULL,
    tsl_version VARCHAR(32) NOT NULL,
    event_id VARCHAR(50) NOT NULL,
    name VARCHAR(128),
    event_type VARCHAR(8),
    params_json JSONB,
    sort_order INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (product_id, tsl_version, event_id),
    FOREIGN KEY (product_id, tsl_version)
        REFERENCES plat_product_tsl (product_id, version) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS plat_tsl_action (
    product_id VARCHAR(64) NOT NULL,
    tsl_version VARCHAR(32) NOT NULL,
    action_id VARCHAR(50) NOT NULL,
    name VARCHAR(128),
    input_json JSONB,
    output_json JSONB,
    sort_order INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (product_id, tsl_version, action_id),
    FOREIGN KEY (product_id, tsl_version)
        REFERENCES plat_product_tsl (product_id, version) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS plat_profile_binding (
    product_id VARCHAR(64) NOT NULL,
    tsl_version VARCHAR(32) NOT NULL,
    tb_profile_id UUID,
    tb_asset_id UUID,
    tb_rule_chain_id UUID,
    schema_version VARCHAR(16),
    sync_result TEXT,
    synced_at TIMESTAMPTZ,
    PRIMARY KEY (product_id, tsl_version),
    FOREIGN KEY (product_id, tsl_version)
        REFERENCES plat_product_tsl (product_id, version) ON DELETE CASCADE
);

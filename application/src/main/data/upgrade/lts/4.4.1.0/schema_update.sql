--
-- SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
-- SPDX-License-Identifier: BUSL-1.1
--

-- Tenant-scoped paging over calculated fields (WHERE tenant_id = ? ORDER BY id LIMIT ?), issued for every tenant
-- when the calculated field actors initialize, otherwise scans the whole table and sorts on each call.
CREATE INDEX IF NOT EXISTS idx_calculated_field_tenant_id_id ON calculated_field(tenant_id, id);

-- EXTENSION TABLE ADDITION START

-- Extensions feature: tenant-scoped REST-API extensions (EntityType.EXTENSION).
-- Keep identical to schema-entities.sql and upgrade/pe/schema_update.sql.
CREATE TABLE IF NOT EXISTS extension (
    id              UUID          NOT NULL CONSTRAINT extension_pkey PRIMARY KEY,
    created_time    BIGINT        NOT NULL,
    tenant_id       UUID          NOT NULL,
    name            VARCHAR(255)  NOT NULL,
    slug            VARCHAR(255)  NOT NULL,
    state           VARCHAR(32),
    container_spec  JSONB,
    last_error      VARCHAR,
    last_error_at   BIGINT,
    additional_info VARCHAR,
    external_id     UUID,
    version         BIGINT        DEFAULT 1,
    CONSTRAINT extension_slug_unq_key        UNIQUE (slug),
    CONSTRAINT extension_tenant_name_unq_key UNIQUE (tenant_id, name),
    CONSTRAINT extension_external_id_unq_key UNIQUE (tenant_id, external_id)
);

-- EXTENSION TABLE ADDITION END

--
-- SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
-- SPDX-License-Identifier: BUSL-1.1
--

-- 4.4.1 flat DDL, applied by V4_4_1_0Migration during a 4.4.0 -> 4.4.1 upgrade on both the no-downtime and the
-- offline path. Keep these idempotent (ALTER ... IF NOT EXISTS): the offline path records the package version once
-- at the end, so a resumed upgrade re-runs this whole file.

-- EDGE PROFILES START

CREATE TABLE IF NOT EXISTS edge_profile (
    id uuid NOT NULL CONSTRAINT edge_profile_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    tenant_id uuid NOT NULL,
    name varchar(255) NOT NULL,
    description varchar,
    is_default boolean NOT NULL DEFAULT false,
    root_rule_chain_id uuid,
    profile_data jsonb,
    version BIGINT DEFAULT 1,
    CONSTRAINT edge_profile_name_unq_key UNIQUE (tenant_id, name)
);
ALTER TABLE edge ADD COLUMN IF NOT EXISTS edge_profile_id uuid;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_edge_profile') THEN
        ALTER TABLE edge ADD CONSTRAINT fk_edge_profile FOREIGN KEY (edge_profile_id) REFERENCES edge_profile(id);
    END IF;
END $$;
CREATE INDEX IF NOT EXISTS idx_edge_edge_profile_id ON edge(tenant_id, edge_profile_id);

-- EDGE PROFILES END

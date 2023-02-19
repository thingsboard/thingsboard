--
-- Copyright © 2016-2023 The Thingsboard Authors
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

ALTER TABLE device
    ADD COLUMN IF NOT EXISTS external_id UUID;
ALTER TABLE device_profile
    ADD COLUMN IF NOT EXISTS external_id UUID;
ALTER TABLE asset
    ADD COLUMN IF NOT EXISTS external_id UUID;
ALTER TABLE rule_chain
    ADD COLUMN IF NOT EXISTS external_id UUID;
ALTER TABLE rule_node
    ADD COLUMN IF NOT EXISTS external_id UUID;
ALTER TABLE dashboard
    ADD COLUMN IF NOT EXISTS external_id UUID;
ALTER TABLE customer
    ADD COLUMN IF NOT EXISTS external_id UUID;
ALTER TABLE widgets_bundle
    ADD COLUMN IF NOT EXISTS external_id UUID;
ALTER TABLE entity_view
    ADD COLUMN IF NOT EXISTS external_id UUID;

CREATE INDEX IF NOT EXISTS idx_rule_node_external_id ON rule_node(rule_chain_id, external_id);
CREATE INDEX IF NOT EXISTS idx_rule_node_type ON rule_node(type);

ALTER TABLE admin_settings
    ADD COLUMN IF NOT EXISTS tenant_id uuid NOT NULL DEFAULT '13814000-1dd2-11b2-8080-808080808080';

CREATE TABLE IF NOT EXISTS queue (
    id uuid NOT NULL CONSTRAINT queue_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    tenant_id uuid,
    name varchar(255),
    topic varchar(255),
    poll_interval int,
    partitions int,
    consumer_per_partition boolean,
    pack_processing_timeout bigint,
    submit_strategy varchar(255),
    processing_strategy varchar(255),
    additional_info varchar
);

CREATE TABLE IF NOT EXISTS user_auth_settings (
    id uuid NOT NULL CONSTRAINT user_auth_settings_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    user_id uuid UNIQUE NOT NULL CONSTRAINT fk_user_auth_settings_user_id REFERENCES tb_user(id),
    two_fa_settings varchar
);

CREATE INDEX IF NOT EXISTS idx_api_usage_state_entity_id ON api_usage_state(entity_id);

ALTER TABLE tenant_profile DROP COLUMN IF EXISTS isolated_tb_core;

DO
$$
    BEGIN
        IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname = 'device_external_id_unq_key') THEN
            ALTER TABLE device ADD CONSTRAINT device_external_id_unq_key UNIQUE (tenant_id, external_id);
        END IF;
    END;
$$;

DO
$$
    BEGIN
        IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname = 'device_profile_external_id_unq_key') THEN
            ALTER TABLE device_profile ADD CONSTRAINT device_profile_external_id_unq_key UNIQUE (tenant_id, external_id);
        END IF;
    END;
$$;

DO
$$
    BEGIN
        IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname = 'asset_external_id_unq_key') THEN
            ALTER TABLE asset ADD CONSTRAINT asset_external_id_unq_key UNIQUE (tenant_id, external_id);
        END IF;
    END;
$$;

DO
$$
    BEGIN
        IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname = 'rule_chain_external_id_unq_key') THEN
            ALTER TABLE rule_chain ADD CONSTRAINT rule_chain_external_id_unq_key UNIQUE (tenant_id, external_id);
        END IF;
    END;
$$;


DO
$$
    BEGIN
        IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname = 'dashboard_external_id_unq_key') THEN
            ALTER TABLE dashboard ADD CONSTRAINT dashboard_external_id_unq_key UNIQUE (tenant_id, external_id);
        END IF;
    END;
$$;

DO
$$
    BEGIN
        IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname = 'customer_external_id_unq_key') THEN
            ALTER TABLE customer ADD CONSTRAINT customer_external_id_unq_key UNIQUE (tenant_id, external_id);
        END IF;
    END;
$$;

DO
$$
    BEGIN
        IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname = 'widgets_bundle_external_id_unq_key') THEN
            ALTER TABLE widgets_bundle ADD CONSTRAINT widgets_bundle_external_id_unq_key UNIQUE (tenant_id, external_id);
        END IF;
    END;
$$;

DO
$$
    BEGIN
        IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname = 'entity_view_external_id_unq_key') THEN
            ALTER TABLE entity_view ADD CONSTRAINT entity_view_external_id_unq_key UNIQUE (tenant_id, external_id);
        END IF;
    END;
$$;


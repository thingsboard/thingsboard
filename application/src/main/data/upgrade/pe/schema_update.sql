--
-- SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
-- SPDX-License-Identifier: BUSL-1.1
--

-- Set TB_LICENSE_SECRET on the upgrade command (or in thingsboard.conf, which it sources) so the pre-upgrade
-- capacity check can verify the device count against the license; without it that check is skipped.

CREATE TABLE IF NOT EXISTS tb_instance_registry (
    service_id varchar(255) NOT NULL CONSTRAINT service_id_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    last_activity_ts bigint NOT NULL
);

CREATE TABLE IF NOT EXISTS tb_cluster (
    cluster_id uuid NOT NULL,
    license_secret varchar,
    license_claim_token varchar,
    non_production_uptime_ms bigint NOT NULL DEFAULT 0,
    non_production_last_tick bigint,
    non_production_confirmed_ts bigint,
    CONSTRAINT tb_cluster_pkey PRIMARY KEY (cluster_id)
);
-- The CREATE above is a no-op when tb_cluster already exists - which it does on a PE install being
-- re-converted, and on a CE install that reaches this script carrying its own table - so add the PE-only
-- columns separately as well.
ALTER TABLE tb_cluster ADD COLUMN IF NOT EXISTS license_secret varchar;
ALTER TABLE tb_cluster ADD COLUMN IF NOT EXISTS license_claim_token varchar;
ALTER TABLE tb_cluster ADD COLUMN IF NOT EXISTS non_production_uptime_ms bigint NOT NULL DEFAULT 0;
ALTER TABLE tb_cluster ADD COLUMN IF NOT EXISTS non_production_last_tick bigint;
ALTER TABLE tb_cluster ADD COLUMN IF NOT EXISTS non_production_confirmed_ts bigint;
-- Backstop against two concurrent install/upgrade jobs both minting an id: a unique index on a constant
-- expression allows at most one row in the table, no matter how many rows race to insert. Added here as
-- well as in schema-entities.sql because a CE instance that predates the constraint keeps its own
-- tb_cluster table across the conversion, and IF NOT EXISTS makes a re-run a no-op.
CREATE UNIQUE INDEX IF NOT EXISTS tb_cluster_single_row ON tb_cluster ((true));

ALTER TABLE customer ADD COLUMN IF NOT EXISTS parent_customer_id uuid;

ALTER TABLE dashboard ADD COLUMN IF NOT EXISTS customer_id uuid;

ALTER TABLE edge_event ADD COLUMN IF NOT EXISTS entity_group_id uuid;

ALTER TABLE alarm ADD COLUMN IF NOT EXISTS propagate_to_owner_hierarchy boolean DEFAULT false;

ALTER TABLE edge ADD COLUMN IF NOT EXISTS edge_license_key varchar DEFAULT 'PUT_YOUR_EDGE_LICENSE_HERE';

ALTER TABLE edge ADD COLUMN IF NOT EXISTS cloud_endpoint varchar(255) DEFAULT 'PUT_YOUR_CLOUD_ENDPOINT_HERE';

ALTER TABLE admin_settings ALTER COLUMN json_value SET DATA TYPE varchar(10000000);

ALTER TABLE resource ADD COLUMN IF NOT EXISTS customer_id uuid;

ALTER TABLE qr_code_settings ADD COLUMN IF NOT EXISTS use_system_settings boolean default true;

ALTER TABLE tb_user ADD COLUMN IF NOT EXISTS custom_menu_id UUID;

ALTER TABLE customer ADD COLUMN IF NOT EXISTS custom_menu_id UUID;

ALTER TABLE mobile_app_bundle ADD COLUMN IF NOT EXISTS self_registration_config varchar(16384),
    ADD COLUMN IF NOT EXISTS terms_of_use varchar(10000000),
    ADD COLUMN IF NOT EXISTS privacy_policy varchar(10000000);

ALTER TABLE domain ADD COLUMN IF NOT EXISTS customer_id uuid not null default '13814000-1dd2-11b2-8080-808080808080';
ALTER TABLE oauth2_client ADD COLUMN IF NOT EXISTS customer_id uuid not null default '13814000-1dd2-11b2-8080-808080808080';

ALTER TABLE oauth2_client ADD COLUMN IF NOT EXISTS basic_parent_customer_name_pattern varchar(255);

ALTER TABLE oauth2_client ADD COLUMN IF NOT EXISTS basic_user_groups_name_pattern varchar(1024);

ALTER TABLE oauth2_client_registration_template ADD COLUMN IF NOT EXISTS basic_parent_customer_name_pattern varchar(255);

ALTER TABLE oauth2_client_registration_template ADD COLUMN IF NOT EXISTS basic_user_groups_name_pattern varchar(1024);

ALTER TABLE component_descriptor ADD COLUMN IF NOT EXISTS has_secrets boolean default false;

ALTER TABLE api_usage_state ADD COLUMN IF NOT EXISTS report_exec varchar(32) DEFAULT 'ENABLED';
ALTER TABLE api_usage_state ADD COLUMN IF NOT EXISTS ai varchar(32) DEFAULT 'ENABLED';

ALTER TABLE api_key ADD COLUMN IF NOT EXISTS internal boolean DEFAULT false;

ALTER TABLE api_key ADD COLUMN IF NOT EXISTS permissions json;

ALTER TABLE tb_user ADD COLUMN IF NOT EXISTS external_id uuid;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'tb_user_external_id_unq_key') THEN
        ALTER TABLE tb_user ADD CONSTRAINT tb_user_external_id_unq_key UNIQUE (tenant_id, external_id);
    END IF;
END $$;

ALTER TABLE job ADD COLUMN IF NOT EXISTS customer_id uuid;

-- Alarm sharding: drop the alarm_comment -> alarm cascade FK (comment cleanup is now async via the
-- Housekeeper service, AlarmCommentsDeletionTaskProcessor) and add entity_alarm.originator_id so
-- entity_alarm can co-locate with alarm on originator_id under the Citus distributed topology
-- (fresh-install-only; no existing Citus install to migrate). On plain PostgreSQL the column is harmless.
ALTER TABLE alarm_comment DROP CONSTRAINT IF EXISTS fk_alarm_comment_alarm_id;
ALTER TABLE entity_alarm ADD COLUMN IF NOT EXISTS originator_id uuid;
-- Backfill originator_id from the parent alarm; IS NULL guard keeps it idempotent. May be heavy on very
-- large entity_alarm tables (single-statement rewrite run once during the upgrade window).
UPDATE entity_alarm ea SET originator_id = a.originator_id
FROM alarm a
WHERE ea.alarm_id = a.id AND ea.originator_id IS NULL;

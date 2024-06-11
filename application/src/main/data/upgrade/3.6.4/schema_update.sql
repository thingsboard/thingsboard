--
-- Copyright © 2016-2024 The Thingsboard Authors
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

-- UPDATE PUBLIC CUSTOMERS START

DO
$$
    BEGIN
        IF NOT EXISTS (
            SELECT FROM information_schema.columns
            WHERE table_name = 'customer' AND column_name = 'is_public'
        ) THEN
            ALTER TABLE customer ADD COLUMN is_public boolean DEFAULT false;
            UPDATE customer SET is_public = true WHERE title = 'Public';
        END IF;
    END;
$$;

-- UPDATE PUBLIC CUSTOMERS END

-- create new attribute_kv table schema
DO
$$
    BEGIN
        -- in case of running the upgrade script a second time:
        IF EXISTS(SELECT 1 FROM information_schema.columns WHERE table_name = 'attribute_kv' and column_name='entity_type') THEN
            DROP VIEW IF EXISTS device_info_view;
            DROP VIEW IF EXISTS device_info_active_attribute_view;
            ALTER INDEX IF EXISTS idx_attribute_kv_by_key_and_last_update_ts RENAME TO idx_attribute_kv_by_key_and_last_update_ts_old;
            IF EXISTS(SELECT 1 FROM pg_constraint WHERE conname = 'attribute_kv_pkey') THEN
                ALTER TABLE attribute_kv RENAME CONSTRAINT attribute_kv_pkey TO attribute_kv_pkey_old;
            END IF;
            ALTER TABLE attribute_kv RENAME TO attribute_kv_old;
            CREATE TABLE IF NOT EXISTS attribute_kv
            (
                entity_id uuid,
                attribute_type int,
                attribute_key int,
                bool_v boolean,
                str_v varchar(10000000),
                long_v bigint,
                dbl_v double precision,
                json_v json,
                last_update_ts bigint,
                CONSTRAINT attribute_kv_pkey PRIMARY KEY (entity_id, attribute_type, attribute_key)
            );
        END IF;
    END;
$$;

-- rename ts_kv_dictionary table to key_dictionary or create table if not exists
DO
$$
    BEGIN
        IF EXISTS(SELECT 1 FROM information_schema.tables WHERE table_name = 'ts_kv_dictionary') THEN
            ALTER TABLE ts_kv_dictionary RENAME CONSTRAINT ts_key_id_pkey TO key_dictionary_id_pkey;
            ALTER TABLE ts_kv_dictionary RENAME TO key_dictionary;
        ELSE CREATE TABLE IF NOT EXISTS key_dictionary(
                key    varchar(255) NOT NULL,
                key_id serial UNIQUE,
                CONSTRAINT key_dictionary_id_pkey PRIMARY KEY (key)
                );
        END IF;
    END;
$$;

-- insert keys into key_dictionary
DO
$$
    BEGIN
        IF EXISTS(SELECT 1 FROM information_schema.tables WHERE table_name = 'attribute_kv_old') THEN
            INSERT INTO key_dictionary(key) SELECT DISTINCT attribute_key FROM attribute_kv_old ON CONFLICT DO NOTHING;
        END IF;
    END;
$$;

-- migrate attributes from attribute_kv_old to attribute_kv
DO
$$
DECLARE
    row_num_old integer;
    row_num integer;
BEGIN
    IF EXISTS(SELECT 1 FROM information_schema.tables WHERE table_name = 'attribute_kv_old') THEN
        INSERT INTO attribute_kv(entity_id, attribute_type, attribute_key, bool_v, str_v, long_v, dbl_v, json_v, last_update_ts)
            SELECT a.entity_id, CASE
                        WHEN a.attribute_type = 'CLIENT_SCOPE' THEN 1
                        WHEN a.attribute_type = 'SERVER_SCOPE' THEN 2
                        WHEN a.attribute_type = 'SHARED_SCOPE' THEN 3
                        ELSE 0
                        END,
                k.key_id,  a.bool_v, a.str_v, a.long_v, a.dbl_v, a.json_v, a.last_update_ts
                FROM attribute_kv_old a INNER JOIN key_dictionary k ON (a.attribute_key = k.key);
        SELECT COUNT(*) INTO row_num_old FROM attribute_kv_old;
        SELECT COUNT(*) INTO row_num FROM attribute_kv;
        RAISE NOTICE 'Migrated % of % rows', row_num, row_num_old;
        DROP TABLE IF EXISTS attribute_kv_old;
        CREATE INDEX IF NOT EXISTS idx_attribute_kv_by_key_and_last_update_ts ON attribute_kv(entity_id, attribute_key, last_update_ts desc);
    END IF;
EXCEPTION
    WHEN others THEN
        ROLLBACK;
        RAISE EXCEPTION 'Error during COPY: %', SQLERRM;
END
$$;

-- OAUTH2 PARAMS ALTER TABLE START

ALTER TABLE oauth2_params
    ADD COLUMN IF NOT EXISTS edge_enabled boolean DEFAULT false;

-- OAUTH2 PARAMS ALTER TABLE END

-- QUEUE STATS UPDATE START

CREATE TABLE IF NOT EXISTS queue_stats (
    id uuid NOT NULL CONSTRAINT queue_stats_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    tenant_id uuid NOT NULL,
    queue_name varchar(255) NOT NULL,
    service_id varchar(255) NOT NULL,
    CONSTRAINT queue_stats_name_unq_key UNIQUE (tenant_id, queue_name, service_id)
);

INSERT INTO queue_stats
    SELECT id, created_time, tenant_id, substring(name FROM 1 FOR position('_' IN name) - 1) AS queue_name,
           substring(name FROM position('_' IN name) + 1) AS service_id
    FROM asset
    WHERE type = 'TbServiceQueue' and name LIKE '%\_%';

DELETE FROM asset WHERE type='TbServiceQueue';
DELETE FROM asset_profile WHERE name ='TbServiceQueue';

-- QUEUE STATS UPDATE END

-- MOBILE APP SETTINGS TABLE CREATE START

CREATE TABLE IF NOT EXISTS mobile_app_settings (
    id uuid NOT NULL CONSTRAINT mobile_app_settings_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    tenant_id uuid NOT NULL,
    use_default_app boolean,
    android_config VARCHAR(1000),
    ios_config VARCHAR(1000),
    qr_code_config VARCHAR(100000),
    CONSTRAINT mobile_app_settings_tenant_id_unq_key UNIQUE (tenant_id)
);

-- MOBILE APP SETTINGS TABLE CREATE END
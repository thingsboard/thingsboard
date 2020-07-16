--
-- Copyright © 2016-2020 The Thingsboard Authors
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

CREATE OR REPLACE FUNCTION to_uuid(IN entity_id varchar, OUT uuid_id uuid) AS
$$
BEGIN
    uuid_id := substring(entity_id, 8, 8) || '-' || substring(entity_id, 4, 4) || '-1' || substring(entity_id, 1, 3) ||
               '-' || substring(entity_id, 16, 4) || '-' || substring(entity_id, 20, 12);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION extract_ts(uuid UUID) RETURNS BIGINT AS
$$
DECLARE
    bytes bytea;
BEGIN
    bytes := uuid_send(uuid);
    RETURN
            (
                        (
                                        (get_byte(bytes, 0)::bigint << 24) |
                                        (get_byte(bytes, 1)::bigint << 16) |
                                        (get_byte(bytes, 2)::bigint << 8) |
                                        (get_byte(bytes, 3)::bigint << 0)
                            ) + (
                                ((get_byte(bytes, 4)::bigint << 8 |
                                  get_byte(bytes, 5)::bigint)) << 32
                            ) + (
                                (((get_byte(bytes, 6)::bigint & 15) << 8 | get_byte(bytes, 7)::bigint) & 4095) << 48
                            ) - 122192928000000000
                ) / 10000::double precision;
END
$$ LANGUAGE plpgsql
    IMMUTABLE
    PARALLEL SAFE
    RETURNS NULL ON NULL INPUT;


CREATE OR REPLACE FUNCTION column_type_to_uuid(table_name varchar, column_name varchar) RETURNS VOID
    LANGUAGE plpgsql AS
$$
BEGIN
    execute format('ALTER TABLE %s RENAME COLUMN %s TO old_%s;', table_name, column_name, column_name);
    execute format('ALTER TABLE %s ADD COLUMN %s UUID;', table_name, column_name);
    execute format('UPDATE %s SET %s = to_uuid(old_%s) WHERE old_%s is not null;', table_name, column_name, column_name, column_name);
    execute format('ALTER TABLE %s DROP COLUMN old_%s;', table_name, column_name);
END;
$$;

CREATE OR REPLACE FUNCTION get_column_type(table_name varchar, column_name varchar, OUT data_type varchar) RETURNS varchar
    LANGUAGE plpgsql AS
$$
BEGIN
    execute (format('SELECT data_type from information_schema.columns where table_name = %L and column_name = %L',
                    table_name, column_name)) INTO data_type;
END;
$$;


CREATE OR REPLACE PROCEDURE drop_all_idx()
    LANGUAGE plpgsql AS
$$
BEGIN
    DROP INDEX IF EXISTS idx_alarm_originator_alarm_type;
    DROP INDEX IF EXISTS idx_alarm_originator_created_time;
    DROP INDEX IF EXISTS idx_alarm_tenant_created_time;
    DROP INDEX IF EXISTS idx_event_type_entity_id;
    DROP INDEX IF EXISTS idx_relation_to_id;
    DROP INDEX IF EXISTS idx_relation_from_id;
    DROP INDEX IF EXISTS idx_device_customer_id;
    DROP INDEX IF EXISTS idx_device_customer_id_and_type;
    DROP INDEX IF EXISTS idx_device_type;
    DROP INDEX IF EXISTS idx_asset_customer_id;
    DROP INDEX IF EXISTS idx_asset_customer_id_and_type;
    DROP INDEX IF EXISTS idx_asset_type;
END;
$$;

CREATE OR REPLACE PROCEDURE create_all_idx()
    LANGUAGE plpgsql AS
$$
BEGIN
    CREATE INDEX IF NOT EXISTS idx_alarm_originator_alarm_type ON alarm(originator_id, type, start_ts DESC);
    CREATE INDEX IF NOT EXISTS idx_alarm_originator_created_time ON alarm(originator_id, created_time DESC);
    CREATE INDEX IF NOT EXISTS idx_alarm_tenant_created_time ON alarm(tenant_id, created_time DESC);
    CREATE INDEX IF NOT EXISTS idx_event_type_entity_id ON event(tenant_id, event_type, entity_type, entity_id);
    CREATE INDEX IF NOT EXISTS idx_relation_to_id ON relation(relation_type_group, to_type, to_id);
    CREATE INDEX IF NOT EXISTS idx_relation_from_id ON relation(relation_type_group, from_type, from_id);
    CREATE INDEX IF NOT EXISTS idx_device_customer_id ON device(tenant_id, customer_id);
    CREATE INDEX IF NOT EXISTS idx_device_customer_id_and_type ON device(tenant_id, customer_id, type);
    CREATE INDEX IF NOT EXISTS idx_device_type ON device(tenant_id, type);
    CREATE INDEX IF NOT EXISTS idx_asset_customer_id ON asset(tenant_id, customer_id);
    CREATE INDEX IF NOT EXISTS idx_asset_customer_id_and_type ON asset(tenant_id, customer_id, type);
    CREATE INDEX IF NOT EXISTS idx_asset_type ON asset(tenant_id, type);
END;
$$;


-- admin_settings
CREATE OR REPLACE PROCEDURE update_admin_settings()
    LANGUAGE plpgsql AS
$$
DECLARE
    data_type  varchar;
    table_name varchar := 'admin_settings';
    column_id  varchar := 'id';
BEGIN
    data_type := get_column_type(table_name, column_id);
    IF data_type = 'character varying' THEN
        ALTER TABLE admin_settings DROP CONSTRAINT admin_settings_pkey;
        PERFORM column_type_to_uuid(table_name, column_id);
        ALTER TABLE admin_settings ADD CONSTRAINT admin_settings_pkey PRIMARY KEY (id);
        ALTER TABLE admin_settings ADD COLUMN created_time BIGINT;
        UPDATE admin_settings SET created_time = extract_ts(id) WHERE id is not null;
        RAISE NOTICE 'Table % column % updated!', table_name, column_id;
    ELSE
        RAISE NOTICE 'Table % column % already updated!', table_name, column_id;
    END IF;
END;
$$;


-- alarm
CREATE OR REPLACE PROCEDURE update_alarm()
    LANGUAGE plpgsql AS
$$
DECLARE
    data_type  varchar;
    table_name varchar := 'alarm';
    column_id  varchar := 'id';
    column_originator_id varchar := 'originator_id';
    column_tenant_id varchar := 'tenant_id';
BEGIN
    data_type := get_column_type(table_name, column_id);
    IF data_type = 'character varying' THEN
        ALTER TABLE alarm DROP CONSTRAINT alarm_pkey;
        PERFORM column_type_to_uuid(table_name, column_id);
        ALTER TABLE alarm ADD COLUMN created_time BIGINT;
        UPDATE alarm SET created_time = extract_ts(id) WHERE id is not null;
        ALTER TABLE alarm ADD CONSTRAINT alarm_pkey PRIMARY KEY (id);
        RAISE NOTICE 'Table % column % updated!', table_name, column_id;
    ELSE
        RAISE NOTICE 'Table % column % already updated!', table_name, column_id;
    END IF;

    data_type := get_column_type(table_name, column_originator_id);
    IF data_type = 'character varying' THEN
        PERFORM column_type_to_uuid(table_name, column_originator_id);
        RAISE NOTICE 'Table % column % updated!', table_name, column_originator_id;
    ELSE
        RAISE NOTICE 'Table % column % already updated!', table_name, column_originator_id;
    END IF;

    data_type := get_column_type(table_name, column_tenant_id);
    IF data_type = 'character varying' THEN
        PERFORM column_type_to_uuid(table_name, column_tenant_id);
        RAISE NOTICE 'Table % column % updated!', table_name, column_tenant_id;
    ELSE
        RAISE NOTICE 'Table % column % already updated!', table_name, column_tenant_id;
    END IF;
END;
$$;


-- asset
CREATE OR REPLACE PROCEDURE update_asset()
    LANGUAGE plpgsql AS
$$
DECLARE
    data_type  varchar;
    table_name varchar := 'asset';
    column_id  varchar := 'id';
    column_customer_id varchar := 'customer_id';
    column_tenant_id varchar := 'tenant_id';
BEGIN
    data_type := get_column_type(table_name, column_id);
    IF data_type = 'character varying' THEN
        ALTER TABLE asset DROP CONSTRAINT asset_pkey;
        PERFORM column_type_to_uuid(table_name, column_id);
        ALTER TABLE asset ADD COLUMN created_time BIGINT;
        UPDATE asset SET created_time = extract_ts(id) WHERE id is not null;
        ALTER TABLE asset ADD CONSTRAINT asset_pkey PRIMARY KEY (id);
        RAISE NOTICE 'Table % column % updated!', table_name, column_id;
    ELSE
        RAISE NOTICE 'Table % column % already updated!', table_name, column_id;
    END IF;

    data_type := get_column_type(table_name, column_customer_id);
    IF data_type = 'character varying' THEN
        PERFORM column_type_to_uuid(table_name, column_customer_id);
        RAISE NOTICE 'Table % column % updated!', table_name, column_customer_id;
    ELSE
        RAISE NOTICE 'Table % column % already updated!', table_name, column_customer_id;
    END IF;

    data_type := get_column_type(table_name, column_tenant_id);
    IF data_type = 'character varying' THEN
        ALTER TABLE asset DROP CONSTRAINT asset_name_unq_key;
        PERFORM column_type_to_uuid(table_name, column_tenant_id);
        ALTER TABLE asset ADD CONSTRAINT asset_name_unq_key UNIQUE (tenant_id, name);
        RAISE NOTICE 'Table % column % updated!', table_name, column_tenant_id;
    ELSE
        RAISE NOTICE 'Table % column % already updated!', table_name, column_tenant_id;
    END IF;
    END;
$$;

-- attribute_kv
CREATE OR REPLACE PROCEDURE update_attribute_kv()
    LANGUAGE plpgsql AS
$$
DECLARE
    data_type  varchar;
    table_name varchar := 'attribute_kv';
    column_entity_id  varchar := 'entity_id';
BEGIN
    data_type := get_column_type(table_name, column_entity_id);
    IF data_type = 'character varying' THEN
        ALTER TABLE attribute_kv DROP CONSTRAINT attribute_kv_pkey;
        PERFORM column_type_to_uuid(table_name, column_entity_id);
        ALTER TABLE attribute_kv ADD CONSTRAINT attribute_kv_pkey PRIMARY KEY (entity_type, entity_id, attribute_type, attribute_key);
        RAISE NOTICE 'Table % column % updated!', table_name, column_entity_id;
    ELSE
        RAISE NOTICE 'Table % column % already updated!', table_name, column_entity_id;
    END IF;
END;
$$;

-- audit_log
CREATE OR REPLACE PROCEDURE update_audit_log()
    LANGUAGE plpgsql AS
$$
DECLARE
    data_type  varchar;
    table_name varchar := 'audit_log';
    column_id  varchar := 'id';
    column_customer_id varchar := 'customer_id';
    column_tenant_id varchar := 'tenant_id';
    column_entity_id varchar := 'entity_id';
    column_user_id varchar := 'user_id';
BEGIN
    data_type := get_column_type(table_name, column_id);
    IF data_type = 'character varying' THEN
        ALTER TABLE audit_log DROP CONSTRAINT audit_log_pkey;
        PERFORM column_type_to_uuid(table_name, column_id);
        ALTER TABLE audit_log ADD COLUMN created_time BIGINT;
        UPDATE audit_log SET created_time = extract_ts(id) WHERE id is not null;
        ALTER TABLE audit_log ADD CONSTRAINT audit_log_pkey PRIMARY KEY (id);
        RAISE NOTICE 'Table % column % updated!', table_name, column_id;
    ELSE
        RAISE NOTICE 'Table % column % already updated!', table_name, column_id;
    END IF;

    data_type := get_column_type(table_name, column_customer_id);
    IF data_type = 'character varying' THEN
        PERFORM column_type_to_uuid(table_name, column_customer_id);
        RAISE NOTICE 'Table % column % updated!', table_name, column_customer_id;
    ELSE
        RAISE NOTICE 'Table % column % already updated!', table_name, column_customer_id;
    END IF;

    data_type := get_column_type(table_name, column_tenant_id);
    IF data_type = 'character varying' THEN
        PERFORM column_type_to_uuid(table_name, column_tenant_id);
        RAISE NOTICE 'Table % column % updated!', table_name, column_tenant_id;
    ELSE
        RAISE NOTICE 'Table % column % already updated!', table_name, column_tenant_id;
    END IF;

    data_type := get_column_type(table_name, column_entity_id);
    IF data_type = 'character varying' THEN
        PERFORM column_type_to_uuid(table_name, column_entity_id);
        RAISE NOTICE 'Table % column % updated!', table_name, column_entity_id;
    ELSE
        RAISE NOTICE 'Table % column % already updated!', table_name, column_entity_id;
    END IF;

    data_type := get_column_type(table_name, column_user_id);
    IF data_type = 'character varying' THEN
        PERFORM column_type_to_uuid(table_name, column_user_id);
        RAISE NOTICE 'Table % column % updated!', table_name, column_user_id;
    ELSE
        RAISE NOTICE 'Table % column % already updated!', table_name, column_user_id;
    END IF;
END;
$$;


-- component_descriptor
CREATE OR REPLACE PROCEDURE update_component_descriptor()
    LANGUAGE plpgsql AS
$$
DECLARE
    data_type  varchar;
    table_name varchar := 'component_descriptor';
    column_id  varchar := 'id';
BEGIN
    data_type := get_column_type(table_name, column_id);
    IF data_type = 'character varying' THEN
        ALTER TABLE component_descriptor DROP CONSTRAINT component_descriptor_pkey;
        PERFORM column_type_to_uuid(table_name, column_id);
        ALTER TABLE component_descriptor ADD CONSTRAINT component_descriptor_pkey PRIMARY KEY (id);
        ALTER TABLE component_descriptor ADD COLUMN created_time BIGINT;
        UPDATE component_descriptor SET created_time = extract_ts(id) WHERE id is not null;
        RAISE NOTICE 'Table % column % updated!', table_name, column_id;
    ELSE
        RAISE NOTICE 'Table % column % already updated!', table_name, column_id;
    END IF;
END;
$$;

-- customer
CREATE OR REPLACE PROCEDURE update_customer()
    LANGUAGE plpgsql AS
$$
DECLARE
    data_type  varchar;
    table_name varchar := 'customer';
    column_id  varchar := 'id';
    column_tenant_id  varchar := 'tenant_id';
BEGIN
    data_type := get_column_type(table_name, column_id);
    IF data_type = 'character varying' THEN
        ALTER TABLE customer DROP CONSTRAINT customer_pkey;
        PERFORM column_type_to_uuid(table_name, column_id);
        ALTER TABLE customer ADD CONSTRAINT customer_pkey PRIMARY KEY (id);
        ALTER TABLE customer ADD COLUMN created_time BIGINT;
        UPDATE customer SET created_time = extract_ts(id) WHERE id is not null;
        RAISE NOTICE 'Table % column % updated!', table_name, column_id;
    ELSE
        RAISE NOTICE 'Table % column % already updated!', table_name, column_id;
    END IF;

    data_type := get_column_type(table_name, column_tenant_id);
    IF data_type = 'character varying' THEN
        PERFORM column_type_to_uuid(table_name, column_tenant_id);
        RAISE NOTICE 'Table % column % updated!', table_name, column_tenant_id;
    ELSE
        RAISE NOTICE 'Table % column % already updated!', table_name, column_tenant_id;
    END IF;
END;
$$;


-- dashboard
CREATE OR REPLACE PROCEDURE update_dashboard()
    LANGUAGE plpgsql AS
$$
DECLARE
    data_type  varchar;
    table_name varchar := 'dashboard';
    column_id  varchar := 'id';
    column_tenant_id  varchar := 'tenant_id';
BEGIN
    data_type := get_column_type(table_name, column_id);
    IF data_type = 'character varying' THEN
        ALTER TABLE dashboard DROP CONSTRAINT dashboard_pkey;
        PERFORM column_type_to_uuid(table_name, column_id);
        ALTER TABLE dashboard ADD CONSTRAINT dashboard_pkey PRIMARY KEY (id);
        ALTER TABLE dashboard ADD COLUMN created_time BIGINT;
        UPDATE dashboard SET created_time = extract_ts(id) WHERE id is not null;
        RAISE NOTICE 'Table % column % updated!', table_name, column_id;
    ELSE
        RAISE NOTICE 'Table % column % already updated!', table_name, column_id;
    END IF;

    data_type := get_column_type(table_name, column_tenant_id);
    IF data_type = 'character varying' THEN
        PERFORM column_type_to_uuid(table_name, column_tenant_id);
        RAISE NOTICE 'Table % column % updated!', table_name, column_tenant_id;
    ELSE
        RAISE NOTICE 'Table % column % already updated!', table_name, column_tenant_id;
    END IF;
END;
$$;

-- device
CREATE OR REPLACE PROCEDURE update_device()
    LANGUAGE plpgsql AS
$$
DECLARE
    data_type  varchar;
    table_name varchar := 'device';
    column_id  varchar := 'id';
    column_customer_id varchar := 'customer_id';
    column_tenant_id varchar := 'tenant_id';
BEGIN
    data_type := get_column_type(table_name, column_id);
    IF data_type = 'character varying' THEN
        ALTER TABLE device DROP CONSTRAINT device_pkey;
        PERFORM column_type_to_uuid(table_name, column_id);
        ALTER TABLE device ADD COLUMN created_time BIGINT;
        UPDATE device SET created_time = extract_ts(id) WHERE id is not null;
        ALTER TABLE device ADD CONSTRAINT device_pkey PRIMARY KEY (id);
        RAISE NOTICE 'Table % column % updated!', table_name, column_id;
    ELSE
        RAISE NOTICE 'Table % column % already updated!', table_name, column_id;
    END IF;

    data_type := get_column_type(table_name, column_customer_id);
    IF data_type = 'character varying' THEN
        PERFORM column_type_to_uuid(table_name, column_customer_id);
        RAISE NOTICE 'Table % column % updated!', table_name, column_customer_id;
    ELSE
        RAISE NOTICE 'Table % column % already updated!', table_name, column_customer_id;
    END IF;

    data_type := get_column_type(table_name, column_tenant_id);
    IF data_type = 'character varying' THEN
        ALTER TABLE device DROP CONSTRAINT device_name_unq_key;
        PERFORM column_type_to_uuid(table_name, column_tenant_id);
        ALTER TABLE device ADD CONSTRAINT device_name_unq_key UNIQUE (tenant_id, name);
        RAISE NOTICE 'Table % column % updated!', table_name, column_tenant_id;
    ELSE
        RAISE NOTICE 'Table % column % already updated!', table_name, column_tenant_id;
    END IF;
END;
$$;


-- device_credentials
CREATE OR REPLACE PROCEDURE update_device_credentials()
    LANGUAGE plpgsql AS
$$
DECLARE
    data_type  varchar;
    table_name varchar := 'device_credentials';
    column_id  varchar := 'id';
    column_device_id varchar := 'device_id';
BEGIN
    data_type := get_column_type(table_name, column_id);
    IF data_type = 'character varying' THEN
        ALTER TABLE device_credentials DROP CONSTRAINT device_credentials_pkey;
        PERFORM column_type_to_uuid(table_name, column_id);
        ALTER TABLE device_credentials ADD COLUMN created_time BIGINT;
        UPDATE device_credentials SET created_time = extract_ts(id) WHERE id is not null;
        ALTER TABLE device_credentials ADD CONSTRAINT device_credentials_pkey PRIMARY KEY (id);
        RAISE NOTICE 'Table % column % updated!', table_name, column_id;
    ELSE
        RAISE NOTICE 'Table % column % already updated!', table_name, column_id;
    END IF;

    data_type := get_column_type(table_name, column_device_id);
    IF data_type = 'character varying' THEN
        PERFORM column_type_to_uuid(table_name, column_device_id);
        RAISE NOTICE 'Table % column % updated!', table_name, column_device_id;
    ELSE
        RAISE NOTICE 'Table % column % already updated!', table_name, column_device_id;
    END IF;
END;
$$;


-- event
CREATE OR REPLACE PROCEDURE update_event()
    LANGUAGE plpgsql AS
$$
DECLARE
    data_type  varchar;
    table_name varchar := 'event';
    column_id  varchar := 'id';
    column_entity_id varchar := 'entity_id';
    column_tenant_id varchar := 'tenant_id';
BEGIN
    data_type := get_column_type(table_name, column_id);
    IF data_type = 'character varying' THEN
        ALTER TABLE event DROP CONSTRAINT event_pkey;
        PERFORM column_type_to_uuid(table_name, column_id);
        ALTER TABLE event ADD COLUMN created_time BIGINT;
        UPDATE event SET created_time = extract_ts(id) WHERE id is not null;
        ALTER TABLE event ADD CONSTRAINT event_pkey PRIMARY KEY (id);
        RAISE NOTICE 'Table % column % updated!', table_name, column_id;
    ELSE
        RAISE NOTICE 'Table % column % already updated!', table_name, column_id;
    END IF;

    ALTER TABLE event DROP CONSTRAINT event_unq_key;

    data_type := get_column_type(table_name, column_entity_id);
    IF data_type = 'character varying' THEN
        PERFORM column_type_to_uuid(table_name, column_entity_id);
        RAISE NOTICE 'Table % column % updated!', table_name, column_entity_id;
    ELSE
        RAISE NOTICE 'Table % column % already updated!', table_name, column_entity_id;
    END IF;

    data_type := get_column_type(table_name, column_tenant_id);
    IF data_type = 'character varying' THEN
        PERFORM column_type_to_uuid(table_name, column_tenant_id);
        RAISE NOTICE 'Table % column % updated!', table_name, column_tenant_id;
    ELSE
        RAISE NOTICE 'Table % column % already updated!', table_name, column_tenant_id;
    END IF;

    ALTER TABLE event ADD CONSTRAINT event_unq_key UNIQUE (tenant_id, entity_type, entity_id, event_type, event_uid);
END;
$$;


-- relation
CREATE OR REPLACE PROCEDURE update_relation()
    LANGUAGE plpgsql AS
$$
BEGIN
    ALTER TABLE relation DROP CONSTRAINT relation_pkey;

    ALTER TABLE relation RENAME TO old_relation;

    CREATE TABLE relation (
                                            from_id uuid,
                                            from_type varchar(255),
                                            to_id uuid,
                                            to_type varchar(255),
                                            relation_type_group varchar(255),
                                            relation_type varchar(255),
                                            additional_info varchar,
                                            CONSTRAINT relation_pkey PRIMARY KEY (from_id, from_type, relation_type_group, relation_type, to_id, to_type)
    ) PARTITION BY LIST (relation_type_group);

    CREATE TABLE IF NOT EXISTS other_relations PARTITION OF relation DEFAULT;
    CREATE TABLE IF NOT EXISTS common_relations PARTITION OF relation FOR VALUES IN ('COMMON');
    CREATE TABLE IF NOT EXISTS alarm_relations PARTITION OF relation FOR VALUES IN ('ALARM');
    CREATE TABLE IF NOT EXISTS dashboard_relations PARTITION OF relation FOR VALUES IN ('DASHBOARD');
    CREATE TABLE IF NOT EXISTS rule_relations PARTITION OF relation FOR VALUES IN ('RULE_CHAIN', 'RULE_NODE');

    INSERT INTO relation (from_id, from_type, to_id, to_type, relation_type_group, relation_type, additional_info)
    SELECT to_uuid(from_id), from_type, to_uuid(to_id), to_type, relation_type_group, relation_type, additional_info FROM old_relation;

    DROP TABLE old_relation;
END;
$$;

-- tb_user
CREATE OR REPLACE PROCEDURE update_tb_user()
    LANGUAGE plpgsql AS
$$
DECLARE
    data_type  varchar;
    table_name varchar := 'tb_user';
    column_id  varchar := 'id';
    column_customer_id varchar := 'customer_id';
    column_tenant_id varchar := 'tenant_id';
BEGIN
    data_type := get_column_type(table_name, column_id);
    IF data_type = 'character varying' THEN
        ALTER TABLE tb_user DROP CONSTRAINT tb_user_pkey;
        PERFORM column_type_to_uuid(table_name, column_id);
        ALTER TABLE tb_user ADD COLUMN created_time BIGINT;
        UPDATE tb_user SET created_time = extract_ts(id) WHERE id is not null;
        ALTER TABLE tb_user ADD CONSTRAINT tb_user_pkey PRIMARY KEY (id);
        RAISE NOTICE 'Table % column % updated!', table_name, column_id;
    ELSE
        RAISE NOTICE 'Table % column % already updated!', table_name, column_id;
    END IF;

    data_type := get_column_type(table_name, column_customer_id);
    IF data_type = 'character varying' THEN
        PERFORM column_type_to_uuid(table_name, column_customer_id);
        RAISE NOTICE 'Table % column % updated!', table_name, column_customer_id;
    ELSE
        RAISE NOTICE 'Table % column % already updated!', table_name, column_customer_id;
    END IF;

    data_type := get_column_type(table_name, column_tenant_id);
    IF data_type = 'character varying' THEN
        PERFORM column_type_to_uuid(table_name, column_tenant_id);
        RAISE NOTICE 'Table % column % updated!', table_name, column_tenant_id;
    ELSE
        RAISE NOTICE 'Table % column % already updated!', table_name, column_tenant_id;
    END IF;
END;
$$;


-- tenant
CREATE OR REPLACE PROCEDURE update_tenant()
    LANGUAGE plpgsql AS
$$
DECLARE
    data_type  varchar;
    table_name varchar := 'tenant';
    column_id  varchar := 'id';
BEGIN
    data_type := get_column_type(table_name, column_id);
    IF data_type = 'character varying' THEN
        ALTER TABLE tenant DROP CONSTRAINT tenant_pkey;
        PERFORM column_type_to_uuid(table_name, column_id);
        ALTER TABLE tenant ADD COLUMN created_time BIGINT;
        UPDATE tenant SET created_time = extract_ts(id) WHERE id is not null;
        ALTER TABLE tenant ADD CONSTRAINT tenant_pkey PRIMARY KEY (id);
        RAISE NOTICE 'Table % column % updated!', table_name, column_id;
    ELSE
        RAISE NOTICE 'Table % column % already updated!', table_name, column_id;
    END IF;
END;
$$;


-- user_credentials
CREATE OR REPLACE PROCEDURE update_user_credentials()
    LANGUAGE plpgsql AS
$$
DECLARE
    data_type  varchar;
    table_name varchar := 'user_credentials';
    column_id  varchar := 'id';
    column_user_id varchar := 'user_id';
BEGIN
    data_type := get_column_type(table_name, column_id);
    IF data_type = 'character varying' THEN
        ALTER TABLE user_credentials DROP CONSTRAINT user_credentials_pkey;
        PERFORM column_type_to_uuid(table_name, column_id);
        ALTER TABLE user_credentials ADD COLUMN created_time BIGINT;
        UPDATE user_credentials SET created_time = extract_ts(id) WHERE id is not null;
        ALTER TABLE user_credentials ADD CONSTRAINT user_credentials_pkey PRIMARY KEY (id);
        RAISE NOTICE 'Table % column % updated!', table_name, column_id;
    ELSE
        RAISE NOTICE 'Table % column % already updated!', table_name, column_id;
    END IF;

    data_type := get_column_type(table_name, column_user_id);
    IF data_type = 'character varying' THEN
        ALTER TABLE user_credentials DROP CONSTRAINT user_credentials_user_id_key;
        ALTER TABLE user_credentials RENAME COLUMN user_id TO old_user_id;
        ALTER TABLE user_credentials ADD COLUMN user_id UUID UNIQUE;
        UPDATE user_credentials SET user_id = to_uuid(old_user_id) WHERE old_user_id is not null;
        ALTER TABLE user_credentials DROP COLUMN old_user_id;
        RAISE NOTICE 'Table % column % updated!', table_name, column_user_id;
    ELSE
        RAISE NOTICE 'Table % column % already updated!', table_name, column_user_id;
    END IF;
END;
$$;


-- widget_type
CREATE OR REPLACE PROCEDURE update_widget_type()
    LANGUAGE plpgsql AS
$$
DECLARE
    data_type  varchar;
    table_name varchar := 'widget_type';
    column_id  varchar := 'id';
    column_tenant_id varchar := 'tenant_id';
BEGIN
    data_type := get_column_type(table_name, column_id);
    IF data_type = 'character varying' THEN
        ALTER TABLE widget_type DROP CONSTRAINT widget_type_pkey;
        PERFORM column_type_to_uuid(table_name, column_id);
        ALTER TABLE widget_type ADD COLUMN created_time BIGINT;
        UPDATE widget_type SET created_time = extract_ts(id) WHERE id is not null;
        ALTER TABLE widget_type ADD CONSTRAINT widget_type_pkey PRIMARY KEY (id);
        RAISE NOTICE 'Table % column % updated!', table_name, column_id;
    ELSE
        RAISE NOTICE 'Table % column % already updated!', table_name, column_id;
    END IF;

    data_type := get_column_type(table_name, column_tenant_id);
    IF data_type = 'character varying' THEN
        PERFORM column_type_to_uuid(table_name, column_tenant_id);
        RAISE NOTICE 'Table % column % updated!', table_name, column_tenant_id;
    ELSE
        RAISE NOTICE 'Table % column % already updated!', table_name, column_tenant_id;
    END IF;
END;
$$;


-- widgets_bundle
CREATE OR REPLACE PROCEDURE update_widgets_bundle()
    LANGUAGE plpgsql AS
$$
DECLARE
    data_type  varchar;
    table_name varchar := 'widgets_bundle';
    column_id  varchar := 'id';
    column_tenant_id varchar := 'tenant_id';
BEGIN
    data_type := get_column_type(table_name, column_id);
    IF data_type = 'character varying' THEN
        ALTER TABLE widgets_bundle DROP CONSTRAINT widgets_bundle_pkey;
        PERFORM column_type_to_uuid(table_name, column_id);
        ALTER TABLE widgets_bundle ADD COLUMN created_time BIGINT;
        UPDATE widgets_bundle SET created_time = extract_ts(id) WHERE id is not null;
        ALTER TABLE widgets_bundle ADD CONSTRAINT widgets_bundle_pkey PRIMARY KEY (id);
        RAISE NOTICE 'Table % column % updated!', table_name, column_id;
    ELSE
        RAISE NOTICE 'Table % column % already updated!', table_name, column_id;
    END IF;

    data_type := get_column_type(table_name, column_tenant_id);
    IF data_type = 'character varying' THEN
        PERFORM column_type_to_uuid(table_name, column_tenant_id);
        RAISE NOTICE 'Table % column % updated!', table_name, column_tenant_id;
    ELSE
        RAISE NOTICE 'Table % column % already updated!', table_name, column_tenant_id;
    END IF;
END;
$$;


-- rule_chain
CREATE OR REPLACE PROCEDURE update_rule_chain()
    LANGUAGE plpgsql AS
$$
DECLARE
    data_type  varchar;
    table_name varchar := 'rule_chain';
    column_id  varchar := 'id';
    column_first_rule_node_id varchar := 'first_rule_node_id';
    column_tenant_id varchar := 'tenant_id';
BEGIN
    data_type := get_column_type(table_name, column_id);
    IF data_type = 'character varying' THEN
        ALTER TABLE rule_chain DROP CONSTRAINT rule_chain_pkey;
        PERFORM column_type_to_uuid(table_name, column_id);
        ALTER TABLE rule_chain ADD COLUMN created_time BIGINT;
        UPDATE rule_chain SET created_time = extract_ts(id) WHERE id is not null;
        ALTER TABLE rule_chain ADD CONSTRAINT rule_chain_pkey PRIMARY KEY (id);
        RAISE NOTICE 'Table % column % updated!', table_name, column_id;
    ELSE
        RAISE NOTICE 'Table % column % already updated!', table_name, column_id;
    END IF;

    data_type := get_column_type(table_name, column_first_rule_node_id);
    IF data_type = 'character varying' THEN
        PERFORM column_type_to_uuid(table_name, column_first_rule_node_id);
        RAISE NOTICE 'Table % column % updated!', table_name, column_first_rule_node_id;
    ELSE
        RAISE NOTICE 'Table % column % already updated!', table_name, column_first_rule_node_id;
    END IF;

    data_type := get_column_type(table_name, column_tenant_id);
    IF data_type = 'character varying' THEN
        PERFORM column_type_to_uuid(table_name, column_tenant_id);
        RAISE NOTICE 'Table % column % updated!', table_name, column_tenant_id;
    ELSE
        RAISE NOTICE 'Table % column % already updated!', table_name, column_tenant_id;
    END IF;
END;
$$;


-- rule_node
CREATE OR REPLACE PROCEDURE update_rule_node()
    LANGUAGE plpgsql AS
$$
DECLARE
    data_type  varchar;
    table_name varchar := 'rule_node';
    column_id  varchar := 'id';
    column_rule_chain_id varchar := 'rule_chain_id';
BEGIN
    data_type := get_column_type(table_name, column_id);
    IF data_type = 'character varying' THEN
        ALTER TABLE rule_node DROP CONSTRAINT rule_node_pkey;
        PERFORM column_type_to_uuid(table_name, column_id);
        ALTER TABLE rule_node ADD COLUMN created_time BIGINT;
        UPDATE rule_node SET created_time = extract_ts(id) WHERE id is not null;
        ALTER TABLE rule_node ADD CONSTRAINT rule_node_pkey PRIMARY KEY (id);
        RAISE NOTICE 'Table % column % updated!', table_name, column_id;
    ELSE
        RAISE NOTICE 'Table % column % already updated!', table_name, column_id;
    END IF;

    data_type := get_column_type(table_name, column_rule_chain_id);
    IF data_type = 'character varying' THEN
        PERFORM column_type_to_uuid(table_name, column_rule_chain_id);
        RAISE NOTICE 'Table % column % updated!', table_name, column_rule_chain_id;
    ELSE
        RAISE NOTICE 'Table % column % already updated!', table_name, column_rule_chain_id;
    END IF;
END;
$$;


-- entity_view
CREATE OR REPLACE PROCEDURE update_entity_view()
    LANGUAGE plpgsql AS
$$
DECLARE
    data_type  varchar;
    table_name varchar := 'entity_view';
    column_id  varchar := 'id';
    column_entity_id varchar := 'entity_id';
    column_tenant_id varchar := 'tenant_id';
    column_customer_id varchar := 'customer_id';
BEGIN
    data_type := get_column_type(table_name, column_id);
    IF data_type = 'character varying' THEN
        ALTER TABLE entity_view DROP CONSTRAINT entity_view_pkey;
        PERFORM column_type_to_uuid(table_name, column_id);
        ALTER TABLE entity_view ADD COLUMN created_time BIGINT;
        UPDATE entity_view SET created_time = extract_ts(id) WHERE id is not null;
        ALTER TABLE entity_view ADD CONSTRAINT entity_view_pkey PRIMARY KEY (id);
        RAISE NOTICE 'Table % column % updated!', table_name, column_id;
    ELSE
        RAISE NOTICE 'Table % column % already updated!', table_name, column_id;
    END IF;

    data_type := get_column_type(table_name, column_entity_id);
    IF data_type = 'character varying' THEN
        PERFORM column_type_to_uuid(table_name, column_entity_id);
        RAISE NOTICE 'Table % column % updated!', table_name, column_entity_id;
    ELSE
        RAISE NOTICE 'Table % column % already updated!', table_name, column_entity_id;
    END IF;

    data_type := get_column_type(table_name, column_tenant_id);
    IF data_type = 'character varying' THEN
        PERFORM column_type_to_uuid(table_name, column_tenant_id);
        RAISE NOTICE 'Table % column % updated!', table_name, column_tenant_id;
    ELSE
        RAISE NOTICE 'Table % column % already updated!', table_name, column_tenant_id;
    END IF;

    data_type := get_column_type(table_name, column_customer_id);
    IF data_type = 'character varying' THEN
        PERFORM column_type_to_uuid(table_name, column_customer_id);
        RAISE NOTICE 'Table % column % updated!', table_name, column_customer_id;
    ELSE
        RAISE NOTICE 'Table % column % already updated!', table_name, column_customer_id;
    END IF;
END;
$$;

CREATE TABLE IF NOT EXISTS ts_kv_latest
(
    entity_id uuid   NOT NULL,
    key       int    NOT NULL,
    ts        bigint NOT NULL,
    bool_v    boolean,
    str_v     varchar(10000000),
    long_v    bigint,
    dbl_v     double precision,
    json_v    json,
    CONSTRAINT ts_kv_latest_pkey PRIMARY KEY (entity_id, key)
);

CREATE TABLE IF NOT EXISTS ts_kv_dictionary
(
    key    varchar(255) NOT NULL,
    key_id serial UNIQUE,
    CONSTRAINT ts_key_id_pkey PRIMARY KEY (key)
);

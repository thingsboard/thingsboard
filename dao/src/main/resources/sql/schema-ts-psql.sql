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

CREATE TABLE IF NOT EXISTS ts_kv
(
    entity_id uuid   NOT NULL,
    key       int    NOT NULL,
    ts        bigint NOT NULL,
    bool_v    boolean,
    str_v     varchar(10000000),
    long_v    bigint,
    dbl_v     double precision,
    json_v    json
) PARTITION BY RANGE (ts);

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

CREATE TABLE IF NOT EXISTS tb_schema_settings
(
    schema_version bigint NOT NULL,
    CONSTRAINT tb_schema_settings_pkey PRIMARY KEY (schema_version)
);

INSERT INTO tb_schema_settings (schema_version) VALUES (2005000) ON CONFLICT (schema_version) DO UPDATE SET schema_version = 2005000;

CREATE OR REPLACE PROCEDURE drop_partitions_by_max_ttl(IN partition_type varchar, IN system_ttl bigint, INOUT deleted bigint)
    LANGUAGE plpgsql AS
$$
DECLARE
    max_tenant_ttl            bigint;
    max_customer_ttl          bigint;
    max_ttl                   bigint;
    date                      timestamp;
    partition_by_max_ttl_date varchar;
    partition_month           varchar;
    partition_day             varchar;
    partition_year            varchar;
    partition                 varchar;
    partition_to_delete       varchar;


BEGIN
    SELECT max(attribute_kv.long_v)
    FROM tenant
             INNER JOIN attribute_kv ON tenant.id = attribute_kv.entity_id
    WHERE attribute_kv.attribute_key = 'TTL'
    into max_tenant_ttl;
    SELECT max(attribute_kv.long_v)
    FROM customer
             INNER JOIN attribute_kv ON customer.id = attribute_kv.entity_id
    WHERE attribute_kv.attribute_key = 'TTL'
    into max_customer_ttl;
    max_ttl := GREATEST(system_ttl, max_customer_ttl, max_tenant_ttl);
    if max_ttl IS NOT NULL AND max_ttl > 0 THEN
        date := to_timestamp(EXTRACT(EPOCH FROM current_timestamp) - (max_ttl / 1000));
        partition_by_max_ttl_date := get_partition_by_max_ttl_date(partition_type, date);
        RAISE NOTICE 'Partition by max ttl: %', partition_by_max_ttl_date;
        IF partition_by_max_ttl_date IS NOT NULL THEN
            CASE
                WHEN partition_type = 'DAYS' THEN
                    partition_year := SPLIT_PART(partition_by_max_ttl_date, '_', 3);
                    partition_month := SPLIT_PART(partition_by_max_ttl_date, '_', 4);
                    partition_day := SPLIT_PART(partition_by_max_ttl_date, '_', 5);
                WHEN partition_type = 'MONTHS' THEN
                    partition_year := SPLIT_PART(partition_by_max_ttl_date, '_', 3);
                    partition_month := SPLIT_PART(partition_by_max_ttl_date, '_', 4);
                ELSE
                    partition_year := SPLIT_PART(partition_by_max_ttl_date, '_', 3);
                END CASE;
            FOR partition IN SELECT tablename
                             FROM pg_tables
                             WHERE schemaname = 'public'
                               AND tablename like 'ts_kv_' || '%'
                               AND tablename != 'ts_kv_latest'
                               AND tablename != 'ts_kv_dictionary'
                LOOP
                    IF partition != partition_by_max_ttl_date THEN
                        IF partition_year IS NOT NULL THEN
                            IF SPLIT_PART(partition, '_', 3)::integer < partition_year::integer THEN
                                partition_to_delete := partition;
                            ELSE
                                IF partition_month IS NOT NULL THEN
                                    IF SPLIT_PART(partition, '_', 4)::integer < partition_month::integer THEN
                                        partition_to_delete := partition;
                                    ELSE
                                        IF partition_day IS NOT NULL THEN
                                            IF SPLIT_PART(partition, '_', 5)::integer < partition_day::integer THEN
                                                partition_to_delete := partition;
                                            END IF;
                                        END IF;
                                    END IF;
                                END IF;
                            END IF;
                        END IF;
                    END IF;
                    IF partition_to_delete IS NOT NULL THEN
                        RAISE NOTICE 'Partition to delete by max ttl: %', partition_to_delete;
                        EXECUTE format('DROP TABLE %I', partition_to_delete);
                        deleted := deleted + 1;
                    END IF;
                END LOOP;
        END IF;
    END IF;
END
$$;

CREATE OR REPLACE FUNCTION get_partition_by_max_ttl_date(IN partition_type varchar, IN date timestamp, OUT partition varchar) AS
$$
BEGIN
    CASE
        WHEN partition_type = 'DAYS' THEN
            partition := 'ts_kv_' || to_char(date, 'yyyy') || '_' || to_char(date, 'MM') || '_' || to_char(date, 'dd');
        WHEN partition_type = 'MONTHS' THEN
            partition := 'ts_kv_' || to_char(date, 'yyyy') || '_' || to_char(date, 'MM');
        WHEN partition_type = 'YEARS' THEN
            partition := 'ts_kv_' || to_char(date, 'yyyy');
        WHEN partition_type = 'INDEFINITE' THEN
            partition := NULL;
        ELSE
            partition := NULL;
        END CASE;
    IF partition IS NOT NULL THEN
        IF NOT EXISTS(SELECT
                      FROM pg_tables
                      WHERE schemaname = 'public'
                        AND tablename = partition) THEN
            partition := NULL;
            RAISE NOTICE 'Failed to found partition by ttl';
        END IF;
    END IF;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION to_uuid(IN entity_id varchar, OUT uuid_id uuid) AS
$$
BEGIN
    uuid_id := substring(entity_id, 8, 8) || '-' || substring(entity_id, 4, 4) || '-1' || substring(entity_id, 1, 3) ||
               '-' || substring(entity_id, 16, 4) || '-' || substring(entity_id, 20, 12);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION delete_device_records_from_ts_kv(tenant_id varchar, customer_id varchar, ttl bigint,
                                                            OUT deleted bigint) AS
$$
BEGIN
    EXECUTE format(
            'WITH deleted AS (DELETE FROM ts_kv WHERE entity_id IN (SELECT to_uuid(device.id) as entity_id FROM device WHERE tenant_id = %L and customer_id = %L) AND ts < %L::bigint RETURNING *) SELECT count(*) FROM deleted',
            tenant_id, customer_id, ttl) into deleted;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION delete_asset_records_from_ts_kv(tenant_id varchar, customer_id varchar, ttl bigint,
                                                           OUT deleted bigint) AS
$$
BEGIN
    EXECUTE format(
            'WITH deleted AS (DELETE FROM ts_kv WHERE entity_id IN (SELECT to_uuid(asset.id) as entity_id FROM asset WHERE tenant_id = %L and customer_id = %L) AND ts < %L::bigint RETURNING *) SELECT count(*) FROM deleted',
            tenant_id, customer_id, ttl) into deleted;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION delete_customer_records_from_ts_kv(tenant_id varchar, customer_id varchar, ttl bigint,
                                                              OUT deleted bigint) AS
$$
BEGIN
    EXECUTE format(
            'WITH deleted AS (DELETE FROM ts_kv WHERE entity_id IN (SELECT to_uuid(customer.id) as entity_id FROM customer WHERE tenant_id = %L and id = %L) AND ts < %L::bigint RETURNING *) SELECT count(*) FROM deleted',
            tenant_id, customer_id, ttl) into deleted;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE PROCEDURE cleanup_timeseries_by_ttl(IN null_uuid varchar(31),
                                                      IN system_ttl bigint, INOUT deleted bigint)
    LANGUAGE plpgsql AS
$$
DECLARE
    tenant_cursor CURSOR FOR select tenant.id as tenant_id
                             from tenant;
    tenant_id_record     varchar;
    customer_id_record   varchar;
    tenant_ttl           bigint;
    customer_ttl         bigint;
    deleted_for_entities bigint;
    tenant_ttl_ts        bigint;
    customer_ttl_ts      bigint;
BEGIN
    OPEN tenant_cursor;
    FETCH tenant_cursor INTO tenant_id_record;
    WHILE FOUND
        LOOP
            EXECUTE format(
                    'select attribute_kv.long_v from attribute_kv where attribute_kv.entity_id = %L and attribute_kv.attribute_key = %L',
                    tenant_id_record, 'TTL') INTO tenant_ttl;
            if tenant_ttl IS NULL THEN
                tenant_ttl := system_ttl;
            END IF;
            IF tenant_ttl > 0 THEN
                tenant_ttl_ts := (EXTRACT(EPOCH FROM current_timestamp) * 1000 - tenant_ttl::bigint * 1000)::bigint;
                deleted_for_entities := delete_device_records_from_ts_kv(tenant_id_record, null_uuid, tenant_ttl_ts);
                deleted := deleted + deleted_for_entities;
                RAISE NOTICE '% telemetry removed for devices where tenant_id = %', deleted_for_entities, tenant_id_record;
                deleted_for_entities := delete_asset_records_from_ts_kv(tenant_id_record, null_uuid, tenant_ttl_ts);
                deleted := deleted + deleted_for_entities;
                RAISE NOTICE '% telemetry removed for assets where tenant_id = %', deleted_for_entities, tenant_id_record;
            END IF;
            FOR customer_id_record IN
                SELECT customer.id AS customer_id FROM customer WHERE customer.tenant_id = tenant_id_record
                LOOP
                    EXECUTE format(
                            'select attribute_kv.long_v from attribute_kv where attribute_kv.entity_id = %L and attribute_kv.attribute_key = %L',
                            customer_id_record, 'TTL') INTO customer_ttl;
                    IF customer_ttl IS NULL THEN
                        customer_ttl_ts := tenant_ttl_ts;
                    ELSE
                        IF customer_ttl > 0 THEN
                            customer_ttl_ts :=
                                    (EXTRACT(EPOCH FROM current_timestamp) * 1000 -
                                     customer_ttl::bigint * 1000)::bigint;
                        END IF;
                    END IF;
                    IF customer_ttl_ts IS NOT NULL AND customer_ttl_ts > 0 THEN
                        deleted_for_entities :=
                                delete_customer_records_from_ts_kv(tenant_id_record, customer_id_record,
                                                                   customer_ttl_ts);
                        deleted := deleted + deleted_for_entities;
                        RAISE NOTICE '% telemetry removed for customer with id = % where tenant_id = %', deleted_for_entities, customer_id_record, tenant_id_record;
                        deleted_for_entities :=
                                delete_device_records_from_ts_kv(tenant_id_record, customer_id_record,
                                                                 customer_ttl_ts);
                        deleted := deleted + deleted_for_entities;
                        RAISE NOTICE '% telemetry removed for devices where tenant_id = % and customer_id = %', deleted_for_entities, tenant_id_record, customer_id_record;
                        deleted_for_entities := delete_asset_records_from_ts_kv(tenant_id_record,
                                                                                customer_id_record,
                                                                                customer_ttl_ts);
                        deleted := deleted + deleted_for_entities;
                        RAISE NOTICE '% telemetry removed for assets where tenant_id = % and customer_id = %', deleted_for_entities, tenant_id_record, customer_id_record;
                    END IF;
                END LOOP;
            FETCH tenant_cursor INTO tenant_id_record;
        END LOOP;
END
$$;

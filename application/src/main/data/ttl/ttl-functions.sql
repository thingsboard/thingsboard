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

CREATE OR REPLACE FUNCTION delete_from_ts_kv(entity_id uuid, ttl bigint, OUT deleted integer) AS
$$
BEGIN
    EXECUTE format(
            'WITH deleted AS (DELETE FROM ts_kv WHERE entity_id = %L::uuid AND ts < EXTRACT(EPOCH FROM current_timestamp) * 1000 - %L::bigint * 1000 RETURNING *) SELECT count(*) FROM deleted',
            entity_id, ttl) into deleted;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE PROCEDURE cleanup_devices_timeseries_by_ttl(IN null_uuid varchar(31), IN system_ttl bigint, INOUT deleted bigint)
    LANGUAGE plpgsql AS
$$
DECLARE
    query_cursor CURSOR FOR SELECT device.id          as device_id,
                                   device.tenant_id   as tenant_id,
                                   device.customer_id as customer_id
                            FROM device;
    device_record RECORD;
    ttl           bigint;
BEGIN
    OPEN query_cursor;
    FETCH query_cursor INTO device_record;
    WHILE FOUND
        LOOP
            IF device_record.customer_id = null_uuid THEN
                SELECT coalesce(attribute_kv.long_v, 0) AS ttl
                FROM attribute_kv
                WHERE attribute_kv.entity_id = device_record.tenant_id
                  AND attribute_kv.attribute_key = 'TTL'
                INTO ttl;
            ELSE
                SELECT coalesce(attribute_kv.long_v, 0) AS ttl
                FROM attribute_kv
                WHERE attribute_kv.entity_id = device_record.customer_id
                  AND attribute_kv.attribute_key = 'TTL'
                INTO ttl;
                IF ttl IS NULL THEN
                    SELECT coalesce(attribute_kv.long_v, 0) AS ttl
                    FROM attribute_kv
                    WHERE attribute_kv.entity_id = device_record.tenant_id
                      AND attribute_kv.attribute_key = 'TTL'
                    INTO ttl;
                END IF;
            END IF;
            IF ttl IS NULL THEN
                ttl = system_ttl;
            END IF;
            IF ttl > 0 THEN
                deleted := deleted + delete_from_ts_kv(to_uuid(device_record.device_id), ttl);
            END IF;
            FETCH query_cursor INTO device_record;
        END LOOP;
    RAISE NOTICE '% records have been removed from ts_kv!', deleted;
END
$$;

CREATE OR REPLACE PROCEDURE cleanup_assets_timeseries_by_ttl(IN null_uuid varchar(31), IN system_ttl bigint, INOUT deleted bigint)
    LANGUAGE plpgsql AS
$$
DECLARE
    query_cursor CURSOR FOR SELECT asset.id          as asset_id,
                                   asset.tenant_id   as tenant_id,
                                   asset.customer_id as customer_id
                            FROM asset;
    asset_record RECORD;
    ttl          bigint;
BEGIN
    OPEN query_cursor;
    FETCH query_cursor INTO asset_record;
    WHILE FOUND
        LOOP
            IF asset_record.customer_id = null_uuid THEN
                SELECT coalesce(attribute_kv.long_v, 0) AS ttl
                FROM attribute_kv
                WHERE attribute_kv.entity_id = asset_record.tenant_id
                  AND attribute_kv.attribute_key = 'TTL'
                INTO ttl;
            ELSE
                SELECT coalesce(attribute_kv.long_v, 0) AS ttl
                FROM attribute_kv
                WHERE attribute_kv.entity_id = asset_record.customer_id
                  AND attribute_kv.attribute_key = 'TTL'
                INTO ttl;
                IF ttl IS NULL THEN
                    SELECT coalesce(attribute_kv.long_v, 0) AS ttl
                    FROM attribute_kv
                    WHERE attribute_kv.entity_id = asset_record.tenant_id
                      AND attribute_kv.attribute_key = 'TTL'
                    INTO ttl;
                END IF;
            END IF;
            IF ttl IS NULL THEN
                ttl = system_ttl;
            END IF;
            IF ttl > 0 THEN
                deleted := deleted + delete_from_ts_kv(to_uuid(asset_record.asset_id), ttl);
            END IF;
            FETCH query_cursor INTO asset_record;
        END LOOP;
    RAISE NOTICE '% records have been removed from ts_kv!', deleted;
END
$$;

CREATE OR REPLACE PROCEDURE cleanup_customers_timeseries_by_ttl(IN system_ttl bigint, INOUT deleted bigint)
    LANGUAGE plpgsql AS
$$
DECLARE
    query_cursor CURSOR FOR SELECT customer.id        as customer_id,
                                   customer.tenant_id as tenant_id
                            FROM customer;
    customer_record RECORD;
    ttl             bigint;
BEGIN
    OPEN query_cursor;
    FETCH query_cursor INTO customer_record;
    WHILE FOUND
        LOOP
            SELECT coalesce(attribute_kv.long_v, 0) AS ttl
            FROM attribute_kv
            WHERE attribute_kv.entity_id = customer_record.customer_id
              AND attribute_kv.attribute_key = 'TTL'
            INTO ttl;
            IF ttl IS NULL THEN
                SELECT coalesce(attribute_kv.long_v, 0) AS ttl
                FROM attribute_kv
                WHERE attribute_kv.entity_id = customer_record.tenant_id
                  AND attribute_kv.attribute_key = 'TTL'
                INTO ttl;
            END IF;
            IF ttl IS NULL THEN
                ttl = system_ttl;
            END IF;
            IF ttl > 0 THEN
                deleted := deleted + delete_from_ts_kv(to_uuid(customer_record.customer_id), ttl);
            END IF;
            FETCH query_cursor INTO customer_record;
        END LOOP;
    RAISE NOTICE '% records have been removed from ts_kv!', deleted;
END
$$;


CREATE OR REPLACE PROCEDURE drop_empty_partitions(INOUT deleted bigint)
    LANGUAGE plpgsql AS
$$
DECLARE
    partitions_cursor CURSOR FOR SELECT tablename
                                 FROM pg_tables
                                 WHERE schemaname = 'public'
                                   AND tablename like 'ts_kv_' || '%'
                                   AND tablename != 'ts_kv_latest'
                                   AND tablename != 'ts_kv_dictionary';
    partition_name varchar;
    validation_record RECORD;

BEGIN
    OPEN partitions_cursor;
    FETCH partitions_cursor INTO partition_name;
    WHILE FOUND
        LOOP
            EXECUTE format(
                    'SELECT * FROM %I LIMIT 1',
                    partition_name) into validation_record;
            IF validation_record IS NULL THEN
                deleted = deleted + 1;
                EXECUTE format('DROP TABLE %I', partition_name);
                RAISE NOTICE '% partition dropped!', partition_name;
            END IF;
            FETCH partitions_cursor INTO partition_name;
        END LOOP;
END
$$;
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

CREATE OR REPLACE FUNCTION delete_from_ts_kv(entity_id uuid, ttl bigint,
                                             OUT deleted bigint) AS
$$
BEGIN
    EXECUTE format(
            'WITH deleted AS (DELETE FROM ts_kv WHERE entity_id = %L::uuid AND ts < EXTRACT(EPOCH FROM current_timestamp) * 1000 - %L::bigint * 1000 RETURNING *) SELECT count(*) FROM deleted',
            entity_id, ttl) into deleted;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE PROCEDURE cleanup_timeseries_by_ttl(IN null_uuid varchar(31), IN system_ttl bigint, INOUT deleted bigint)
    LANGUAGE plpgsql AS
$$
DECLARE
    tenant_cursor CURSOR FOR select tenant.id as tenant_id
                             from tenant;
    tenant_id_record   varchar;
    customer_id_record varchar;
    entity_id_record   uuid;
    tenant_ttl         bigint;
    customer_ttl       bigint;
    deleted_per_entity bigint;
BEGIN
    OPEN tenant_cursor;
    FETCH tenant_cursor INTO tenant_id_record;
    WHILE FOUND
        LOOP
            EXECUTE format(
                    'select attribute_kv.long_v from attribute_kv where attribute_kv.entity_id = %L and attribute_kv.attribute_key = %L',
                    tenant_id_record, 'TTL') INTO tenant_ttl;
            if tenant_ttl IS NULL THEN
                tenant_ttl = system_ttl;
            END IF;
            IF tenant_ttl > 0 THEN
                FOR entity_id_record IN
                    SELECT to_uuid(device.id)
                    FROM device
                    WHERE device.tenant_id = tenant_id_record
                      and device.customer_id = null_uuid
                    LOOP
                        deleted_per_entity = delete_from_ts_kv(entity_id_record, tenant_ttl);
                        deleted := deleted + deleted_per_entity;
                        RAISE NOTICE '% telemetry removed for device with id: % and tenant_id: %', deleted_per_entity, entity_id_record, tenant_id_record;
                    END LOOP;
                FOR entity_id_record IN
                    SELECT to_uuid(asset.id)
                    FROM asset
                    WHERE asset.tenant_id = tenant_id_record
                      AND asset.customer_id = null_uuid
                    LOOP
                        deleted_per_entity = delete_from_ts_kv(entity_id_record, tenant_ttl);
                        deleted := deleted + deleted_per_entity;
                        RAISE NOTICE '% telemetry removed for asset with id: % and tenant_id: %', deleted_per_entity, entity_id_record, tenant_id_record;
                    END LOOP;
            END IF;
            FOR customer_id_record IN
                SELECT customer.id AS customer_id FROM customer WHERE customer.tenant_id = tenant_id_record
                LOOP
                    EXECUTE format(
                            'select attribute_kv.long_v from attribute_kv where attribute_kv.entity_id = %L and attribute_kv.attribute_key = %L',
                            customer_id_record, 'TTL') INTO customer_ttl;
                    IF customer_ttl IS NULL THEN
                        customer_ttl = tenant_ttl;
                    END IF;
                    IF customer_ttl > 0 THEN
                        deleted_per_entity = delete_from_ts_kv(to_uuid(customer_id_record), customer_ttl);
                        deleted := deleted + deleted_per_entity;
                        RAISE NOTICE '% telemetry removed for customer with id: % and tenant_id: %', deleted_per_entity, customer_id_record, tenant_id_record;
                        FOR entity_id_record IN
                            SELECT to_uuid(device.id)
                            FROM device
                            WHERE device.tenant_id = tenant_id_record
                              and device.customer_id = customer_id_record
                            LOOP
                                deleted_per_entity = delete_from_ts_kv(entity_id_record, customer_ttl);
                                deleted := deleted + deleted_per_entity;
                                RAISE NOTICE '% telemetry removed for device with id: % and tenant_id: % and customer_id: %', deleted_per_entity, entity_id_record, tenant_id_record, customer_id_record;
                            END LOOP;
                        FOR entity_id_record IN
                            SELECT to_uuid(asset.id)
                            FROM asset
                            WHERE asset.tenant_id = tenant_id_record
                              and asset.customer_id = customer_id_record
                            LOOP
                                deleted_per_entity = delete_from_ts_kv(entity_id_record, customer_ttl);
                                deleted := deleted + deleted_per_entity;
                                RAISE NOTICE '% telemetry removed for asset with id: % and tenant_id: % and customer_id: %', deleted_per_entity, entity_id_record, tenant_id_record, customer_id_record;
                            END LOOP;
                    END IF;
                END LOOP;
            FETCH tenant_cursor INTO tenant_id_record;
        END LOOP;
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
    partition_name    varchar;
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
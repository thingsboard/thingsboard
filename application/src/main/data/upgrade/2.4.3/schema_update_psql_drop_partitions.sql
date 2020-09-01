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
                               AND tablename != 'ts_kv_indefinite'
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

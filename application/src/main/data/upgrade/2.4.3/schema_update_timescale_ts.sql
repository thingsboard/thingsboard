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

-- select check_version();

CREATE OR REPLACE FUNCTION check_version() RETURNS boolean AS $$
DECLARE
    current_version integer;
    valid_version boolean;
BEGIN
    RAISE NOTICE 'Check the current installed PostgreSQL version...';
    SELECT current_setting('server_version_num') INTO current_version;
    IF current_version < 90600 THEN
        valid_version := FALSE;
    ELSE
        valid_version := TRUE;
    END IF;
    IF valid_version = FALSE THEN
        RAISE NOTICE 'Postgres version should be at least more than 9.6!';
    ELSE
        RAISE NOTICE 'PostgreSQL version is valid!';
        RAISE NOTICE 'Schema update started...';
    END IF;
    RETURN valid_version;
END;
$$ LANGUAGE 'plpgsql';

-- select create_new_tenant_ts_kv_table();

CREATE OR REPLACE FUNCTION create_new_tenant_ts_kv_table() RETURNS VOID AS $$

BEGIN
  ALTER TABLE tenant_ts_kv
    RENAME TO tenant_ts_kv_old;
  CREATE TABLE IF NOT EXISTS tenant_ts_kv
  (
    LIKE tenant_ts_kv_old
  );
  ALTER TABLE tenant_ts_kv
    ALTER COLUMN tenant_id TYPE uuid USING tenant_id::uuid;
  ALTER TABLE tenant_ts_kv
    ALTER COLUMN entity_id TYPE uuid USING entity_id::uuid;
  ALTER TABLE tenant_ts_kv
    ALTER COLUMN key TYPE integer USING key::integer;
  ALTER TABLE tenant_ts_kv
    ADD CONSTRAINT tenant_ts_kv_pkey PRIMARY KEY(tenant_id, entity_id, key, ts);
  ALTER INDEX idx_tenant_ts_kv RENAME TO idx_tenant_ts_kv_old;
  ALTER INDEX tenant_ts_kv_ts_idx RENAME TO tenant_ts_kv_ts_idx_old;
--   PERFORM create_hypertable('tenant_ts_kv', 'ts', chunk_time_interval => 86400000, if_not_exists => true);
  CREATE INDEX IF NOT EXISTS idx_tenant_ts_kv ON tenant_ts_kv(tenant_id, entity_id, key, ts);
END;
$$ LANGUAGE 'plpgsql';


-- select create_ts_kv_latest_table();

CREATE OR REPLACE FUNCTION create_ts_kv_latest_table() RETURNS VOID AS $$

BEGIN
    CREATE TABLE IF NOT EXISTS ts_kv_latest
    (
        entity_id uuid NOT NULL,
        key int NOT NULL,
        ts bigint NOT NULL,
        bool_v boolean,
        str_v varchar(10000000),
        long_v bigint,
        dbl_v double precision,
        CONSTRAINT ts_kv_latest_pkey PRIMARY KEY (entity_id, key)
    );
END;
$$ LANGUAGE 'plpgsql';


-- select create_ts_kv_dictionary_table();

CREATE OR REPLACE FUNCTION create_ts_kv_dictionary_table() RETURNS VOID AS $$

BEGIN
  CREATE TABLE IF NOT EXISTS ts_kv_dictionary
  (
    key    varchar(255) NOT NULL,
    key_id serial UNIQUE,
    CONSTRAINT ts_key_id_pkey PRIMARY KEY (key)
  );
END;
$$ LANGUAGE 'plpgsql';

-- select insert_into_dictionary();

CREATE OR REPLACE FUNCTION insert_into_dictionary() RETURNS VOID AS
$$
DECLARE
    insert_record RECORD;
    key_cursor CURSOR FOR SELECT DISTINCT key
                          FROM tenant_ts_kv_old
                          ORDER BY key;
BEGIN
    OPEN key_cursor;
    LOOP
        FETCH key_cursor INTO insert_record;
        EXIT WHEN NOT FOUND;
        IF NOT EXISTS(SELECT key FROM ts_kv_dictionary WHERE key = insert_record.key) THEN
            INSERT INTO ts_kv_dictionary(key) VALUES (insert_record.key);
            RAISE NOTICE 'Key: % has been inserted into the dictionary!',insert_record.key;
        ELSE
            RAISE NOTICE 'Key: % already exists in the dictionary!',insert_record.key;
        END IF;
    END LOOP;
    CLOSE key_cursor;
END;
$$ language 'plpgsql';

-- select insert_into_tenant_ts_kv();

CREATE OR REPLACE FUNCTION insert_into_tenant_ts_kv() RETURNS void AS
$$
DECLARE
    insert_size CONSTANT integer := 10000;
    insert_counter       integer DEFAULT 0;
    insert_record        RECORD;
    insert_cursor CURSOR FOR SELECT CONCAT(tenant_id_first_part_uuid, '-', tenant_id_second_part_uuid, '-1', tenant_id_third_part_uuid, '-', tenant_id_fourth_part_uuid, '-', tenant_id_fifth_part_uuid)::uuid AS tenant_id,
                                    CONCAT(entity_id_first_part_uuid, '-', entity_id_second_part_uuid, '-1', entity_id_third_part_uuid, '-', entity_id_fourth_part_uuid, '-', entity_id_fifth_part_uuid)::uuid AS entity_id,
                                    tenant_ts_kv_records.key                                                         AS key,
                                    tenant_ts_kv_records.ts                                                          AS ts,
                                    tenant_ts_kv_records.bool_v                                                      AS bool_v,
                                    tenant_ts_kv_records.str_v                                                       AS str_v,
                                    tenant_ts_kv_records.long_v                                                      AS long_v,
                                    tenant_ts_kv_records.dbl_v                                                       AS dbl_v
                             FROM (SELECT SUBSTRING(tenant_id, 8, 8)  AS tenant_id_first_part_uuid,
                                          SUBSTRING(tenant_id, 4, 4)  AS tenant_id_second_part_uuid,
                                          SUBSTRING(tenant_id, 1, 3)  AS tenant_id_third_part_uuid,
                                          SUBSTRING(tenant_id, 16, 4) AS tenant_id_fourth_part_uuid,
                                          SUBSTRING(tenant_id, 20)    AS tenant_id_fifth_part_uuid,
                                          SUBSTRING(entity_id, 8, 8)  AS entity_id_first_part_uuid,
                                          SUBSTRING(entity_id, 4, 4)  AS entity_id_second_part_uuid,
                                          SUBSTRING(entity_id, 1, 3)  AS entity_id_third_part_uuid,
                                          SUBSTRING(entity_id, 16, 4) AS entity_id_fourth_part_uuid,
                                          SUBSTRING(entity_id, 20)    AS entity_id_fifth_part_uuid,
                                          key_id                      AS key,
                                          ts,
                                          bool_v,
                                          str_v,
                                          long_v,
                                          dbl_v
                                   FROM tenant_ts_kv_old
                                            INNER JOIN ts_kv_dictionary ON (tenant_ts_kv_old.key = ts_kv_dictionary.key)) AS tenant_ts_kv_records;
BEGIN
    OPEN insert_cursor;
    LOOP
        insert_counter := insert_counter + 1;
        FETCH insert_cursor INTO insert_record;
        IF NOT FOUND THEN
            RAISE NOTICE '% records have been inserted into the new tenant_ts_kv table!',insert_counter - 1;
            EXIT;
        END IF;
        INSERT INTO tenant_ts_kv(tenant_id, entity_id, key, ts, bool_v, str_v, long_v, dbl_v)
        VALUES (insert_record.tenant_id, insert_record.entity_id, insert_record.key, insert_record.ts, insert_record.bool_v, insert_record.str_v,
                insert_record.long_v, insert_record.dbl_v);
        IF MOD(insert_counter, insert_size) = 0 THEN
            RAISE NOTICE '% records have been inserted into the new tenant_ts_kv table!',insert_counter;
        END IF;
    END LOOP;
    CLOSE insert_cursor;
END;
$$ LANGUAGE 'plpgsql';

-- select insert_into_ts_kv_latest();

CREATE OR REPLACE FUNCTION insert_into_ts_kv_latest() RETURNS void AS
$$
DECLARE
    insert_size CONSTANT integer := 10000;
    insert_counter       integer DEFAULT 0;
    latest_record        RECORD;
    insert_record        RECORD;
    insert_cursor CURSOR FOR SELECT
                                    latest_records.key          AS key,
                                    latest_records.entity_id    AS entity_id,
                                    latest_records.ts           AS ts
                             FROM (SELECT DISTINCT key AS key, entity_id AS entity_id, MAX(ts) AS ts FROM tenant_ts_kv GROUP BY key, entity_id) AS latest_records;
BEGIN
    OPEN insert_cursor;
    LOOP
        insert_counter := insert_counter + 1;
        FETCH insert_cursor INTO latest_record;
        IF NOT FOUND THEN
            RAISE NOTICE '% records have been inserted into the ts_kv_latest table!',insert_counter - 1;
            EXIT;
        END IF;
        SELECT entity_id AS entity_id, key AS key, ts AS ts, bool_v AS bool_v, str_v AS str_v, long_v AS long_v, dbl_v AS dbl_v INTO insert_record FROM tenant_ts_kv WHERE entity_id = latest_record.entity_id AND key = latest_record.key AND ts = latest_record.ts;
        INSERT INTO ts_kv_latest(entity_id, key, ts, bool_v, str_v, long_v, dbl_v)
        VALUES (insert_record.entity_id, insert_record.key, insert_record.ts, insert_record.bool_v, insert_record.str_v, insert_record.long_v, insert_record.dbl_v);
        IF MOD(insert_counter, insert_size) = 0 THEN
            RAISE NOTICE '% records have been inserted into the ts_kv_latest table!',insert_counter;
        END IF;
    END LOOP;
    CLOSE insert_cursor;
END;
$$ LANGUAGE 'plpgsql';

--
-- Copyright © 2016-2021 The Thingsboard Authors
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

-- call create_new_ts_kv_table();

CREATE OR REPLACE PROCEDURE create_new_ts_kv_table() LANGUAGE plpgsql AS $$

BEGIN
  ALTER TABLE tenant_ts_kv
    RENAME TO tenant_ts_kv_old;
  CREATE TABLE IF NOT EXISTS ts_kv
  (
    LIKE tenant_ts_kv_old
  );
  ALTER TABLE ts_kv ALTER COLUMN entity_id TYPE uuid USING entity_id::uuid;
  ALTER TABLE ts_kv ALTER COLUMN key TYPE integer USING key::integer;
  ALTER INDEX ts_kv_pkey RENAME TO tenant_ts_kv_pkey_old;
  ALTER INDEX idx_tenant_ts_kv RENAME TO idx_tenant_ts_kv_old;
  ALTER INDEX tenant_ts_kv_ts_idx RENAME TO tenant_ts_kv_ts_idx_old;
  ALTER TABLE ts_kv ADD CONSTRAINT ts_kv_pkey PRIMARY KEY(entity_id, key, ts);
--   CREATE INDEX IF NOT EXISTS ts_kv_ts_idx ON ts_kv(ts DESC);
  ALTER TABLE ts_kv DROP COLUMN IF EXISTS tenant_id;
END;
$$;


-- call create_ts_kv_latest_table();

CREATE OR REPLACE PROCEDURE create_ts_kv_latest_table() LANGUAGE plpgsql AS $$

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
$$;


-- call create_ts_kv_dictionary_table();

CREATE OR REPLACE PROCEDURE create_ts_kv_dictionary_table() LANGUAGE plpgsql AS $$

BEGIN
  CREATE TABLE IF NOT EXISTS ts_kv_dictionary
  (
    key    varchar(255) NOT NULL,
    key_id serial UNIQUE,
    CONSTRAINT ts_key_id_pkey PRIMARY KEY (key)
  );
END;
$$;

-- call insert_into_dictionary();

CREATE OR REPLACE PROCEDURE insert_into_dictionary() LANGUAGE plpgsql AS $$

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
$$;

CREATE OR REPLACE FUNCTION to_uuid(IN entity_id varchar, OUT uuid_id uuid) AS
$$
BEGIN
    uuid_id := substring(entity_id, 8, 8) || '-' || substring(entity_id, 4, 4) || '-1' || substring(entity_id, 1, 3) ||
               '-' || substring(entity_id, 16, 4) || '-' || substring(entity_id, 20, 12);
END;
$$ LANGUAGE plpgsql;

-- call insert_into_ts_kv();

CREATE OR REPLACE PROCEDURE insert_into_ts_kv(IN path_to_file varchar) LANGUAGE plpgsql AS $$
BEGIN

    EXECUTE format ('COPY (SELECT to_uuid(entity_id)                                     AS entity_id,
           new_ts_kv_records.key                                                         AS key,
           new_ts_kv_records.ts                                                          AS ts,
           new_ts_kv_records.bool_v                                                      AS bool_v,
           new_ts_kv_records.str_v                                                       AS str_v,
           new_ts_kv_records.long_v                                                      AS long_v,
           new_ts_kv_records.dbl_v                                                       AS dbl_v
    FROM (SELECT entity_id                   AS entity_id,
                 key_id                      AS key,
                 ts,
                 bool_v,
                 str_v,
                 long_v,
                 dbl_v
          FROM tenant_ts_kv_old
                   INNER JOIN ts_kv_dictionary ON (tenant_ts_kv_old.key = ts_kv_dictionary.key)) AS new_ts_kv_records) TO %L;', path_to_file);
    EXECUTE format ('COPY ts_kv FROM %L', path_to_file);
END;
$$;

-- call insert_into_ts_kv_latest();

CREATE OR REPLACE PROCEDURE insert_into_ts_kv_latest() LANGUAGE plpgsql AS $$

DECLARE
    insert_size CONSTANT integer := 10000;
    insert_counter       integer DEFAULT 0;
    latest_record        RECORD;
    insert_record        RECORD;
    insert_cursor CURSOR FOR SELECT
                                    latest_records.key          AS key,
                                    latest_records.entity_id    AS entity_id,
                                    latest_records.ts           AS ts
                             FROM (SELECT DISTINCT key AS key, entity_id AS entity_id, MAX(ts) AS ts FROM ts_kv GROUP BY key, entity_id) AS latest_records;
BEGIN
    OPEN insert_cursor;
    LOOP
        insert_counter := insert_counter + 1;
        FETCH insert_cursor INTO latest_record;
        IF NOT FOUND THEN
            RAISE NOTICE '% records have been inserted into the ts_kv_latest table!',insert_counter - 1;
            EXIT;
        END IF;
        SELECT entity_id AS entity_id, key AS key, ts AS ts, bool_v AS bool_v, str_v AS str_v, long_v AS long_v, dbl_v AS dbl_v INTO insert_record FROM ts_kv WHERE entity_id = latest_record.entity_id AND key = latest_record.key AND ts = latest_record.ts;
        INSERT INTO ts_kv_latest(entity_id, key, ts, bool_v, str_v, long_v, dbl_v)
        VALUES (insert_record.entity_id, insert_record.key, insert_record.ts, insert_record.bool_v, insert_record.str_v, insert_record.long_v, insert_record.dbl_v);
        IF MOD(insert_counter, insert_size) = 0 THEN
            RAISE NOTICE '% records have been inserted into the ts_kv_latest table!',insert_counter;
        END IF;
    END LOOP;
    CLOSE insert_cursor;
END;
$$;

-- call insert_into_ts_kv_cursor();

CREATE OR REPLACE PROCEDURE insert_into_ts_kv_cursor() LANGUAGE plpgsql AS $$

DECLARE
    insert_size CONSTANT integer := 10000;
    insert_counter       integer DEFAULT 0;
    insert_record        RECORD;
    insert_cursor CURSOR FOR SELECT to_uuid(entity_id)                                                            AS entity_id,
                                    new_ts_kv_records.key                                                         AS key,
                                    new_ts_kv_records.ts                                                          AS ts,
                                    new_ts_kv_records.bool_v                                                      AS bool_v,
                                    new_ts_kv_records.str_v                                                       AS str_v,
                                    new_ts_kv_records.long_v                                                      AS long_v,
                                    new_ts_kv_records.dbl_v                                                       AS dbl_v
                             FROM (SELECT entity_id                   AS entity_id,
                                          key_id                      AS key,
                                          ts,
                                          bool_v,
                                          str_v,
                                          long_v,
                                          dbl_v
                                   FROM tenant_ts_kv_old
                                            INNER JOIN ts_kv_dictionary ON (tenant_ts_kv_old.key = ts_kv_dictionary.key)) AS new_ts_kv_records;
BEGIN
    OPEN insert_cursor;
    LOOP
        insert_counter := insert_counter + 1;
        FETCH insert_cursor INTO insert_record;
        IF NOT FOUND THEN
            RAISE NOTICE '% records have been inserted into the new ts_kv table!',insert_counter - 1;
            EXIT;
        END IF;
        INSERT INTO ts_kv(entity_id, key, ts, bool_v, str_v, long_v, dbl_v)
        VALUES (insert_record.entity_id, insert_record.key, insert_record.ts, insert_record.bool_v, insert_record.str_v,
                insert_record.long_v, insert_record.dbl_v);
        IF MOD(insert_counter, insert_size) = 0 THEN
            RAISE NOTICE '% records have been inserted into the new ts_kv table!',insert_counter;
        END IF;
    END LOOP;
    CLOSE insert_cursor;
END;
$$;
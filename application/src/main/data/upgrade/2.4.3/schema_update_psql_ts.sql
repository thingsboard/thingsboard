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

-- load function check_version()

CREATE OR REPLACE FUNCTION check_version() RETURNS boolean AS $$
DECLARE
    current_version integer;
    valid_version boolean;
BEGIN
    RAISE NOTICE 'Check the current installed PostgreSQL version...';
    SELECT current_setting('server_version_num') INTO current_version;
    IF current_version < 100000 THEN
        valid_version := FALSE;
    ELSE
        valid_version := TRUE;
    END IF;
    IF valid_version = FALSE THEN
        RAISE NOTICE 'Postgres version should be at least more than 10!';
    ELSE
        RAISE NOTICE 'PostgreSQL version is valid!';
        RAISE NOTICE 'Schema update started...';
    END IF;
    RETURN valid_version;
END;
$$ LANGUAGE 'plpgsql';

-- load function create_partition_table()

CREATE OR REPLACE FUNCTION create_partition_table() RETURNS VOID AS $$

BEGIN
  ALTER TABLE ts_kv
    RENAME TO ts_kv_old;
  CREATE TABLE IF NOT EXISTS ts_kv
  (
    LIKE ts_kv_old
  )
    PARTITION BY RANGE (ts);
  ALTER TABLE ts_kv
    DROP COLUMN entity_type;
  ALTER TABLE ts_kv
    ALTER COLUMN entity_id TYPE uuid USING entity_id::uuid;
  ALTER TABLE ts_kv
    ALTER COLUMN key TYPE integer USING key::integer;
END;
$$ LANGUAGE 'plpgsql';


-- load function create_partitions()

CREATE OR REPLACE FUNCTION create_partitions() RETURNS VOID AS
$$
DECLARE
    partition_date varchar;
    from_ts        bigint;
    to_ts          bigint;
    key_cursor CURSOR FOR select SUBSTRING(month_date.first_date, 1, 7)                        AS partition_date,
                                 extract(epoch from (month_date.first_date)::timestamp) * 1000 as from_ts,
                                 extract(epoch from (month_date.first_date::date + INTERVAL '1 MONTH')::timestamp) *
                                 1000                                                          as to_ts
                          FROM (SELECT DISTINCT TO_CHAR(TO_TIMESTAMP(ts / 1000), 'YYYY_MM_01') AS first_date
                                FROM ts_kv_old) AS month_date;
BEGIN
    OPEN key_cursor;
    LOOP
        FETCH key_cursor INTO partition_date, from_ts, to_ts;
        EXIT WHEN NOT FOUND;
        EXECUTE 'CREATE TABLE IF NOT EXISTS ts_kv_' || partition_date ||
                ' PARTITION OF ts_kv(PRIMARY KEY (entity_id, key, ts)) FOR VALUES FROM (' || from_ts ||
                ') TO (' || to_ts || ');';
        RAISE NOTICE 'A partition % has been created!',CONCAT('ts_kv_', partition_date);
    END LOOP;

    CLOSE key_cursor;
END;
$$ language 'plpgsql';

-- load function create_ts_kv_dictionary_table()

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

-- load function insert_into_dictionary()

CREATE OR REPLACE FUNCTION insert_into_dictionary() RETURNS VOID AS
$$
DECLARE
    insert_record RECORD;
    key_cursor CURSOR FOR SELECT DISTINCT key
                          FROM ts_kv_old
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

-- load function insert_into_ts_kv()

CREATE OR REPLACE FUNCTION insert_into_ts_kv() RETURNS void AS
$$
DECLARE
    insert_size CONSTANT integer := 10000;
    insert_counter       integer DEFAULT 0;
    insert_record        RECORD;
    insert_cursor CURSOR FOR SELECT CONCAT(first, '-', second, '-1', third, '-', fourth, '-', fifth)::uuid AS entity_id,
                                    substrings.key                                                         AS key,
                                    substrings.ts                                                          AS ts,
                                    substrings.bool_v                                                      AS bool_v,
                                    substrings.str_v                                                       AS str_v,
                                    substrings.long_v                                                      AS long_v,
                                    substrings.dbl_v                                                       AS dbl_v
                             FROM (SELECT SUBSTRING(entity_id, 8, 8)  AS first,
                                          SUBSTRING(entity_id, 4, 4)  AS second,
                                          SUBSTRING(entity_id, 1, 3)  AS third,
                                          SUBSTRING(entity_id, 16, 4) AS fourth,
                                          SUBSTRING(entity_id, 20)    AS fifth,
                                          key_id                      AS key,
                                          ts,
                                          bool_v,
                                          str_v,
                                          long_v,
                                          dbl_v
                                   FROM ts_kv_old
                                            INNER JOIN ts_kv_dictionary ON (ts_kv_old.key = ts_kv_dictionary.key)) AS substrings;
BEGIN
    OPEN insert_cursor;
    LOOP
        insert_counter := insert_counter + 1;
        FETCH insert_cursor INTO insert_record;
        IF NOT FOUND THEN
            RAISE NOTICE '% records have been inserted into the partitioned ts_kv!',insert_counter - 1;
            EXIT;
        END IF;
        INSERT INTO ts_kv(entity_id, key, ts, bool_v, str_v, long_v, dbl_v)
        VALUES (insert_record.entity_id, insert_record.key, insert_record.ts, insert_record.bool_v, insert_record.str_v,
                insert_record.long_v, insert_record.dbl_v);
        IF MOD(insert_counter, insert_size) = 0 THEN
            RAISE NOTICE '% records have been inserted into the partitioned ts_kv!',insert_counter;
        END IF;
    END LOOP;
    CLOSE insert_cursor;
END;
$$ LANGUAGE 'plpgsql';



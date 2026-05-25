--
-- Copyright © 2016-2026 The Thingsboard Authors
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

-- CALCULATED FIELD ADDITIONAL INFO ADDITION START

ALTER TABLE calculated_field ADD COLUMN IF NOT EXISTS additional_info varchar;

-- CALCULATED FIELD ADDITIONAL INFO ADDITION END

-- RULE CHAIN NOTES MIGRATION START

ALTER TABLE rule_chain ADD COLUMN IF NOT EXISTS notes varchar(1000000);

-- RULE CHAIN NOTES MIGRATION END

-- CLEANUP ORPHANED OTA PACKAGE LARGE OBJECTS START

-- Cleans up orphaned PostgreSQL large objects no longer referenced by ota_package.
-- These accumulate when OTA packages are deleted or updated and can consume significant disk space.
-- Note: only the ota_package.data column uses PostgreSQL large objects (OID type) in ThingsBoard.
-- This script removes all large objects not referenced by ota_package.data.
-- If external applications sharing this database also use large objects, their objects WILL be deleted.
--
-- Processes orphans in batches with a COMMIT between each batch. Does not block ota_package
-- (only row-level locks on pg_largeobject rows being deleted), so it is safe to run on a live
-- server. Each batch releases row locks, flushes WAL, and advances the xmin horizon.

CREATE OR REPLACE PROCEDURE cleanup_orphan_ota_lobs(batch_size int DEFAULT 500)
LANGUAGE plpgsql AS
$$
DECLARE
    orphan_oid bigint;
    batch_processed int;
    deleted bigint := 0;
    failed bigint := 0;
    total_orphans bigint;
    start_ts timestamptz := clock_timestamp();
    elapsed_sec numeric;
BEGIN
    DROP TABLE IF EXISTS orphan_ota_lob_queue;
    CREATE TEMP TABLE orphan_ota_lob_queue (oid bigint PRIMARY KEY) ON COMMIT PRESERVE ROWS;

    INSERT INTO orphan_ota_lob_queue
    SELECT m.oid
    FROM pg_largeobject_metadata m
    LEFT JOIN ota_package p ON p.data = m.oid
    WHERE p.data IS NULL;

    SELECT COUNT(*) INTO total_orphans FROM orphan_ota_lob_queue;

    IF total_orphans = 0 THEN
        RAISE NOTICE 'No orphaned large objects found';
        DROP TABLE orphan_ota_lob_queue;
        RETURN;
    END IF;

    RAISE NOTICE 'Found % orphaned large objects, cleaning up in batches of %', total_orphans, batch_size;
    COMMIT;

    LOOP
        batch_processed := 0;

        FOR orphan_oid IN
            SELECT oid FROM orphan_ota_lob_queue LIMIT batch_size
        LOOP
            BEGIN
                PERFORM lo_unlink(orphan_oid);
                deleted := deleted + 1;
            EXCEPTION WHEN OTHERS THEN
                failed := failed + 1;
                RAISE WARNING 'Failed to unlink large object %: %', orphan_oid, SQLERRM;
            END;

            DELETE FROM orphan_ota_lob_queue WHERE oid = orphan_oid;
            batch_processed := batch_processed + 1;
        END LOOP;

        COMMIT;

        EXIT WHEN batch_processed = 0;

        elapsed_sec := EXTRACT(EPOCH FROM clock_timestamp() - start_ts);
        RAISE NOTICE 'Progress: % deleted, % failed, % remaining (%s elapsed)',
            deleted, failed, total_orphans - deleted - failed, ROUND(elapsed_sec, 1);
    END LOOP;

    DROP TABLE orphan_ota_lob_queue;

    elapsed_sec := EXTRACT(EPOCH FROM clock_timestamp() - start_ts);
    IF failed > 0 THEN
        RAISE WARNING 'OTA large object cleanup finished: % deleted, % failed out of % (%s elapsed)',
            deleted, failed, total_orphans, ROUND(elapsed_sec, 1);
    ELSE
        RAISE NOTICE 'OTA large object cleanup finished: % deleted (%s elapsed)',
            deleted, ROUND(elapsed_sec, 1);
    END IF;
END;
$$;

CALL cleanup_orphan_ota_lobs(500);

DROP PROCEDURE IF EXISTS cleanup_orphan_ota_lobs(int);

-- CLEANUP ORPHANED OTA PACKAGE LARGE OBJECTS END

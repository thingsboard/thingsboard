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

-- CLEANUP ORPHANED OTA PACKAGE LARGE OBJECTS START

-- This script cleans up orphaned PostgreSQL large objects that are no longer referenced by the ota_package table.
-- These orphaned objects accumulate when OTA packages are deleted or updated and can consume significant disk space.
-- Note: only the ota_package.data column uses PostgreSQL large objects (OID type) in ThingsBoard.
-- This script removes all large objects not referenced by ota_package.data.
--
-- This runs as a single transaction, which is acceptable for typical installations (up to tens of thousands
-- of orphaned objects). For installations with millions of orphaned objects, WAL pressure may be a concern.

DO
$$
DECLARE
    orphan_oid bigint;
    deleted_count int := 0;
    failed_count int := 0;
    total_orphans int;
BEGIN
    -- Collect orphan OIDs into a temp table to avoid repeating the JOIN each iteration
    CREATE TEMP TABLE IF NOT EXISTS orphan_oids AS
        SELECT m.oid AS orphan_oid
        FROM pg_largeobject_metadata m
        LEFT JOIN ota_package p ON p.data = m.oid
        WHERE p.data IS NULL;

    SELECT COUNT(*) INTO total_orphans FROM orphan_oids;

    IF total_orphans = 0 THEN
        RAISE NOTICE 'No orphaned large objects found';
        DROP TABLE IF EXISTS orphan_oids;
        RETURN;
    END IF;

    RAISE NOTICE 'Found % orphaned large objects to clean up', total_orphans;

    FOR orphan_oid IN SELECT o.orphan_oid FROM orphan_oids o
    LOOP
        BEGIN
            PERFORM lo_unlink(orphan_oid);
            deleted_count := deleted_count + 1;

            IF deleted_count % 1000 = 0 THEN
                RAISE NOTICE 'Progress: deleted % of % orphaned large objects...', deleted_count, total_orphans;
            END IF;
        EXCEPTION WHEN OTHERS THEN
            failed_count := failed_count + 1;
            RAISE WARNING 'Failed to delete large object with OID %: %', orphan_oid, SQLERRM;
        END;
    END LOOP;

    IF failed_count > 0 THEN
        RAISE NOTICE 'Completed cleanup: deleted %, failed % out of % orphaned large objects', deleted_count, failed_count, total_orphans;
    ELSE
        RAISE NOTICE 'Successfully cleaned up all % orphaned large objects', deleted_count;
    END IF;

    DROP TABLE IF EXISTS orphan_oids;
EXCEPTION WHEN OTHERS THEN
    DROP TABLE IF EXISTS orphan_oids;
    RAISE;
END;
$$;

-- CLEANUP ORPHANED OTA PACKAGE LARGE OBJECTS END

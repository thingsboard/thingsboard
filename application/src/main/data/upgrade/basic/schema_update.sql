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

-- CLEANUP ORPHANED OTA PACKAGE LARGE OBJECTS START

-- This script cleans up orphaned PostgreSQL large objects that are no longer referenced by the ota_package table.
-- These orphaned objects accumulate when OTA packages are deleted or updated and can consume significant disk space.

DO
$$
DECLARE
    orphan_oid bigint;
    deleted_count int := 0;
    batch_deleted int;
    batch_size int := 1000;
    total_orphans int;
    iteration int := 0;
BEGIN
    SELECT COUNT(*) INTO total_orphans
    FROM pg_largeobject_metadata m LEFT JOIN ota_package p ON p.data = m.oid
    WHERE p.data IS NULL;

    RAISE NOTICE 'Found % orphaned large objects to clean up', total_orphans;

    IF total_orphans = 0 THEN
        RAISE NOTICE 'No orphaned large objects found';
        RETURN;
    END IF;

    LOOP
        iteration := iteration + 1;
        batch_deleted := 0;

        FOR orphan_oid IN
            SELECT m.oid
            FROM pg_largeobject_metadata m
            LEFT JOIN ota_package p ON p.data = m.oid
            WHERE p.data IS NULL
            LIMIT batch_size
        LOOP
            BEGIN
                PERFORM lo_unlink(orphan_oid);
                batch_deleted := batch_deleted + 1;
                deleted_count := deleted_count + 1;

                IF deleted_count % 1000 = 0 THEN
                    RAISE NOTICE 'Cleaned up % of % orphaned large objects...', deleted_count, total_orphans;
                END IF;

            EXCEPTION WHEN OTHERS THEN
                RAISE WARNING 'Failed to delete large object with OID %: %', orphan_oid, SQLERRM;
            END;
        END LOOP;

        EXIT WHEN batch_deleted = 0;

        IF iteration % 10 = 0 THEN
            RAISE NOTICE 'Completed % iterations, deleted % objects so far', iteration, deleted_count;
        END IF;
    END LOOP;

    RAISE NOTICE 'Successfully cleaned up all % orphaned large objects', deleted_count;
END;
$$;

-- CLEANUP ORPHANED OTA PACKAGE LARGE OBJECTS END

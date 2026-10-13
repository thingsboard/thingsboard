--
-- SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
-- SPDX-License-Identifier: BUSL-1.1
--

-- ALARM SHARDING (entity_alarm.originator_id + alarm_comment FK drop) START
-- Drop the alarm_comment -> alarm cascade FK. Comment cleanup on alarm deletion is now performed
-- asynchronously by the Housekeeper service (AlarmCommentsDeletionTaskProcessor), so the DB-level
-- ON DELETE CASCADE constraint is no longer needed. Removing it also keeps alarm_comment independent
-- of alarm for the Citus distributed topology (fresh-install-only; no existing Citus install to migrate).
ALTER TABLE alarm_comment DROP CONSTRAINT IF EXISTS fk_alarm_comment_alarm_id;

-- Add entity_alarm.originator_id. On Citus this co-locates entity_alarm with alarm on the originator_id
-- distribution column (fresh-install-only conversion); on plain PostgreSQL it is a harmless extra column
-- that lets the alarm <-> entity_alarm joins filter/co-locate by originator. This is a fast metadata-only
-- change (nullable, no default), so the ACCESS EXCLUSIVE lock it takes is released as soon as this
-- migration transaction commits.
--
-- The originator_id backfill for pre-existing rows is intentionally NOT done here. LtsMigrationService runs
-- this whole file inside a single transaction, so an in-line UPDATE would hold the ADD COLUMN's
-- ACCESS EXCLUSIVE lock for the entire backfill -- blocking all reads and writes on entity_alarm while the
-- no-downtime patch path (SystemPatchApplier) runs it on a serving node. Instead the backfill runs after
-- this transaction commits, in its own small self-committing batches, in V4_3_1_4Migration.applyAfterCommit().
ALTER TABLE entity_alarm ADD COLUMN IF NOT EXISTS originator_id uuid;
-- ALARM SHARDING (entity_alarm.originator_id + alarm_comment FK drop) END

--
-- SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
-- SPDX-License-Identifier: BUSL-1.1
--

-- 4.4.1 flat DDL, applied by V4_4_1_0Migration during a 4.4.0 -> 4.4.1 upgrade on both the no-downtime and the
-- offline path. Keep these idempotent (ALTER ... IF NOT EXISTS): the offline path records the package version once
-- at the end, so a resumed upgrade re-runs this whole file.

-- EDGE & INTEGRATION API USAGE STATE ADDITION START

ALTER TABLE api_usage_state ADD COLUMN IF NOT EXISTS edge varchar(32) DEFAULT 'ENABLED';
ALTER TABLE api_usage_state ADD COLUMN IF NOT EXISTS integration varchar(32) DEFAULT 'ENABLED';

-- EDGE & INTEGRATION API USAGE STATE ADDITION END

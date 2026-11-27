--
-- SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
-- SPDX-License-Identifier: BUSL-1.1
--

-- Also added by upgrade/lts/4.3.1.10/schema_update.sql, which an installation already on 4.4 never runs.
ALTER TABLE calculated_field ADD COLUMN IF NOT EXISTS compute_on varchar(32);

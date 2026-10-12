--
-- SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
-- SPDX-License-Identifier: BUSL-1.1
--

-- 4.4.1 flat DDL, applied by V4_4_1_0Migration during a 4.4.0 -> 4.4.1 upgrade on both the no-downtime and the
-- offline path. Keep these idempotent (ALTER ... IF NOT EXISTS): the offline path records the package version once
-- at the end, so a resumed upgrade re-runs this whole file.

-- SECRETS CUSTOMER ID MIGRATION START

ALTER TABLE secret ADD COLUMN IF NOT EXISTS customer_id uuid NOT NULL DEFAULT '13814000-1dd2-11b2-8080-808080808080';

ALTER TABLE secret DROP CONSTRAINT IF EXISTS secret_unq_key;

ALTER TABLE secret ADD CONSTRAINT secret_unq_key UNIQUE (tenant_id, customer_id, name);

-- SECRETS CUSTOMER ID MIGRATION END

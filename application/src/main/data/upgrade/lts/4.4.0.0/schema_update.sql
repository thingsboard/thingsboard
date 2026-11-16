--
-- SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
-- SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
-- SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
--

-- 4.4 baseline flat DDL, applied by V4_4_0_0Migration during a 4.3.x -> 4.4 offline upgrade. Keep these
-- idempotent (ALTER ... IF NOT EXISTS): the offline path records the package version once at the end, so a
-- resumed upgrade re-runs this whole file.

-- RULE CHAIN NOTES MIGRATION START

ALTER TABLE rule_chain ADD COLUMN IF NOT EXISTS notes varchar(1000000);

-- RULE CHAIN NOTES MIGRATION END

-- PE-only 4.4 baseline DDL below (carried over from basic/schema_update.sql; these columns/index have no
-- owning bean of their own). report.* live on the PE-only report table; job.customer_id is a PE column on the
-- shared job table.

-- REPORT PUBLIC LINK ADDITION START

ALTER TABLE report ADD COLUMN IF NOT EXISTS public_key varchar(32);
ALTER TABLE report ADD COLUMN IF NOT EXISTS is_public boolean DEFAULT false;
CREATE INDEX IF NOT EXISTS idx_report_public_key ON report(public_key) WHERE public_key IS NOT NULL;

-- REPORT PUBLIC LINK ADDITION END

-- JOB TABLE UPDATE

ALTER TABLE job ADD COLUMN IF NOT EXISTS customer_id uuid;

-- JOB TABLE UPDATE END


-- WHITE LABELING primaryColorPanels DEFAULT START

-- For every tenant-level GENERAL white-labeling row that does not yet
-- carry the primaryColorPanels flag, add it with value true so upgraded
-- installations keep the previous primary-colored panels behavior.
UPDATE white_labeling
   SET settings = jsonb_set(settings::jsonb, '{primaryColorPanels}', 'true'::jsonb)::text
 WHERE type = 'GENERAL'
   AND tenant_id <> '13814000-1dd2-11b2-8080-808080808080'
   AND customer_id = '13814000-1dd2-11b2-8080-808080808080'
   AND settings IS NOT NULL
   AND settings <> ''
   AND NOT (settings::jsonb ? 'primaryColorPanels');

-- WHITE LABELING primaryColorPanels DEFAULT END

-- PERSISTENT RPC REQUEST TRACKING START

ALTER TABLE rpc ADD COLUMN IF NOT EXISTS request_id integer;
ALTER TABLE rpc ADD COLUMN IF NOT EXISTS oneway boolean;

-- PERSISTENT RPC REQUEST TRACKING END

-- UI LICENSE ACTIVATION START

-- License secret is stored in the DB (source of truth), seeded from the TB_LICENSE_SECRET env on first launch.
ALTER TABLE tb_cluster ADD COLUMN IF NOT EXISTS license_secret varchar;
ALTER TABLE tb_cluster ADD COLUMN IF NOT EXISTS license_claim_token varchar;

-- UI LICENSE ACTIVATION END

-- NON-PRODUCTION UPTIME MIGRATION START

ALTER TABLE tb_cluster ADD COLUMN IF NOT EXISTS non_production_uptime_ms bigint NOT NULL DEFAULT 0;
ALTER TABLE tb_cluster ADD COLUMN IF NOT EXISTS non_production_last_tick bigint;

-- NON-PRODUCTION UPTIME MIGRATION END

-- NON-PRODUCTION CONFIRMATION MIGRATION START

ALTER TABLE tb_cluster ADD COLUMN IF NOT EXISTS non_production_confirmed_ts bigint;

-- NON-PRODUCTION CONFIRMATION MIGRATION END

-- TB_CLUSTER SINGLE ROW CONSTRAINT START

-- tb_cluster holds exactly one row; a unique index on a constant expression enforces that.
CREATE UNIQUE INDEX IF NOT EXISTS tb_cluster_single_row ON tb_cluster ((true));

-- TB_CLUSTER SINGLE ROW CONSTRAINT END

-- WHITE LABELING loginPrimaryColorPanels DEFAULT START

-- For the system-level LOGIN white-labeling row that does not yet carry the
-- loginPrimaryColorPanels flag, add it with value true so upgraded installations keep the
-- previous primary-colored login page theme. Tenant and customer rows are deliberately left
-- alone: getMergedLoginWhiteLabelingParams() cascades the flag down to them, so they keep the
-- previous appearance without pinning a value that would ignore a later system-level change.
UPDATE white_labeling
SET settings = jsonb_set(settings::jsonb, '{loginPrimaryColorPanels}', 'true'::jsonb)::text
WHERE type = 'LOGIN'
  AND tenant_id = '13814000-1dd2-11b2-8080-808080808080'
  AND customer_id = '13814000-1dd2-11b2-8080-808080808080'
  AND settings IS NOT NULL
  AND settings <> ''
  AND NOT (settings::jsonb ? 'loginPrimaryColorPanels');

-- The UPDATE above only fixes rows that already exist. Most installations never saved
-- LOGIN-scope white-labeling settings at all, so there is no system-level row for it to
-- touch -- and getMergedLoginWhiteLabelingParams() always falls back to the system-level
-- row for anything not overridden at tenant/customer level. Guarantee that row exists
-- with the flag set, regardless of whether it was there before this upgrade.
INSERT INTO white_labeling (tenant_id, customer_id, type, settings)
VALUES ('13814000-1dd2-11b2-8080-808080808080', '13814000-1dd2-11b2-8080-808080808080', 'LOGIN',
        '{"loginPrimaryColorPanels":true}')
ON CONFLICT (tenant_id, customer_id, type) DO NOTHING;

-- WHITE LABELING loginPrimaryColorPanels DEFAULT END

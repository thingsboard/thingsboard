--
-- SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
-- SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
-- SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
--

ALTER TABLE api_usage_state ADD COLUMN IF NOT EXISTS ai varchar(32) DEFAULT 'ENABLED';

-- CALCULATED FIELD ADDITIONAL INFO ADDITION START

ALTER TABLE calculated_field ADD COLUMN IF NOT EXISTS additional_info varchar;

-- CALCULATED FIELD ADDITIONAL INFO ADDITION END

-- ROLE EXCLUDED PERMISSIONS ADDITION START

ALTER TABLE role ADD COLUMN IF NOT EXISTS excluded_permissions varchar(1000000);

-- ROLE EXCLUDED PERMISSIONS ADDITION END

-- USER EXTERNAL ID ADDITION START
ALTER TABLE tb_user ADD COLUMN IF NOT EXISTS external_id uuid;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'tb_user_external_id_unq_key') THEN
        ALTER TABLE tb_user ADD CONSTRAINT tb_user_external_id_unq_key UNIQUE (tenant_id, external_id);
    END IF;
END $$;
-- USER EXTERNAL ID ADDITION END

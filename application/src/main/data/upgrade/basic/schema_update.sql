--
-- SPDX-FileCopyrightText: Copyright The Thingsboard Authors
-- SPDX-License-Identifier: Apache-2.0
--

-- UPDATE OTA PACKAGE EXTERNAL ID START

ALTER TABLE ota_package
    ADD COLUMN IF NOT EXISTS external_id uuid;

DO
$$
    BEGIN
        IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname = 'ota_package_external_id_unq_key') THEN
            ALTER TABLE ota_package ADD CONSTRAINT ota_package_external_id_unq_key UNIQUE (tenant_id, external_id);
        END IF;
    END;
$$;

-- UPDATE OTA PACKAGE EXTERNAL ID END

-- DROP INDEXES THAT DUPLICATE UNIQUE CONSTRAINT START

DROP INDEX IF EXISTS idx_device_external_id;
DROP INDEX IF EXISTS idx_device_profile_external_id;
DROP INDEX IF EXISTS idx_asset_external_id;
DROP INDEX IF EXISTS idx_entity_view_external_id;
DROP INDEX IF EXISTS idx_rule_chain_external_id;
DROP INDEX IF EXISTS idx_dashboard_external_id;
DROP INDEX IF EXISTS idx_customer_external_id;
DROP INDEX IF EXISTS idx_widgets_bundle_external_id;

-- DROP INDEXES THAT DUPLICATE UNIQUE CONSTRAINT END

ALTER TABLE mobile_app ADD COLUMN IF NOT EXISTS title varchar(255);

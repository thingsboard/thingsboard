--
-- Copyright © 2016-2025 The Thingsboard Authors
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

-- UPDATE OTA PACKAGE EXTERNAL ID START

ALTER TABLE ota_package
    ADD COLUMN IF NOT EXISTS external_id uuid;
ALTER TABLE ota_package
    ADD CONSTRAINT ota_package_external_id_unq_key UNIQUE (tenant_id, external_id);

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

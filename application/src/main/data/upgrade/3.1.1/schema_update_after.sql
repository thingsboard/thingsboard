--
-- Copyright © 2016-2021 The Thingsboard Authors
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

DROP PROCEDURE IF EXISTS update_tenant_profiles;
DROP PROCEDURE IF EXISTS update_device_profiles;

ALTER TABLE tenant ALTER COLUMN tenant_profile_id SET NOT NULL;
ALTER TABLE tenant DROP CONSTRAINT IF EXISTS fk_tenant_profile;
ALTER TABLE tenant ADD CONSTRAINT fk_tenant_profile FOREIGN KEY (tenant_profile_id) REFERENCES tenant_profile(id);
ALTER TABLE tenant DROP COLUMN IF EXISTS isolated_tb_core;
ALTER TABLE tenant DROP COLUMN IF EXISTS isolated_tb_rule_engine;

ALTER TABLE device ALTER COLUMN device_profile_id SET NOT NULL;
ALTER TABLE device DROP CONSTRAINT IF EXISTS fk_device_profile;
ALTER TABLE device ADD CONSTRAINT fk_device_profile FOREIGN KEY (device_profile_id) REFERENCES device_profile(id);

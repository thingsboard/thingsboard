--
-- Copyright © 2016-2023 The Thingsboard Authors
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

DROP PROCEDURE IF EXISTS update_asset_profiles;

ALTER TABLE asset ALTER COLUMN asset_profile_id SET NOT NULL;
ALTER TABLE asset DROP CONSTRAINT IF EXISTS fk_asset_profile;
ALTER TABLE asset ADD CONSTRAINT fk_asset_profile FOREIGN KEY (asset_profile_id) REFERENCES asset_profile(id);

--
-- Copyright © 2016-2022 The Thingsboard Authors
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

ALTER TABLE device
    ADD COLUMN IF NOT EXISTS external_id UUID;
ALTER TABLE device_profile
    ADD COLUMN IF NOT EXISTS external_id UUID;
ALTER TABLE asset
    ADD COLUMN IF NOT EXISTS external_id UUID;
ALTER TABLE rule_chain
    ADD COLUMN IF NOT EXISTS external_id UUID;
ALTER TABLE dashboard
    ADD COLUMN IF NOT EXISTS external_id UUID;
ALTER TABLE customer
    ADD COLUMN IF NOT EXISTS external_id UUID;

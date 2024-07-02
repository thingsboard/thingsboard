--
-- Copyright © 2016-2024 The Thingsboard Authors
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

-- Optimistic locking update START

ALTER TABLE device ADD COLUMN IF NOT EXISTS version INT DEFAULT 1;
ALTER TABLE device_profile ADD COLUMN IF NOT EXISTS version INT DEFAULT 1;
ALTER TABLE device_credentials ADD COLUMN IF NOT EXISTS version INT DEFAULT 1;
ALTER TABLE asset ADD COLUMN IF NOT EXISTS version INT DEFAULT 1;
ALTER TABLE asset_profile ADD COLUMN IF NOT EXISTS version INT DEFAULT 1;
ALTER TABLE entity_view ADD COLUMN IF NOT EXISTS version INT DEFAULT 1;
ALTER TABLE tb_user ADD COLUMN IF NOT EXISTS version INT DEFAULT 1;
ALTER TABLE customer ADD COLUMN IF NOT EXISTS version INT DEFAULT 1;
ALTER TABLE edge ADD COLUMN IF NOT EXISTS version INT DEFAULT 1;
ALTER TABLE rule_chain ADD COLUMN IF NOT EXISTS version INT DEFAULT 1;
ALTER TABLE dashboard ADD COLUMN IF NOT EXISTS version INT DEFAULT 1;
ALTER TABLE widget_type ADD COLUMN IF NOT EXISTS version INT DEFAULT 1;
ALTER TABLE widgets_bundle ADD COLUMN IF NOT EXISTS version INT DEFAULT 1;

-- Optimistic locking update END

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


-- ALARM ASSIGN TO USER START

ALTER TABLE alarm ADD COLUMN IF NOT EXISTS assign_ts BIGINT;
ALTER TABLE alarm ADD COLUMN IF NOT EXISTS assignee_id UUID;

ALTER TABLE entity_alarm ADD COLUMN IF NOT EXISTS assignee_id UUID;

CREATE INDEX IF NOT EXISTS idx_entity_alarm_assignee_id ON entity_alarm(assignee_id);

-- ALARM ASSIGN TO USER END
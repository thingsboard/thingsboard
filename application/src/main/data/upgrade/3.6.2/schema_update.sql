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

-- RULE NODE INDEXES UPDATE START

DROP INDEX IF EXISTS idx_rule_node_type;
DROP INDEX IF EXISTS idx_rule_node_type_configuration_version;
CREATE INDEX IF NOT EXISTS idx_rule_node_type_id_configuration_version ON rule_node(type, id, configuration_version);

-- RULE NODE INDEXES UPDATE END

-- RULE NODE QUEUE UPDATE START

ALTER TABLE rule_node ADD COLUMN IF NOT EXISTS queue_name varchar(255);
ALTER TABLE component_descriptor ADD COLUMN IF NOT EXISTS has_queue_name boolean DEFAULT false;

-- RULE NODE QUEUE UPDATE END

-- CUSTOMER UNIQUE CONSTRAINT UPDATE START

ALTER TABLE customer DROP CONSTRAINT IF EXISTS customer_title_unq_key;
ALTER TABLE customer ADD CONSTRAINT customer_title_unq_key UNIQUE (tenant_id, title);

-- CUSTOMER UNIQUE CONSTRAINT UPDATE END
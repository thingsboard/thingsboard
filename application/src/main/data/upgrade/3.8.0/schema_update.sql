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

-- UPDATE RULE NODE DEBUG MODE TO DEBUG STRATEGY START

ALTER TABLE rule_node
    ADD COLUMN IF NOT EXISTS debug_strategy varchar(32) DEFAULT 'DISABLED';
ALTER TABLE rule_node
    ADD COLUMN IF NOT EXISTS last_update_ts bigint NOT NULL DEFAULT extract(epoch from now()) * 1000;
DO
$$
    BEGIN
        IF EXISTS (SELECT 1
                   FROM information_schema.columns
                   WHERE table_name = 'rule_node' AND column_name = 'debug_mode') THEN
            UPDATE rule_node
            SET debug_strategy = CASE WHEN debug_mode = true THEN 'ALL_EVENTS' ELSE 'DISABLED' END;
            ALTER TABLE rule_node
                DROP COLUMN debug_mode;
        END IF;
    END
$$;

-- UPDATE RULE NODE DEBUG MODE TO DEBUG STRATEGY END

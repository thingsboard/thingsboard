--
-- Copyright © 2016-2026 The Thingsboard Authors
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

-- CALCULATED FIELD ADDITIONAL INFO ADDITION START

ALTER TABLE calculated_field ADD COLUMN IF NOT EXISTS additional_info varchar;

-- CALCULATED FIELD ADDITIONAL INFO ADDITION END

-- RULE CHAIN NOTES MIGRATION START

ALTER TABLE rule_chain ADD COLUMN IF NOT EXISTS notes varchar(1000000);

-- RULE CHAIN NOTES MIGRATION END

-- EDGE API USAGE STATE ADDITION START

ALTER TABLE api_usage_state ADD COLUMN IF NOT EXISTS edge varchar(32) DEFAULT 'ENABLED';

-- EDGE API USAGE STATE ADDITION END

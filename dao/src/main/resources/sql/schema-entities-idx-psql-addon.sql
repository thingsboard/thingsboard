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

-- This file describes PostgreSQL-specific indexes that not supported by hsql
-- It is not a stand-alone file! Run schema-entities-idx.sql before!
-- Note: Hibernate DESC order translates to native SQL "ORDER BY .. DESC NULLS LAST"
--       While creating index PostgreSQL transforms short notation (ts DESC) to the full (DESC NULLS FIRST)
--       That difference between NULLS LAST and NULLS FIRST prevents to hit index while querying latest by ts
--       That why we need to define DESC index explicitly as (ts DESC NULLS LAST)

CREATE INDEX IF NOT EXISTS idx_rule_node_debug_event_main
    ON rule_node_debug_event (tenant_id ASC, entity_id ASC, ts DESC NULLS LAST);

CREATE INDEX IF NOT EXISTS idx_rule_chain_debug_event_main
    ON rule_chain_debug_event (tenant_id ASC, entity_id ASC, ts DESC NULLS LAST);

CREATE INDEX IF NOT EXISTS idx_stats_event_main
    ON stats_event (tenant_id ASC, entity_id ASC, ts DESC NULLS LAST);

CREATE INDEX IF NOT EXISTS idx_lc_event_main
    ON lc_event (tenant_id ASC, entity_id ASC, ts DESC NULLS LAST);

CREATE INDEX IF NOT EXISTS idx_error_event_main
    ON error_event (tenant_id ASC, entity_id ASC, ts DESC NULLS LAST);

CREATE INDEX IF NOT EXISTS idx_cf_debug_event_main
    ON cf_debug_event (tenant_id ASC, entity_id ASC, ts DESC NULLS LAST);

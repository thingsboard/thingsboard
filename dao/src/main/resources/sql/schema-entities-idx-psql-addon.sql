--
-- SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
-- SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
-- SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
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

CREATE INDEX IF NOT EXISTS idx_converter_debug_event_main
    ON converter_debug_event (tenant_id ASC, entity_id ASC, ts DESC NULLS LAST) WITH (FILLFACTOR=95);

CREATE INDEX IF NOT EXISTS idx_integration_debug_event_main
    ON integration_debug_event (tenant_id ASC, entity_id ASC, ts DESC NULLS LAST) WITH (FILLFACTOR=95);

CREATE INDEX IF NOT EXISTS idx_raw_data_event_main
    ON raw_data_event (tenant_id ASC, entity_id ASC, ts DESC NULLS LAST) WITH (FILLFACTOR=95);

CREATE INDEX IF NOT EXISTS idx_cf_debug_event_main
    ON cf_debug_event (tenant_id ASC, entity_id ASC, ts DESC NULLS LAST);

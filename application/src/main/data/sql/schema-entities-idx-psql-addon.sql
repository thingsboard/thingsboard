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

-- This file describes PostgreSQL-specific indexes that not supported by hsql
-- It is not a stand-alone file! Run schema-entities-idx.sql before!
-- Note: Hibernate DESC order translates to native SQL "ORDER BY .. DESC NULLS LAST"
--       While creating index PostgreSQL transforms short notation (ts DESC) to the full (DESC NULLS FIRST)
--       That difference between NULLS LAST and NULLS FIRST prevents to hit index while querying latest by ts
--       That why we need to define DESC index explicitly as (ts DESC NULLS LAST)

CREATE INDEX IF NOT EXISTS idx_event_ts
    ON public.event
    (ts DESC NULLS LAST)
    WITH (FILLFACTOR=95);

COMMENT ON INDEX public.idx_event_ts
    IS 'This index helps to delete events by TTL using timestamp';

CREATE INDEX IF NOT EXISTS idx_event_tenant_entity_type_entity_event_type_created_time_des
    ON public.event
    (tenant_id ASC, entity_type ASC, entity_id ASC, event_type ASC, created_time DESC NULLS LAST)
    WITH (FILLFACTOR=95);

COMMENT ON INDEX public.idx_event_tenant_entity_type_entity_event_type_created_time_des
    IS 'This index helps to open latest events on UI fast';

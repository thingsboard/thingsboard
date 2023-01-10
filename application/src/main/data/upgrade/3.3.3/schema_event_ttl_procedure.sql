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


DROP PROCEDURE IF EXISTS public.cleanup_events_by_ttl(bigint, bigint, bigint);

CREATE OR REPLACE PROCEDURE cleanup_events_by_ttl(
    IN regular_events_start_ts bigint,
    IN regular_events_end_ts bigint,
    IN debug_events_start_ts bigint,
    IN debug_events_end_ts bigint,
    INOUT deleted bigint)
    LANGUAGE plpgsql AS
$$
DECLARE
    ttl_deleted_count bigint DEFAULT 0;
    debug_ttl_deleted_count bigint DEFAULT 0;
BEGIN
    IF regular_events_start_ts > 0 AND regular_events_end_ts > 0 THEN
        EXECUTE format(
                'WITH deleted AS (DELETE FROM event WHERE id in (SELECT id from event WHERE ts > %L::bigint AND ts < %L::bigint AND ' ||
                '(event_type != %L::varchar AND event_type != %L::varchar)) RETURNING *) ' ||
                'SELECT count(*) FROM deleted', regular_events_start_ts, regular_events_end_ts,
                'DEBUG_RULE_NODE', 'DEBUG_RULE_CHAIN') into ttl_deleted_count;
    END IF;
    IF debug_events_start_ts > 0 AND debug_events_end_ts > 0 THEN
        EXECUTE format(
                'WITH deleted AS (DELETE FROM event WHERE id in (SELECT id from event WHERE ts > %L::bigint AND ts < %L::bigint AND ' ||
                '(event_type = %L::varchar OR event_type = %L::varchar)) RETURNING *) ' ||
                'SELECT count(*) FROM deleted', debug_events_start_ts, debug_events_end_ts,
                'DEBUG_RULE_NODE', 'DEBUG_RULE_CHAIN') into debug_ttl_deleted_count;
    END IF;
    RAISE NOTICE 'Events removed by ttl: %', ttl_deleted_count;
    RAISE NOTICE 'Debug Events removed by ttl: %', debug_ttl_deleted_count;
    deleted := ttl_deleted_count + debug_ttl_deleted_count;
END
$$;

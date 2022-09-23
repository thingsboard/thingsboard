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

CREATE TABLE IF NOT EXISTS rule_node_debug_event (
    id uuid NOT NULL,
    tenant_id uuid NOT NULL ,
    ts bigint NOT NULL,
    entity_id uuid NOT NULL,
    service_id varchar,
    e_type varchar,
    e_entity_id uuid,
    e_entity_type varchar,
    e_msg_id uuid,
    e_msg_type varchar,
    e_data_type varchar,
    e_relation_type varchar,
    e_data varchar,
    e_metadata varchar,
    e_error varchar
) PARTITION BY RANGE (ts);

CREATE TABLE IF NOT EXISTS rule_chain_debug_event (
    id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    ts bigint NOT NULL,
    entity_id uuid NOT NULL,
    service_id varchar NOT NULL,
    e_message varchar,
    e_error varchar
) PARTITION BY RANGE (ts);

CREATE TABLE IF NOT EXISTS stats_event (
    id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    ts bigint NOT NULL,
    entity_id uuid NOT NULL,
    service_id varchar NOT NULL,
    e_messages_processed bigint NOT NULL,
    e_errors_occurred bigint NOT NULL
) PARTITION BY RANGE (ts);

CREATE TABLE IF NOT EXISTS lc_event (
    id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    ts bigint NOT NULL,
    entity_id uuid NOT NULL,
    service_id varchar NOT NULL,
    e_type varchar NOT NULL,
    e_success boolean NOT NULL,
    e_error varchar
) PARTITION BY RANGE (ts);

CREATE TABLE IF NOT EXISTS error_event (
    id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    ts bigint NOT NULL,
    entity_id uuid NOT NULL,
    service_id varchar NOT NULL,
    e_method varchar NOT NULL,
    e_error varchar
) PARTITION BY RANGE (ts);

CREATE INDEX IF NOT EXISTS idx_rule_node_debug_event_main
    ON rule_node_debug_event (tenant_id ASC, entity_id ASC, ts DESC NULLS LAST) WITH (FILLFACTOR=95);

CREATE INDEX IF NOT EXISTS idx_rule_chain_debug_event_main
    ON rule_chain_debug_event (tenant_id ASC, entity_id ASC, ts DESC NULLS LAST) WITH (FILLFACTOR=95);

CREATE INDEX IF NOT EXISTS idx_stats_event_main
    ON stats_event (tenant_id ASC, entity_id ASC, ts DESC NULLS LAST) WITH (FILLFACTOR=95);

CREATE INDEX IF NOT EXISTS idx_lc_event_main
    ON lc_event (tenant_id ASC, entity_id ASC, ts DESC NULLS LAST) WITH (FILLFACTOR=95);

CREATE INDEX IF NOT EXISTS idx_error_event_main
    ON error_event (tenant_id ASC, entity_id ASC, ts DESC NULLS LAST) WITH (FILLFACTOR=95);

CREATE OR REPLACE FUNCTION to_safe_json(p_json text) RETURNS json
LANGUAGE plpgsql AS
$$
BEGIN
  return REPLACE(p_json, '\u0000', '' )::json;
EXCEPTION
  WHEN OTHERS THEN
  return '{}'::json;
END;
$$;

-- Useful to migrate old events to the new table structure;
CREATE OR REPLACE PROCEDURE migrate_regular_events(IN start_ts_in_ms bigint, IN end_ts_in_ms bigint, IN partition_size_in_hours int)
    LANGUAGE plpgsql AS
$$
DECLARE
    partition_size_in_ms bigint;
    p record;
    table_name varchar;
BEGIN
    partition_size_in_ms = partition_size_in_hours * 3600 * 1000;

    FOR p IN SELECT DISTINCT event_type as event_type, (created_time - created_time % partition_size_in_ms) as partition_ts FROM event e WHERE e.event_type in ('STATS', 'LC_EVENT', 'ERROR') and ts >= start_ts_in_ms and ts < end_ts_in_ms
    LOOP
        IF p.event_type = 'STATS' THEN
            table_name := 'stats_event';
        ELSEIF p.event_type = 'LC_EVENT' THEN
            table_name := 'lc_event';
        ELSEIF p.event_type = 'ERROR' THEN
            table_name := 'error_event';
        END IF;
        RAISE NOTICE '[%] Partition to create : [%-%]', table_name, p.partition_ts, (p.partition_ts + partition_size_in_ms);
        EXECUTE format('CREATE TABLE IF NOT EXISTS %s_%s PARTITION OF %s FOR VALUES FROM ( %s ) TO ( %s )', table_name, p.partition_ts, table_name, p.partition_ts, (p.partition_ts + partition_size_in_ms));
    END LOOP;

    INSERT INTO stats_event
    SELECT id,
           tenant_id,
           ts,
           entity_id,
           body ->> 'server',
           (body ->> 'messagesProcessed')::bigint,
           (body ->> 'errorsOccurred')::bigint
    FROM
    (select id, tenant_id, ts, entity_id, to_safe_json(body) as body
     FROM event WHERE ts >= start_ts_in_ms and ts < end_ts_in_ms AND event_type = 'STATS' AND to_safe_json(body) ->> 'server' IS NOT NULL
    ) safe_event
    ON CONFLICT DO NOTHING;

    INSERT INTO lc_event
    SELECT id,
           tenant_id,
           ts,
           entity_id,
           body ->> 'server',
           body ->> 'event',
           (body ->> 'success')::boolean,
           body ->> 'error'
    FROM
    (select id, tenant_id, ts, entity_id, to_safe_json(body) as body
     FROM event WHERE ts >= start_ts_in_ms and ts < end_ts_in_ms AND event_type = 'LC_EVENT' AND to_safe_json(body) ->> 'server' IS NOT NULL
    ) safe_event
    ON CONFLICT DO NOTHING;

    INSERT INTO error_event
    SELECT id,
           tenant_id,
           ts,
           entity_id,
           body ->> 'server',
           body ->> 'method',
           body ->> 'error'
    FROM
    (select id, tenant_id, ts, entity_id, to_safe_json(body) as body
     FROM event WHERE ts >= start_ts_in_ms and ts < end_ts_in_ms AND event_type = 'ERROR' AND to_safe_json(body) ->> 'server' IS NOT NULL
    ) safe_event
    ON CONFLICT DO NOTHING;

END
$$;

-- Useful to migrate old debug events to the new table structure;
CREATE OR REPLACE PROCEDURE migrate_debug_events(IN start_ts_in_ms bigint, IN end_ts_in_ms bigint, IN partition_size_in_hours int)
    LANGUAGE plpgsql AS
$$
DECLARE
    partition_size_in_ms bigint;
    p record;
    table_name varchar;
BEGIN
    partition_size_in_ms = partition_size_in_hours * 3600 * 1000;

    FOR p IN SELECT DISTINCT event_type as event_type, (created_time - created_time % partition_size_in_ms) as partition_ts FROM event e WHERE e.event_type in ('DEBUG_RULE_NODE', 'DEBUG_RULE_CHAIN') and ts >= start_ts_in_ms and ts < end_ts_in_ms
    LOOP
        IF p.event_type = 'DEBUG_RULE_NODE' THEN
            table_name := 'rule_node_debug_event';
        ELSEIF p.event_type = 'DEBUG_RULE_CHAIN' THEN
            table_name := 'rule_chain_debug_event';
        END IF;
        RAISE NOTICE '[%] Partition to create : [%-%]', table_name, p.partition_ts, (p.partition_ts + partition_size_in_ms);
        EXECUTE format('CREATE TABLE IF NOT EXISTS %s_%s PARTITION OF %s FOR VALUES FROM ( %s ) TO ( %s )', table_name, p.partition_ts, table_name, p.partition_ts, (p.partition_ts + partition_size_in_ms));
    END LOOP;

    INSERT INTO rule_node_debug_event
    SELECT id,
           tenant_id,
           ts,
           entity_id,
           body ->> 'server',
           body ->> 'type',
           (body ->> 'entityId')::uuid,
           body ->> 'entityName',
           (body ->> 'msgId')::uuid,
           body ->> 'msgType',
           body ->> 'dataType',
           body ->> 'relationType',
           body ->> 'data',
           body ->> 'metadata',
           body ->> 'error'
    FROM
    (select id, tenant_id, ts, entity_id, to_safe_json(body) as body
     FROM event WHERE ts >= start_ts_in_ms and ts < end_ts_in_ms AND event_type = 'DEBUG_RULE_NODE' AND to_safe_json(body) ->> 'server' IS NOT NULL
    ) safe_event
    ON CONFLICT DO NOTHING;

    INSERT INTO rule_chain_debug_event
    SELECT id,
           tenant_id,
           ts,
           entity_id,
           body ->> 'server',
           body ->> 'message',
           body ->> 'error'
    FROM
    (select id, tenant_id, ts, entity_id, to_safe_json(body) as body
     FROM event WHERE ts >= start_ts_in_ms and ts < end_ts_in_ms AND event_type = 'DEBUG_RULE_CHAIN' AND to_safe_json(body) ->> 'server' IS NOT NULL
    ) safe_event
    ON CONFLICT DO NOTHING;
END
$$;

UPDATE tb_user
    SET additional_info = REPLACE(additional_info, '"lang":"ja_JA"', '"lang":"ja_JP"')
    WHERE additional_info LIKE '%"lang":"ja_JA"%';

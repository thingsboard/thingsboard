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



-- 创建 kafka_event 表
CREATE TABLE IF NOT EXISTS kafka_event (
    id UUID,
    tenant_id UUID,
    ts Int64,
    entity_id UUID,
    service_id String,
    e_type String,
    e_entity_id UUID,
    e_entity_type String,
    e_msg_id UUID,
    e_msg_type String,
    e_data_type String,
    e_relation_type String,
    e_data String,
    e_metadata String,
    e_error String,
    e_message String,
    e_in_message_type String,
    e_in_message String,
    e_out_message_type String,
    e_out_message String,
    e_status String,
    e_uuid String,
    e_message_type String,
    e_messages_processed Int64,
    e_errors_occurred Int64,
    e_success UInt8,
    e_method String,
    event_type String
) ENGINE = Kafka
    SETTINGS kafka_broker_list = 'kafka:9092',
    kafka_topic_list = 'tb_event_clickhouse',
    kafka_group_name = 'tb_event_clickhouse_consumer',
    kafka_format = 'JSONEachRow',
    kafka_num_consumers = 1;

------------------------------------------实体表---------------------------------------------------------------

-- 创建 rule_node_debug_event 表
CREATE TABLE IF NOT EXISTS rule_node_debug_event (
    id UUID,
    tenant_id UUID,
    ts Int64,
    entity_id UUID,
    service_id String,
    e_type String,
    e_entity_id UUID,
    e_entity_type String,
    e_msg_id UUID,
    e_msg_type String,
    e_data_type String,
    e_relation_type String,
    e_data String,
    e_metadata String,
    e_error String
) ENGINE = MergeTree()
    ORDER BY (tenant_id, entity_id, ts)
    PARTITION BY formatDateTime(fromUnixTimestamp64Milli(ts),'%Y_%m_%d')
    TTL toDateTime(fromUnixTimestamp64Milli(ts)) + toIntervalMonth(12)
    SETTINGS index_granularity = 8192
;

-- 创建 rule_chain_debug_event 表和物化视图
CREATE TABLE IF NOT EXISTS rule_chain_debug_event (
    id UUID,
    tenant_id UUID,
    ts Int64,
    entity_id UUID,
    service_id String,
    e_message String,
    e_error String
) ENGINE = MergeTree()
      ORDER BY (tenant_id, entity_id, ts)
      PARTITION BY formatDateTime(fromUnixTimestamp64Milli(ts),'%Y_%m_%d')
;

-- 创建 converter_debug_event 表
CREATE TABLE IF NOT EXISTS converter_debug_event (
    id UUID,
    tenant_id UUID,
    ts Int64,
    entity_id UUID,
    service_id String,
    e_type String,
    e_in_message_type String,
    e_in_message String,
    e_out_message_type String,
    e_out_message String,
    e_metadata String,
    e_error String
) ENGINE = MergeTree()
    ORDER BY (tenant_id, entity_id, ts)
    PARTITION BY formatDateTime(fromUnixTimestamp64Milli(ts),'%Y_%m_%d')
    TTL toDateTime(fromUnixTimestamp64Milli(ts)) + toIntervalDay(30)
    SETTINGS index_granularity = 8192
;


-- 创建 integration_debug_event 表
CREATE TABLE IF NOT EXISTS integration_debug_event (
    id UUID,
    tenant_id UUID,
    ts Int64,
    entity_id UUID,
    service_id String,
    e_type String,
    e_message_type String,
    e_message String,
    e_status String,
    e_error String
) ENGINE = MergeTree()
      ORDER BY (tenant_id, entity_id, ts)
      PARTITION BY formatDateTime(fromUnixTimestamp64Milli(ts),'%Y_%m_%d')
;

-- 创建 raw_data_event 表
CREATE TABLE IF NOT EXISTS raw_data_event (
    id UUID,
    tenant_id UUID,
    ts Int64,
    entity_id UUID,
    service_id String,
    e_uuid String,
    e_message_type String,
    e_message String
) ENGINE = MergeTree()
      ORDER BY (tenant_id, entity_id, ts)
      PARTITION BY formatDateTime(fromUnixTimestamp64Milli(ts),'%Y_%m_%d')
;


-- 创建 stats_event 表和物化视图
CREATE TABLE IF NOT EXISTS stats_event (
    id UUID,
    tenant_id UUID,
    ts Int64,
    entity_id UUID,
    service_id String,
    e_messages_processed Int64,
    e_errors_occurred Int64
) ENGINE = MergeTree()
      ORDER BY (tenant_id, entity_id, ts)
      PARTITION BY formatDateTime(fromUnixTimestamp64Milli(ts),'%Y_%m_%d')
;

-- 创建 lc_event 表
CREATE TABLE IF NOT EXISTS lc_event (
    id UUID,
    tenant_id UUID,
    ts Int64,
    entity_id UUID,
    service_id String,
    e_type String,
    e_success UInt8,
    e_error String
) ENGINE = MergeTree()
      ORDER BY (tenant_id, entity_id, ts)
      PARTITION BY formatDateTime(fromUnixTimestamp64Milli(ts),'%Y_%m_%d')
;

-- 创建 error_event 表
CREATE TABLE IF NOT EXISTS error_event (
    id UUID,
    tenant_id UUID,
    ts Int64,
    entity_id UUID,
    service_id String,
    e_method String,
    e_error String
) ENGINE = MergeTree()
      ORDER BY (tenant_id, entity_id, ts)
      PARTITION BY formatDateTime(fromUnixTimestamp64Milli(ts),'%Y_%m_%d')
;


------------------------------------------物化视图---------------------------------------------------------------

-- 创建 rule_node_debug_event 物化视图
CREATE MATERIALIZED VIEW rule_node_debug_event_mv TO rule_node_debug_event
 AS
SELECT
    id,
    tenant_id,
    ts,
    entity_id,
    service_id,
    e_type,
    e_entity_id,
    e_entity_type,
    e_msg_id,
    e_msg_type,
    e_data_type,
    e_relation_type,
    e_data,
    e_metadata,
    e_error
FROM
    kafka_event
WHERE
    event_type = 'rule_node_debug_event';

-- 创建 rule_chain_debug_event 物化视图
CREATE MATERIALIZED VIEW rule_chain_debug_event_mv TO rule_chain_debug_event
 AS
SELECT
    id,
    tenant_id,
    ts,
    entity_id,
    service_id,
    e_message,
    e_error
FROM
    kafka_event
WHERE
    event_type = 'rule_chain_debug_event';

-- 创建 converter_debug_event 物化视图
CREATE MATERIALIZED VIEW converter_debug_event_mv TO converter_debug_event
 AS
SELECT
    id,
    tenant_id,
    ts,
    entity_id,
    service_id,
    e_type,
    e_in_message_type,
    e_in_message,
    e_out_message_type,
    e_out_message,
    e_metadata,
    e_error
FROM
    kafka_event
WHERE
    event_type = 'converter_debug_event';

-- 创建 integration_debug_event 物化视图
CREATE MATERIALIZED VIEW integration_debug_event_mv TO integration_debug_event
 AS
SELECT
    id,
    tenant_id,
    ts,
    entity_id,
    service_id,
    e_type,
    e_message_type,
    e_message,
    e_status,
    e_error
FROM
    kafka_event
WHERE
    event_type = 'integration_debug_event';

-- 创建 raw_data_event 物化视图
CREATE MATERIALIZED VIEW raw_data_event_mv TO raw_data_event
 AS
SELECT
    id,
    tenant_id,
    ts,
    entity_id,
    service_id,
    e_uuid,
    e_message_type,
    e_message
FROM
    kafka_event
WHERE
    event_type = 'raw_data_event';

-- 创建 stats_event 物化视图
CREATE MATERIALIZED VIEW stats_event_mv TO stats_event
 AS
SELECT
    id,
    tenant_id,
    ts,
    entity_id,
    service_id,
    e_messages_processed,
    e_errors_occurred
FROM
    kafka_event
WHERE
    event_type = 'stats_event';

-- 创建 lc_event 物化视图
CREATE MATERIALIZED VIEW lc_event_mv TO lc_event
 AS
SELECT
    id,
    tenant_id,
    ts,
    entity_id,
    service_id,
    e_type,
    e_success,
    e_error
FROM
    kafka_event
WHERE
    event_type = 'lc_event';

-- 创建 error_event 物化视图
CREATE MATERIALIZED VIEW error_event_mv TO error_event
 AS
SELECT
    id,
    tenant_id,
    ts,
    entity_id,
    service_id,
    e_method,
    e_error
FROM
    kafka_event
WHERE
    event_type = 'error_event';


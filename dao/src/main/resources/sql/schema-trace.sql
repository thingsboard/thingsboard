--
-- ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
--
-- Copyright © 2016-2026 ThingsBoard, Inc. All Rights Reserved.
--
-- NOTICE: All information contained herein is, and remains
-- the property of ThingsBoard, Inc. and its suppliers,
-- if any.  The intellectual and technical concepts contained
-- herein are proprietary to ThingsBoard, Inc.
-- and its suppliers and may be covered by U.S. and Foreign Patents,
-- patents in process, and are protected by trade secret or copyright law.
--
-- Dissemination of this information or reproduction of this material is strictly forbidden
-- unless prior written permission is obtained from COMPANY.
--
-- Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
-- managers or contractors who have executed Confidentiality and Non-disclosure agreements
-- explicitly covering such access.
--
-- The copyright notice above does not evidence any actual or intended publication
-- or disclosure  of  this source code, which includes
-- information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
-- ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
-- OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
-- THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
-- AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
-- THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
-- DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
-- OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
--

-- Span table - stores span data
CREATE TABLE IF NOT EXISTS span
(
    span_id          varchar(255)  NOT NULL,
    trace_id         varchar(255)  NOT NULL,
    tenant_id        uuid          NOT NULL,
    service_name     varchar(255),
    parent_span_id   varchar(255),
    name             varchar(1000) NOT NULL,
    kind             varchar(50),
    attributes_jsonb jsonb         NOT NULL DEFAULT '{}'::jsonb,
    events_jsonb     jsonb         NOT NULL DEFAULT '[]'::jsonb,
    links_jsonb      jsonb         NOT NULL DEFAULT '[]'::jsonb,
    start_time       bigint        NOT NULL,
    end_time         bigint,
    duration         bigint,
    status_code      varchar(20),
    status_message   varchar(5000),
    created_time     bigint        NOT NULL,
    CONSTRAINT span_pkey PRIMARY KEY (tenant_id, trace_id, span_id)
);

-- Indexes for span table
CREATE INDEX IF NOT EXISTS idx_span_tenant_trace ON span (tenant_id, trace_id);
CREATE INDEX IF NOT EXISTS idx_span_attributes_jsonb_gin ON span USING gin (attributes_jsonb);
CREATE INDEX IF NOT EXISTS idx_span_tenant_end_time
    ON span (tenant_id, end_time DESC);

-- Leader-election row for the span-stats aggregation job
-- One pod at a time holds the lease (lock_name = 'aggregation'); lease self-renews on each successful run
CREATE TABLE IF NOT EXISTS span_stats_leader
(
    lock_name    varchar(64)  NOT NULL,
    leader_id    varchar(255) NOT NULL DEFAULT '',
    leader_until bigint       NOT NULL DEFAULT 0,
    CONSTRAINT span_stats_leader_pkey PRIMARY KEY (lock_name)
);
INSERT INTO span_stats_leader (lock_name, leader_id, leader_until)
VALUES ('aggregation', '', 0)
ON CONFLICT DO NOTHING;
INSERT INTO span_stats_leader (lock_name, leader_id, leader_until)
VALUES ('queue-lag', '', 0)
ON CONFLICT DO NOTHING;

-- Pre-aggregated trace summary table (one row per trace, kept current by the aggregation job)
CREATE TABLE IF NOT EXISTS trace_summary
(
    trace_id                  varchar(255) NOT NULL,
    tenant_id                 uuid         NOT NULL,
    service_names_jsonb       jsonb        NOT NULL DEFAULT '[]'::jsonb,
    start_time                bigint       NOT NULL,
    end_time                  bigint,
    duration                  bigint,
    span_count                integer      NOT NULL DEFAULT 0,
    error_count               integer      NOT NULL DEFAULT 0,
    created_time              bigint       NOT NULL,
    first_span_name           varchar(1000),
    total_span_time           bigint,
    in_queue_time             bigint,
    root_queue_id             uuid,
    root_message_type         varchar(255) NOT NULL DEFAULT '',
    root_rule_chain_id        varchar(255) NOT NULL DEFAULT '',
    originator_ids_jsonb      jsonb        NOT NULL DEFAULT '[]'::jsonb,
    queue_names_jsonb         jsonb        NOT NULL DEFAULT '[]'::jsonb,
    rule_chain_ids_jsonb      jsonb        NOT NULL DEFAULT '[]'::jsonb,
    rule_node_ids_jsonb       jsonb        NOT NULL DEFAULT '[]'::jsonb,
    message_types_jsonb       jsonb        NOT NULL DEFAULT '[]'::jsonb,
    message_ids_jsonb         jsonb        NOT NULL DEFAULT '[]'::jsonb,
    has_errors                boolean      NOT NULL DEFAULT false,
    has_queue_timeouts        boolean      NOT NULL DEFAULT false,
    last_updated              bigint       NOT NULL,
    CONSTRAINT trace_summary_pkey PRIMARY KEY (tenant_id, trace_id)
);

CREATE INDEX IF NOT EXISTS idx_ts_tenant_start_time ON trace_summary (tenant_id, start_time DESC);
CREATE INDEX IF NOT EXISTS idx_ts_tenant_errors ON trace_summary (tenant_id, start_time DESC) WHERE has_errors = true;
CREATE INDEX IF NOT EXISTS idx_ts_tenant_end_time ON trace_summary (tenant_id, end_time DESC);

CREATE TABLE IF NOT EXISTS rule_node_exec_stats
(
    bucket_time       bigint       NOT NULL,
    tenant_id         uuid         NOT NULL,
    queue_id          uuid         NOT NULL,
    rule_chain_id     uuid         NOT NULL,
    rule_node_id      uuid         NOT NULL,
    service_id        varchar(255) NOT NULL,
    exec_count        bigint       NOT NULL DEFAULT 0,
    error_count       bigint       NOT NULL DEFAULT 0,
    total_duration_ms bigint       NOT NULL DEFAULT 0,
    max_duration_ms   bigint       NOT NULL DEFAULT 0,
    p95_duration_ms   bigint       NOT NULL DEFAULT 0,
    CONSTRAINT rule_node_exec_stats_pkey
        PRIMARY KEY (bucket_time, queue_id, tenant_id, service_id, rule_chain_id, rule_node_id)
);

CREATE TABLE IF NOT EXISTS rule_node_queue_stats
(
    bucket_time               bigint       NOT NULL,
    queue_tenant_id           uuid         NOT NULL,
    tenant_id                 uuid         NOT NULL,
    queue_id                  uuid         NOT NULL,
    rule_chain_id             uuid         NOT NULL,
    last_visited_rule_node_id uuid         NOT NULL,
    service_id                varchar(255) NOT NULL,
    timeout_count             bigint       NOT NULL DEFAULT 0,
    failure_count             bigint       NOT NULL DEFAULT 0,
    success_count             bigint       NOT NULL DEFAULT 0,
    CONSTRAINT rule_node_queue_stats_pkey
        PRIMARY KEY (bucket_time, queue_tenant_id, queue_id, tenant_id, service_id, rule_chain_id,
                     last_visited_rule_node_id)
);

CREATE TABLE IF NOT EXISTS rule_node_queue_lag_stats
(
    bucket_time     bigint NOT NULL,
    queue_tenant_id uuid   NOT NULL,
    queue_id        uuid   NOT NULL,
    lag             bigint NOT NULL DEFAULT 0,
    CONSTRAINT rule_node_queue_lag_stats_pkey
        PRIMARY KEY (bucket_time, queue_tenant_id, queue_id)
);

-- Trace group: pre-aggregated path-level rollup (one row per distinct path within a bucket)
-- A "path" is identified by path_id (root queue + root message type + root rule chain + visited rule nodes)
CREATE TABLE IF NOT EXISTS trace_group
(
    bucket_time                     bigint       NOT NULL,
    tenant_id                       uuid         NOT NULL,
    path_id                         varchar(255) NOT NULL,
    root_queue_id                   uuid,
    root_message_type               varchar(255),
    root_rule_chain_id              uuid,
    visited_rule_nodes_jsonb        jsonb        NOT NULL DEFAULT '[]'::jsonb,
    related_trace_ids               varchar(255) ARRAY,
    last_related_trace_added_time   bigint       NOT NULL,
    last_observed                   bigint       NOT NULL,
    processed_traces                bigint       NOT NULL DEFAULT 0,
    processed_traces_with_timeout   bigint       NOT NULL DEFAULT 0,
    processed_traces_with_error     bigint       NOT NULL DEFAULT 0,
    stored_traces                   bigint       NOT NULL DEFAULT 0,
    total_time                      bigint       NOT NULL DEFAULT 0,
    max_time                        bigint       NOT NULL DEFAULT 0,
    total_in_queue_time             bigint       NOT NULL DEFAULT 0,
    total_rule_node_processing_time bigint       NOT NULL DEFAULT 0,
    rule_node_path_metrics_jsonb    jsonb        NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT trace_group_pkey PRIMARY KEY (bucket_time, tenant_id, path_id)
);

-- Per-tenant trace coverage setting (singleton per tenant): drives pack-level trace-id injection
CREATE TABLE IF NOT EXISTS trace_coverage_setting
(
    id                                    uuid        NOT NULL
        CONSTRAINT trace_coverage_setting_pkey PRIMARY KEY,
    created_time                          bigint      NOT NULL,
    tenant_id                             uuid        NOT NULL,
    enabled                               boolean     NOT NULL DEFAULT false,
    traces_per_interval                   int         NOT NULL DEFAULT 10,
    traces_per_pack                       int         NOT NULL DEFAULT 1,
    interval_seconds                      integer     NOT NULL DEFAULT 1,
    rule_engine_rotation                  boolean     NOT NULL DEFAULT true,
    switch_period_seconds                 int         NOT NULL DEFAULT 60,
    related_trace_sample_interval_seconds int         NOT NULL DEFAULT 1800,
    message_payload_recording             varchar(16) NOT NULL DEFAULT 'NONE',
    max_trace_groups_per_day              int         NOT NULL DEFAULT 10000
);

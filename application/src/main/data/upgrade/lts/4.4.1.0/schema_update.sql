--
-- SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
-- SPDX-License-Identifier: BUSL-1.1
--

-- Tenant-scoped paging over calculated fields (WHERE tenant_id = ? ORDER BY id LIMIT ?), issued for every tenant
-- when the calculated field actors initialize, otherwise scans the whole table and sorts on each call.
CREATE INDEX IF NOT EXISTS idx_calculated_field_tenant_id_id ON calculated_field(tenant_id, id);

-- Rule chain tracing and rule engine monitoring tables, the same as in schema-entities.sql.

-- Per-tenant trace coverage setting (singleton per tenant): drives pack-level trace-id injection.
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
    related_trace_sample_interval_seconds int         NOT NULL DEFAULT 1800,
    message_payload_recording             varchar(16) NOT NULL DEFAULT 'NONE',
    max_trace_groups_per_day              int         NOT NULL DEFAULT 10000,
    CONSTRAINT trace_coverage_setting_tenant_id_unq_key UNIQUE (tenant_id)
);

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
    CONSTRAINT span_pkey PRIMARY KEY (tenant_id, trace_id, span_id, start_time)
) PARTITION BY RANGE (start_time);
-- Partitions (span_<partition start ms>, sql.traces.partition_size wide) are created on insert and dropped by the
-- trace TTL cleanup. start_time never changes for a span, so a re-saved span still lands on its existing row.

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
-- For the trace TTL cleanup
CREATE INDEX IF NOT EXISTS idx_ts_start_time ON trace_summary (start_time);

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
    CONSTRAINT rule_node_exec_stats_pkey
        PRIMARY KEY (bucket_time, queue_id, tenant_id, service_id, rule_chain_id, rule_node_id)
) PARTITION BY RANGE (bucket_time);
-- Monitoring queries are per tenant over a time range; the PK leads with bucket_time and would scan every tenant.
CREATE INDEX IF NOT EXISTS idx_rule_node_exec_stats_tenant_bucket ON rule_node_exec_stats (tenant_id, bucket_time);

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
) PARTITION BY RANGE (bucket_time);
CREATE INDEX IF NOT EXISTS idx_rule_node_queue_stats_tenant_bucket ON rule_node_queue_stats (tenant_id, bucket_time);
CREATE INDEX IF NOT EXISTS idx_rule_node_queue_stats_queue_tenant_bucket ON rule_node_queue_stats (queue_tenant_id, bucket_time);

CREATE TABLE IF NOT EXISTS rule_node_queue_lag_stats
(
    bucket_time     bigint NOT NULL,
    queue_tenant_id uuid   NOT NULL,
    queue_id        uuid   NOT NULL,
    lag             bigint NOT NULL DEFAULT 0,
    CONSTRAINT rule_node_queue_lag_stats_pkey
        PRIMARY KEY (bucket_time, queue_tenant_id, queue_id)
) PARTITION BY RANGE (bucket_time);
CREATE INDEX IF NOT EXISTS idx_rule_node_queue_lag_stats_queue_tenant_bucket ON rule_node_queue_lag_stats (queue_tenant_id, bucket_time);

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
-- The PK leads with bucket_time for the TTL cleanup; reads and tenant deletion are per tenant over a bucket range.
CREATE INDEX IF NOT EXISTS idx_trace_group_tenant_bucket ON trace_group (tenant_id, bucket_time);

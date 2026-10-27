--
-- SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
-- SPDX-License-Identifier: BUSL-1.1
--

-- Tenant-scoped paging over calculated fields (WHERE tenant_id = ? ORDER BY id LIMIT ?), issued for every tenant
-- when the calculated field actors initialize, otherwise scans the whole table and sorts on each call.
CREATE INDEX IF NOT EXISTS idx_calculated_field_tenant_id_id ON calculated_field(tenant_id, id);

-- Per-tenant trace coverage setting (singleton per tenant): drives pack-level trace-id injection.
-- The trace tables themselves are created by V4_4_1_0Migration, as they may live in the dedicated events database.
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

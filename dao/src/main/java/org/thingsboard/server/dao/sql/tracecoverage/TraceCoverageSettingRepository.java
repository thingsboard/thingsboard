// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.tracecoverage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.tracecoverage.TraceCoverageSettingEntity;

import java.util.UUID;

public interface TraceCoverageSettingRepository extends JpaRepository<TraceCoverageSettingEntity, UUID> {
    TraceCoverageSettingEntity findByTenantId(UUID tenantId);

    void deleteByTenantId(UUID tenantId);

    // One row per tenant: the first save inserts, later (and concurrent) saves update it in place, keeping its id
    // and created_time.
    @Query(value = "INSERT INTO trace_coverage_setting (id, created_time, tenant_id, enabled, traces_per_interval, " +
            "traces_per_pack, interval_seconds, rule_engine_rotation, switch_period_seconds, " +
            "related_trace_sample_interval_seconds, message_payload_recording, max_trace_groups_per_day) " +
            "VALUES (:#{#s.uuid}, :#{#s.createdTime}, :#{#s.tenantId}, :#{#s.enabled}, :#{#s.tracesPerInterval}, " +
            ":#{#s.tracesPerPack}, :#{#s.intervalSeconds}, :#{#s.ruleEngineRotation}, :#{#s.switchPeriodSeconds}, " +
            ":#{#s.relatedTraceSampleIntervalSeconds}, :#{#s.messagePayloadRecording}, :#{#s.maxTraceGroupsPerDay}) " +
            "ON CONFLICT (tenant_id) DO UPDATE SET enabled = EXCLUDED.enabled, " +
            "traces_per_interval = EXCLUDED.traces_per_interval, traces_per_pack = EXCLUDED.traces_per_pack, " +
            "interval_seconds = EXCLUDED.interval_seconds, rule_engine_rotation = EXCLUDED.rule_engine_rotation, " +
            "switch_period_seconds = EXCLUDED.switch_period_seconds, " +
            "related_trace_sample_interval_seconds = EXCLUDED.related_trace_sample_interval_seconds, " +
            "message_payload_recording = EXCLUDED.message_payload_recording, " +
            "max_trace_groups_per_day = EXCLUDED.max_trace_groups_per_day " +
            "RETURNING *", nativeQuery = true)
    TraceCoverageSettingEntity upsertByTenantId(@Param("s") TraceCoverageSettingEntity setting);
}

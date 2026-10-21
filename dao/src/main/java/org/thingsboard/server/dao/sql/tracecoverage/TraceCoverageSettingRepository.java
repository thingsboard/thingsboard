/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2026 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
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

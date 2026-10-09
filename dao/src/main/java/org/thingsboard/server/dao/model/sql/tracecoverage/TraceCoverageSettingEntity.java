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
package org.thingsboard.server.dao.model.sql.tracecoverage;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.trace.TraceCoverageSetting;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseSqlEntity;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.TRACE_COVERAGE_SETTING_ENABLED_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.TRACE_COVERAGE_SETTING_INTERVAL_SECONDS_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.TRACE_COVERAGE_SETTING_MAX_TRACE_GROUPS_PER_DAY_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.TRACE_COVERAGE_SETTING_MESSAGE_PAYLOAD_RECORDING_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.TRACE_COVERAGE_SETTING_RELATED_TRACE_SAMPLE_INTERVAL_SECONDS_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.TRACE_COVERAGE_SETTING_RULE_ENGINE_ROTATION_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.TRACE_COVERAGE_SETTING_SWITCH_PERIOD_SECONDS_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.TRACE_COVERAGE_SETTING_TABLE_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.TRACE_COVERAGE_SETTING_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.TRACE_COVERAGE_SETTING_TRACES_PER_INTERVAL_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.TRACE_COVERAGE_SETTING_TRACES_PER_PACK_PROPERTY;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = TRACE_COVERAGE_SETTING_TABLE_NAME)
public final class TraceCoverageSettingEntity extends BaseSqlEntity<TraceCoverageSetting> implements BaseEntity<TraceCoverageSetting> {
    @Column(name = TRACE_COVERAGE_SETTING_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = TRACE_COVERAGE_SETTING_ENABLED_PROPERTY)
    private boolean enabled;

    @Column(name = TRACE_COVERAGE_SETTING_TRACES_PER_INTERVAL_PROPERTY)
    private int tracesPerInterval;

    @Column(name = TRACE_COVERAGE_SETTING_TRACES_PER_PACK_PROPERTY)
    private int tracesPerPack;

    @Column(name = TRACE_COVERAGE_SETTING_INTERVAL_SECONDS_PROPERTY)
    private int intervalSeconds;

    @Column(name = TRACE_COVERAGE_SETTING_RULE_ENGINE_ROTATION_PROPERTY)
    private boolean ruleEngineRotation;

    @Column(name = TRACE_COVERAGE_SETTING_SWITCH_PERIOD_SECONDS_PROPERTY)
    private int switchPeriodSeconds;

    @Column(name = TRACE_COVERAGE_SETTING_RELATED_TRACE_SAMPLE_INTERVAL_SECONDS_PROPERTY)
    private int relatedTraceSampleIntervalSeconds;

    @Column(name = TRACE_COVERAGE_SETTING_MESSAGE_PAYLOAD_RECORDING_PROPERTY)
    private String messagePayloadRecording;

    @Column(name = TRACE_COVERAGE_SETTING_MAX_TRACE_GROUPS_PER_DAY_PROPERTY)
    private int maxTraceGroupsPerDay;

    public TraceCoverageSettingEntity() {
        super();
    }

    public TraceCoverageSettingEntity(TraceCoverageSetting setting) {
        if (setting.getId() != null) {
            this.setUuid(setting.getId());
        }
        this.setCreatedTime(setting.getCreatedTime());
        this.tenantId = setting.getTenantId();
        this.enabled = setting.isEnabled();
        this.tracesPerInterval = setting.getTracesPerInterval();
        this.tracesPerPack = setting.getTracesPerPack();
        this.intervalSeconds = setting.getIntervalSeconds();
        this.ruleEngineRotation = setting.isRuleEngineRotation();
        this.switchPeriodSeconds = setting.getSwitchPeriodSeconds();
        this.relatedTraceSampleIntervalSeconds = setting.getRelatedTraceSampleIntervalSeconds();
        this.messagePayloadRecording = setting.getMessagePayloadRecording() != null
                ? setting.getMessagePayloadRecording().name()
                : TraceCoverageSetting.DEFAULT_MESSAGE_PAYLOAD_RECORDING.name();
        this.maxTraceGroupsPerDay = setting.getMaxTraceGroupsPerDay();
    }

    @Override
    public TraceCoverageSetting toData() {
        TraceCoverageSetting setting = new TraceCoverageSetting();
        setting.setId(id);
        setting.setCreatedTime(createdTime);
        setting.setTenantId(tenantId);
        setting.setEnabled(enabled);
        setting.setTracesPerInterval(tracesPerInterval);
        setting.setTracesPerPack(tracesPerPack);
        setting.setIntervalSeconds(intervalSeconds > 0
                ? intervalSeconds
                : TraceCoverageSetting.DEFAULT_INTERVAL_SECONDS);
        setting.setRuleEngineRotation(ruleEngineRotation);
        setting.setSwitchPeriodSeconds(switchPeriodSeconds);
        setting.setRelatedTraceSampleIntervalSeconds(relatedTraceSampleIntervalSeconds);
        setting.setMessagePayloadRecording(parseMessagePayloadRecording(messagePayloadRecording));
        setting.setMaxTraceGroupsPerDay(maxTraceGroupsPerDay);
        return setting;
    }

    private static TraceCoverageSetting.MessagePayloadRecording parseMessagePayloadRecording(String messagePayloadRecording) {
        if (messagePayloadRecording == null) {
            return TraceCoverageSetting.DEFAULT_MESSAGE_PAYLOAD_RECORDING;
        }
        if ("ROOT_MESSAGE".equals(messagePayloadRecording)) {
            return TraceCoverageSetting.MessagePayloadRecording.FIRST_SPAN;
        }
        return TraceCoverageSetting.MessagePayloadRecording.valueOf(messagePayloadRecording);
    }
}

/**
 * Copyright © 2016-2018 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.model.nosql;

import com.datastax.driver.core.utils.UUIDs;
import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmId;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.type.AlarmSeverityCodec;
import org.thingsboard.server.dao.model.type.AlarmStatusCodec;
import org.thingsboard.server.dao.model.type.EntityTypeCodec;
import org.thingsboard.server.dao.model.type.JsonCodec;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.ALARM_ACK_TS_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_CLEAR_TS_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_DETAILS_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_END_TS_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_ORIGINATOR_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_ORIGINATOR_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_PROPAGATE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_SEVERITY_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_START_TS_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_STATUS_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ID_PROPERTY;

@Table(name = ALARM_COLUMN_FAMILY_NAME)
@EqualsAndHashCode
@ToString
public final class AlarmEntity implements BaseEntity<Alarm> {

    @ClusteringColumn(value = 1)
    @Column(name = ID_PROPERTY)
    private UUID id;

    @PartitionKey(value = 0)
    @Column(name = ALARM_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @PartitionKey(value = 1)
    @Column(name = ALARM_ORIGINATOR_ID_PROPERTY)
    private UUID originatorId;

    @PartitionKey(value = 2)
    @Column(name = ALARM_ORIGINATOR_TYPE_PROPERTY, codec = EntityTypeCodec.class)
    private EntityType originatorType;

    @ClusteringColumn(value = 0)
    @Column(name = ALARM_TYPE_PROPERTY)
    private String type;

    @Column(name = ALARM_SEVERITY_PROPERTY, codec = AlarmSeverityCodec.class)
    private AlarmSeverity severity;

    @Column(name = ALARM_STATUS_PROPERTY, codec = AlarmStatusCodec.class)
    private AlarmStatus status;

    @Column(name = ALARM_START_TS_PROPERTY)
    private Long startTs;

    @Column(name = ALARM_END_TS_PROPERTY)
    private Long endTs;

    @Column(name = ALARM_ACK_TS_PROPERTY)
    private Long ackTs;

    @Column(name = ALARM_CLEAR_TS_PROPERTY)
    private Long clearTs;

    @Column(name = ALARM_DETAILS_PROPERTY, codec = JsonCodec.class)
    private JsonNode details;

    @Column(name = ALARM_PROPAGATE_PROPERTY)
    private Boolean propagate;

    public AlarmEntity() {
        super();
    }

    public AlarmEntity(Alarm alarm) {
        if (alarm.getId() != null) {
            this.id = alarm.getId().getId();
        }
        if (alarm.getTenantId() != null) {
            this.tenantId = alarm.getTenantId().getId();
        }
        this.type = alarm.getType();
        this.originatorId = alarm.getOriginator().getId();
        this.originatorType = alarm.getOriginator().getEntityType();
        this.type = alarm.getType();
        this.severity = alarm.getSeverity();
        this.status = alarm.getStatus();
        this.propagate = alarm.isPropagate();
        this.startTs = alarm.getStartTs();
        this.endTs = alarm.getEndTs();
        this.ackTs = alarm.getAckTs();
        this.clearTs = alarm.getClearTs();
        this.details = alarm.getDetails();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getOriginatorId() {
        return originatorId;
    }

    public void setOriginatorId(UUID originatorId) {
        this.originatorId = originatorId;
    }

    public EntityType getOriginatorType() {
        return originatorType;
    }

    public void setOriginatorType(EntityType originatorType) {
        this.originatorType = originatorType;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public AlarmSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(AlarmSeverity severity) {
        this.severity = severity;
    }

    public AlarmStatus getStatus() {
        return status;
    }

    public void setStatus(AlarmStatus status) {
        this.status = status;
    }

    public Long getStartTs() {
        return startTs;
    }

    public void setStartTs(Long startTs) {
        this.startTs = startTs;
    }

    public Long getEndTs() {
        return endTs;
    }

    public void setEndTs(Long endTs) {
        this.endTs = endTs;
    }

    public Long getAckTs() {
        return ackTs;
    }

    public void setAckTs(Long ackTs) {
        this.ackTs = ackTs;
    }

    public Long getClearTs() {
        return clearTs;
    }

    public void setClearTs(Long clearTs) {
        this.clearTs = clearTs;
    }

    public JsonNode getDetails() {
        return details;
    }

    public void setDetails(JsonNode details) {
        this.details = details;
    }

    public Boolean getPropagate() {
        return propagate;
    }

    public void setPropagate(Boolean propagate) {
        this.propagate = propagate;
    }

    @Override
    public Alarm toData() {
        Alarm alarm = new Alarm(new AlarmId(id));
        alarm.setCreatedTime(UUIDs.unixTimestamp(id));
        if (tenantId != null) {
            alarm.setTenantId(new TenantId(tenantId));
        }
        alarm.setOriginator(EntityIdFactory.getByTypeAndUuid(originatorType, originatorId));
        alarm.setType(type);
        alarm.setSeverity(severity);
        alarm.setStatus(status);
        alarm.setPropagate(propagate);
        alarm.setStartTs(startTs);
        alarm.setEndTs(endTs);
        alarm.setAckTs(ackTs);
        alarm.setClearTs(clearTs);
        alarm.setDetails(details);
        return alarm;
    }

}
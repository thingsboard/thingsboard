// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.dao.model.BaseVersionedEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.SCHEDULER_EVENT_CUSTOMER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.SCHEDULER_EVENT_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.SCHEDULER_EVENT_ORIGINATOR_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.SCHEDULER_EVENT_ORIGINATOR_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.SCHEDULER_EVENT_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.SCHEDULER_EVENT_TYPE_PROPERTY;

@Data
@EqualsAndHashCode(callSuper = true)
@MappedSuperclass
public abstract class AbstractSchedulerEventInfoEntity<T extends SchedulerEventInfo> extends BaseVersionedEntity<T> {

    @Column(name = SCHEDULER_EVENT_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = SCHEDULER_EVENT_CUSTOMER_ID_PROPERTY)
    private UUID customerId;

    @Column(name = SCHEDULER_EVENT_ORIGINATOR_ID_PROPERTY)
    private UUID originatorId;

    @Enumerated(EnumType.STRING)
    @Column(name = SCHEDULER_EVENT_ORIGINATOR_TYPE_PROPERTY)
    private EntityType originatorType;

    @Column(name = SCHEDULER_EVENT_NAME_PROPERTY)
    private String name;

    @Column(name = SCHEDULER_EVENT_TYPE_PROPERTY)
    private String type;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.SCHEDULER_EVENT_ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.SCHEDULER_EVENT_SCHEDULE_PROPERTY)
    private JsonNode schedule;

    @Column(name = ModelConstants.SCHEDULER_EVENT_ENABLED_PROPERTY)
    private boolean enabled;

    @Column(name = ModelConstants.EXTERNAL_ID_PROPERTY)
    private UUID externalId;

    public AbstractSchedulerEventInfoEntity() {
        super();
    }

    public AbstractSchedulerEventInfoEntity(SchedulerEventInfo schedulerEventInfo) {
        if (schedulerEventInfo.getId() != null) {
            this.setUuid(schedulerEventInfo.getId().getId());
        }
        this.setCreatedTime(schedulerEventInfo.getCreatedTime());
        if (schedulerEventInfo.getTenantId() != null) {
            this.tenantId = schedulerEventInfo.getTenantId().getId();
        }
        if (schedulerEventInfo.getCustomerId() != null) {
            this.customerId = schedulerEventInfo.getCustomerId().getId();
        }
        if (schedulerEventInfo.getOriginatorId() != null) {
            this.originatorId = schedulerEventInfo.getOriginatorId().getId();
            this.originatorType = schedulerEventInfo.getOriginatorId().getEntityType();
        }
        this.name = schedulerEventInfo.getName();
        this.type = schedulerEventInfo.getType();
        this.additionalInfo = schedulerEventInfo.getAdditionalInfo();
        this.schedule = schedulerEventInfo.getSchedule();
        this.enabled = schedulerEventInfo.isEnabled();
        this.externalId = getUuid(schedulerEventInfo.getExternalId());
        this.version = schedulerEventInfo.getVersion();
    }

    public AbstractSchedulerEventInfoEntity(SchedulerEventInfoEntity schedulerEventInfoEntity) {
        this.setId(schedulerEventInfoEntity.getId());
        this.setCreatedTime(schedulerEventInfoEntity.getCreatedTime());
        this.tenantId = schedulerEventInfoEntity.getTenantId();
        this.customerId = schedulerEventInfoEntity.getCustomerId();
        this.originatorId = schedulerEventInfoEntity.getOriginatorId();
        this.originatorType = schedulerEventInfoEntity.getOriginatorType();
        this.type = schedulerEventInfoEntity.getType();
        this.name = schedulerEventInfoEntity.getName();
        this.schedule = schedulerEventInfoEntity.getSchedule();
        this.additionalInfo = schedulerEventInfoEntity.getAdditionalInfo();
        this.enabled = schedulerEventInfoEntity.isEnabled();
        this.externalId = schedulerEventInfoEntity.getExternalId();
        this.version = schedulerEventInfoEntity.getVersion();
    }

    protected SchedulerEventInfo toSchedulerEventInfo() {
        SchedulerEventInfo schedulerEventInfo = new SchedulerEventInfo(new SchedulerEventId(id));
        schedulerEventInfo.setCreatedTime(createdTime);
        if (tenantId != null) {
            schedulerEventInfo.setTenantId(TenantId.fromUUID(tenantId));
        }
        if (customerId != null) {
            schedulerEventInfo.setCustomerId(new CustomerId(customerId));
        }
        if (originatorId != null && originatorType != null) {
            schedulerEventInfo.setOriginatorId(EntityIdFactory.getByTypeAndUuid(originatorType, originatorId));
        }
        schedulerEventInfo.setName(name);
        schedulerEventInfo.setType(type);
        schedulerEventInfo.setSchedule(schedule);
        schedulerEventInfo.setAdditionalInfo(additionalInfo);
        schedulerEventInfo.setEnabled(enabled);
        schedulerEventInfo.setExternalId(getEntityId(externalId, SchedulerEventId::new));
        schedulerEventInfo.setVersion(version);
        return schedulerEventInfo;
    }

}

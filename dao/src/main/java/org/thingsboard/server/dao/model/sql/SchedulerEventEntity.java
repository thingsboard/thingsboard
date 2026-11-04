// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import static org.thingsboard.server.dao.model.ModelConstants.SCHEDULER_EVENT_TABLE_NAME;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = SCHEDULER_EVENT_TABLE_NAME)
public final class SchedulerEventEntity extends AbstractSchedulerEventInfoEntity<SchedulerEvent> {

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.SCHEDULER_EVENT_CONFIGURATION_PROPERTY)
    private JsonNode configuration;

    public SchedulerEventEntity() {
        super();
    }

    public SchedulerEventEntity(SchedulerEvent schedulerEvent) {
        super(schedulerEvent);
        this.configuration = schedulerEvent.getConfiguration();
    }

    @Override
    public SchedulerEvent toData() {
        return new SchedulerEvent(super.toSchedulerEventInfo(), configuration);
    }

}

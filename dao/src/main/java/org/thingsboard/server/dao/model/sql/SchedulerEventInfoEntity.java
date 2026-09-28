// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;

import static org.thingsboard.server.dao.model.ModelConstants.SCHEDULER_EVENT_TABLE_NAME;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = SCHEDULER_EVENT_TABLE_NAME)
public final class SchedulerEventInfoEntity extends AbstractSchedulerEventInfoEntity<SchedulerEventInfo> {

    public SchedulerEventInfoEntity() {
        super();
    }

    public SchedulerEventInfoEntity(SchedulerEventInfo schedulerEventInfo) {
        super(schedulerEventInfo);
    }

    @Override
    public SchedulerEventInfo toData() {
        return super.toSchedulerEventInfo();
    }

}

// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.sync.ie.exporting.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.sync.ie.SchedulerEventExportData;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesExportCtx;

import java.util.Set;

@Service
@TbCoreComponent
public class SchedulerEventExportService extends BaseEntityExportService<SchedulerEventId, SchedulerEvent, SchedulerEventExportData> {

    @Override
    protected void setRelatedEntities(EntitiesExportCtx<?> ctx, SchedulerEvent schedulerEvent, SchedulerEventExportData exportData) {
        schedulerEvent.setOriginatorId(getExternalIdOrElseInternal(ctx, schedulerEvent.getOriginatorId()));
        schedulerEvent.setCustomerId(getExternalIdOrElseInternal(ctx, schedulerEvent.getCustomerId()));
        JsonNode configuration = exportData.prepareConfiguration(schedulerEvent.getConfiguration(), schedulerEvent.getType(),
                id -> getExternalIdOrElseInternal(ctx, id), ctx.getUser().getId());
        schedulerEvent.setConfiguration(configuration);
    }

    @Override
    public Set<EntityType> getSupportedEntityTypes() {
        return Set.of(EntityType.SCHEDULER_EVENT);
    }

}

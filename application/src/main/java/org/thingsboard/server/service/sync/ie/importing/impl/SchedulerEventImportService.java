// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.sync.ie.importing.impl;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.sync.ie.SchedulerEventExportData;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class SchedulerEventImportService extends BaseEntityImportService<SchedulerEventId, SchedulerEvent, SchedulerEventExportData> {

    private final SchedulerEventService schedulerEventService;

    @Override
    protected void setOwner(TenantId tenantId, SchedulerEvent schedulerEvent, IdProvider idProvider) {
        schedulerEvent.setTenantId(tenantId);
        if (schedulerEvent.getOwnerId() instanceof TenantId) {
            schedulerEvent.setOwnerId(tenantId);
        } else {
            schedulerEvent.setOwnerId(idProvider.getInternalId(schedulerEvent.getOwnerId()));
        }
    }

    @Override
    protected SchedulerEvent prepare(EntitiesImportCtx ctx, SchedulerEvent schedulerEvent, SchedulerEvent oldSchedulerEvent, SchedulerEventExportData exportData, IdProvider idProvider) {
        // Groups are imported after entities, so a group-originator lookup may return null. Validation forbids a null originator,
        // so we assign a temporary originator id and rely on reimport to correct it later
        EntityId originatorId = schedulerEvent.getOriginatorId();
        boolean isEntityGroup = originatorId != null && originatorId.getEntityType() == EntityType.ENTITY_GROUP;
        EntityId internalId = idProvider.getInternalId(originatorId, !isEntityGroup || ctx.isFinalImportAttempt());
        schedulerEvent.setOriginatorId(internalId != null ? internalId : originatorId);
        JsonNode configuration = exportData.prepareConfiguration(schedulerEvent.getConfiguration(), schedulerEvent.getType(),
                idProvider::getInternalId, ctx.getUser().getId());
        schedulerEvent.setConfiguration(configuration);
        return schedulerEvent;
    }

    @Override
    protected SchedulerEvent deepCopy(SchedulerEvent schedulerEvent) {
        return new SchedulerEvent(schedulerEvent);
    }

    @Override
    protected SchedulerEvent saveOrUpdate(EntitiesImportCtx ctx, SchedulerEvent schedulerEvent, SchedulerEventExportData exportData, IdProvider idProvider, CompareResult compareResult) throws Exception {
        return schedulerEventService.saveSchedulerEvent(schedulerEvent);
    }

    @Override
    protected void cleanupForComparison(SchedulerEvent schedulerEvent) {
        super.cleanupForComparison(schedulerEvent);
        if (schedulerEvent.getCustomerId() != null && schedulerEvent.getCustomerId().isNullUid()) {
            schedulerEvent.setCustomerId(null);
        }
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.SCHEDULER_EVENT;
    }

}

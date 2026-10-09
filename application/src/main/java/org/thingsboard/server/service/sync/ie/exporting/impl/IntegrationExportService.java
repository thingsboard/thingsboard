// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.sync.ie.exporting.impl;

import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesExportCtx;

import java.util.Set;

@Service
@TbCoreComponent
public class IntegrationExportService extends BaseEntityExportService<IntegrationId, Integration, EntityExportData<Integration>> {

    @Override
    protected void setRelatedEntities(EntitiesExportCtx<?> ctx, Integration integration, EntityExportData<Integration> exportData) {
        integration.setDefaultConverterId(getExternalIdOrElseInternal(ctx, integration.getDefaultConverterId()));
        integration.setDownlinkConverterId(getExternalIdOrElseInternal(ctx, integration.getDownlinkConverterId()));
    }

    @Override
    public Set<EntityType> getSupportedEntityTypes() {
        return Set.of(EntityType.INTEGRATION);
    }

}

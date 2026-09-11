// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sync.ie.exporting.impl;

import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesExportCtx;

import java.util.Set;

@Service
@TbCoreComponent
public class EntityViewExportService extends BaseEntityExportService<EntityViewId, EntityView, EntityExportData<EntityView>> {

    @Override
    protected void setRelatedEntities(EntitiesExportCtx<?> ctx, EntityView entityView, EntityExportData<EntityView> exportData) {
        entityView.setEntityId(getExternalIdOrElseInternal(ctx, entityView.getEntityId()));
        entityView.setCustomerId(getExternalIdOrElseInternal(ctx, entityView.getCustomerId()));
    }

    @Override
    public Set<EntityType> getSupportedEntityTypes() {
        return Set.of(EntityType.ENTITY_VIEW);
    }

}

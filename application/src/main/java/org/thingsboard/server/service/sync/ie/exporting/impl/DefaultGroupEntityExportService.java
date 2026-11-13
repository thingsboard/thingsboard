// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.sync.ie.exporting.impl;

import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.GroupEntity;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesExportCtx;

import java.util.Set;

@Service
@TbCoreComponent
public class DefaultGroupEntityExportService<I extends EntityId, E extends ExportableEntity<I> & GroupEntity<I>, D extends EntityExportData<E>> extends BaseEntityExportService<I, E, D> {

    @Override
    protected final void setAdditionalExportData(EntitiesExportCtx<?> ctx, E entity, D exportData) throws ThingsboardException {
        super.setAdditionalExportData(ctx, entity, exportData);
    }


    @Override
    public Set<EntityType> getSupportedEntityTypes() {
        return Set.of(EntityType.DEVICE, EntityType.CUSTOMER, EntityType.ASSET, EntityType.DASHBOARD, EntityType.ENTITY_VIEW);
    }

}

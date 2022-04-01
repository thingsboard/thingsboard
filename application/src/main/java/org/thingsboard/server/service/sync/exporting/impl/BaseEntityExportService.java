/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.service.sync.exporting.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.service.sync.exporting.ExportableEntitiesService;
import org.thingsboard.server.service.sync.exporting.EntityExportService;
import org.thingsboard.server.service.sync.exporting.EntityExportSettings;
import org.thingsboard.server.service.sync.exporting.data.EntityExportData;

import java.util.List;

public abstract class BaseEntityExportService<I extends EntityId, E extends ExportableEntity<I>, D extends EntityExportData<E>> implements EntityExportService<I, E, D> {

    @Autowired @Lazy
    private ExportableEntitiesService exportableEntitiesService;
    @Autowired
    private RelationService relationService;

    @Override
    public final D getExportData(TenantId tenantId, I entityId, EntityExportSettings exportSettings) {
        D exportData = newExportData();

        E entity = exportableEntitiesService.findEntityById(tenantId, entityId);
        exportData.setEntity(entity);
        setRelatedEntities(tenantId, entity, exportData);

        if (exportSettings.isExportInboundRelations()) {
            List<EntityRelation> inboundRelations = relationService.findByTo(tenantId, entityId, RelationTypeGroup.COMMON);
            exportData.setInboundRelations(inboundRelations);
        }
        if (exportSettings.isExportOutboundRelations()) {
            List<EntityRelation> outboundRelations = relationService.findByFrom(tenantId, entityId, RelationTypeGroup.COMMON);
            exportData.setOutboundRelations(outboundRelations);
        }

        return exportData;
    }

    protected void setRelatedEntities(TenantId tenantId, E mainEntity, D exportData) {}

    protected abstract D newExportData();

}

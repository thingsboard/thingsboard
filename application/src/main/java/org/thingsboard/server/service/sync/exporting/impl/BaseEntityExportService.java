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
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.sync.exporting.EntityExportService;
import org.thingsboard.server.service.sync.exporting.EntityExportSettings;
import org.thingsboard.server.service.sync.exporting.ExportableEntitiesService;
import org.thingsboard.server.service.sync.exporting.data.EntityExportData;

import java.util.List;

public abstract class BaseEntityExportService<I extends EntityId, E extends ExportableEntity<I>, D extends EntityExportData<E>> implements EntityExportService<I, E, D> {

    @Autowired @Lazy
    private ExportableEntitiesService exportableEntitiesService;
    @Autowired
    private RelationService relationService;

    @Override
    public final D getExportData(SecurityUser user, I entityId, EntityExportSettings exportSettings) throws ThingsboardException {
        D exportData = newExportData();

        E entity = exportableEntitiesService.findEntityByTenantIdAndId(user.getTenantId(), entityId);
        if (entity == null) {
            throw new IllegalArgumentException("Entity not found");
        }
        exportableEntitiesService.checkPermission(user, entity, getEntityType(), Operation.READ);

        exportData.setEntity(entity);
        setRelatedEntities(user.getTenantId(), entity, exportData);

        if (exportSettings.isExportInboundRelations()) {
            List<EntityRelation> inboundRelations = relationService.findByTo(user.getTenantId(), entityId, RelationTypeGroup.COMMON);
            if (inboundRelations != null) {
                for (EntityRelation relation : inboundRelations) {
                    exportableEntitiesService.checkPermission(user, relation.getFrom(), Operation.READ);
                }
            }
            exportData.setInboundRelations(inboundRelations);
        }
        if (exportSettings.isExportOutboundRelations()) {
            List<EntityRelation> outboundRelations = relationService.findByFrom(user.getTenantId(), entityId, RelationTypeGroup.COMMON);
            if (outboundRelations != null) {
                for (EntityRelation relation : outboundRelations) {
                    exportableEntitiesService.checkPermission(user, relation.getTo(), Operation.READ);
                }
            }
            exportData.setOutboundRelations(outboundRelations);
        }

        return exportData;
    }

    protected void setRelatedEntities(TenantId tenantId, E mainEntity, D exportData) {}

    protected abstract D newExportData();

}

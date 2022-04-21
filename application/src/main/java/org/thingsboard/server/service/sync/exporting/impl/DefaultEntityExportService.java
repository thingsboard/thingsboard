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
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.sync.exporting.EntityExportService;
import org.thingsboard.server.service.sync.exporting.ExportableEntitiesService;
import org.thingsboard.server.service.sync.exporting.data.EntityExportData;
import org.thingsboard.server.service.sync.exporting.data.request.EntityExportSettings;

import java.util.ArrayList;
import java.util.List;

@Service
@TbCoreComponent
@Primary
public class DefaultEntityExportService<I extends EntityId, E extends ExportableEntity<I>, D extends EntityExportData<E>> implements EntityExportService<I, E, D> {

    @Autowired @Lazy
    private ExportableEntitiesService exportableEntitiesService;
    @Autowired
    private RelationService relationService;

    @Override
    public final D getExportData(SecurityUser user, I entityId, EntityExportSettings exportSettings) throws ThingsboardException {
        D exportData = newExportData();

        E entity = exportableEntitiesService.findEntityByTenantIdAndId(user.getTenantId(), entityId);
        if (entity == null) {
            throw new IllegalArgumentException(entityId.getEntityType() + " [" + entityId.getId() + "] not found");
        }
        exportableEntitiesService.checkPermission(user, entity, entity.getId().getEntityType(), Operation.READ);

        exportData.setEntity(entity);
        exportData.setEntityType(entityId.getEntityType());
        setAdditionalExportData(user, entity, exportData, exportSettings);

        return exportData;
    }

    protected void setAdditionalExportData(SecurityUser user, E entity, D exportData, EntityExportSettings exportSettings) throws ThingsboardException {
        if (exportSettings.isExportRelations()) {
            List<EntityRelation> relations = new ArrayList<>();

            List<EntityRelation> inboundRelations = relationService.findByTo(user.getTenantId(), entity.getId(), RelationTypeGroup.COMMON);
            for (EntityRelation relation : inboundRelations) {
                exportableEntitiesService.checkPermission(user, relation.getFrom(), Operation.READ);
            }
            relations.addAll(inboundRelations);

            List<EntityRelation> outboundRelations = relationService.findByFrom(user.getTenantId(), entity.getId(), RelationTypeGroup.COMMON);
            for (EntityRelation relation : outboundRelations) {
                exportableEntitiesService.checkPermission(user, relation.getTo(), Operation.READ);
            }
            relations.addAll(outboundRelations);

            exportData.setRelations(relations);
        }
    }

    protected D newExportData() {
        return (D) new EntityExportData<E>();
    }

}

/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.service.sync.ie.exporting.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.service.sync.vc.data.EntitiesExportCtx;

import java.util.Set;

public abstract class BaseEntityExportService<I extends EntityId, E extends ExportableEntity<I>, D extends EntityExportData<E>> extends DefaultEntityExportService<I, E, D> {

    @Override
    protected void setAdditionalExportData(EntitiesExportCtx<?> ctx, E entity, D exportData) throws ThingsboardException {
        setRelatedEntities(ctx, entity, (D) exportData);
        super.setAdditionalExportData(ctx, entity, exportData);
    }

    protected void setRelatedEntities(EntitiesExportCtx<?> ctx, E mainEntity, D exportData) {
    }

    protected D newExportData() {
        return (D) new EntityExportData<E>();
    }

    public abstract Set<EntityType> getSupportedEntityTypes();

    protected void replaceUuidsRecursively(EntitiesExportCtx<?> ctx, JsonNode node, Set<String> skipFieldsSet) {
        JacksonUtil.replaceUuidsRecursively(node, skipFieldsSet, uuid -> getExternalIdOrElseInternalByUuid(ctx, uuid));
    }

}

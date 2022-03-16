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
package org.thingsboard.server.service.expimp.imp.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.thingsboard.server.common.data.export.EntityExportData;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.service.expimp.ExportableEntitiesService;
import org.thingsboard.server.service.expimp.imp.EntityImportService;

public abstract class AbstractEntityImportService<I extends EntityId, E extends HasId<I>, D extends EntityExportData<E>> implements EntityImportService<I, E, D> {

    @Autowired @Lazy
    private ExportableEntitiesService exportableEntitiesService;


    protected final E findByExternalId(TenantId tenantId, I externalId) {
        return exportableEntitiesService.findEntityByExternalId(tenantId, externalId);
    }

    protected final <ID extends EntityId> ID getInternalId(TenantId tenantId, ID externalId) {
        if (externalId == null) {
            return null;
        }
        HasId<ID> entity = exportableEntitiesService.findEntityByExternalId(tenantId, externalId);
        if (entity == null) {
            throw new IllegalStateException("Cannot find " + externalId.getEntityType() + " by external id " + externalId);
        }
        return entity.getId();
    }

}

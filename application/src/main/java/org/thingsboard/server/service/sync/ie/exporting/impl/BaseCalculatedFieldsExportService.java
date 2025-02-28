/**
 * Copyright © 2016-2025 The Thingsboard Authors
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

import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.sync.ie.CalculatedFieldExportData;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.service.sync.vc.data.EntitiesExportCtx;

import java.util.List;

public abstract class BaseCalculatedFieldsExportService<ID extends EntityId, E extends ExportableEntity<ID> & HasTenantId, D extends CalculatedFieldExportData<E>> extends BaseEntityExportService<ID, E, D> {

    protected final CalculatedFieldService calculatedFieldService;

    protected BaseCalculatedFieldsExportService(CalculatedFieldService calculatedFieldService) {
        this.calculatedFieldService = calculatedFieldService;
    }

    protected void setCalculatedFields(EntitiesExportCtx<?> ctx, E entity, D exportData) {
        if (ctx.getSettings().isExportCalculatedFields()) {
            List<CalculatedField> calculatedFields = calculatedFieldService.findCalculatedFieldsByEntityId(ctx.getTenantId(), entity.getId());
            calculatedFields.forEach(calculatedField -> {
                calculatedField.getConfiguration().getArguments().values().forEach(argument -> {
                    if (argument.getRefEntityId() != null) {
                        EntityId externalId = getExternalIdOrElseInternal(ctx, argument.getRefEntityId());
                        argument.setRefEntityId(externalId);
                    }
                });
            });
            exportData.setCalculatedFields(calculatedFields);
        }
    }

}

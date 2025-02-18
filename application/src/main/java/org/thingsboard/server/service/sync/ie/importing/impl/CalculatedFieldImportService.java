/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.service.sync.ie.importing.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.dao.service.ConstraintValidator;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class CalculatedFieldImportService extends BaseEntityImportService<CalculatedFieldId, CalculatedField, EntityExportData<CalculatedField>> {

    private final CalculatedFieldService calculatedFieldService;

    @Override
    protected void setOwner(TenantId tenantId, CalculatedField calculatedField, IdProvider idProvider) {
        calculatedField.setTenantId(tenantId);
    }

    @Override
    protected CalculatedField prepare(EntitiesImportCtx ctx, CalculatedField calculatedField, CalculatedField oldEntity, EntityExportData<CalculatedField> exportData, IdProvider idProvider) {
        calculatedField.setEntityId(idProvider.getInternalId(calculatedField.getEntityId()));
        return calculatedField;
    }

    @Override
    protected CalculatedField saveOrUpdate(EntitiesImportCtx ctx, CalculatedField calculatedField, EntityExportData<CalculatedField> exportData, IdProvider idProvider) {
        ConstraintValidator.validateFields(calculatedField);
        return calculatedFieldService.save(calculatedField);
    }

    @Override
    protected CalculatedField deepCopy(CalculatedField calculatedField) {
        return new CalculatedField(calculatedField);
    }

    @Override
    protected void onEntitySaved(User user, CalculatedField savedEntity, CalculatedField oldEntity) throws ThingsboardException {
        entityActionService.logEntityAction(user, savedEntity.getId(), savedEntity, null,
                oldEntity == null ? ActionType.ADDED : ActionType.UPDATED, null);
    }

    @Override
    protected void cleanupForComparison(CalculatedField e) {
        super.cleanupForComparison(e);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.CALCULATED_FIELD;
    }

}

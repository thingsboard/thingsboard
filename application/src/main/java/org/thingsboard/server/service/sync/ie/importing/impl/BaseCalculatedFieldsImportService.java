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
package org.thingsboard.server.service.sync.ie.importing.impl;

import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.sync.ie.CalculatedFieldExportData;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class BaseCalculatedFieldsImportService<ID extends EntityId, E extends ExportableEntity<ID> & HasTenantId, D extends CalculatedFieldExportData<E>> extends BaseEntityImportService<ID, E, D> {

    private final CalculatedFieldService calculatedFieldService;

    protected BaseCalculatedFieldsImportService(CalculatedFieldService calculatedFieldService) {
        this.calculatedFieldService = calculatedFieldService;
    }

    protected E saveOrUpdateEntity(EntitiesImportCtx ctx, E entity, D exportData, IdProvider idProvider, Function<E, E> saveFunction) {
        E savedEntity = saveFunction.apply(entity);

        if (ctx.isFinalImportAttempt() || ctx.getCurrentImportResult().isUpdatedAllExternalIds()) {
            saveCalculatedFields(ctx, savedEntity, exportData, idProvider);
        }
        return savedEntity;
    }

    protected void saveCalculatedFields(EntitiesImportCtx ctx, E savedEntity, D exportData, IdProvider idProvider) {
        if (exportData.getCalculatedFields() == null || !ctx.isSaveCalculatedFields()) {
            return;
        }

        exportData.getCalculatedFields().forEach(calculatedField -> {
            calculatedField.setTenantId(savedEntity.getTenantId());
            calculatedField.setExternalId(calculatedField.getId());
            calculatedField.setId(idProvider.getInternalId(calculatedField.getId(), false));
            calculatedField.setEntityId(savedEntity.getId());

            calculatedField.getConfiguration().getArguments().values().forEach(argument -> {
                if (argument.getRefEntityId() != null) {
                    argument.setRefEntityId(idProvider.getInternalId(argument.getRefEntityId(), false));
                }
            });

            calculatedFieldService.save(calculatedField);
        });
    }

    @Override
    protected boolean updateRelatedEntitiesIfUnmodified(EntitiesImportCtx ctx, E prepared, D exportData, IdProvider idProvider) {
        boolean updated = super.updateRelatedEntitiesIfUnmodified(ctx, prepared, exportData, idProvider);
        updated |= updateCalculatedFields(ctx, prepared, exportData, idProvider);
        return updated;
    }

    private boolean updateCalculatedFields(EntitiesImportCtx ctx, E prepared, D exportData, IdProvider idProvider) {
        var calculatedFields = exportData.getCalculatedFields();
        if (calculatedFields == null || !ctx.isSaveCalculatedFields()) {
            return false;
        }
        Map<CalculatedFieldId, CalculatedField> calculatedFieldMap = calculatedFields.stream()
                .peek(newField -> {
                    newField.setTenantId(ctx.getTenantId());
                    newField.setExternalId(newField.getId());
                    newField.setId(idProvider.getInternalId(newField.getId(), false));
                    newField.setEntityId(prepared.getId());
                    newField.getConfiguration().getArguments().values().forEach(argument -> {
                        argument.setRefEntityId(idProvider.getInternalId(argument.getRefEntityId(), false));
                    });
                })
                .collect(Collectors.toMap(CalculatedField::getId, field -> field));

        List<CalculatedField> existingFields = calculatedFieldService.findCalculatedFieldsByEntityId(ctx.getTenantId(), prepared.getId());
        boolean updated = false;

        Map<CalculatedField, Boolean> result = new LinkedHashMap<>();
        for (CalculatedField existingField : existingFields) {
            if (calculatedFieldMap.containsKey(existingField.getId())) {
                CalculatedField newField = calculatedFieldMap.get(existingField.getId());
                if (!newField.equals(existingField)) {
                    result.put(newField, false);
                }
                calculatedFieldMap.remove(existingField.getId());
            } else {
                updated = true;
                calculatedFieldService.deleteCalculatedField(ctx.getTenantId(), existingField.getId());
            }
        }

        for (CalculatedField newField : calculatedFieldMap.values()) {
            result.put(newField, true);
        }

        if (!result.isEmpty()) {
            updated = true;
            ctx.addCalculatedFields(result);
        }
        return updated;
    }

}

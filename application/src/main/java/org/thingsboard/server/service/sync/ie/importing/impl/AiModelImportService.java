// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sync.ie.importing.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ai.AiModel;
import org.thingsboard.server.common.data.id.AiModelId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.dao.ai.AiModelService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

@Service
@TbCoreComponent
@RequiredArgsConstructor
class AiModelImportService extends BaseEntityImportService<AiModelId, AiModel, EntityExportData<AiModel>> {

    private final AiModelService aiModelService;

    @Override
    protected void setOwner(
            TenantId tenantId,
            AiModel model,
            BaseEntityImportService<AiModelId, AiModel, EntityExportData<AiModel>>.IdProvider idProvider
    ) {
        model.setTenantId(tenantId);
    }

    @Override
    protected AiModel prepare(
            EntitiesImportCtx ctx,
            AiModel model,
            AiModel oldModel,
            EntityExportData<AiModel> exportData,
            BaseEntityImportService<AiModelId, AiModel, EntityExportData<AiModel>>.IdProvider idProvider
    ) {
        return model;
    }

    @Override
    protected AiModel deepCopy(AiModel model) {
        return new AiModel(model);
    }

    @Override
    protected AiModel saveOrUpdate(
            EntitiesImportCtx ctx,
            AiModel model,
            EntityExportData<AiModel> exportData,
            BaseEntityImportService<AiModelId, AiModel, EntityExportData<AiModel>>.IdProvider idProvider,
            CompareResult compareResult
    ) {
        return aiModelService.save(model);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.AI_MODEL;
    }

}

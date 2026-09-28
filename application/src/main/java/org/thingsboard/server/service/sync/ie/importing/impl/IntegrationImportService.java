// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.sync.ie.importing.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.integration.IntegrationManagerService;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

import java.util.UUID;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class IntegrationImportService extends BaseEntityImportService<IntegrationId, Integration, EntityExportData<Integration>> {

    private final IntegrationService integrationService;
    private final IntegrationManagerService integrationManagerService;

    @Override
    protected void setOwner(TenantId tenantId, Integration integration, IdProvider idProvider) {
        integration.setTenantId(tenantId);
    }

    @Override
    protected Integration findExistingEntity(EntitiesImportCtx ctx, Integration integration, IdProvider idProvider) {
        Integration existingIntegration = super.findExistingEntity(ctx, integration, idProvider);
        if (existingIntegration == null && ctx.isFindExistingByName()) {
            existingIntegration = integrationService.findTenantIntegrationsByName(ctx.getTenantId(), integration.getName()).stream().findFirst().orElse(null);
        }
        return existingIntegration;
    }

    @Override
    protected Integration prepare(EntitiesImportCtx ctx, Integration integration, Integration oldEntity, EntityExportData<Integration> exportData, IdProvider idProvider) {
        if (ctx.isAutoGenerateIntegrationKey()) {
            if (integration.getId() == null) {
                integration.setRoutingKey(UUID.randomUUID().toString());
            } else {
                integration.setRoutingKey(oldEntity.getRoutingKey());
            }
        }
        integration.setDefaultConverterId(idProvider.getInternalId(integration.getDefaultConverterId()));
        integration.setDownlinkConverterId(idProvider.getInternalId(integration.getDownlinkConverterId()));
        return integration;
    }

    @Override
    protected Integration deepCopy(Integration integration) {
        return new Integration(integration);
    }

    //    @SneakyThrows({InterruptedException.class, ExecutionException.class, TimeoutException.class})
    @Override
    protected Integration saveOrUpdate(EntitiesImportCtx ctx, Integration integration, EntityExportData<Integration> exportData, IdProvider idProvider, CompareResult compareResult) {
        // Too aggressive operation
        // integrationManagerService.validateIntegrationConfiguration(integration).get(20, TimeUnit.SECONDS);
        return integrationService.saveIntegration(integration);
    }

    @Override
    protected void onEntitySaved(User user, Integration savedIntegration, Integration oldIntegration) throws ThingsboardException {
        super.onEntitySaved(user, savedIntegration, oldIntegration);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.INTEGRATION;
    }

}

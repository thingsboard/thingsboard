package org.thingsboard.server.service.sync.ie.importing.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.common.data.sync.ie.EntityImportSettings;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class EntityViewImportService extends BaseEntityImportService<EntityViewId, EntityView, EntityExportData<EntityView>> {

    private final EntityViewService entityViewService;

    @Override
    protected void setOwner(TenantId tenantId, EntityView entityView, IdProvider idProvider) {
        entityView.setTenantId(tenantId);
        entityView.setCustomerId(idProvider.getInternalId(entityView.getCustomerId()));
    }

    @Override
    protected EntityView prepareAndSave(TenantId tenantId, EntityView entityView, EntityExportData<EntityView> exportData, IdProvider idProvider, EntityImportSettings importSettings) {
        entityView.setEntityId(idProvider.getInternalId(entityView.getEntityId()));
        return entityViewService.saveEntityView(entityView);
    }

    @Override
    protected void onEntitySaved(SecurityUser user, EntityView savedEntityView, EntityView oldEntityView) throws ThingsboardException {
        entityNotificationService.notifyCreateOrUpdateEntity(user.getTenantId(), savedEntityView.getId(), savedEntityView,
                null, oldEntityView == null ? ActionType.ADDED : ActionType.UPDATED, user);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ENTITY_VIEW;
    }

}

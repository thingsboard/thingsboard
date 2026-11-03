// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sync.ie.importing.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.dao.notification.NotificationTemplateService;
import org.thingsboard.server.dao.service.ConstraintValidator;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class NotificationTemplateImportService extends BaseEntityImportService<NotificationTemplateId, NotificationTemplate, EntityExportData<NotificationTemplate>> {

    private final NotificationTemplateService notificationTemplateService;

    @Override
    protected void setOwner(TenantId tenantId, NotificationTemplate notificationTemplate, IdProvider idProvider) {
        notificationTemplate.setTenantId(tenantId);
    }

    @Override
    protected NotificationTemplate prepare(EntitiesImportCtx ctx, NotificationTemplate notificationTemplate, NotificationTemplate oldEntity, EntityExportData<NotificationTemplate> exportData, IdProvider idProvider) {
        return notificationTemplate;
    }

    @Override
    protected NotificationTemplate saveOrUpdate(EntitiesImportCtx ctx, NotificationTemplate notificationTemplate, EntityExportData<NotificationTemplate> exportData, IdProvider idProvider, CompareResult compareResult) {
        ConstraintValidator.validateFields(notificationTemplate);
        return notificationTemplateService.saveNotificationTemplate(ctx.getTenantId(), notificationTemplate);
    }

    @Override
    protected void onEntitySaved(User user, NotificationTemplate savedEntity, NotificationTemplate oldEntity) throws ThingsboardException {
        entityActionService.logEntityAction(user, savedEntity.getId(), savedEntity, null,
                oldEntity == null ? ActionType.ADDED : ActionType.UPDATED, null);
    }

    @Override
    protected NotificationTemplate deepCopy(NotificationTemplate notificationTemplate) {
        return new NotificationTemplate(notificationTemplate);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.NOTIFICATION_TEMPLATE;
    }

}

// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sync.ie.exporting.impl;

import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesExportCtx;

import java.util.Set;

@Service
@TbCoreComponent
public class NotificationTemplateExportService extends BaseEntityExportService<NotificationTemplateId, NotificationTemplate, EntityExportData<NotificationTemplate>> {

    @Override
    protected void setRelatedEntities(EntitiesExportCtx<?> ctx, NotificationTemplate notificationTemplate, EntityExportData<NotificationTemplate> exportData) {

    }

    @Override
    public Set<EntityType> getSupportedEntityTypes() {
        return Set.of(EntityType.NOTIFICATION_TEMPLATE);
    }

}

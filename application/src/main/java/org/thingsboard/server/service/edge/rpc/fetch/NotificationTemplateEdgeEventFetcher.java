// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.rpc.fetch;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.notification.NotificationTemplateService;

import java.util.List;

@AllArgsConstructor
@Slf4j
public class NotificationTemplateEdgeEventFetcher extends BasePageableEdgeEventFetcher<NotificationTemplate> {

    private NotificationTemplateService notificationTemplateService;

    @Override
    PageData<NotificationTemplate> fetchEntities(TenantId tenantId, Edge edge, PageLink pageLink) {
        return notificationTemplateService.findNotificationTemplatesByTenantIdAndNotificationTypes(tenantId, List.of(NotificationType.values()), pageLink);
    }

    @Override
    EdgeEvent constructEdgeEvent(TenantId tenantId, Edge edge, NotificationTemplate notificationTemplate) {
        return EdgeUtils.constructEdgeEvent(tenantId, edge.getId(), EdgeEventType.NOTIFICATION_TEMPLATE,
                EdgeEventActionType.ADDED, notificationTemplate.getId(), null);
    }

}

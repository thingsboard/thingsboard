// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.sync.ie.exporting.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesExportCtx;

import java.util.Set;
import java.util.UUID;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class UserExportService extends BaseEntityExportService<UserId, User, EntityExportData.UserExportData> {

    private static final String DEFAULT_DASHBOARD_ID = "defaultDashboardId";
    private static final String HOME_DASHBOARD_ID = "homeDashboardId";

    @Override
    protected void setRelatedEntities(EntitiesExportCtx<?> ctx, User user, EntityExportData.UserExportData exportData) {
        if (user.getCustomerId() != null && !user.getCustomerId().isNullUid()) {
            user.setCustomerId(getExternalIdOrElseInternal(ctx, user.getCustomerId()));
        }
        user.setCustomMenuId(null);
        remapDashboardRef(ctx, user, DEFAULT_DASHBOARD_ID);
        remapDashboardRef(ctx, user, HOME_DASHBOARD_ID);
    }

    private void remapDashboardRef(EntitiesExportCtx<?> ctx, User user, String key) {
        JsonNode additionalInfo = user.getAdditionalInfo();
        if (additionalInfo == null || !additionalInfo.isObject()) {
            return;
        }
        JsonNode node = additionalInfo.get(key);
        if (node == null || !node.isTextual()) {
            return;
        }
        UUID internalUuid;
        try {
            internalUuid = UUID.fromString(node.asText());
        } catch (IllegalArgumentException e) {
            return;
        }
        DashboardId externalId = getExternalIdOrElseInternal(ctx, new DashboardId(internalUuid));
        ((ObjectNode) additionalInfo).put(key, externalId.getId().toString());
    }

    @Override
    public Set<EntityType> getSupportedEntityTypes() {
        return Set.of(EntityType.USER);
    }

}

// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.sync.ie.importing.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

import java.util.Objects;
import java.util.UUID;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class UserImportService extends BaseEntityImportService<UserId, User, EntityExportData.UserExportData> {

    private static final String DEFAULT_DASHBOARD_ID = "defaultDashboardId";
    private static final String HOME_DASHBOARD_ID = "homeDashboardId";

    private final UserService userService;

    @Override
    protected void setOwner(TenantId tenantId, User user, IdProvider idProvider) {
        user.setTenantId(tenantId);
        if (user.getCustomerId() != null && !user.getCustomerId().isNullUid()) {
            user.setCustomerId(idProvider.getInternalId(user.getCustomerId()));
        }
    }

    @Override
    protected User prepare(EntitiesImportCtx ctx, User user, User oldUser, EntityExportData.UserExportData exportData, IdProvider idProvider) {
        if (Authority.SYS_ADMIN.equals(user.getAuthority())) {
            throw new DataValidationException("SYS_ADMIN users cannot be imported");
        }
        if (oldUser != null && !Objects.equals(user.getAuthority(), oldUser.getAuthority())) {
            throw new DataValidationException("Authority change is not allowed (existing: " + oldUser.getAuthority()
                    + ", incoming: " + user.getAuthority() + ")");
        }
        // Defense-in-depth: UserExportService also strips customMenuId on the export side.
        user.setCustomMenuId(null);
        resolveDashboardRef(ctx, user, DEFAULT_DASHBOARD_ID, idProvider);
        resolveDashboardRef(ctx, user, HOME_DASHBOARD_ID, idProvider);
        return user;
    }

    private void resolveDashboardRef(EntitiesImportCtx ctx, User user, String key, IdProvider idProvider) {
        JsonNode additionalInfo = user.getAdditionalInfo();
        if (additionalInfo == null || !additionalInfo.isObject()) {
            return;
        }
        JsonNode node = additionalInfo.get(key);
        if (node == null || !node.isTextual()) {
            return;
        }
        UUID externalUuid;
        try {
            externalUuid = UUID.fromString(node.asText());
        } catch (IllegalArgumentException e) {
            return;
        }
        DashboardId internalId = idProvider.getInternalId(new DashboardId(externalUuid), ctx.isFinalImportAttempt());
        if (internalId == null) {
            return;
        }
        ((ObjectNode) additionalInfo).put(key, internalId.getId().toString());
    }

    @Override
    protected User deepCopy(User user) {
        return new User(user);
    }

    @Override
    protected User saveOrUpdate(EntitiesImportCtx ctx, User user, EntityExportData.UserExportData exportData, IdProvider idProvider, CompareResult compareResult) {
        return userService.saveUser(ctx.getTenantId(), user);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.USER;
    }

}

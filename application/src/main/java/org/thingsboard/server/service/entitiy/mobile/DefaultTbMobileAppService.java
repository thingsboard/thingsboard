// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.entitiy.mobile;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.MobileAppId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.app.MobileApp;
import org.thingsboard.server.dao.mobile.MobileAppService;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

@Service
@AllArgsConstructor
public class DefaultTbMobileAppService extends AbstractTbEntityService implements TbMobileAppService {

    private final MobileAppService mobileAppService;

    @Override
    public MobileApp save(MobileApp mobileApp, User user) throws Exception {
        ActionType actionType = mobileApp.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = mobileApp.getTenantId();
        try {
            MobileApp savedMobileApp = checkNotNull(mobileAppService.saveMobileApp(tenantId, mobileApp));
            logEntityActionService.logEntityAction(tenantId, savedMobileApp.getId(), savedMobileApp, actionType, user);
            return savedMobileApp;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.MOBILE_APP), mobileApp, actionType, user, e);
            throw e;
        }
    }


    @Override
    public void delete(MobileApp mobileApp, User user) {
        ActionType actionType = ActionType.DELETED;
        TenantId tenantId = mobileApp.getTenantId();
        MobileAppId mobileAppId = mobileApp.getId();
        try {
            mobileAppService.deleteMobileAppById(tenantId, mobileAppId);
            logEntityActionService.logEntityAction(tenantId, mobileAppId, mobileApp, actionType, user);
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, mobileAppId, mobileApp, actionType, user, e);
            throw e;
        }
    }
}

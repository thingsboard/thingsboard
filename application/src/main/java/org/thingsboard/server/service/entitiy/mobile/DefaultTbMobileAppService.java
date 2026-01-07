/**
 * Copyright © 2016-2026 The Thingsboard Authors
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

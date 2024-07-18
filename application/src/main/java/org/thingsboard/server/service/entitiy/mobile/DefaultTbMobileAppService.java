/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.MobileAppId;
import org.thingsboard.server.common.data.id.OAuth2RegistrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.MobileApp;
import org.thingsboard.server.dao.mobile.MobileAppService;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

import java.util.List;

@Service
@AllArgsConstructor
public class DefaultTbMobileAppService extends AbstractTbEntityService implements TbMobileAppService {

    private final MobileAppService mobileAppService;

    @Override
    public MobileApp save(MobileApp mobileApp, List<OAuth2RegistrationId> oauth2Clients, User user) throws Exception {
        ActionType actionType = mobileApp.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = mobileApp.getTenantId();
        try {
            MobileApp savedMobileApp = checkNotNull(mobileAppService.saveMobileApp(tenantId, mobileApp));
            logEntityActionService.logEntityAction(tenantId, savedMobileApp.getId(), mobileApp, actionType, user);
            if (!CollectionUtils.isEmpty(oauth2Clients)) {
                mobileAppService.updateOauth2Clients(tenantId, savedMobileApp.getId(), oauth2Clients);
                logEntityActionService.logEntityAction(tenantId, savedMobileApp.getId(), savedMobileApp,
                        ActionType.UPDATED_OAUTH2_CLIENTS, user, oauth2Clients.toString());
            }
            return savedMobileApp;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.MOBILE_APP), mobileApp, actionType, user, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public void delete(MobileApp mobileApp, User user) {
        ActionType actionType = ActionType.DELETED;
        TenantId tenantId = mobileApp.getTenantId();
        MobileAppId mobileAppId = mobileApp.getId();
        try {
            mobileAppService.deleteMobileAppById(tenantId, mobileAppId);
            logEntityActionService.logEntityAction(tenantId, mobileAppId, mobileApp, actionType, user, mobileApp.getPkgName());
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.MOBILE_APP), actionType, user, e,
                    mobileAppId.toString());
            throw e;
        }
    }
}

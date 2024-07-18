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
package org.thingsboard.server.service.entitiy.oauth2client;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.OAuth2RegistrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.OAuth2Registration;
import org.thingsboard.server.dao.oauth2.OAuth2ClientService;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

@Service
@AllArgsConstructor
public class DefaultTbOauth2ClientService extends AbstractTbEntityService implements TbOauth2ClientService {

    private final OAuth2ClientService oAuth2ClientService;

    @Override
    public OAuth2Registration save(OAuth2Registration oAuth2Registration, User user) throws Exception {
        ActionType actionType = oAuth2Registration.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = oAuth2Registration.getTenantId();
        try {
            OAuth2Registration savedRegistration = checkNotNull(oAuth2ClientService.saveOAuth2Client(tenantId, oAuth2Registration));
            logEntityActionService.logEntityAction(tenantId, savedRegistration.getId(), oAuth2Registration, actionType, user);
            return savedRegistration;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.OAUTH2_CLIENT), oAuth2Registration, actionType, user, e);
            throw e;
        }
    }

    @Override
    public void delete(OAuth2Registration oAuth2Registration, User user) {
        ActionType actionType = ActionType.DELETED;
        TenantId tenantId = oAuth2Registration.getTenantId();
        OAuth2RegistrationId oAuth2RegistrationId = oAuth2Registration.getId();
        try {
            oAuth2ClientService.deleteById(tenantId, oAuth2RegistrationId);
            logEntityActionService.logEntityAction(tenantId, oAuth2RegistrationId, oAuth2Registration, actionType, user, oAuth2Registration.getName());
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.OAUTH2_CLIENT), actionType, user, e,
                    oAuth2RegistrationId.toString());
            throw e;
        }
    }
}

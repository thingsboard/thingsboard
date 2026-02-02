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
package org.thingsboard.server.service.entitiy.oauth2client;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.OAuth2ClientId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.OAuth2Client;
import org.thingsboard.server.dao.oauth2.OAuth2ClientService;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

@Service
@AllArgsConstructor
public class DefaultTbOauth2ClientService extends AbstractTbEntityService implements TbOauth2ClientService {

    private final OAuth2ClientService oAuth2ClientService;

    @Override
    public OAuth2Client save(OAuth2Client oAuth2Client, User user) throws Exception {
        ActionType actionType = oAuth2Client.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = oAuth2Client.getTenantId();
        try {
            OAuth2Client savedClient = checkNotNull(oAuth2ClientService.saveOAuth2Client(tenantId, oAuth2Client));
            logEntityActionService.logEntityAction(tenantId, savedClient.getId(), savedClient, actionType, user);
            return savedClient;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.OAUTH2_CLIENT), oAuth2Client, actionType, user, e);
            throw e;
        }
    }

    @Override
    public void delete(OAuth2Client oAuth2Client, User user) {
        ActionType actionType = ActionType.DELETED;
        TenantId tenantId = oAuth2Client.getTenantId();
        OAuth2ClientId oAuth2ClientId = oAuth2Client.getId();
        try {
            oAuth2ClientService.deleteOAuth2ClientById(tenantId, oAuth2ClientId);
            logEntityActionService.logEntityAction(tenantId, oAuth2ClientId, oAuth2Client, actionType, user);
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, oAuth2ClientId, oAuth2Client, actionType, user, e);
            throw e;
        }
    }
}

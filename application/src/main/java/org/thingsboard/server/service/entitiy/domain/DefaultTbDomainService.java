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
package org.thingsboard.server.service.entitiy.domain;

import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.domain.Domain;
import org.thingsboard.server.common.data.id.DomainId;
import org.thingsboard.server.common.data.id.OAuth2ClientId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.domain.DomainService;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

import java.util.List;

@Service
@AllArgsConstructor
public class DefaultTbDomainService extends AbstractTbEntityService implements TbDomainService {

    private final DomainService domainService;

    @Override
    public Domain save(Domain domain, List<OAuth2ClientId> oAuth2Clients, User user) throws Exception {
        ActionType actionType = domain.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = domain.getTenantId();
        try {
            Domain savedDomain = checkNotNull(domainService.saveDomain(tenantId, domain));
            if (CollectionUtils.isNotEmpty(oAuth2Clients)) {
                domainService.updateOauth2Clients(domain.getTenantId(), savedDomain.getId(), oAuth2Clients);
            }
            logEntityActionService.logEntityAction(tenantId, savedDomain.getId(), savedDomain, actionType, user, oAuth2Clients);
            return savedDomain;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.DOMAIN), domain, actionType, user, e, oAuth2Clients);
            throw e;
        }
    }

    @Override
    public void updateOauth2Clients(Domain domain, List<OAuth2ClientId> oAuth2ClientIds, User user) {
        ActionType actionType = ActionType.UPDATED;
        TenantId tenantId = domain.getTenantId();
        DomainId domainId = domain.getId();
        try {
            domainService.updateOauth2Clients(tenantId, domainId, oAuth2ClientIds);
            logEntityActionService.logEntityAction(tenantId, domainId, domain, actionType, user, oAuth2ClientIds);
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, domainId, domain, actionType, user, e, oAuth2ClientIds);
            throw e;
        }
    }

    @Override
    public void delete(Domain domain, User user) {
        ActionType actionType = ActionType.DELETED;
        TenantId tenantId = domain.getTenantId();
        DomainId domainId = domain.getId();
        try {
            domainService.deleteDomainById(tenantId, domainId);
            logEntityActionService.logEntityAction(tenantId, domainId, domain, actionType, user);
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, domainId, domain, actionType, user, e);
            throw e;
        }
    }

}

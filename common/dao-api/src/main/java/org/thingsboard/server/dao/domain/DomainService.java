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
package org.thingsboard.server.dao.domain;

import org.thingsboard.server.common.data.domain.Domain;
import org.thingsboard.server.common.data.domain.DomainInfo;
import org.thingsboard.server.common.data.id.DomainId;
import org.thingsboard.server.common.data.id.OAuth2ClientId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;

public interface DomainService extends EntityDaoService {

    Domain saveDomain(TenantId tenantId, Domain domain);

    void deleteDomainById(TenantId tenantId, DomainId domainId);

    Domain findDomainById(TenantId tenantId, DomainId domainId);

    PageData<DomainInfo> findDomainInfosByTenantId(TenantId tenantId, PageLink pageLink);

    DomainInfo findDomainInfoById(TenantId tenantId, DomainId domainId);

    boolean isOauth2Enabled(TenantId tenantId);

    void updateOauth2Clients(TenantId tenantId, DomainId domainId, List<OAuth2ClientId> oAuth2ClientIds);

    void deleteDomainsByTenantId(TenantId tenantId);
}

// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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

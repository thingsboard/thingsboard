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
package org.thingsboard.server.dao.service;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.domain.Domain;
import org.thingsboard.server.common.data.domain.DomainInfo;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.OAuth2Client;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientLoginInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.domain.DomainService;
import org.thingsboard.server.dao.oauth2.OAuth2ClientService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.dao.oauth2.OAuth2Utils.OAUTH2_AUTHORIZATION_PATH_TEMPLATE;

@DaoSqlTest
public class DomainServiceTest extends AbstractServiceTest {

    @Autowired
    protected DomainService domainService;

    @Autowired
    protected OAuth2ClientService oAuth2ClientService;

    @After
    public void after() {
        domainService.deleteByTenantId(TenantId.SYS_TENANT_ID);
        oAuth2ClientService.deleteByTenantId(TenantId.SYS_TENANT_ID);
    }

    @Test
    public void testSaveDomain() {
        Domain domain = constructDomain(TenantId.SYS_TENANT_ID, "test.domain.com", true, true);
        Domain savedDomain = domainService.saveDomain(SYSTEM_TENANT_ID, domain);

        Domain retrievedDomain = domainService.findDomainById(savedDomain.getTenantId(), savedDomain.getId());
        assertThat(retrievedDomain).isEqualTo(savedDomain);

        // update domain name
        savedDomain.setName("test.domain2.com");
        Domain updatedDomain = domainService.saveDomain(SYSTEM_TENANT_ID, savedDomain);

        Domain retrievedDomain2 = domainService.findDomainById(savedDomain.getTenantId(), savedDomain.getId());
        assertThat(retrievedDomain2).isEqualTo(updatedDomain);

        // check domain info
        DomainInfo retrievedInfo = domainService.findDomainInfoById(SYSTEM_TENANT_ID, savedDomain.getId());
        assertThat(retrievedInfo).isEqualTo(new DomainInfo(updatedDomain, Collections.emptyList()));

        boolean oauth2Enabled = domainService.isOauth2Enabled(SYSTEM_TENANT_ID);
        assertThat(oauth2Enabled).isTrue();

        // update domain oauth2 enabled to false
        updatedDomain.setOauth2Enabled(false);
        domainService.saveDomain(SYSTEM_TENANT_ID, updatedDomain);

        boolean oauth2Enabled2 = domainService.isOauth2Enabled(SYSTEM_TENANT_ID);
        assertThat(oauth2Enabled2).isFalse();

        //delete domain
        domainService.deleteDomainById(SYSTEM_TENANT_ID, savedDomain.getId());
        assertThat(domainService.findDomainById(SYSTEM_TENANT_ID, savedDomain.getId())).isNull();
    }

    @Test
    public void testGetTenantDomains() {
        List<Domain> domains = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Domain oAuth2Client = constructDomain(TenantId.SYS_TENANT_ID, StringUtils.randomAlphabetic(5).toLowerCase(), true, false);
            Domain savedOauth2Client = domainService.saveDomain(SYSTEM_TENANT_ID, oAuth2Client);
            domains.add(savedOauth2Client);
        }
        PageData<DomainInfo> retrieved = domainService.findDomainInfosByTenantId(TenantId.SYS_TENANT_ID, new PageLink(10, 0));
        List<DomainInfo> domainInfos = domains.stream().map(domain -> new DomainInfo(domain, Collections.emptyList())).toList();
        assertThat(retrieved.getData()).containsOnlyOnceElementsOf(domainInfos);
    }

    @Test
    public void testGetDomainInfo() {
        OAuth2Client oAuth2Client = validClientInfo(TenantId.SYS_TENANT_ID, "Test google client");
        OAuth2Client savedOauth2Client = oAuth2ClientService.saveOAuth2Client(SYSTEM_TENANT_ID, oAuth2Client);
        List<OAuth2ClientInfo> oAuth2ClientInfosByIds = oAuth2ClientService.findOAuth2ClientInfosByIds(TenantId.SYS_TENANT_ID, List.of(savedOauth2Client.getId()));

        Domain domain = constructDomain(TenantId.SYS_TENANT_ID, "test.domain.com", true, true);
        Domain savedDomain = domainService.saveDomain(SYSTEM_TENANT_ID, domain);

        domainService.updateOauth2Clients(TenantId.SYS_TENANT_ID, savedDomain.getId(), List.of(savedOauth2Client.getId()));

        // check domain info
        DomainInfo retrievedInfo = domainService.findDomainInfoById(SYSTEM_TENANT_ID, savedDomain.getId());
        assertThat(retrievedInfo).isEqualTo(new DomainInfo(savedDomain, oAuth2ClientInfosByIds));

        //find clients by domain name
        List<OAuth2ClientLoginInfo> oauth2LoginInfo = oAuth2ClientService.findOAuth2ClientLoginInfosByDomainName(savedDomain.getName());
        assertThat(oauth2LoginInfo).containsOnly(new OAuth2ClientLoginInfo(savedOauth2Client.getLoginButtonLabel(), savedOauth2Client.getLoginButtonIcon(), String.format(OAUTH2_AUTHORIZATION_PATH_TEMPLATE, savedOauth2Client.getUuidId().toString())));
    }

    private Domain constructDomain(TenantId tenantId, String domainName, boolean oauth2Enabled, boolean propagateToEdge) {
        Domain domain = new Domain();
        domain.setTenantId(tenantId);
        domain.setName(domainName);
        domain.setOauth2Enabled(oauth2Enabled);
        domain.setPropagateToEdge(propagateToEdge);
        return domain;
    }

}

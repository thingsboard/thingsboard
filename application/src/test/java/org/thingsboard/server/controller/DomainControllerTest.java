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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.domain.Domain;
import org.thingsboard.server.common.data.domain.DomainInfo;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.OAuth2Client;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@DaoSqlTest
public class DomainControllerTest extends AbstractControllerTest {

    static final TypeReference<PageData<DomainInfo>> PAGE_DATA_DOMAIN_TYPE_REF = new TypeReference<>() {
    };
    static final TypeReference<PageData<OAuth2ClientInfo>> PAGE_DATA_OAUTH2_CLIENT_TYPE_REF = new TypeReference<>() {
    };

    @Before
    public void setUp() throws Exception {
        loginSysAdmin();
    }

    @After
    public void tearDown() throws Exception {
        PageData<DomainInfo> pageData = doGetTypedWithPageLink("/api/domain/infos?", PAGE_DATA_DOMAIN_TYPE_REF, new PageLink(10, 0));
        for (Domain domain : pageData.getData()) {
            doDelete("/api/domain/" + domain.getId().getId())
                    .andExpect(status().isOk());
        }

        PageData<OAuth2ClientInfo> clients = doGetTypedWithPageLink("/api/oauth2/client/infos?", PAGE_DATA_OAUTH2_CLIENT_TYPE_REF, new PageLink(10, 0));
        for (OAuth2ClientInfo oAuth2ClientInfo : clients.getData()) {
            doDelete("/api/oauth2/client/" + oAuth2ClientInfo.getId().getId().toString())
                    .andExpect(status().isOk());
        }
    }

    @Test
    public void testSaveDomain() throws Exception {
        PageData<DomainInfo> pageData = doGetTypedWithPageLink("/api/domain/infos?", PAGE_DATA_DOMAIN_TYPE_REF, new PageLink(10, 0));
        assertThat(pageData.getData()).isEmpty();

        Domain domain = constructDomain(TenantId.SYS_TENANT_ID, "my.test.domain", true, true);
        Domain savedDomain = doPost("/api/domain", domain, Domain.class);

        PageData<DomainInfo> pageData2 = doGetTypedWithPageLink("/api/domain/infos?", PAGE_DATA_DOMAIN_TYPE_REF, new PageLink(10, 0));
        assertThat(pageData2.getData()).hasSize(1);
        assertThat(pageData2.getData().get(0)).isEqualTo(new DomainInfo(savedDomain, Collections.emptyList()));

        DomainInfo retrievedDomainInfo = doGet("/api/domain/info/{id}", DomainInfo.class, savedDomain.getId().getId());
        assertThat(retrievedDomainInfo).isEqualTo(new DomainInfo(savedDomain, Collections.emptyList()));

        doDelete("/api/domain/" + savedDomain.getId().getId());
        doGet("/api/domain/info/{id}", savedDomain.getId().getId())
                .andExpect(status().isNotFound());
    }

    @Test
    public void testSaveDomainWithoutName() throws Exception {
        Domain domain = constructDomain(TenantId.SYS_TENANT_ID, null, true, true);
        doPost("/api/domain", domain)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("name must not be blank")));
    }

    @Test
    public void testUpdateDomainOauth2Clients() throws Exception {
        Domain domain = constructDomain(TenantId.SYS_TENANT_ID, "my.test.domain", true, true);
        Domain savedDomain = doPost("/api/domain", domain, Domain.class);

        OAuth2Client oAuth2Client = createOauth2Client(TenantId.SYS_TENANT_ID, "test google client");
        OAuth2Client savedOAuth2Client = doPost("/api/oauth2/client", oAuth2Client, OAuth2Client.class);

        OAuth2Client oAuth2Client2 = createOauth2Client(TenantId.SYS_TENANT_ID, "test facebook client");
        OAuth2Client savedOAuth2Client2 = doPost("/api/oauth2/client", oAuth2Client2, OAuth2Client.class);

        doPut("/api/domain/" + savedDomain.getId() + "/oauth2Clients", List.of(savedOAuth2Client.getId().getId(), savedOAuth2Client2.getId().getId()));

        DomainInfo retrievedDomainInfo = doGet("/api/domain/info/{id}", DomainInfo.class, savedDomain.getId().getId());
        assertThat(retrievedDomainInfo).isEqualTo(new DomainInfo(savedDomain, List.of(new OAuth2ClientInfo(savedOAuth2Client2),
                new OAuth2ClientInfo(savedOAuth2Client))));

        doPut("/api/domain/" + savedDomain.getId() + "/oauth2Clients", List.of(savedOAuth2Client2.getId().getId()));
        DomainInfo retrievedDomainInfo2 = doGet("/api/domain/info/{id}", DomainInfo.class, savedDomain.getId().getId());
        assertThat(retrievedDomainInfo2).isEqualTo(new DomainInfo(savedDomain, List.of(new OAuth2ClientInfo(savedOAuth2Client2))));
    }

    @Test
    public void testCreateDomainWithOauth2Clients() throws Exception {
        OAuth2Client oAuth2Client = createOauth2Client(TenantId.SYS_TENANT_ID, "test google client");
        OAuth2Client savedOAuth2Client = doPost("/api/oauth2/client", oAuth2Client, OAuth2Client.class);

        Domain domain = constructDomain(TenantId.SYS_TENANT_ID, "my.test.domain", true, true);
        Domain savedDomain = doPost("/api/domain?oauth2ClientIds=" + savedOAuth2Client.getId().getId(), domain, Domain.class);

        DomainInfo retrievedDomainInfo = doGet("/api/domain/info/{id}", DomainInfo.class, savedDomain.getId().getId());
        assertThat(retrievedDomainInfo).isEqualTo(new DomainInfo(savedDomain, List.of(new OAuth2ClientInfo(savedOAuth2Client))));
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

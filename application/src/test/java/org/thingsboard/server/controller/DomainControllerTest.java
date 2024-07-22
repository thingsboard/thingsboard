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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.domain.Domain;
import org.thingsboard.server.common.data.domain.DomainInfo;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.MapperType;
import org.thingsboard.server.common.data.oauth2.OAuth2Client;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2CustomMapperConfig;
import org.thingsboard.server.common.data.oauth2.OAuth2MapperConfig;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@DaoSqlTest
public class DomainControllerTest extends AbstractControllerTest {

    @Before
    public void setUp() throws Exception {
        loginSysAdmin();
    }

    @After
    public void tearDown() throws Exception {
        List<DomainInfo> domains = doGetTyped("/api/domain/infos", new TypeReference<List<DomainInfo>>() {
        });
        for (Domain domain : domains) {
            doDelete("/api/domain/" + domain.getId().getId())
                    .andExpect(status().isOk());
        }

        List<OAuth2ClientInfo> oAuth2ClientInfos = doGetTyped("/api/oauth2/client/infos", new TypeReference<List<OAuth2ClientInfo>>() {
        });
        for (OAuth2ClientInfo oAuth2ClientInfo : oAuth2ClientInfos) {
            doDelete("/api/oauth2/client/" + oAuth2ClientInfo.getId().getId().toString())
                    .andExpect(status().isOk());
        }
    }

    @Test
    public void testSaveDomain() throws Exception {
        List<DomainInfo> domainInfos = doGetTyped("/api/domain/infos", new TypeReference<List<DomainInfo>>() {
        });
        assertThat(domainInfos).isEmpty();

        Domain domain = constructDomain(TenantId.SYS_TENANT_ID, "my.test.domain", true, true);
        Domain savedDomain = doPost("/api/domain", domain, Domain.class);

        List<DomainInfo> domainInfos2 = doGetTyped("/api/domain/infos", new TypeReference<List<DomainInfo>>() {
        });
        assertThat(domainInfos2).hasSize(1);
        assertThat(domainInfos2.get(0)).isEqualTo(new DomainInfo(savedDomain, Collections.emptyList()));

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

        OAuth2Client oAuth2Client = validClientInfo(TenantId.SYS_TENANT_ID, "test google client");
        OAuth2Client savedOAuth2Client = doPost("/api/oauth2/client", oAuth2Client, OAuth2Client.class);

        OAuth2Client oAuth2Client2 = validClientInfo(TenantId.SYS_TENANT_ID, "test facebook client");
        OAuth2Client savedOAuth2Client2 = doPost("/api/oauth2/client", oAuth2Client2, OAuth2Client.class);

        doPut("/api/domain/" + savedDomain.getId() + "/oauth2Clients", List.of(savedOAuth2Client.getId().getId(), savedOAuth2Client2.getId().getId()));

        DomainInfo retrievedDomainInfo = doGet("/api/domain/info/{id}", DomainInfo.class, savedDomain.getId().getId());
        assertThat(retrievedDomainInfo).isEqualTo(new DomainInfo(savedDomain, List.of(new OAuth2ClientInfo(savedOAuth2Client),
                new OAuth2ClientInfo(savedOAuth2Client2))));

        doPut("/api/domain/" + savedDomain.getId() + "/oauth2Clients", List.of(savedOAuth2Client2.getId().getId()));
        DomainInfo retrievedDomainInfo2 = doGet("/api/domain/info/{id}", DomainInfo.class, savedDomain.getId().getId());
        assertThat(retrievedDomainInfo2).isEqualTo(new DomainInfo(savedDomain, List.of(new OAuth2ClientInfo(savedOAuth2Client2))));
    }

    @Test
    public void testCreateDomainWithOauth2Clients() throws Exception {
        OAuth2Client oAuth2Client = validClientInfo(TenantId.SYS_TENANT_ID, "test google client");
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

/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.*;
import org.thingsboard.server.dao.oauth2.OAuth2Service;
import org.thingsboard.server.dao.oauth2.OAuth2Utils;

import java.util.*;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.oauth2.OAuth2Utils.toClientRegistrations;

public class BaseOAuth2ServiceTest extends AbstractServiceTest {

    @Autowired
    protected OAuth2Service oAuth2Service;

    @Before
    public void beforeRun() {
        Assert.assertTrue(oAuth2Service.findAllClientRegistrations().isEmpty());
    }

    @After
    public void after() {
        oAuth2Service.findAllClientRegistrations().forEach(clientRegistration -> {
            oAuth2Service.deleteClientRegistrationById(clientRegistration.getId());
        });
        Assert.assertTrue(oAuth2Service.findAllClientRegistrations().isEmpty());
    }

    @Test
    public void testCreateNewParams() {
        OAuth2ClientRegistration clientRegistration = validClientRegistration("domain-name");
        List<OAuth2ClientsDomainParams> savedDomainsParams = oAuth2Service.saveDomainsParams(OAuth2Utils.toDomainsParams(Collections.singletonList(clientRegistration)));
        Assert.assertNotNull(savedDomainsParams);

        List<OAuth2ClientRegistration> savedClientRegistrations = OAuth2Utils.toClientRegistrations(savedDomainsParams);
        Assert.assertEquals(1, savedClientRegistrations.size());

        OAuth2ClientRegistration savedClientRegistration = savedClientRegistrations.get(0);
        Assert.assertNotNull(savedClientRegistration.getId());
        clientRegistration.setId(savedClientRegistration.getId());
        clientRegistration.setCreatedTime(savedClientRegistration.getCreatedTime());
        Assert.assertEquals(clientRegistration, savedClientRegistration);

        oAuth2Service.deleteClientRegistrationsByDomain("domain-name");
    }

    @Test
    public void testFindDomainParams() {
        OAuth2ClientRegistration clientRegistration = validClientRegistration();
        oAuth2Service.saveDomainsParams(OAuth2Utils.toDomainsParams(Collections.singletonList(clientRegistration)));

        List<OAuth2ClientsDomainParams> foundDomainsParams = oAuth2Service.findDomainsParams();
        Assert.assertEquals(1, foundDomainsParams.size());
        Assert.assertEquals(1, oAuth2Service.findAllClientRegistrations().size());

        List<OAuth2ClientRegistration> foundClientRegistrations = OAuth2Utils.toClientRegistrations(foundDomainsParams);
        OAuth2ClientRegistration foundClientRegistration = foundClientRegistrations.get(0);
        Assert.assertNotNull(foundClientRegistration);
        clientRegistration.setId(foundClientRegistration.getId());
        clientRegistration.setCreatedTime(foundClientRegistration.getCreatedTime());
        Assert.assertEquals(clientRegistration, foundClientRegistration);
    }

    @Test
    public void testGetOAuth2Clients() {
        String testDomainName = "test_domain";
        OAuth2ClientRegistration first = validClientRegistration(testDomainName);
        OAuth2ClientRegistration second = validClientRegistration(testDomainName);

        oAuth2Service.saveDomainsParams(OAuth2Utils.toDomainsParams(Collections.singletonList(first)));
        oAuth2Service.saveDomainsParams(OAuth2Utils.toDomainsParams(Collections.singletonList(second)));

        List<OAuth2ClientInfo> oAuth2Clients = oAuth2Service.getOAuth2Clients(testDomainName);

        Set<String> actualLabels = new HashSet<>(Arrays.asList(first.getLoginButtonLabel(),
                second.getLoginButtonLabel()));

        Set<String> foundLabels = oAuth2Clients.stream().map(OAuth2ClientInfo::getName).collect(Collectors.toSet());
        Assert.assertEquals(actualLabels, foundLabels);
    }

    @Test
    public void testGetEmptyOAuth2Clients() {
        String testDomainName = "test_domain";
        OAuth2ClientRegistration tenantClientRegistration = validClientRegistration(testDomainName);
        OAuth2ClientRegistration sysAdminClientRegistration = validClientRegistration(testDomainName);
        oAuth2Service.saveDomainsParams(OAuth2Utils.toDomainsParams(Collections.singletonList(tenantClientRegistration)));
        oAuth2Service.saveDomainsParams(OAuth2Utils.toDomainsParams(Collections.singletonList(sysAdminClientRegistration)));
        List<OAuth2ClientInfo> oAuth2Clients = oAuth2Service.getOAuth2Clients("random-domain");
        Assert.assertTrue(oAuth2Clients.isEmpty());
    }

    @Test
    public void testDeleteOAuth2ClientRegistration() {
        OAuth2ClientRegistration first = validClientRegistration();
        OAuth2ClientRegistration second = validClientRegistration();

        List<OAuth2ClientsDomainParams> savedFirstDomainsParams = oAuth2Service.saveDomainsParams(
                OAuth2Utils.toDomainsParams(Collections.singletonList(first)));
        List<OAuth2ClientsDomainParams> savedSecondDomainsParams = oAuth2Service.saveDomainsParams(
                OAuth2Utils.toDomainsParams(Collections.singletonList(second)));

        OAuth2ClientRegistration savedFirstRegistration = toClientRegistrations(savedFirstDomainsParams).get(0);
        OAuth2ClientRegistration savedSecondRegistration = toClientRegistrations(savedSecondDomainsParams).get(0);

        oAuth2Service.deleteClientRegistrationById(savedFirstRegistration.getId());
        List<OAuth2ClientRegistration> foundRegistrations = oAuth2Service.findAllClientRegistrations();
        Assert.assertEquals(1, foundRegistrations.size());
        Assert.assertEquals(savedSecondRegistration, foundRegistrations.get(0));
    }

    @Test
    public void testDeleteDomainOAuth2ClientRegistrations() {
        oAuth2Service.saveDomainsParams(OAuth2Utils.toDomainsParams(Arrays.asList(
                validClientRegistration("domain1"),
                validClientRegistration("domain1"),
                validClientRegistration("domain2")
        )));
        oAuth2Service.saveDomainsParams(OAuth2Utils.toDomainsParams(Arrays.asList(
                validClientRegistration("domain2")
        )));
        Assert.assertEquals(4, oAuth2Service.findAllClientRegistrations().size());
        List<OAuth2ClientsDomainParams> domainsParams = oAuth2Service.findDomainsParams();
        List<OAuth2ClientRegistration> clientRegistrations = toClientRegistrations(domainsParams);
        Assert.assertEquals(2, domainsParams.size());
        Assert.assertEquals(4, clientRegistrations.size());

        oAuth2Service.deleteClientRegistrationsByDomain("domain1");
        Assert.assertEquals(2, oAuth2Service.findAllClientRegistrations().size());
        Assert.assertEquals(1, oAuth2Service.findDomainsParams().size());
        Assert.assertEquals(2, toClientRegistrations(oAuth2Service.findDomainsParams()).size());
    }

    private OAuth2ClientRegistration validClientRegistration() {
        return validClientRegistration("domainName");
    }

    private OAuth2ClientRegistration validClientRegistration(String domainName) {
        OAuth2ClientRegistration clientRegistration = new OAuth2ClientRegistration();
        clientRegistration.setDomainName(domainName);
        clientRegistration.setMapperConfig(
                OAuth2MapperConfig.builder()
                        .allowUserCreation(true)
                        .activateUser(true)
                        .type(MapperType.CUSTOM)
                        .custom(
                                OAuth2CustomMapperConfig.builder()
                                        .url("localhost:8082")
                                        .build()
                        )
                        .build()
        );
        clientRegistration.setClientId("clientId");
        clientRegistration.setClientSecret("clientSecret");
        clientRegistration.setAuthorizationUri("authorizationUri");
        clientRegistration.setAccessTokenUri("tokenUri");
        clientRegistration.setRedirectUriTemplate("redirectUriTemplate");
        clientRegistration.setScope(Arrays.asList("scope1", "scope2"));
        clientRegistration.setUserInfoUri("userInfoUri");
        clientRegistration.setUserNameAttributeName("userNameAttributeName");
        clientRegistration.setJwkSetUri("jwkSetUri");
        clientRegistration.setClientAuthenticationMethod("clientAuthenticationMethod");
        clientRegistration.setLoginButtonLabel("loginButtonLabel");
        clientRegistration.setLoginButtonIcon("loginButtonIcon");
        clientRegistration.setAdditionalInfo(mapper.createObjectNode().put(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
        return clientRegistration;
    }
}

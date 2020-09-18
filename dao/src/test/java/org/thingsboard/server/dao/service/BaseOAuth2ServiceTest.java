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
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.*;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.oauth2.OAuth2Service;
import org.thingsboard.server.dao.oauth2.OAuth2Utils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.oauth2.OAuth2Utils.ALLOW_OAUTH2_CONFIGURATION;
import static org.thingsboard.server.dao.oauth2.OAuth2Utils.toClientRegistrations;

public class BaseOAuth2ServiceTest extends AbstractServiceTest {

    @Autowired
    protected OAuth2Service oAuth2Service;

    @Autowired
    protected AttributesService attributesService;

    private TenantId tenantId;

    @Before
    public void beforeRun() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = tenantService.saveTenant(tenant);
        Assert.assertNotNull(savedTenant);
        tenantId = savedTenant.getId();

        Assert.assertTrue(oAuth2Service.findAllClientRegistrations().isEmpty());
    }

    @After
    public void after() throws Exception {
        tenantService.deleteTenant(tenantId);
        oAuth2Service.deleteClientRegistrationsByTenantId(TenantId.SYS_TENANT_ID);

        Assert.assertTrue(oAuth2Service.findAllClientRegistrations().isEmpty());
    }

    @Test
    public void testIsOAuth2Allowed_null() throws IOException {
        updateTenantAllowOAuth2Setting(null);
        Assert.assertTrue(oAuth2Service.isOAuth2ClientRegistrationAllowed(tenantId));
    }

    @Test
    public void testIsOAuth2Allowed_false() throws IOException {
        updateTenantAllowOAuth2Setting(false);
        Assert.assertFalse(oAuth2Service.isOAuth2ClientRegistrationAllowed(tenantId));
    }

    @Test
    public void testIsOAuth2Allowed_true() throws IOException {
        updateTenantAllowOAuth2Setting(true);
        Assert.assertTrue(oAuth2Service.isOAuth2ClientRegistrationAllowed(tenantId));
    }


    @Test
    public void testCreateNewSystemParams() {
        OAuth2ClientRegistration clientRegistration = validClientRegistration(TenantId.SYS_TENANT_ID);
        List<OAuth2ClientsDomainParams> savedDomainsParams = oAuth2Service.saveDomainsParams(TenantId.SYS_TENANT_ID, OAuth2Utils.toDomainsParams(Collections.singletonList(clientRegistration)));
        Assert.assertNotNull(savedDomainsParams);

        List<OAuth2ClientRegistration> savedClientRegistrations = OAuth2Utils.toClientRegistrations(TenantId.SYS_TENANT_ID, savedDomainsParams);
        Assert.assertEquals(1, savedClientRegistrations.size());

        OAuth2ClientRegistration savedClientRegistration = savedClientRegistrations.get(0);
        Assert.assertNotNull(savedClientRegistration.getId());
        clientRegistration.setId(savedClientRegistration.getId());
        clientRegistration.setCreatedTime(savedClientRegistration.getCreatedTime());
        Assert.assertEquals(clientRegistration, savedClientRegistration);
    }

    @Test
    public void testFindSystemParamsByTenant() {
        OAuth2ClientRegistration clientRegistration = validClientRegistration(TenantId.SYS_TENANT_ID);
        oAuth2Service.saveDomainsParams(TenantId.SYS_TENANT_ID, OAuth2Utils.toDomainsParams(Collections.singletonList(clientRegistration)));

        List<OAuth2ClientsDomainParams> foundDomainsParams = oAuth2Service.findDomainsParamsByTenantId(TenantId.SYS_TENANT_ID);
        Assert.assertEquals(1, foundDomainsParams.size());
        Assert.assertEquals(1, oAuth2Service.findAllClientRegistrations().size());

        List<OAuth2ClientRegistration> foundClientRegistrations = OAuth2Utils.toClientRegistrations(TenantId.SYS_TENANT_ID, foundDomainsParams);
        OAuth2ClientRegistration foundClientRegistration = foundClientRegistrations.get(0);
        Assert.assertNotNull(foundClientRegistration);
        clientRegistration.setId(foundClientRegistration.getId());
        clientRegistration.setCreatedTime(foundClientRegistration.getCreatedTime());
        Assert.assertEquals(clientRegistration, foundClientRegistration);
    }

    @Test
    public void testCreateNewTenantParams() {
        OAuth2ClientRegistration clientRegistration = validClientRegistration(tenantId);
        List<OAuth2ClientsDomainParams> savedDomainsParams = oAuth2Service.saveDomainsParams(tenantId, OAuth2Utils.toDomainsParams(Collections.singletonList(clientRegistration)));
        Assert.assertNotNull(savedDomainsParams);

        List<OAuth2ClientRegistration> savedClientRegistrations = OAuth2Utils.toClientRegistrations(tenantId, savedDomainsParams);
        Assert.assertEquals(1, savedClientRegistrations.size());

        OAuth2ClientRegistration savedClientRegistration = savedClientRegistrations.get(0);

        Assert.assertNotNull(savedClientRegistration);
        Assert.assertNotNull(savedClientRegistration.getId());
        clientRegistration.setId(savedClientRegistration.getId());
        clientRegistration.setCreatedTime(savedClientRegistration.getCreatedTime());
        Assert.assertEquals(clientRegistration, savedClientRegistration);
    }

    @Test
    public void testFindTenantParams() {
        OAuth2ClientRegistration clientRegistration = validClientRegistration(tenantId);
        oAuth2Service.saveDomainsParams(tenantId, OAuth2Utils.toDomainsParams(Collections.singletonList(clientRegistration)));

        List<OAuth2ClientsDomainParams> foundDomainsParams = oAuth2Service.findDomainsParamsByTenantId(tenantId);
        Assert.assertEquals(1, foundDomainsParams.size());
        Assert.assertEquals(1, oAuth2Service.findAllClientRegistrations().size());

        List<OAuth2ClientRegistration> foundClientRegistrations = OAuth2Utils.toClientRegistrations(tenantId, foundDomainsParams);
        OAuth2ClientRegistration foundClientRegistration = foundClientRegistrations.get(0);

        Assert.assertNotNull(foundClientRegistration);
        clientRegistration.setId(foundClientRegistration.getId());
        clientRegistration.setCreatedTime(foundClientRegistration.getCreatedTime());
        Assert.assertEquals(clientRegistration, foundClientRegistration);
    }

    @Test
    public void testGetClientRegistrationWithTenant() {
        OAuth2ClientRegistration tenantClientRegistration = validClientRegistration(tenantId);
        OAuth2ClientRegistration sysAdminClientRegistration = validClientRegistration(TenantId.SYS_TENANT_ID);

        List<OAuth2ClientsDomainParams> savedTenantDomainsParams = oAuth2Service.saveDomainsParams(tenantId,
                OAuth2Utils.toDomainsParams(Collections.singletonList(tenantClientRegistration)));
        List<OAuth2ClientsDomainParams> savedSysAdminDomainsParams = oAuth2Service.saveDomainsParams(TenantId.SYS_TENANT_ID,
                OAuth2Utils.toDomainsParams(Collections.singletonList(sysAdminClientRegistration)));

        Assert.assertEquals(2, oAuth2Service.findAllClientRegistrations().size());

        Assert.assertEquals(savedTenantDomainsParams, oAuth2Service.findDomainsParamsByTenantId(tenantId));
        Assert.assertEquals(savedSysAdminDomainsParams, oAuth2Service.findDomainsParamsByTenantId(TenantId.SYS_TENANT_ID));

        OAuth2ClientRegistration savedTenantClientRegistration = toClientRegistrations(tenantId, savedTenantDomainsParams).get(0);
        Assert.assertEquals(savedTenantClientRegistration, oAuth2Service.findClientRegistration(savedTenantClientRegistration.getUuidId()));
        OAuth2ClientRegistration savedSysAdminClientRegistration = toClientRegistrations(TenantId.SYS_TENANT_ID, savedSysAdminDomainsParams).get(0);
        Assert.assertEquals(savedSysAdminClientRegistration, oAuth2Service.findClientRegistration(savedSysAdminClientRegistration.getUuidId()));
    }

    @Test
    public void testGetOAuth2Clients() {
        String testDomainName = "test_domain";
        OAuth2ClientRegistration tenantClientRegistration = validClientRegistration(tenantId, testDomainName);
        OAuth2ClientRegistration sysAdminClientRegistration = validClientRegistration(TenantId.SYS_TENANT_ID, testDomainName);

        oAuth2Service.saveDomainsParams(tenantId, OAuth2Utils.toDomainsParams(Collections.singletonList(tenantClientRegistration)));
        oAuth2Service.saveDomainsParams(TenantId.SYS_TENANT_ID, OAuth2Utils.toDomainsParams(Collections.singletonList(sysAdminClientRegistration)));

        List<OAuth2ClientInfo> oAuth2Clients = oAuth2Service.getOAuth2Clients(testDomainName);

        Set<String> actualLabels = new HashSet<>(Arrays.asList(tenantClientRegistration.getLoginButtonLabel(),
                sysAdminClientRegistration.getLoginButtonLabel()));

        Set<String> foundLabels = oAuth2Clients.stream().map(OAuth2ClientInfo::getName).collect(Collectors.toSet());
        Assert.assertEquals(actualLabels, foundLabels);
    }

    @Test
    public void testGetEmptyOAuth2Clients() {
        String testDomainName = "test_domain";
        OAuth2ClientRegistration tenantClientRegistration = validClientRegistration(tenantId, testDomainName);
        OAuth2ClientRegistration sysAdminClientRegistration = validClientRegistration(TenantId.SYS_TENANT_ID, testDomainName);
        oAuth2Service.saveDomainsParams(tenantId, OAuth2Utils.toDomainsParams(Collections.singletonList(tenantClientRegistration)));
        oAuth2Service.saveDomainsParams(TenantId.SYS_TENANT_ID, OAuth2Utils.toDomainsParams(Collections.singletonList(sysAdminClientRegistration)));
        List<OAuth2ClientInfo> oAuth2Clients = oAuth2Service.getOAuth2Clients("random-domain");
        Assert.assertTrue(oAuth2Clients.isEmpty());
    }

    @Test
    public void testDeleteOAuth2ClientRegistration() {
        OAuth2ClientRegistration tenantClientRegistration = validClientRegistration(tenantId);
        OAuth2ClientRegistration sysAdminClientRegistration = validClientRegistration(TenantId.SYS_TENANT_ID);

        List<OAuth2ClientsDomainParams> savedTenantDomainsParams = oAuth2Service.saveDomainsParams(tenantId,
                OAuth2Utils.toDomainsParams(Collections.singletonList(tenantClientRegistration)));
        List<OAuth2ClientsDomainParams> savedSysAdminDomainsParams = oAuth2Service.saveDomainsParams(TenantId.SYS_TENANT_ID,
                OAuth2Utils.toDomainsParams(Collections.singletonList(sysAdminClientRegistration)));

        OAuth2ClientRegistration savedTenantRegistration = toClientRegistrations(tenantId, savedTenantDomainsParams).get(0);
        OAuth2ClientRegistration savedSysAdminRegistration = toClientRegistrations(TenantId.SYS_TENANT_ID, savedSysAdminDomainsParams).get(0);

        oAuth2Service.deleteClientRegistrationById(tenantId, savedTenantRegistration.getId());
        List<OAuth2ClientRegistration> foundRegistrations = oAuth2Service.findAllClientRegistrations();
        Assert.assertEquals(1, foundRegistrations.size());
        Assert.assertEquals(savedSysAdminRegistration, foundRegistrations.get(0));
    }

    @Test
    public void testDeleteTenantOAuth2ClientRegistrations() {
        oAuth2Service.saveDomainsParams(tenantId, OAuth2Utils.toDomainsParams(Arrays.asList(
                validClientRegistration(tenantId, "domain"),
                validClientRegistration(tenantId, "domain"),
                validClientRegistration(tenantId, "domain")
        )));
        Assert.assertEquals(3, oAuth2Service.findAllClientRegistrations().size());
        Assert.assertEquals(1, oAuth2Service.findDomainsParamsByTenantId(tenantId).size());

        oAuth2Service.deleteClientRegistrationsByTenantId(tenantId);
        Assert.assertEquals(0, oAuth2Service.findAllClientRegistrations().size());
        Assert.assertEquals(0, oAuth2Service.findDomainsParamsByTenantId(tenantId).size());
    }

    @Test
    public void testDeleteTenantDomainOAuth2ClientRegistrations() {
        oAuth2Service.saveDomainsParams(tenantId, OAuth2Utils.toDomainsParams(Arrays.asList(
                validClientRegistration(tenantId, "domain1"),
                validClientRegistration(tenantId, "domain1"),
                validClientRegistration(tenantId, "domain2")
        )));
        oAuth2Service.saveDomainsParams(TenantId.SYS_TENANT_ID, OAuth2Utils.toDomainsParams(Arrays.asList(
                validClientRegistration(TenantId.SYS_TENANT_ID, "domain2")
        )));
        Assert.assertEquals(4, oAuth2Service.findAllClientRegistrations().size());
        List<OAuth2ClientsDomainParams> tenantDomainsParams = oAuth2Service.findDomainsParamsByTenantId(tenantId);
        List<OAuth2ClientRegistration> tenantClientRegistrations = toClientRegistrations(tenantId, tenantDomainsParams);
        Assert.assertEquals(2, tenantDomainsParams.size());
        Assert.assertEquals(3, tenantClientRegistrations.size());

        oAuth2Service.deleteClientRegistrationsByDomain(tenantId, "domain1");
        Assert.assertEquals(2, oAuth2Service.findAllClientRegistrations().size());
        Assert.assertEquals(1, oAuth2Service.findDomainsParamsByTenantId(tenantId).size());
        Assert.assertEquals(1, toClientRegistrations(tenantId, oAuth2Service.findDomainsParamsByTenantId(tenantId)).size());
    }

    private void updateTenantAllowOAuth2Setting(Boolean allowOAuth2) throws IOException {
        Tenant tenant = tenantService.findTenantById(tenantId);
        if (allowOAuth2 == null) {
            tenant.setAdditionalInfo(mapper.readTree("{}"));
        } else {
            String additionalInfo = "{\"" + ALLOW_OAUTH2_CONFIGURATION + "\":" + allowOAuth2 + "}";
            tenant.setAdditionalInfo(mapper.readTree(additionalInfo));
            tenantService.saveTenant(tenant);
        }
    }

    private OAuth2ClientRegistration validClientRegistration(TenantId tenantId) {
        return validClientRegistration(tenantId, "domainName");
    }

    private OAuth2ClientRegistration validClientRegistration(TenantId tenantId, String domainName) {
        OAuth2ClientRegistration clientRegistration = new OAuth2ClientRegistration();
        clientRegistration.setTenantId(tenantId);
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

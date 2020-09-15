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

import org.apache.commons.lang3.tuple.Pair;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.OAuth2ClientRegistrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.*;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.oauth2.OAuth2Service;
import org.thingsboard.server.dao.oauth2.OAuth2Utils;

import javax.transaction.Transactional;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.thingsboard.server.dao.oauth2.OAuth2Utils.ALLOW_OAUTH2_CONFIGURATION;

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
        OAuth2ClientRegistration savedClientRegistration = oAuth2Service.saveClientRegistration(clientRegistration);

        Assert.assertNotNull(savedClientRegistration);
        Assert.assertNotNull(savedClientRegistration.getId());
        clientRegistration.setId(savedClientRegistration.getId());
        clientRegistration.setCreatedTime(savedClientRegistration.getCreatedTime());
        Assert.assertEquals(clientRegistration, savedClientRegistration);
    }

    @Test
    public void testFindSystemParamsByTenant() {
        OAuth2ClientRegistration clientRegistration = validClientRegistration(TenantId.SYS_TENANT_ID);
        oAuth2Service.saveClientRegistration(clientRegistration);

        List<OAuth2ClientRegistration> clientRegistrationsByTenantId = oAuth2Service.findClientRegistrationsByTenantId(TenantId.SYS_TENANT_ID);
        Assert.assertEquals(1, clientRegistrationsByTenantId.size());
        Assert.assertEquals(1, oAuth2Service.findAllClientRegistrations().size());
        OAuth2ClientRegistration foundClientRegistration = clientRegistrationsByTenantId.get(0);
        Assert.assertNotNull(foundClientRegistration);
        clientRegistration.setId(foundClientRegistration.getId());
        clientRegistration.setCreatedTime(foundClientRegistration.getCreatedTime());
        Assert.assertEquals(clientRegistration, foundClientRegistration);
    }

    @Test
    public void testCreateNewTenantParams() {
        OAuth2ClientRegistration clientRegistration = validClientRegistration(tenantId);
        OAuth2ClientRegistration savedClientRegistration = oAuth2Service.saveClientRegistration(clientRegistration);

        Assert.assertNotNull(savedClientRegistration);
        Assert.assertNotNull(savedClientRegistration.getId());
        clientRegistration.setId(savedClientRegistration.getId());
        clientRegistration.setCreatedTime(savedClientRegistration.getCreatedTime());
        Assert.assertEquals(clientRegistration, savedClientRegistration);
    }

    @Test
    public void testFindTenantParams() {
        OAuth2ClientRegistration clientRegistration = validClientRegistration(tenantId);
        oAuth2Service.saveClientRegistration(clientRegistration);

        List<OAuth2ClientRegistration> clientRegistrationsByTenantId = oAuth2Service.findClientRegistrationsByTenantId(tenantId);
        Assert.assertEquals(1, clientRegistrationsByTenantId.size());
        Assert.assertEquals(1, oAuth2Service.findAllClientRegistrations().size());
        OAuth2ClientRegistration foundClientRegistration = clientRegistrationsByTenantId.get(0);
        Assert.assertNotNull(foundClientRegistration);
        clientRegistration.setId(foundClientRegistration.getId());
        clientRegistration.setCreatedTime(foundClientRegistration.getCreatedTime());
        Assert.assertEquals(clientRegistration, foundClientRegistration);
    }

    @Test
    public void testGetClientRegistrationWithTenant() {
        OAuth2ClientRegistration tenantClientRegistration = validClientRegistration(tenantId);
        OAuth2ClientRegistration sysAdminClientRegistration = validClientRegistration(TenantId.SYS_TENANT_ID);

        OAuth2ClientRegistration savedTenantClientRegistration = oAuth2Service.saveClientRegistration(tenantClientRegistration);
        OAuth2ClientRegistration savedSysAdminClientRegistration = oAuth2Service.saveClientRegistration(sysAdminClientRegistration);

        Assert.assertEquals(2, oAuth2Service.findAllClientRegistrations().size());

        Assert.assertEquals(savedTenantClientRegistration, oAuth2Service.findClientRegistrationsByTenantId(tenantId).get(0));
        Assert.assertEquals(savedSysAdminClientRegistration, oAuth2Service.findClientRegistrationsByTenantId(TenantId.SYS_TENANT_ID).get(0));

        Assert.assertEquals(savedTenantClientRegistration,
                oAuth2Service.findClientRegistration(savedTenantClientRegistration.getUuidId()));
        Assert.assertEquals(savedSysAdminClientRegistration,
                oAuth2Service.findClientRegistration(savedSysAdminClientRegistration.getUuidId()));
    }

    @Test
    public void testGetOAuth2Clients() {
        String testDomainName = "test_domain";
        OAuth2ClientRegistration tenantClientRegistration = validClientRegistration(tenantId, testDomainName);
        OAuth2ClientRegistration sysAdminClientRegistration = validClientRegistration(TenantId.SYS_TENANT_ID, testDomainName);

        oAuth2Service.saveClientRegistration(tenantClientRegistration);
        oAuth2Service.saveClientRegistration(sysAdminClientRegistration);

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
        oAuth2Service.saveClientRegistration(tenantClientRegistration);
        oAuth2Service.saveClientRegistration(sysAdminClientRegistration);
        List<OAuth2ClientInfo> oAuth2Clients = oAuth2Service.getOAuth2Clients("random-domain");
        Assert.assertTrue(oAuth2Clients.isEmpty());
    }

    @Test
    public void testDeleteOAuth2ClientRegistration() {
        OAuth2ClientRegistration tenantClientRegistration = validClientRegistration(tenantId);
        OAuth2ClientRegistration sysAdminClientRegistration = validClientRegistration(TenantId.SYS_TENANT_ID);
        OAuth2ClientRegistration savedTenantRegistration = oAuth2Service.saveClientRegistration(tenantClientRegistration);
        OAuth2ClientRegistration savedSysAdminRegistration = oAuth2Service.saveClientRegistration(sysAdminClientRegistration);

        oAuth2Service.deleteClientRegistrationById(tenantId, savedTenantRegistration.getId());
        List<OAuth2ClientRegistration> foundRegistrations = oAuth2Service.findAllClientRegistrations();
        Assert.assertEquals(1, foundRegistrations.size());
        Assert.assertEquals(savedSysAdminRegistration, foundRegistrations.get(0));
    }

    @Test
    public void testDeleteTenantOAuth2ClientRegistrations() {
        oAuth2Service.saveClientRegistration(validClientRegistration(tenantId));
        oAuth2Service.saveClientRegistration(validClientRegistration(tenantId));
        oAuth2Service.saveClientRegistration(validClientRegistration(tenantId));
        Assert.assertEquals(3, oAuth2Service.findAllClientRegistrations().size());
        Assert.assertEquals(3, oAuth2Service.findClientRegistrationsByTenantId(tenantId).size());

        oAuth2Service.deleteClientRegistrationsByTenantId(tenantId);
        Assert.assertEquals(0, oAuth2Service.findAllClientRegistrations().size());
        Assert.assertEquals(0, oAuth2Service.findClientRegistrationsByTenantId(tenantId).size());
    }

    @Test
    public void testDeleteTenantDomainOAuth2ClientRegistrations() {
        oAuth2Service.saveClientRegistration(validClientRegistration(tenantId, "domain1"));
        oAuth2Service.saveClientRegistration(validClientRegistration(tenantId, "domain1"));
        oAuth2Service.saveClientRegistration(validClientRegistration(tenantId, "domain2"));
        oAuth2Service.saveClientRegistration(validClientRegistration(TenantId.SYS_TENANT_ID, "domain2"));
        Assert.assertEquals(4, oAuth2Service.findAllClientRegistrations().size());
        Assert.assertEquals(3, oAuth2Service.findClientRegistrationsByTenantId(tenantId).size());

        oAuth2Service.deleteClientRegistrationsByDomain(tenantId, "domain1");
        Assert.assertEquals(2, oAuth2Service.findAllClientRegistrations().size());
        Assert.assertEquals(1, oAuth2Service.findClientRegistrationsByTenantId(tenantId).size());
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
        return clientRegistration;
    }
}

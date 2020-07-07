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
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.*;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.oauth2.OAuth2Service;
import org.thingsboard.server.dao.oauth2.OAuth2Utils;

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

        Assert.assertNull(oAuth2Service.getSystemOAuth2ClientsParams().getClientsDomainsParams());
        Assert.assertNull(oAuth2Service.getTenantOAuth2ClientsParams(tenantId).getClientsDomainsParams());

        Assert.assertTrue(attributesService.findAll(tenantId, tenantId, DataConstants.SERVER_SCOPE).get().isEmpty());
        Assert.assertNull(adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, OAuth2Utils.OAUTH2_CLIENT_REGISTRATIONS_PARAMS));
    }

    @After
    public void after() throws Exception {
        clearSysAdmin();

        tenantService.deleteTenant(tenantId);

        Assert.assertNull(oAuth2Service.getSystemOAuth2ClientsParams().getClientsDomainsParams());
        Assert.assertNull(oAuth2Service.getTenantOAuth2ClientsParams(tenantId).getClientsDomainsParams());

        Assert.assertTrue(attributesService.findAll(tenantId, tenantId, DataConstants.SERVER_SCOPE).get().isEmpty());
        Assert.assertNull(adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, OAuth2Utils.OAUTH2_CLIENT_REGISTRATIONS_PARAMS));
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
    public void testSaveSystemOAuth2() throws IOException {
        updateTenantAllowOAuth2Setting(true);
        Assert.assertTrue(oAuth2Service.isOAuth2ClientRegistrationAllowed(tenantId));
    }

    @Test(expected = DataValidationException.class)
    public void testSaveSystemParamsWithDuplicateDomains() {
        oAuth2Service.saveSystemOAuth2ClientsParams(clientsParamsWithDuplicateDomains());
    }

    @Test(expected = DataValidationException.class)
    public void testSaveSystemParamsWithDuplicateRegistrationIds() {
        oAuth2Service.saveSystemOAuth2ClientsParams(clientsParamsWithDuplicateRegistrationIds());
    }

    @Test(expected = DataValidationException.class)
    public void testSaveTenantParamsWithDuplicateRegistrationIds() {
        oAuth2Service.saveTenantOAuth2ClientsParams(tenantId, clientsParamsWithDuplicateRegistrationIds());
    }

    @Test
    public void testSaveSystemParams() {
        OAuth2ClientsParams clientsParams = validClientsParams();
        OAuth2ClientsParams savedClientParams = oAuth2Service.saveSystemOAuth2ClientsParams(clientsParams);

        Assert.assertNotNull(adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, OAuth2Utils.OAUTH2_CLIENT_REGISTRATIONS_PARAMS));
        Assert.assertEquals(clientsParams, savedClientParams);
    }

    @Test
    public void testSaveSystemParamsWithMultipleDomains() {
        OAuth2ClientsParams clientsParams = validClientsParamsWithThreeDomains();
        OAuth2ClientsParams savedClientParams = oAuth2Service.saveSystemOAuth2ClientsParams(clientsParams);

        Assert.assertNotNull(adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, OAuth2Utils.OAUTH2_CLIENT_REGISTRATIONS_PARAMS));
        Assert.assertEquals(clientsParams, savedClientParams);
    }

    @Test
    public void testFindSystemParams() {
        OAuth2ClientsParams clientsParams = validClientsParams();
        oAuth2Service.saveSystemOAuth2ClientsParams(clientsParams);

        Assert.assertNotNull(adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, OAuth2Utils.OAUTH2_CLIENT_REGISTRATIONS_PARAMS));

        OAuth2ClientsParams foundClientParams = oAuth2Service.getSystemOAuth2ClientsParams();
        Assert.assertNotNull(foundClientParams);
        Assert.assertEquals(clientsParams, foundClientParams);
    }

    @Test
    public void testSaveTenantParams() {
        OAuth2ClientsParams clientsParams = validClientsParams();
        OAuth2ClientsDomainParams domainParams = clientsParams.getClientsDomainsParams().get(0);
        String domainKey = OAuth2Utils.constructAdminSettingsDomainKey(domainParams.getDomainName());

        Assert.assertNull(adminSettingsService.findAdminSettingsByKey(tenantId, domainKey));

        OAuth2ClientsParams savedClientParams = oAuth2Service.saveTenantOAuth2ClientsParams(tenantId, clientsParams);

        Assert.assertNotNull(adminSettingsService.findAdminSettingsByKey(tenantId, domainKey));
        Assert.assertNotNull(savedClientParams);

        OAuth2ClientsDomainParams savedDomainParams = savedClientParams.getClientsDomainsParams().get(0);
        Assert.assertEquals(domainParams.getDomainName(), savedDomainParams.getDomainName());
        Assert.assertEquals(domainParams.getClientRegistrations(), savedDomainParams.getClientRegistrations());
    }

    @Test
    public void testSaveTenantMultipleParams() {
        OAuth2ClientsParams clientsParams = validClientsParamsWithThreeDomains();

        clientsParams.getClientsDomainsParams().forEach(oAuth2ClientsDomainParams -> {
            String domainName = oAuth2ClientsDomainParams.getDomainName();
            String domainKey = OAuth2Utils.constructAdminSettingsDomainKey(domainName);
            Assert.assertNull(adminSettingsService.findAdminSettingsByKey(tenantId, domainKey));
        });

        OAuth2ClientsParams savedClientParams = oAuth2Service.saveTenantOAuth2ClientsParams(tenantId, clientsParams);
        Assert.assertNotNull(savedClientParams);

        clientsParams.getClientsDomainsParams().forEach(oAuth2ClientsDomainParams -> {
            String domainName = oAuth2ClientsDomainParams.getDomainName();
            String domainKey = OAuth2Utils.constructAdminSettingsDomainKey(domainName);
            Assert.assertNotNull(adminSettingsService.findAdminSettingsByKey(tenantId, domainKey));
        });

        Assert.assertEquals(clientsParams, savedClientParams);
    }

    @Test
    public void testRewriteSameDomainTenantParams() {
        OAuth2ClientsParams clientsParams = validClientsParamsWithThreeDomains();
        oAuth2Service.saveTenantOAuth2ClientsParams(tenantId, clientsParams);

        List<OAuth2ClientsDomainParams> clientsDomainsParams = clientsParams.getClientsDomainsParams();
        OAuth2ClientsParams updatedClientsParams = validClientsParamsWithThreeDomains();
        String sameDomainName = clientsDomainsParams.get(0).getDomainName();
        updatedClientsParams.getClientsDomainsParams().get(0).setDomainName(sameDomainName);
        OAuth2ClientsParams rewrittenClientParams = oAuth2Service.saveTenantOAuth2ClientsParams(tenantId, updatedClientsParams);
        Assert.assertEquals(updatedClientsParams, rewrittenClientParams);

        clientsParams.getClientsDomainsParams().forEach(oAuth2ClientsDomainParams -> {
            String domainName = oAuth2ClientsDomainParams.getDomainName();
            String domainKey = OAuth2Utils.constructAdminSettingsDomainKey(domainName);
            if (domainName.equals(sameDomainName)) {
                Assert.assertNotNull(adminSettingsService.findAdminSettingsByKey(tenantId, domainKey));
            } else {
                Assert.assertNull(adminSettingsService.findAdminSettingsByKey(tenantId, domainKey));
            }
        });
        updatedClientsParams.getClientsDomainsParams().forEach(oAuth2ClientsDomainParams -> {
            String domainName = oAuth2ClientsDomainParams.getDomainName();
            String domainKey = OAuth2Utils.constructAdminSettingsDomainKey(domainName);
            Assert.assertNotNull(adminSettingsService.findAdminSettingsByKey(tenantId, domainKey));
        });
    }

    @Test
    public void testAddDeleteTenantDomainParams() {
        OAuth2ClientsParams clientsParams = validClientsParamsWithThreeDomains();
        oAuth2Service.saveTenantOAuth2ClientsParams(tenantId, clientsParams);

        List<OAuth2ClientsDomainParams> clientsDomainsParams = clientsParams.getClientsDomainsParams();
        OAuth2ClientsParams updatedClientsParams = validClientsParamsWithThreeDomains();
        for (int i = 0; i < updatedClientsParams.getClientsDomainsParams().size(); i++) {
            String domainName = clientsDomainsParams.get(i).getDomainName();
            updatedClientsParams.getClientsDomainsParams().get(i).setDomainName(domainName);
        }
        OAuth2ClientsParams rewrittenClientParams = oAuth2Service.saveTenantOAuth2ClientsParams(tenantId, updatedClientsParams);
        Assert.assertEquals(updatedClientsParams, rewrittenClientParams);

        clientsParams.getClientsDomainsParams().forEach(oAuth2ClientsDomainParams -> {
            String domainName = oAuth2ClientsDomainParams.getDomainName();
            String domainKey = OAuth2Utils.constructAdminSettingsDomainKey(domainName);
            Assert.assertNotNull(adminSettingsService.findAdminSettingsByKey(tenantId, domainKey));
        });
    }

    @Test
    public void testFindTenantParams() {
        OAuth2ClientsParams clientsParams = validClientsParams();
        OAuth2ClientsDomainParams domainParams = clientsParams.getClientsDomainsParams().get(0);
        String domainKey = OAuth2Utils.constructAdminSettingsDomainKey(domainParams.getDomainName());

        Assert.assertNull(adminSettingsService.findAdminSettingsByKey(tenantId, domainKey));

        OAuth2ClientsParams savedClientsParams = oAuth2Service.saveTenantOAuth2ClientsParams(tenantId, clientsParams);

        Assert.assertNotNull(adminSettingsService.findAdminSettingsByKey(tenantId, domainKey));

        OAuth2ClientsParams foundClientsParams = oAuth2Service.getTenantOAuth2ClientsParams(tenantId);
        Assert.assertEquals(savedClientsParams, foundClientsParams);
    }

    @Test
    public void testGetClientRegistrationWithTenant() {
        OAuth2ClientsParams tenantClientsParams = validClientsParams();
        OAuth2ClientsParams sysAdminClientsParams = validClientsParams();

        oAuth2Service.saveTenantOAuth2ClientsParams(tenantId, tenantClientsParams);
        oAuth2Service.saveSystemOAuth2ClientsParams(sysAdminClientsParams);

        OAuth2Utils.toClientRegistrationStream(tenantClientsParams)
                .forEach(clientRegistration -> {
                    Pair<TenantId, OAuth2ClientRegistration> pair = oAuth2Service.getClientRegistrationWithTenant(clientRegistration.getRegistrationId());
                    Assert.assertEquals(tenantId, pair.getKey());
                    Assert.assertEquals(clientRegistration.getRegistrationId(), pair.getValue().getRegistrationId());
                });
        OAuth2Utils.toClientRegistrationStream(sysAdminClientsParams)
                .forEach(clientRegistration -> {
                    Pair<TenantId, OAuth2ClientRegistration> pair = oAuth2Service.getClientRegistrationWithTenant(clientRegistration.getRegistrationId());
                    Assert.assertNotNull(pair);
                    Assert.assertEquals(TenantId.SYS_TENANT_ID, pair.getKey());
                    Assert.assertEquals(clientRegistration.getRegistrationId(), pair.getValue().getRegistrationId());
                });
    }

    @Test
    public void testGetClientRegistration() {
        OAuth2ClientsParams tenantClientsParams = validClientsParams();
        OAuth2ClientsParams sysAdminClientsParams = validClientsParams();

        oAuth2Service.saveTenantOAuth2ClientsParams(tenantId, tenantClientsParams);
        oAuth2Service.saveSystemOAuth2ClientsParams(sysAdminClientsParams);

        Stream.concat(
                OAuth2Utils.toClientRegistrationStream(tenantClientsParams),
                OAuth2Utils.toClientRegistrationStream(sysAdminClientsParams)
        )
                .forEach(clientRegistration -> {
                    OAuth2ClientRegistration foundClientRegistration = oAuth2Service.getClientRegistration(clientRegistration.getRegistrationId());
                    Assert.assertNotNull(foundClientRegistration);
                    Assert.assertEquals(clientRegistration.getRegistrationId(), foundClientRegistration.getRegistrationId());
                });

    }

    @Test
    public void testGetOAuth2Clients() {
        OAuth2ClientsParams tenantClientsParams = validClientsParams();
        OAuth2ClientsParams sysAdminClientsParams = validClientsParams();

        OAuth2ClientsDomainParams tenantDomainParams = tenantClientsParams.getClientsDomainsParams().get(0);
        OAuth2ClientsDomainParams systemDomainParams = sysAdminClientsParams.getClientsDomainsParams().get(0);
        systemDomainParams.setDomainName(tenantDomainParams.getDomainName());

        oAuth2Service.saveTenantOAuth2ClientsParams(tenantId, tenantClientsParams);
        oAuth2Service.saveSystemOAuth2ClientsParams(sysAdminClientsParams);

        List<OAuth2ClientInfo> oAuth2Clients = oAuth2Service.getOAuth2Clients(tenantDomainParams.getDomainName());

        Set<String> actualLabels = Stream.concat(
                tenantDomainParams.getClientRegistrations().stream()
                        .map(OAuth2ClientRegistration::getLoginButtonLabel),
                systemDomainParams.getClientRegistrations().stream()
                        .map(OAuth2ClientRegistration::getLoginButtonLabel)
        ).collect(Collectors.toSet());


        Set<String> foundLabels = oAuth2Clients.stream().map(OAuth2ClientInfo::getName).collect(Collectors.toSet());
        Assert.assertEquals(actualLabels, foundLabels);
    }

    @Test
    public void testGetEmptyOAuth2Clients() {
        List<OAuth2ClientInfo> oAuth2Clients = oAuth2Service.getOAuth2Clients("random-domain");
        Assert.assertTrue(oAuth2Clients.isEmpty());
    }

    @Test
    public void testGetAllOAuth2ClientsParams() {
        OAuth2ClientsParams tenantClientsParams = validClientsParams();
        OAuth2ClientsParams sysAdminClientsParams = validClientsParams();

        Map<TenantId, OAuth2ClientsParams> emptyParams = oAuth2Service.getAllOAuth2ClientsParams();
        Assert.assertTrue(emptyParams.isEmpty());

        OAuth2ClientsParams savedTenantParams = oAuth2Service.saveTenantOAuth2ClientsParams(tenantId, tenantClientsParams);
        OAuth2ClientsParams savedSystemParams = oAuth2Service.saveSystemOAuth2ClientsParams(sysAdminClientsParams);

        Map<TenantId, OAuth2ClientsParams> clientsParams = oAuth2Service.getAllOAuth2ClientsParams();

        OAuth2ClientsParams foundTenantParams = clientsParams.get(tenantId);
        Assert.assertEquals(savedTenantParams, foundTenantParams);

        OAuth2ClientsParams foundSystemParams = clientsParams.get(TenantId.SYS_TENANT_ID);
        Assert.assertEquals(savedSystemParams, foundSystemParams);
    }

    @Test
    public void testDeleteSystemOAuth2ClientsParams() {
        OAuth2ClientsParams sysAdminClientsParams = validClientsParams();

        Assert.assertNull(oAuth2Service.getSystemOAuth2ClientsParams().getClientsDomainsParams());

        oAuth2Service.saveSystemOAuth2ClientsParams(sysAdminClientsParams);

        Assert.assertNotNull(oAuth2Service.getSystemOAuth2ClientsParams().getClientsDomainsParams());

        oAuth2Service.deleteSystemOAuth2ClientsParams();
        Assert.assertNull(adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, OAuth2Utils.OAUTH2_CLIENT_REGISTRATIONS_PARAMS));
    }

    @Test
    public void testDeleteTenantOAuth2ClientsParams() {
        OAuth2ClientsParams tenantClientsParams = validClientsParams();

        Assert.assertNull(oAuth2Service.getTenantOAuth2ClientsParams(tenantId).getClientsDomainsParams());

        oAuth2Service.saveTenantOAuth2ClientsParams(tenantId, tenantClientsParams);

        Assert.assertNotNull(oAuth2Service.getTenantOAuth2ClientsParams(tenantId).getClientsDomainsParams());

        oAuth2Service.deleteTenantOAuth2ClientsParams(tenantId);
        Assert.assertNull(oAuth2Service.getTenantOAuth2ClientsParams(tenantId).getClientsDomainsParams());
        tenantClientsParams.getClientsDomainsParams().forEach(oAuth2ClientsDomainParams -> {
            String domainName = oAuth2ClientsDomainParams.getDomainName();
            String domainKey = OAuth2Utils.constructAdminSettingsDomainKey(domainName);
            Assert.assertNull(adminSettingsService.findAdminSettingsByKey(tenantId, domainKey));
        });
    }


    private void clearSysAdmin() {
        oAuth2Service.deleteSystemOAuth2ClientsParams();
        Assert.assertNull(adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, OAuth2Utils.OAUTH2_CLIENT_REGISTRATIONS_PARAMS));
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

    private OAuth2ClientsParams validClientsParams() {
        OAuth2ClientRegistration first = validClientRegistration();
        OAuth2ClientRegistration second = validClientRegistration();
        return OAuth2ClientsParams.builder()
                .clientsDomainsParams(Collections.singletonList(
                        OAuth2ClientsDomainParams.builder()
                                .domainName(UUID.randomUUID().toString())
                                .clientRegistrations(Arrays.asList(first, second))
                                .build()
                ))
                .build();
    }

    private OAuth2ClientsParams validClientsParamsWithThreeDomains() {
        OAuth2ClientRegistration first = validClientRegistration();
        OAuth2ClientRegistration second = validClientRegistration();
        OAuth2ClientRegistration third = validClientRegistration();
        return OAuth2ClientsParams.builder()
                .clientsDomainsParams(Arrays.asList(
                        OAuth2ClientsDomainParams.builder()
                                .domainName(UUID.randomUUID().toString())
                                .clientRegistrations(Arrays.asList(first, second))
                                .build(),
                        OAuth2ClientsDomainParams.builder()
                                .domainName(UUID.randomUUID().toString())
                                .clientRegistrations(Arrays.asList(third))
                                .build()
                ))
                .build();
    }


    private OAuth2ClientsParams clientsParamsWithDuplicateDomains() {
        OAuth2ClientRegistration first = validClientRegistration();
        OAuth2ClientRegistration second = validClientRegistration();
        OAuth2ClientRegistration third = validClientRegistration();
        return OAuth2ClientsParams.builder()
                .clientsDomainsParams(Arrays.asList(
                        OAuth2ClientsDomainParams.builder()
                                .domainName("domain")
                                .clientRegistrations(Collections.singletonList(first))
                                .build(),
                        OAuth2ClientsDomainParams.builder()
                                .domainName("domain")
                                .clientRegistrations(Collections.singletonList(second))
                                .build(),
                        OAuth2ClientsDomainParams.builder()
                                .domainName(UUID.randomUUID().toString())
                                .clientRegistrations(Collections.singletonList(third))
                                .build()
                ))
                .build();
    }

    private OAuth2ClientsParams clientsParamsWithDuplicateRegistrationIds() {
        OAuth2ClientRegistration first = validClientRegistration();
        first.setRegistrationId("registrationId");
        OAuth2ClientRegistration second = validClientRegistration();
        OAuth2ClientRegistration third = validClientRegistration();
        third.setRegistrationId("registrationId");
        return OAuth2ClientsParams.builder()
                .clientsDomainsParams(Arrays.asList(
                        OAuth2ClientsDomainParams.builder()
                                .domainName(UUID.randomUUID().toString())
                                .clientRegistrations(Arrays.asList(first, second, third))
                                .build()
                ))
                .build();
    }

    private OAuth2ClientRegistration validClientRegistration() {
        return OAuth2ClientRegistration.builder()
                .registrationId(UUID.randomUUID().toString())
                .mapperConfig(OAuth2MapperConfig.builder()
                        .allowUserCreation(true)
                        .activateUser(true)
                        .type(MapperType.CUSTOM)
                        .customConfig(
                                OAuth2CustomMapperConfig.builder()
                                        .url("localhost:8082")
                                        .username("test")
                                        .password("test")
                                        .build()
                        )
                        .build())
                .clientId("clientId")
                .clientSecret("clientSecret")
                .authorizationUri("authorizationUri")
                .tokenUri("tokenUri")
                .redirectUriTemplate("http://localhost:8080/login/oauth2/code/")
                .scope("scope")
                .userInfoUri("userInfoUri")
                .userNameAttributeName("userNameAttributeName")
                .jwkSetUri("jwkSetUri")
                .clientAuthenticationMethod("clientAuthenticationMethod")
                .clientName("clientName")
                .loginButtonLabel("loginButtonLabel")
                .build();
    }
}

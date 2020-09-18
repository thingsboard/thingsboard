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
import org.thingsboard.server.common.data.oauth2.OAuth2BasicMapperConfig;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientRegistrationTemplate;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.oauth2.OAuth2ConfigTemplateService;

import java.util.Arrays;
import java.util.UUID;

public class BaseOAuth2ConfigTemplateServiceTest extends AbstractServiceTest {

    @Autowired
    protected OAuth2ConfigTemplateService oAuth2ConfigTemplateService;

    private TenantId tenantId;

    @Before
    public void beforeRun() throws Exception {
        Assert.assertTrue(oAuth2ConfigTemplateService.findAllClientRegistrationTemplates().isEmpty());
    }

    @After
    public void after() throws Exception {
        oAuth2ConfigTemplateService.findAllClientRegistrationTemplates().forEach(clientRegistrationTemplate -> {
            oAuth2ConfigTemplateService.deleteClientRegistrationTemplateById(clientRegistrationTemplate.getId());
        });

        Assert.assertTrue(oAuth2ConfigTemplateService.findAllClientRegistrationTemplates().isEmpty());
    }


    @Test(expected = DataValidationException.class)
    public void testSaveDuplicateProviderId() {
        OAuth2ClientRegistrationTemplate first = validClientRegistrationTemplate(TenantId.SYS_TENANT_ID, "providerId");
        OAuth2ClientRegistrationTemplate second = validClientRegistrationTemplate(TenantId.SYS_TENANT_ID, "providerId");
        oAuth2ConfigTemplateService.saveClientRegistrationTemplate(first);
        oAuth2ConfigTemplateService.saveClientRegistrationTemplate(second);
    }

    @Test
    public void testCreateNewTemplate() {
        OAuth2ClientRegistrationTemplate clientRegistrationTemplate = validClientRegistrationTemplate(TenantId.SYS_TENANT_ID, UUID.randomUUID().toString());
        OAuth2ClientRegistrationTemplate savedClientRegistrationTemplate = oAuth2ConfigTemplateService.saveClientRegistrationTemplate(clientRegistrationTemplate);

        Assert.assertNotNull(savedClientRegistrationTemplate);
        Assert.assertNotNull(savedClientRegistrationTemplate.getId());
        clientRegistrationTemplate.setId(savedClientRegistrationTemplate.getId());
        clientRegistrationTemplate.setCreatedTime(savedClientRegistrationTemplate.getCreatedTime());
        Assert.assertEquals(clientRegistrationTemplate, savedClientRegistrationTemplate);
    }

    @Test
    public void testFindTemplate() {
        OAuth2ClientRegistrationTemplate clientRegistrationTemplate = validClientRegistrationTemplate(TenantId.SYS_TENANT_ID, UUID.randomUUID().toString());
        OAuth2ClientRegistrationTemplate savedClientRegistrationTemplate = oAuth2ConfigTemplateService.saveClientRegistrationTemplate(clientRegistrationTemplate);

        OAuth2ClientRegistrationTemplate foundClientRegistrationTemplate = oAuth2ConfigTemplateService.findClientRegistrationTemplateById(savedClientRegistrationTemplate.getId());
        Assert.assertEquals(savedClientRegistrationTemplate, foundClientRegistrationTemplate);
    }

    @Test
    public void testFindAll() {
        oAuth2ConfigTemplateService.saveClientRegistrationTemplate(validClientRegistrationTemplate(TenantId.SYS_TENANT_ID, UUID.randomUUID().toString()));
        oAuth2ConfigTemplateService.saveClientRegistrationTemplate(validClientRegistrationTemplate(TenantId.SYS_TENANT_ID, UUID.randomUUID().toString()));

        Assert.assertEquals(2, oAuth2ConfigTemplateService.findAllClientRegistrationTemplates().size());
    }

    @Test
    public void testDeleteTemplate() {
        oAuth2ConfigTemplateService.saveClientRegistrationTemplate(validClientRegistrationTemplate(TenantId.SYS_TENANT_ID, UUID.randomUUID().toString()));
        oAuth2ConfigTemplateService.saveClientRegistrationTemplate(validClientRegistrationTemplate(TenantId.SYS_TENANT_ID, UUID.randomUUID().toString()));
        OAuth2ClientRegistrationTemplate saved = oAuth2ConfigTemplateService.saveClientRegistrationTemplate(validClientRegistrationTemplate(TenantId.SYS_TENANT_ID, UUID.randomUUID().toString()));

        Assert.assertEquals(3, oAuth2ConfigTemplateService.findAllClientRegistrationTemplates().size());
        Assert.assertNotNull(oAuth2ConfigTemplateService.findClientRegistrationTemplateById(saved.getId()));

        oAuth2ConfigTemplateService.deleteClientRegistrationTemplateById(saved.getId());

        Assert.assertEquals(2, oAuth2ConfigTemplateService.findAllClientRegistrationTemplates().size());
        Assert.assertNull(oAuth2ConfigTemplateService.findClientRegistrationTemplateById(saved.getId()));
    }

    private OAuth2ClientRegistrationTemplate validClientRegistrationTemplate(TenantId tenantId, String providerId) {
        OAuth2ClientRegistrationTemplate clientRegistrationTemplate = new OAuth2ClientRegistrationTemplate();
        clientRegistrationTemplate.setProviderId(providerId);
        clientRegistrationTemplate.setTenantId(tenantId);
        clientRegistrationTemplate.setAdditionalInfo(mapper.createObjectNode().put(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
        clientRegistrationTemplate.setBasic(
                OAuth2BasicMapperConfig.builder()
                        .firstNameAttributeKey("firstName")
                        .lastNameAttributeKey("lastName")
                        .emailAttributeKey("email")
                        .tenantNamePattern("tenant")
                        .defaultDashboardName("Test")
                        .alwaysFullScreen(true)
                        .build()
        );
        clientRegistrationTemplate.setAuthorizationUri("authorizationUri");
        clientRegistrationTemplate.setAccessTokenUri("tokenUri");
        clientRegistrationTemplate.setScope(Arrays.asList("scope1", "scope2"));
        clientRegistrationTemplate.setUserInfoUri("userInfoUri");
        clientRegistrationTemplate.setUserNameAttributeName("userNameAttributeName");
        clientRegistrationTemplate.setJwkSetUri("jwkSetUri");
        clientRegistrationTemplate.setClientAuthenticationMethod("clientAuthenticationMethod");
        clientRegistrationTemplate.setComment("comment");
        clientRegistrationTemplate.setLoginButtonIcon("icon");
        clientRegistrationTemplate.setLoginButtonLabel("label");
        clientRegistrationTemplate.setHelpLink("helpLink");
        return clientRegistrationTemplate;
    }
}

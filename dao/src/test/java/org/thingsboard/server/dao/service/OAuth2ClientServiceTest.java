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
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.OAuth2Client;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2CustomMapperConfig;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.oauth2.OAuth2ClientService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DaoSqlTest
public class OAuth2ClientServiceTest extends AbstractServiceTest {

    @Autowired
    protected OAuth2ClientService oAuth2ClientService;

    @After
    public void after() {
        oAuth2ClientService.deleteByTenantId(TenantId.SYS_TENANT_ID);
    }

    @Test
    public void testSaveOauth2Client() {
        OAuth2Client oAuth2Client = validClientInfo(TenantId.SYS_TENANT_ID, "Test google client", List.of(PlatformType.ANDROID));
        OAuth2Client savedOauth2Client = oAuth2ClientService.saveOAuth2Client(SYSTEM_TENANT_ID, oAuth2Client);

        OAuth2Client retrievedOauth2Client = oAuth2ClientService.findOAuth2ClientById(savedOauth2Client.getTenantId(), savedOauth2Client.getId());
        assertThat(retrievedOauth2Client).isEqualTo(savedOauth2Client);

        savedOauth2Client.setTitle("New title");
        OAuth2Client updatedOauth2Client = oAuth2ClientService.saveOAuth2Client(SYSTEM_TENANT_ID, savedOauth2Client);

        OAuth2Client retrievedOauth2Client2 = oAuth2ClientService.findOAuth2ClientById(savedOauth2Client.getTenantId(), savedOauth2Client.getId());
        assertThat(retrievedOauth2Client2).isEqualTo(updatedOauth2Client);
    }

    @Test
    public void testSaveOauth2ClientWithoutMapper() {
        OAuth2Client oAuth2Client = validClientInfo(TenantId.SYS_TENANT_ID, "Test google client", List.of(PlatformType.ANDROID));
        oAuth2Client.setMapperConfig(null);

        assertThatThrownBy(() -> {
            oAuth2ClientService.saveOAuth2Client(TenantId.SYS_TENANT_ID, oAuth2Client);
        }).hasMessageContaining("mapperConfig must not be null");
    }

    @Test
    public void testSaveOauth2ClientWithoutCustomConfig() {
        OAuth2Client oAuth2Client = validClientInfo(TenantId.SYS_TENANT_ID, "Test google client", List.of(PlatformType.ANDROID));
        oAuth2Client.getMapperConfig().setCustom(null);

        assertThatThrownBy(() -> {
            oAuth2ClientService.saveOAuth2Client(TenantId.SYS_TENANT_ID, oAuth2Client);
        }).hasMessageContaining("Custom config should be specified!");
    }

    @Test
    public void testSaveOauth2ClientWithoutCustomUrl() {
        OAuth2Client oAuth2Client = validClientInfo(TenantId.SYS_TENANT_ID, "Test google client", List.of(PlatformType.ANDROID));
        oAuth2Client.getMapperConfig().setCustom(OAuth2CustomMapperConfig.builder().build());
        assertThatThrownBy(() -> {
            oAuth2ClientService.saveOAuth2Client(TenantId.SYS_TENANT_ID, oAuth2Client);
        }).hasMessageContaining("Custom mapper URL should be specified!");
    }

    @Test
    public void testGetTenantOAuth2Clients() {
        List<OAuth2Client> oAuth2Clients = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            OAuth2Client oAuth2Client = validClientInfo(TenantId.SYS_TENANT_ID, StringUtils.randomAlphabetic(5));
            OAuth2Client savedOauth2Client = oAuth2ClientService.saveOAuth2Client(SYSTEM_TENANT_ID, oAuth2Client);
            oAuth2Clients.add(savedOauth2Client);
        }
        List<OAuth2Client> retrieved = oAuth2ClientService.findOAuth2ClientsByTenantId(TenantId.SYS_TENANT_ID);
        assertThat(retrieved).containsOnlyOnceElementsOf(oAuth2Clients);

        PageData<OAuth2ClientInfo> retrievedInfos = oAuth2ClientService.findOAuth2ClientInfosByTenantId(TenantId.SYS_TENANT_ID, new PageLink(10));
        List<OAuth2ClientInfo> oAuth2ClientInfos = oAuth2Clients.stream().map(OAuth2ClientInfo::new).collect(Collectors.toList());
        assertThat(retrievedInfos.getData()).containsOnlyOnceElementsOf(oAuth2ClientInfos);
    }

}

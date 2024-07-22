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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@DaoSqlTest
public class Oauth2ClientControllerTest extends AbstractControllerTest {

    @Before
    public void setUp() throws Exception {
        loginSysAdmin();
    }

    @After
    public void tearDown() throws Exception {
        List<OAuth2ClientInfo> oAuth2ClientInfos = doGetTyped("/api/oauth2/client/infos", new TypeReference<List<OAuth2ClientInfo>>() {
        });
        for (OAuth2ClientInfo oAuth2ClientInfo : oAuth2ClientInfos) {
            doDelete("/api/oauth2/client/" + oAuth2ClientInfo.getId().getId().toString())
                    .andExpect(status().isOk());
        }
    }

    @Test
    public void testSaveOauth2Client() throws Exception {
        loginSysAdmin();
        List<OAuth2ClientInfo> oAuth2ClientInfos = doGetTyped("/api/oauth2/client/infos",  new TypeReference<List<OAuth2ClientInfo>>() {
        });
        assertThat(oAuth2ClientInfos).isEmpty();

        OAuth2Client oAuth2Client = validClientInfo(TenantId.SYS_TENANT_ID, "test google client");
        OAuth2Client savedOAuth2Client = doPost("/api/oauth2/client", oAuth2Client, OAuth2Client.class);

        List<OAuth2ClientInfo> oAuth2ClientInfos2 = doGetTyped("/api/oauth2/client/infos", new TypeReference<List<OAuth2ClientInfo>>() {
        });
        assertThat(oAuth2ClientInfos2).hasSize(1);
        assertThat(oAuth2ClientInfos2.get(0)).isEqualTo(new OAuth2ClientInfo(savedOAuth2Client));

        OAuth2Client retrievedOAuth2ClientInfo = doGet("/api/oauth2/client/{id}", OAuth2Client.class, savedOAuth2Client.getId().getId());
        assertThat(retrievedOAuth2ClientInfo).isEqualTo(savedOAuth2Client);

        doDelete("/api/oauth2/client/" + savedOAuth2Client.getId().getId());
        doGet("/api/oauth2/client/{id}", savedOAuth2Client.getId().getId())
                .andExpect(status().isNotFound());
    }

}

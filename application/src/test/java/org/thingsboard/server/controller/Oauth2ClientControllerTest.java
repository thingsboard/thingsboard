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
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.OAuth2Client;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.service.DaoSqlTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@DaoSqlTest
public class Oauth2ClientControllerTest extends AbstractControllerTest {

    static final TypeReference<PageData<OAuth2ClientInfo>> PAGE_DATA_OAUTH2_CLIENT_TYPE_REF = new TypeReference<>() {
    };

    @Before
    public void setUp() throws Exception {
        loginSysAdmin();
    }

    @After
    public void tearDown() throws Exception {
        PageData<OAuth2ClientInfo> pageData = doGetTypedWithPageLink("/api/oauth2/client/infos?", PAGE_DATA_OAUTH2_CLIENT_TYPE_REF, new PageLink(10, 0));
        for (OAuth2ClientInfo oAuth2ClientInfo : pageData.getData()) {
            doDelete("/api/oauth2/client/" + oAuth2ClientInfo.getId().getId().toString())
                    .andExpect(status().isOk());
        }
    }

    @Test
    public void testSaveOauth2Client() throws Exception {
        loginSysAdmin();
        PageData<OAuth2ClientInfo> pageData = doGetTypedWithPageLink("/api/oauth2/client/infos?", PAGE_DATA_OAUTH2_CLIENT_TYPE_REF, new PageLink(10, 0));
        assertThat(pageData.getData()).isEmpty();

        OAuth2Client oAuth2Client = createOauth2Client(TenantId.SYS_TENANT_ID, "test google client");
        OAuth2Client savedOAuth2Client = doPost("/api/oauth2/client", oAuth2Client, OAuth2Client.class);

        PageData<OAuth2ClientInfo> pageData2 = doGetTypedWithPageLink("/api/oauth2/client/infos?", PAGE_DATA_OAUTH2_CLIENT_TYPE_REF, new PageLink(10, 0));

        assertThat(pageData2.getData()).hasSize(1);
        assertThat(pageData2.getData().get(0)).isEqualTo(new OAuth2ClientInfo(savedOAuth2Client));

        OAuth2Client retrievedOAuth2ClientInfo = doGet("/api/oauth2/client/{id}", OAuth2Client.class, savedOAuth2Client.getId().getId());
        assertThat(retrievedOAuth2ClientInfo).isEqualTo(savedOAuth2Client);

        doDelete("/api/oauth2/client/" + savedOAuth2Client.getId().getId());
        doGet("/api/oauth2/client/{id}", savedOAuth2Client.getId().getId())
                .andExpect(status().isNotFound());
    }

}

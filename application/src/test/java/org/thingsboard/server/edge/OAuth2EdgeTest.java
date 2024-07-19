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
package org.thingsboard.server.edge;

import com.google.protobuf.AbstractMessage;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.oauth2.MapperType;
import org.thingsboard.server.common.data.oauth2.OAuth2CustomMapperConfig;
import org.thingsboard.server.common.data.oauth2.OAuth2MapperConfig;
import org.thingsboard.server.common.data.oauth2.OAuth2Client;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.OAuth2UpdateMsg;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

@DaoSqlTest
public class OAuth2EdgeTest extends AbstractEdgeTest {

    @Test
    public void testOAuth2Support() throws Exception {
        loginSysAdmin();

        // enable oauth
        edgeImitator.allowIgnoredTypes();
        edgeImitator.expectMessageAmount(1);
        OAuth2Client oAuth2Client = createDefaultOAuth2Info();
        oAuth2Client = doPost("/api/oauth2/config", oAuth2Client, OAuth2Client.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof OAuth2UpdateMsg);
        OAuth2UpdateMsg oAuth2ProviderUpdateMsg = (OAuth2UpdateMsg) latestMessage;
        OAuth2Client result = JacksonUtil.fromString(oAuth2ProviderUpdateMsg.getEntity(), OAuth2Client.class, true);
        Assert.assertEquals(oAuth2Client, result);

        // disable oauth support
        edgeImitator.expectMessageAmount(1);
        doPost("/api/oauth2/config", oAuth2Client, OAuth2Client.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof OAuth2UpdateMsg);
        oAuth2ProviderUpdateMsg = (OAuth2UpdateMsg) latestMessage;
        result = JacksonUtil.fromString(oAuth2ProviderUpdateMsg.getEntity(), OAuth2Client.class, true);
        Assert.assertEquals(oAuth2Client, result);

        edgeImitator.ignoreType(OAuth2UpdateMsg.class);
        loginTenantAdmin();
    }

    private OAuth2Client createDefaultOAuth2Info() {
        return validRegistrationInfo();
    }

    private OAuth2Client validRegistrationInfo() {
        OAuth2Client oAuth2Client = new OAuth2Client();
        oAuth2Client.setClientId(UUID.randomUUID().toString());
        oAuth2Client.setClientSecret(UUID.randomUUID().toString());
        oAuth2Client.setAuthorizationUri(UUID.randomUUID().toString());
        oAuth2Client.setAccessTokenUri(UUID.randomUUID().toString());
        oAuth2Client.setScope(Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
        oAuth2Client.setPlatforms(Collections.emptyList());
        oAuth2Client.setUserInfoUri(UUID.randomUUID().toString());
        oAuth2Client.setUserNameAttributeName(UUID.randomUUID().toString());
        oAuth2Client.setJwkSetUri(UUID.randomUUID().toString());
        oAuth2Client.setClientAuthenticationMethod(UUID.randomUUID().toString());
        oAuth2Client.setLoginButtonLabel(UUID.randomUUID().toString());
        oAuth2Client.setMapperConfig(
                        OAuth2MapperConfig.builder()
                                .type(MapperType.CUSTOM)
                                .custom(
                                        OAuth2CustomMapperConfig.builder()
                                                .url(UUID.randomUUID().toString())
                                                .build()
                                )
                                .build());
        return oAuth2Client;
    }

}

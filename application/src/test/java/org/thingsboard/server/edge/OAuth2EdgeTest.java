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

import com.google.common.collect.Lists;
import com.google.protobuf.AbstractMessage;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.oauth2.MapperType;
import org.thingsboard.server.common.data.oauth2.OAuth2CustomMapperConfig;
import org.thingsboard.server.common.data.oauth2.OAuth2DomainInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2Info;
import org.thingsboard.server.common.data.oauth2.OAuth2MapperConfig;
import org.thingsboard.server.common.data.oauth2.OAuth2ParamsInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2RegistrationInfo;
import org.thingsboard.server.common.data.oauth2.SchemeType;
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
        OAuth2Info oAuth2Info = createDefaultOAuth2Info();
        oAuth2Info = doPost("/api/oauth2/config", oAuth2Info, OAuth2Info.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof OAuth2UpdateMsg);
        OAuth2UpdateMsg oAuth2UpdateMsg = (OAuth2UpdateMsg) latestMessage;
        OAuth2Info result = JacksonUtil.fromString(oAuth2UpdateMsg.getEntity(), OAuth2Info.class, true);
        Assert.assertEquals(oAuth2Info, result);

        // disable oauth support
        edgeImitator.expectMessageAmount(1);
        oAuth2Info.setEnabled(false);
        oAuth2Info.setEdgeEnabled(false);
        doPost("/api/oauth2/config", oAuth2Info, OAuth2Info.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof OAuth2UpdateMsg);
        oAuth2UpdateMsg = (OAuth2UpdateMsg) latestMessage;
        result = JacksonUtil.fromString(oAuth2UpdateMsg.getEntity(), OAuth2Info.class, true);
        Assert.assertEquals(oAuth2Info, result);

        edgeImitator.ignoreType(OAuth2UpdateMsg.class);
        loginTenantAdmin();
    }

    private OAuth2Info createDefaultOAuth2Info() {
        return new OAuth2Info(true, true, Lists.newArrayList(
                OAuth2ParamsInfo.builder()
                        .domainInfos(Lists.newArrayList(
                                OAuth2DomainInfo.builder().name("domain").scheme(SchemeType.MIXED).build()
                        ))
                        .mobileInfos(Collections.emptyList())
                        .clientRegistrations(Lists.newArrayList(
                                validRegistrationInfo()
                        ))
                        .build()
        ));
    }

    private OAuth2RegistrationInfo validRegistrationInfo() {
        return OAuth2RegistrationInfo.builder()
                .clientId(UUID.randomUUID().toString())
                .clientSecret(UUID.randomUUID().toString())
                .authorizationUri(UUID.randomUUID().toString())
                .accessTokenUri(UUID.randomUUID().toString())
                .scope(Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString()))
                .platforms(Collections.emptyList())
                .userInfoUri(UUID.randomUUID().toString())
                .userNameAttributeName(UUID.randomUUID().toString())
                .jwkSetUri(UUID.randomUUID().toString())
                .clientAuthenticationMethod(UUID.randomUUID().toString())
                .loginButtonLabel(UUID.randomUUID().toString())
                .mapperConfig(
                        OAuth2MapperConfig.builder()
                                .type(MapperType.CUSTOM)
                                .custom(
                                        OAuth2CustomMapperConfig.builder()
                                                .url(UUID.randomUUID().toString())
                                                .build()
                                )
                                .build()
                )
                .build();
    }

}

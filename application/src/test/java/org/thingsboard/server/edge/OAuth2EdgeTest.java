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
package org.thingsboard.server.edge;

import com.google.protobuf.AbstractMessage;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.domain.Domain;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.MapperType;
import org.thingsboard.server.common.data.oauth2.OAuth2Client;
import org.thingsboard.server.common.data.oauth2.OAuth2CustomMapperConfig;
import org.thingsboard.server.common.data.oauth2.OAuth2MapperConfig;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.OAuth2ClientUpdateMsg;
import org.thingsboard.server.gen.edge.v1.OAuth2DomainUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@DaoSqlTest
public class OAuth2EdgeTest extends AbstractEdgeTest {

    @Test
    public void testOAuth2DomainSupport() throws Exception {
        loginSysAdmin();

        // enable oauth and save domain
        edgeImitator.allowIgnoredTypes();
        edgeImitator.expectMessageAmount(1);

        Domain savedDomain = doPost("/api/domain", constructDomain(), Domain.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof OAuth2DomainUpdateMsg);
        OAuth2DomainUpdateMsg oAuth2DomainUpdateMsg = (OAuth2DomainUpdateMsg) latestMessage;
        Domain result = JacksonUtil.fromString(oAuth2DomainUpdateMsg.getEntity(), Domain.class, true);
        Assert.assertEquals(savedDomain, result);

        // disable oauth support: no update of domain events is sending to Edge
        edgeImitator.expectMessageAmount(1);
        savedDomain.setPropagateToEdge(false);
        doPost("/api/domain", savedDomain, Domain.class);
        Assert.assertFalse(edgeImitator.waitForMessages(5));

        // delete domain
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/domain/" + savedDomain.getId().getId());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof OAuth2DomainUpdateMsg);
        oAuth2DomainUpdateMsg = (OAuth2DomainUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, oAuth2DomainUpdateMsg.getMsgType());
        Assert.assertEquals(savedDomain.getUuidId().getMostSignificantBits(), oAuth2DomainUpdateMsg.getIdMSB());
        Assert.assertEquals(savedDomain.getUuidId().getLeastSignificantBits(), oAuth2DomainUpdateMsg.getIdLSB());

        edgeImitator.ignoreType(OAuth2DomainUpdateMsg.class);
        edgeImitator.ignoreType(OAuth2ClientUpdateMsg.class);
        loginTenantAdmin();
    }

    @Test
    public void testOAuth2ClientSupport() throws Exception {
        loginSysAdmin();

        // enable oauth and save domain
        edgeImitator.allowIgnoredTypes();

        edgeImitator.expectMessageAmount(2);
        OAuth2Client savedOAuth2Client = validClientInfo(TenantId.SYS_TENANT_ID, "test edge google client");
        savedOAuth2Client = doPost("/api/oauth2/client", savedOAuth2Client, OAuth2Client.class);
        Domain savedDomain = doPost("/api/domain?oauth2ClientIds=" + savedOAuth2Client.getId().getId(), constructDomain(), Domain.class);

        Assert.assertTrue(edgeImitator.waitForMessages());
        Optional<OAuth2DomainUpdateMsg> oAuth2DomainUpdateMsgOpt = edgeImitator.findMessageByType(OAuth2DomainUpdateMsg.class);
        Assert.assertTrue(oAuth2DomainUpdateMsgOpt.isPresent());
        Domain result = JacksonUtil.fromString(oAuth2DomainUpdateMsgOpt.get().getEntity(), Domain.class, true);
        Assert.assertEquals(savedDomain, result);

        Optional<OAuth2ClientUpdateMsg> oAuth2ClientUpdateMsgOpt = edgeImitator.findMessageByType(OAuth2ClientUpdateMsg.class);
        Assert.assertTrue(oAuth2ClientUpdateMsgOpt.isPresent());
        OAuth2Client clientResult = JacksonUtil.fromString(oAuth2ClientUpdateMsgOpt.get().getEntity(), OAuth2Client.class, true);
        Assert.assertEquals(savedOAuth2Client, clientResult);

        // disable oauth support: no update of domain events and client events are sending to Edge
        edgeImitator.expectMessageAmount(1);
        savedDomain.setPropagateToEdge(false);
        doPost("/api/domain", savedDomain, Domain.class);
        Assert.assertFalse(edgeImitator.waitForMessages(5));

        edgeImitator.expectMessageAmount(1);
        savedOAuth2Client.setTitle("Updated title");
        doPost("/api/oauth2/client", savedOAuth2Client, OAuth2Client.class);
        Assert.assertFalse(edgeImitator.waitForMessages(5));

        // delete oauth2Client
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/oauth2/client/" + savedOAuth2Client.getId().getId());
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof OAuth2ClientUpdateMsg);
        OAuth2ClientUpdateMsg oAuth2ClientUpdateMsg = (OAuth2ClientUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, oAuth2ClientUpdateMsg.getMsgType());
        Assert.assertEquals(savedOAuth2Client.getUuidId().getMostSignificantBits(), oAuth2ClientUpdateMsg.getIdMSB());
        Assert.assertEquals(savedOAuth2Client.getUuidId().getLeastSignificantBits(), oAuth2ClientUpdateMsg.getIdLSB());

        // delete domain
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/domain/" + savedDomain.getId().getId());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof OAuth2DomainUpdateMsg);
        OAuth2DomainUpdateMsg oAuth2DomainUpdateMsg = (OAuth2DomainUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, oAuth2DomainUpdateMsg.getMsgType());
        Assert.assertEquals(savedDomain.getUuidId().getMostSignificantBits(), oAuth2DomainUpdateMsg.getIdMSB());
        Assert.assertEquals(savedDomain.getUuidId().getLeastSignificantBits(), oAuth2DomainUpdateMsg.getIdLSB());

        edgeImitator.ignoreType(OAuth2DomainUpdateMsg.class);
        edgeImitator.ignoreType(OAuth2ClientUpdateMsg.class);
        loginTenantAdmin();
    }

    private OAuth2Client validClientInfo(TenantId tenantId, String title) {
        OAuth2Client oAuth2Client = new OAuth2Client();
        oAuth2Client.setTenantId(tenantId);
        oAuth2Client.setTitle(title);
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
        oAuth2Client.setLoginButtonIcon(UUID.randomUUID().toString());
        oAuth2Client.setAdditionalInfo(JacksonUtil.newObjectNode().put(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
        oAuth2Client.setMapperConfig(
                OAuth2MapperConfig.builder()
                        .allowUserCreation(true)
                        .activateUser(true)
                        .type(MapperType.CUSTOM)
                        .custom(
                                OAuth2CustomMapperConfig.builder()
                                        .url(UUID.randomUUID().toString())
                                        .build()
                        )
                        .build());
        return oAuth2Client;
    }

    private Domain constructDomain() {
        Domain domain = new Domain();
        domain.setTenantId(TenantId.SYS_TENANT_ID);
        domain.setName("my.edge.domain");
        domain.setOauth2Enabled(true);
        domain.setPropagateToEdge(true);
        return domain;
    }

}

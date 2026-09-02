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
package org.thingsboard.server.client;

import org.junit.Test;
import org.thingsboard.client.model.MapperType;
import org.thingsboard.client.model.OAuth2BasicMapperConfig;
import org.thingsboard.client.model.OAuth2Client;
import org.thingsboard.client.model.OAuth2ClientInfo;
import org.thingsboard.client.model.OAuth2MapperConfig;
import org.thingsboard.client.model.PageDataOAuth2ClientInfo;
import org.thingsboard.client.model.PlatformType;
import org.thingsboard.client.model.TenantNameStrategyType;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@DaoSqlTest
public class Oauth2ApiClientTest extends AbstractApiClientTest {

    private OAuth2Client createOAuth2Client(String title, String clientId, String clientSecret) {
        OAuth2BasicMapperConfig basicConfig = new OAuth2BasicMapperConfig();
        basicConfig.setEmailAttributeKey("email");
        basicConfig.setFirstNameAttributeKey("given_name");
        basicConfig.setLastNameAttributeKey("family_name");
        basicConfig.setTenantNameStrategy(TenantNameStrategyType.DOMAIN);

        OAuth2MapperConfig mapperConfig = new OAuth2MapperConfig();
        mapperConfig.setType(MapperType.BASIC);
        mapperConfig.setAllowUserCreation(true);
        mapperConfig.setActivateUser(false);
        mapperConfig.setBasic(basicConfig);

        OAuth2Client oAuth2Client = new OAuth2Client();
        oAuth2Client.setTitle(title);
        oAuth2Client.setClientId(clientId);
        oAuth2Client.setClientSecret(clientSecret);
        oAuth2Client.setAuthorizationUri("https://accounts.google.com/o/oauth2/v2/auth");
        oAuth2Client.setAccessTokenUri("https://oauth2.googleapis.com/token");
        oAuth2Client.setScope(List.of("openid", "email", "profile"));
        oAuth2Client.setUserInfoUri("https://openidconnect.googleapis.com/v1/userinfo");
        oAuth2Client.setUserNameAttributeName("email");
        oAuth2Client.setClientAuthenticationMethod("POST");
        oAuth2Client.setLoginButtonLabel(title);
        oAuth2Client.setMapperConfig(mapperConfig);
        oAuth2Client.setPlatforms(List.of(PlatformType.WEB));

        return oAuth2Client;
    }

    @Test
    public void testOAuth2ClientLifecycle() throws Exception {
        long timestamp = System.currentTimeMillis();
        List<OAuth2Client> createdClients = new ArrayList<>();

        // create 5 OAuth2 clients
        for (int i = 0; i < 5; i++) {
            String title = TEST_PREFIX + "OAuth2_" + timestamp + "_" + i;
            OAuth2Client oAuth2Client = createOAuth2Client(title,
                    "client_id_" + timestamp + "_" + i,
                    "client_secret_" + timestamp + "_" + i);

            OAuth2Client created = client.saveOAuth2Client(oAuth2Client);
            assertNotNull(created);
            assertNotNull(created.getId());
            assertEquals(title, created.getTitle());
            assertEquals("POST", created.getClientAuthenticationMethod());
            assertNotNull(created.getMapperConfig());
            assertEquals(MapperType.BASIC, created.getMapperConfig().getType());

            createdClients.add(created);
        }

        // list tenant OAuth2 client infos
        PageDataOAuth2ClientInfo clientInfos = client.findOAuth2ClientInfos(100, 0,
                TEST_PREFIX + "OAuth2_" + timestamp, null, null);
        assertNotNull(clientInfos);
        assertEquals(5, clientInfos.getData().size());

        // get OAuth2 client by id
        OAuth2Client searchClient = createdClients.get(2);
        OAuth2Client fetchedClient = client.getOAuth2ClientById(searchClient.getId().getId());
        assertEquals(searchClient.getTitle(), fetchedClient.getTitle());
        assertEquals(searchClient.getClientId(), fetchedClient.getClientId());
        assertEquals(searchClient.getAuthorizationUri(), fetchedClient.getAuthorizationUri());
        assertEquals(3, fetchedClient.getScope().size());

        // fetch client infos by ids
        List<String> idsToFetch = List.of(
                createdClients.get(0).getId().getId().toString(),
                createdClients.get(1).getId().getId().toString()
        );
        List<OAuth2ClientInfo> fetchedInfos = client.findTenantOAuth2ClientInfosByIds(idsToFetch);
        assertEquals(2, fetchedInfos.size());

        // update OAuth2 client
        OAuth2Client clientToUpdate = client.getOAuth2ClientById(createdClients.get(3).getId().getId());
        clientToUpdate.setTitle(clientToUpdate.getTitle() + "_updated");
        clientToUpdate.setLoginButtonLabel("Updated Login");
        clientToUpdate.setPlatforms(List.of(PlatformType.WEB, PlatformType.ANDROID));
        OAuth2Client updatedClient = client.saveOAuth2Client(clientToUpdate);
        assertEquals(clientToUpdate.getTitle(), updatedClient.getTitle());
        assertEquals("Updated Login", updatedClient.getLoginButtonLabel());
        assertEquals(2, updatedClient.getPlatforms().size());

        // delete OAuth2 client
        UUID clientToDeleteId = createdClients.get(0).getId().getId();
        client.deleteOauth2Client(clientToDeleteId);

        // verify deletion
        assertReturns404(() ->
                client.getOAuth2ClientById(clientToDeleteId)
        );

        PageDataOAuth2ClientInfo clientsAfterDelete = client.findOAuth2ClientInfos(100, 0,
                TEST_PREFIX + "OAuth2_" + timestamp, null, null);
        assertEquals(4, clientsAfterDelete.getData().size());
    }

}

// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.client;

import org.junit.Test;
import org.thingsboard.client.api.ThingsboardApi.DeleteOauth2ClientArgs;
import org.thingsboard.client.api.ThingsboardApi.FindOAuth2ClientInfosArgs;
import org.thingsboard.client.api.ThingsboardApi.FindTenantOAuth2ClientInfosByIdsArgs;
import org.thingsboard.client.api.ThingsboardApi.GetOAuth2ClientByIdArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveOAuth2ClientArgs;
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

            OAuth2Client created = client.saveOAuth2Client(SaveOAuth2ClientArgs.builder()
                    .oauth2Client(oAuth2Client)
                    .build());
            assertNotNull(created);
            assertNotNull(created.getId());
            assertEquals(title, created.getTitle());
            assertEquals("POST", created.getClientAuthenticationMethod());
            assertNotNull(created.getMapperConfig());
            assertEquals(MapperType.BASIC, created.getMapperConfig().getType());

            createdClients.add(created);
        }

        // list tenant OAuth2 client infos
        PageDataOAuth2ClientInfo clientInfos = client.findOAuth2ClientInfos(FindOAuth2ClientInfosArgs.builder()
                .pageSize(100)
                .page(0)
                .textSearch(TEST_PREFIX + "OAuth2_" + timestamp)
                .build());
        assertNotNull(clientInfos);
        assertEquals(5, clientInfos.getData().size());

        // get OAuth2 client by id
        OAuth2Client searchClient = createdClients.get(2);
        OAuth2Client fetchedClient = client.getOAuth2ClientById(GetOAuth2ClientByIdArgs.builder()
                .id(searchClient.getId().getId())
                .build());
        assertEquals(searchClient.getTitle(), fetchedClient.getTitle());
        assertEquals(searchClient.getClientId(), fetchedClient.getClientId());
        assertEquals(searchClient.getAuthorizationUri(), fetchedClient.getAuthorizationUri());
        assertEquals(3, fetchedClient.getScope().size());

        // fetch client infos by ids
        List<String> idsToFetch = List.of(
                createdClients.get(0).getId().getId().toString(),
                createdClients.get(1).getId().getId().toString()
        );
        List<OAuth2ClientInfo> fetchedInfos = client.findTenantOAuth2ClientInfosByIds(FindTenantOAuth2ClientInfosByIdsArgs.builder()
                .clientIds(idsToFetch)
                .build());
        assertEquals(2, fetchedInfos.size());

        // update OAuth2 client
        OAuth2Client clientToUpdate = client.getOAuth2ClientById(GetOAuth2ClientByIdArgs.builder()
                .id(createdClients.get(3).getId().getId())
                .build());
        clientToUpdate.setTitle(clientToUpdate.getTitle() + "_updated");
        clientToUpdate.setLoginButtonLabel("Updated Login");
        clientToUpdate.setPlatforms(List.of(PlatformType.WEB, PlatformType.ANDROID));
        OAuth2Client updatedClient = client.saveOAuth2Client(SaveOAuth2ClientArgs.builder()
                .oauth2Client(clientToUpdate)
                .build());
        assertEquals(clientToUpdate.getTitle(), updatedClient.getTitle());
        assertEquals("Updated Login", updatedClient.getLoginButtonLabel());
        assertEquals(2, updatedClient.getPlatforms().size());

        // delete OAuth2 client
        UUID clientToDeleteId = createdClients.get(0).getId().getId();
        client.deleteOauth2Client(DeleteOauth2ClientArgs.builder()
                .id(clientToDeleteId)
                .build());

        // verify deletion
        assertReturns404(() ->
                client.getOAuth2ClientById(GetOAuth2ClientByIdArgs.builder()
                        .id(clientToDeleteId)
                        .build())
        );

        PageDataOAuth2ClientInfo clientsAfterDelete = client.findOAuth2ClientInfos(FindOAuth2ClientInfosArgs.builder()
                .pageSize(100)
                .page(0)
                .textSearch(TEST_PREFIX + "OAuth2_" + timestamp)
                .build());
        assertEquals(4, clientsAfterDelete.getData().size());
    }

}

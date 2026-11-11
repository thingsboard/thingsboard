// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.security.auth.oauth2;

import com.sun.net.httpserver.HttpServer;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.thingsboard.common.util.SsrfProtectionValidator;
import org.thingsboard.server.common.data.oauth2.MapperType;
import org.thingsboard.server.common.data.oauth2.OAuth2Client;
import org.thingsboard.server.common.data.oauth2.OAuth2CustomMapperConfig;
import org.thingsboard.server.common.data.oauth2.OAuth2MapperConfig;
import org.thingsboard.server.dao.oauth2.OAuth2User;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CustomOAuth2ClientMapperTest {

    private static final String PROVIDER_ACCESS_TOKEN = "provider-access-token";

    private final List<Map<String, String>> receivedHeaders = new CopyOnWriteArrayList<>();
    private final CustomOAuth2ClientMapper mapper = new CustomOAuth2ClientMapper() {
        @Override
        protected SecurityUser getOrCreateSecurityUserFromOAuth2User(OAuth2User oauth2User, OAuth2Client oAuth2Client) {
            return null;
        }
    };

    private HttpServer server;
    private boolean uriValidationEnabled;
    private OAuth2AuthenticationToken token;

    @BeforeEach
    public void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/mapper", exchange -> {
            Map<String, String> headers = new HashMap<>();
            String authorization = exchange.getRequestHeaders().getFirst("Authorization");
            if (authorization != null) {
                headers.put("Authorization", authorization);
            }
            String providerToken = exchange.getRequestHeaders().getFirst(PROVIDER_ACCESS_TOKEN);
            if (providerToken != null) {
                headers.put(PROVIDER_ACCESS_TOKEN, providerToken);
            }
            receivedHeaders.add(headers);
            byte[] body = "{\"email\":\"user@example.com\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        uriValidationEnabled = SsrfProtectionValidator.isEnabled();
        SsrfProtectionValidator.setEnabled(false);

        token = mock(OAuth2AuthenticationToken.class);
        when(token.getPrincipal()).thenReturn(new DefaultOAuth2User(List.of(), Map.of("email", "user@example.com"), "email"));
    }

    @AfterEach
    public void tearDown() {
        server.stop(0);
        SsrfProtectionValidator.setEnabled(uriValidationEnabled);
    }

    @Test
    public void sendsOwnCredentialsAndTokenWhenConfigured() {
        login(client("user", "pass", true), "token-1");
        login(client("user2", "pass2", true), "token-2");

        assertThat(receivedHeaders).containsExactly(
                Map.of("Authorization", basic("user", "pass"), PROVIDER_ACCESS_TOKEN, "token-1"),
                Map.of("Authorization", basic("user2", "pass2"), PROVIDER_ACCESS_TOKEN, "token-2"));
    }

    @Test
    public void sendsNoAuthorizationHeaderWhenClientHasNoCredentials() {
        login(client("user", "pass", true), "token-1");
        login(client(null, null, false), "token-2");

        assertThat(receivedHeaders).hasSize(2);
        assertThat(receivedHeaders.get(1)).isEmpty();
    }

    @Test
    public void sendsNoProviderTokenWhenSendTokenIsOff() {
        login(client("user", "pass", true), "token-1");
        login(client("user", "pass", false), "token-2");

        assertThat(receivedHeaders).hasSize(2);
        assertThat(receivedHeaders.get(1)).isEqualTo(Map.of("Authorization", basic("user", "pass")));
    }

    private void login(OAuth2Client client, String providerAccessToken) {
        mapper.getOrCreateUserByClientPrincipal(mock(HttpServletRequest.class), token, providerAccessToken, client);
    }

    private OAuth2Client client(String username, String password, boolean sendToken) {
        OAuth2CustomMapperConfig custom = OAuth2CustomMapperConfig.builder()
                .url("http://127.0.0.1:" + server.getAddress().getPort() + "/mapper")
                .username(username)
                .password(password)
                .sendToken(sendToken)
                .build();
        OAuth2Client client = new OAuth2Client();
        client.setMapperConfig(OAuth2MapperConfig.builder().type(MapperType.CUSTOM).custom(custom).build());
        return client;
    }

    private static String basic(String username, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

}

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
package org.thingsboard.monitoring.client;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.thingsboard.server.common.data.User;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TbClientTest {

    private TbClient newClient(TbClient.AuthMode authMode, String apiKey, String username, String password) {
        return new TbClient("http://localhost:8080", 5000, authMode, apiKey, username, password);
    }

    private TbClient newClient(TbClient.AuthMode authMode, String apiKey) {
        return newClient(authMode, apiKey, "", "");
    }

    @Test
    void loginMode_getWsCredentialCallsUsernamePasswordLogin() {
        TbClient client = spy(newClient(TbClient.AuthMode.LOGIN, "unused-key", "tenant@thingsboard.org", "tenant"));
        doReturn("jwt-token").when(client).getToken();
        doNothing().when(client).login("tenant@thingsboard.org", "tenant");

        String result = client.getWsCredential();

        verify(client).login("tenant@thingsboard.org", "tenant");
        assertThat(result).isEqualTo("jwt-token");
    }

    @Test
    void apiKeyMode_getWsCredentialCallsGetUserNotLogin() {
        TbClient client = spy(newClient(TbClient.AuthMode.API_KEY, "my-api-key"));
        doReturn(Optional.of(mock(User.class))).when(client).getUser();

        String result = client.getWsCredential();

        verify(client, never()).login(anyString(), anyString());
        verify(client).getUser();
        assertThat(result).isEqualTo("my-api-key");
    }

    @Test
    void apiKeyMode_getUserReturnsEmpty_getWsCredentialThrows() {
        TbClient client = spy(newClient(TbClient.AuthMode.API_KEY, "my-api-key"));
        doReturn(Optional.empty()).when(client).getUser();

        assertThatIllegalStateException().isThrownBy(client::getWsCredential);
    }

    @Test
    void apiKeyMode_getUserThrowsHttpError_getWsCredentialPropagates() {
        TbClient client = spy(newClient(TbClient.AuthMode.API_KEY, "my-api-key"));
        HttpClientErrorException httpError = HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "Unauthorized", null, null, null);
        doThrow(httpError).when(client).getUser();

        assertThatThrownBy(client::getWsCredential).isSameAs(httpError);
    }

    @Test
    void apiKeyMode_blankApiKey_constructorThrows() {
        assertThatIllegalStateException().isThrownBy(() -> newClient(TbClient.AuthMode.API_KEY, ""));
    }

    @Test
    void loginMode_blankUsername_constructorThrows() {
        assertThatIllegalStateException().isThrownBy(() -> newClient(TbClient.AuthMode.LOGIN, "unused-key", "", "tenant"));
    }

    @Test
    void loginMode_blankPassword_constructorThrows() {
        assertThatIllegalStateException().isThrownBy(() -> newClient(TbClient.AuthMode.LOGIN, "unused-key", "tenant@thingsboard.org", ""));
    }

    @Test
    void apiKeyMode_getWsCredential_sendsApiKeyAuthorizationHeaderNotBearer() {
        TbClient client = newClient(TbClient.AuthMode.API_KEY, "my-api-key");
        MockRestServiceServer server = MockRestServiceServer.createServer(client.getRestTemplate());
        server.expect(requestTo("http://localhost:8080/api/auth/user"))
                .andExpect(header("X-Authorization", "ApiKey my-api-key"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        client.getWsCredential();

        server.verify();
    }

}

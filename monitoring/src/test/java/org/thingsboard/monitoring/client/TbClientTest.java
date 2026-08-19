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
import org.thingsboard.server.common.data.User;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class TbClientTest {

    @Test
    public void loginMode_logInCallsUsernamePasswordLogin() {
        TbClient client = spy(new TbClient("http://localhost:8080", 5000, TbClient.AuthMode.LOGIN, "unused-key"));
        doReturn("jwt-token").when(client).getToken();
        org.springframework.test.util.ReflectionTestUtils.setField(client, "username", "tenant@thingsboard.org");
        org.springframework.test.util.ReflectionTestUtils.setField(client, "password", "tenant");
        org.mockito.Mockito.doNothing().when(client).login("tenant@thingsboard.org", "tenant");

        String result = client.logIn();

        verify(client).login("tenant@thingsboard.org", "tenant");
        assertThat(result).isEqualTo("jwt-token");
    }

    @Test
    public void apiKeyMode_logInCallsGetUserNotLogin() {
        TbClient client = spy(new TbClient("http://localhost:8080", 5000, TbClient.AuthMode.API_KEY, "my-api-key"));
        doReturn(Optional.of(mock(User.class))).when(client).getUser();

        String result = client.logIn();

        verify(client, never()).login(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
        verify(client).getUser();
        assertThat(result).isEqualTo("my-api-key");
    }

    @Test
    public void apiKeyMode_getUserThrows_logInPropagates() {
        TbClient client = spy(new TbClient("http://localhost:8080", 5000, TbClient.AuthMode.API_KEY, "my-api-key"));
        doReturn(Optional.empty()).when(client).getUser();

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, client::logIn);
    }

}

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
import org.mockito.ArgumentCaptor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.monitoring.data.cmd.CmdsWrapper;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class WsClientTest {

    private WsClient newClient() throws Exception {
        WsClient client = spy(new WsClient(new URI("ws://localhost:8080/api/ws"), 1000));
        doNothing().when(client).send(anyString());
        return client;
    }

    @Test
    public void authenticate_apiKeyMode_sendsAuthCmdWithApiKeySet() throws Exception {
        WsClient client = newClient();

        client.authenticate(TbClient.AuthMode.API_KEY, "my-api-key");

        String json = captureSentJson(client);
        // wire-level assertions: the unused field must be omitted entirely, not just null-valued
        // (some deployed server versions predate the apiKey field and reject the whole message
        // under FAIL_ON_UNKNOWN_PROPERTIES if it's present at all, even as an explicit null -
        // round-tripping through CmdsWrapper alone can't catch that, since "apiKey":null and an
        // omitted apiKey key deserialize to the same Java object).
        assertThat(json).contains("\"apiKey\"");
        assertThat(json).doesNotContain("\"token\"");

        CmdsWrapper wrapper = JacksonUtil.fromString(json, CmdsWrapper.class);
        assertThat(wrapper.getAuthCmd()).isNotNull();
        assertThat(wrapper.getAuthCmd().getApiKey()).isEqualTo("my-api-key");
        assertThat(wrapper.getAuthCmd().getToken()).isNull();
        assertThat(wrapper.getCmds()).isNull();
    }

    @Test
    public void authenticate_loginMode_sendsAuthCmdWithTokenSet() throws Exception {
        WsClient client = newClient();

        client.authenticate(TbClient.AuthMode.LOGIN, "jwt-token");

        String json = captureSentJson(client);
        // wire-level assertions: see comment in authenticate_apiKeyMode_sendsAuthCmdWithApiKeySet
        assertThat(json).contains("\"token\"");
        assertThat(json).doesNotContain("\"apiKey\"");

        CmdsWrapper wrapper = JacksonUtil.fromString(json, CmdsWrapper.class);
        assertThat(wrapper.getAuthCmd()).isNotNull();
        assertThat(wrapper.getAuthCmd().getToken()).isEqualTo("jwt-token");
        assertThat(wrapper.getAuthCmd().getApiKey()).isNull();
        assertThat(wrapper.getCmds()).isNull();
    }

    @Test
    public void subscribeForTelemetry_sendsCmdsListWithEntityDataTypeDiscriminator() throws Exception {
        WsClient client = newClient();

        client.subscribeForTelemetry(List.of(UUID.randomUUID()), List.of("temperature"));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(client).send(captor.capture());
        String json = captor.getValue();

        assertThat(json).doesNotContain("entityDataCmds");
        assertThat(json).contains("\"type\":\"ENTITY_DATA\"");

        CmdsWrapper wrapper = JacksonUtil.fromString(json, CmdsWrapper.class);
        assertThat(wrapper.getAuthCmd()).isNull();
        assertThat(wrapper.getCmds()).hasSize(1);
        assertThat(wrapper.getCmds().get(0).getLatestCmd().getKeys()).hasSize(1);
    }

    private String captureSentJson(WsClient client) {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(client).send(captor.capture());
        return captor.getValue();
    }

}

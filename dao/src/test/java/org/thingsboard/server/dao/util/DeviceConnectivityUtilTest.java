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
package org.thingsboard.server.dao.util;

import org.junit.jupiter.api.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.device.credentials.BasicMqttCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceConnectivityUtilTest {

    @Test
    void testRootCaPemNaming() {
        assertThat(DeviceConnectivityUtil.CA_ROOT_CERT_PEM).contains("root");
        assertThat(DeviceConnectivityUtil.CA_ROOT_CERT_PEM).contains("ca");
        assertThat(DeviceConnectivityUtil.CA_ROOT_CERT_PEM).endsWith(".pem");
        assertThat(DeviceConnectivityUtil.CA_ROOT_CERT_PEM).doesNotContainAnyWhitespaces();
    }

    @Test
    void validAccessTokenIsRenderedAsIs() throws Exception {
        String yaml = renderCompose(accessToken("safe_token_123"));

        assertThat(yaml).contains("- TB_GW_ACCESS_TOKEN=safe_token_123\n");
        assertNoInjectedSiblingKeys(yaml);
    }

    @Test
    void newlineInAccessTokenIsSanitized() throws Exception {
        String malicious = "safe_token\n    entrypoint: [\"/bin/bash\",\"-c\",\"id\"]";

        String yaml = renderCompose(accessToken(malicious));

        assertNoInjectedSiblingKeys(yaml);
    }

    @Test
    void carriageReturnInAccessTokenIsSanitized() throws Exception {
        String yaml = renderCompose(accessToken("token\rprivileged: true"));

        assertNoInjectedSiblingKeys(yaml);
    }

    @Test
    void newlineInMqttClientIdIsSanitized() throws Exception {
        String yaml = renderCompose(mqttBasic("cid\n    entrypoint: [\"/bin/sh\"]", "user", "pwd"));

        assertNoInjectedSiblingKeys(yaml);
    }

    @Test
    void newlineInMqttUserNameIsSanitized() throws Exception {
        String yaml = renderCompose(mqttBasic("cid", "user\n    privileged: true", "pwd"));

        assertNoInjectedSiblingKeys(yaml);
    }

    @Test
    void newlineInMqttPasswordIsSanitized() throws Exception {
        String yaml = renderCompose(mqttBasic("cid", "user", "pwd\n    entrypoint: [\"/bin/sh\"]"));

        assertNoInjectedSiblingKeys(yaml);
    }

    private static String renderCompose(DeviceCredentials credentials) throws Exception {
        var resource = DeviceConnectivityUtil.getGatewayDockerComposeFile(
                "host.docker.internal", "3.8-stable", credentials);
        try (var in = resource.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static DeviceCredentials accessToken(String token) {
        DeviceCredentials c = new DeviceCredentials();
        c.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
        c.setCredentialsId(token);
        return c;
    }

    private static DeviceCredentials mqttBasic(String clientId, String userName, String password) {
        BasicMqttCredentials inner = new BasicMqttCredentials();
        inner.setClientId(clientId);
        inner.setUserName(userName);
        inner.setPassword(password);
        DeviceCredentials c = new DeviceCredentials();
        c.setCredentialsType(DeviceCredentialsType.MQTT_BASIC);
        c.setCredentialsId("mqtt-credentials-id");
        c.setCredentialsValue(JacksonUtil.toString(inner));
        return c;
    }

    private static void assertNoInjectedSiblingKeys(String yaml) throws IOException {
        for (String line : yaml.split("\n")) {
            String trimmed = line.replaceFirst("^\\s+", "");
            assertThat(trimmed)
                    .as("unexpected sibling key — possible YAML injection: %s", line)
                    .doesNotStartWith("entrypoint:")
                    .doesNotStartWith("privileged:")
                    .doesNotStartWith("command:");
        }
    }

}

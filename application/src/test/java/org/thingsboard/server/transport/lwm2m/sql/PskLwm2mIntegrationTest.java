/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.transport.lwm2m.sql;

import org.eclipse.leshan.client.object.Security;
import org.eclipse.leshan.core.util.Hex;
import org.junit.Test;
import org.thingsboard.server.common.data.device.credentials.lwm2m.PSKClientCredentials;
import org.thingsboard.server.transport.lwm2m.AbstractLwM2MIntegrationTest;

import java.nio.charset.StandardCharsets;

import static org.eclipse.leshan.client.object.Security.psk;

public class PskLwm2mIntegrationTest extends AbstractLwM2MIntegrationTest {

    @Test
    public void testConnectWithPSKAndObserveTelemetry() throws Exception {
        String pskIdentity = "SOME_PSK_ID";
        String pskKey = "73656372657450534b73656372657450";
        PSKClientCredentials clientCredentials = new PSKClientCredentials();
        clientCredentials.setEndpoint(ENDPOINT);
        clientCredentials.setKey(pskKey);
        clientCredentials.setIdentity(pskIdentity);
        Security security = psk(SECURE_URI,
                123,
                pskIdentity.getBytes(StandardCharsets.UTF_8),
                Hex.decodeHex(pskKey.toCharArray()));
        super.basicTestConnectionObserveTelemetry(security, clientCredentials, SECURE_COAP_CONFIG, ENDPOINT);
    }
}

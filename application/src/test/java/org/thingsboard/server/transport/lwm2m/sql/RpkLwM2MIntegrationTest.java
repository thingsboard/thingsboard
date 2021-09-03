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
import org.thingsboard.server.common.data.device.credentials.lwm2m.RPKClientCredentials;
import org.thingsboard.server.transport.lwm2m.AbstractLwM2MIntegrationTest;

import static org.eclipse.leshan.client.object.Security.rpk;

public class RpkLwM2MIntegrationTest extends AbstractLwM2MIntegrationTest {

    @Test
    public void testConnectWithRPKAndObserveTelemetry() throws Exception {
        RPKClientCredentials rpkClientCredentials = new RPKClientCredentials();
        rpkClientCredentials.setEndpoint(ENDPOINT);
        rpkClientCredentials.setKey(Hex.encodeHexString(clientPublicKey.getEncoded()));
        Security security = rpk(SECURE_URI,
                123,
                clientPublicKey.getEncoded(),
                clientPrivateKey.getEncoded(),
                serverX509Cert.getPublicKey().getEncoded());
        super.basicTestConnectionObserveTelemetry(security, rpkClientCredentials, SECURE_COAP_CONFIG, ENDPOINT);
    }

}

/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.transport.lwm2m.security.sql;

import org.eclipse.leshan.client.object.Security;
import org.junit.Test;
import org.thingsboard.server.common.data.device.credentials.lwm2m.RPKClientCredential;
import org.thingsboard.server.transport.lwm2m.security.AbstractSecurityLwM2MIntegrationTest;

import org.apache.commons.codec.binary.Base64;;

import static org.eclipse.leshan.client.object.Security.rpk;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.SECURE_COAP_CONFIG;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.SECURE_URI;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.SHORT_SERVER_ID;

public class RpkLwM2MIntegrationTest extends AbstractSecurityLwM2MIntegrationTest {

    @Test
    public void testConnectWithRPKAndObserveTelemetry() throws Exception {
        RPKClientCredential rpkClientCredentials = new RPKClientCredential();
        rpkClientCredentials.setEndpoint(CLIENT_ENDPOINT_RPK);
        rpkClientCredentials.setKey(new String(Base64.encodeBase64(clientPublicKeyFromCertTrust.getEncoded())));
        Security security = rpk(SECURE_URI,
                SHORT_SERVER_ID,
                clientPublicKeyFromCertTrust.getEncoded(),
                clientPrivateKeyFromCertTrust.getEncoded(),
                serverPublicKeyFromCert.getEncoded());
        super.basicTestConnectionObserveTelemetry(security, rpkClientCredentials, SECURE_COAP_CONFIG, CLIENT_ENDPOINT_RPK);
    }
}

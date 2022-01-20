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
import org.thingsboard.server.common.data.device.credentials.lwm2m.X509ClientCredential;
import org.thingsboard.server.transport.lwm2m.security.AbstractSecurityLwM2MIntegrationTest;

import static org.eclipse.leshan.client.object.Security.x509;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.SECURE_COAP_CONFIG;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.SECURE_URI;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.SHORT_SERVER_ID;

public class X509_TrustLwM2MIntegrationTest extends AbstractSecurityLwM2MIntegrationTest {

    @Test
    public void testConnectAndObserveTelemetry() throws Exception {
        X509ClientCredential credentials = new X509ClientCredential();
        credentials.setEndpoint(CLIENT_ENDPOINT_X509_TRUST);
        Security security = x509(SECURE_URI,
                SHORT_SERVER_ID,
                clientX509CertTrust.getEncoded(),
                clientPrivateKeyFromCertTrust.getEncoded(),
                serverX509Cert.getEncoded());
        super.basicTestConnectionObserveTelemetry(security, credentials, SECURE_COAP_CONFIG, CLIENT_ENDPOINT_X509_TRUST);
    }

}

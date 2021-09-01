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
import org.junit.Ignore;
import org.junit.Test;
import org.thingsboard.server.common.data.device.credentials.lwm2m.X509ClientCredentials;
import org.thingsboard.server.common.transport.util.SslUtil;
import org.thingsboard.server.transport.lwm2m.AbstractLwM2MIntegrationTest;

import static org.eclipse.leshan.client.object.Security.x509;

public class X509LwM2MIntegrationTest extends AbstractLwM2MIntegrationTest {

    @Test
    public void testConnectAndObserveTelemetry() throws Exception {
        X509ClientCredentials credentials = new X509ClientCredentials();
        credentials.setEndpoint(ENDPOINT);
        Security security = x509(SECURE_URI,
                123,
                clientX509Cert.getEncoded(),
                clientPrivateKeyFromCert.getEncoded(),
                serverX509Cert.getEncoded());
        super.basicTestConnectionObserveTelemetry(security, credentials, SECURE_COAP_CONFIG, ENDPOINT);
    }

    @Ignore //See LwM2mClientContextImpl.unregister
    @Test
    public void testConnectWithCertAndObserveTelemetry() throws Exception {
        X509ClientCredentials credentials = new X509ClientCredentials();
        credentials.setEndpoint(ENDPOINT);
        credentials.setCert(SslUtil.getCertificateString(clientX509CertNotTrusted));
        Security security = x509(SECURE_URI,
                123,
                clientX509CertNotTrusted.getEncoded(),
                clientPrivateKeyFromCert.getEncoded(),
                serverX509Cert.getEncoded());
        super.basicTestConnectionObserveTelemetry(security, credentials, SECURE_COAP_CONFIG, ENDPOINT);
    }

}

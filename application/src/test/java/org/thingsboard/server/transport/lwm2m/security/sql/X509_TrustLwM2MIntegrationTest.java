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
package org.thingsboard.server.transport.lwm2m.security.sql;

import org.eclipse.leshan.client.object.Security;
import org.junit.Test;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MDeviceCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.X509ClientCredential;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.transport.lwm2m.security.AbstractSecurityLwM2MIntegrationTest;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import static org.eclipse.leshan.client.object.Security.x509;
import static org.eclipse.leshan.client.object.Security.x509Bootstrap;
import static org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MSecurityMode.X509;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_REGISTRATION_SUCCESS;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.BOTH;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.NONE;

public class X509_TrustLwM2MIntegrationTest extends AbstractSecurityLwM2MIntegrationTest {

    //Lwm2m only
    @Test
    public void testWithX509TrustConnectLwm2mSuccess() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_X509_TRUST;
        X509Certificate certificate = clientX509CertTrust;
        PrivateKey privateKey = clientPrivateKeyFromCertTrust;
        X509ClientCredential clientCredentials = new X509ClientCredential();
        clientCredentials.setEndpoint(clientEndpoint);
        clientCredentials.setCert("");
        Security security = x509(SECURE_URI,
                shortServerId,
                certificate.getEncoded(),
                privateKey.getEncoded(),
                serverX509Cert.getEncoded());
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITHOUT_PARAMS, getBootstrapServerCredentialsSecure(X509, NONE));
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsSecure(clientCredentials, privateKey, certificate, X509, false);
        this.basicTestConnection(security,
                null,
                deviceCredentials,
                clientEndpoint,
                transportConfiguration,
                "await on client state (X509_Trust_Lwm2m)",
                expectedStatusesRegistrationLwm2mSuccess,
                true,
                ON_REGISTRATION_SUCCESS,
                true);
    }

    // Bootstrap + Lwm2m
    @Test
    public void testWithX509TrustConnectBsSuccess_UpdateTwoSectionsBootstrapAndLm2m_ConnectLwm2mSuccess() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_X509_TRUST;
        X509Certificate certificate = clientX509CertTrust;
        PrivateKey privateKey = clientPrivateKeyFromCertTrust;
        X509ClientCredential clientCredentials = new X509ClientCredential();
        clientCredentials.setEndpoint(clientEndpoint);
        clientCredentials.setCert("");
        Security security = x509Bootstrap(SECURE_URI_BS,
                certificate.getEncoded(),
                privateKey.getEncoded(),
                serverX509CertBs.getEncoded());
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITHOUT_PARAMS, getBootstrapServerCredentialsSecure(X509, BOTH));
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsSecure(clientCredentials, privateKey, certificate, X509, false);
        this.basicTestConnection(security,
                null,
                deviceCredentials,
                clientEndpoint,
                transportConfiguration,
                "await on client state (X509Trust two section)",
                expectedStatusesRegistrationBsSuccess,
                false,
                ON_REGISTRATION_SUCCESS,
                true);
    }
}

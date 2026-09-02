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

import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.codec.binary.Base64;
import org.eclipse.leshan.client.object.Security;
import org.eclipse.leshan.core.util.Hex;
import org.junit.Test;
import org.springframework.test.web.servlet.MvcResult;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MDeviceCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.RPKClientCredential;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.transport.lwm2m.security.AbstractSecurityLwM2MIntegrationTest;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import static org.eclipse.leshan.client.object.Security.rpk;
import static org.eclipse.leshan.client.object.Security.rpkBootstrap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MSecurityMode.PSK;
import static org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MSecurityMode.RPK;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_REGISTRATION_SUCCESS;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.BOTH;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.NONE;

public class RpkLwM2MIntegrationTest extends AbstractSecurityLwM2MIntegrationTest {

    //Lwm2m only
    @Test
    public void testWithRpkConnectLwm2mSuccess() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_RPK;
        X509Certificate certificate = clientX509CertTrust;
        PrivateKey privateKey = clientPrivateKeyFromCertTrust;
        RPKClientCredential clientCredentials = new RPKClientCredential();
        clientCredentials.setEndpoint(clientEndpoint);
        clientCredentials.setKey(Base64.encodeBase64String(certificate.getPublicKey().getEncoded()));
        Security security = rpk(SECURE_URI,
                shortServerId,
                certificate.getPublicKey().getEncoded(),
                privateKey.getEncoded(),
                serverX509Cert.getPublicKey().getEncoded());
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITHOUT_PARAMS, getBootstrapServerCredentialsSecure(RPK, NONE));
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsSecure(clientCredentials, privateKey, certificate, RPK, false);
        this.basicTestConnection(security, null,
                deviceCredentials,
                clientEndpoint,
                transportConfiguration,
                "await on client state (Rpk_Lwm2m)",
                expectedStatusesRegistrationLwm2mSuccess,
                true,
                ON_REGISTRATION_SUCCESS,
                true);
    }

    @Test
    public void testWithRpkValidationPublicKeyBase64format_BAD_REQUEST() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_RPK + "BadPublicKey";
        X509Certificate certificate = clientX509CertTrust;
        PrivateKey privateKey = clientPrivateKeyFromCertTrust;
        RPKClientCredential clientCredentials = new RPKClientCredential();
        clientCredentials.setEndpoint(clientEndpoint);
        clientCredentials.setKey(Hex.encodeHexString(certificate.getPublicKey().getEncoded()));
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITHOUT_PARAMS, getBootstrapServerCredentialsSecure(PSK, NONE));
        DeviceProfile deviceProfile = createLwm2mDeviceProfile("profileFor" + clientEndpoint, transportConfiguration);

        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsSecure(clientCredentials, privateKey, certificate, RPK, false);
        MvcResult result = createDeviceWithMvcResult(deviceCredentials, clientEndpoint, deviceProfile.getId());
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, result.getResponse().getStatus());
        String msgExpected = "LwM2M client RPK key must be in standard [RFC7250] and support only EC algorithm and then encoded to Base64 format!";
        assertTrue(result.getResponse().getContentAsString().contains(msgExpected));
    }

    @Test
    public void testWithRpkValidationPrivateKeyBase64format_BAD_REQUEST() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_RPK + "BadPrivateKey";
        X509Certificate certificate = clientX509CertTrust;
        PrivateKey privateKey = clientPrivateKeyFromCertTrust;
        RPKClientCredential clientCredentials = new RPKClientCredential();
        clientCredentials.setEndpoint(clientEndpoint);
        clientCredentials.setKey(Base64.encodeBase64String(certificate.getPublicKey().getEncoded()));
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITHOUT_PARAMS, getBootstrapServerCredentialsSecure(PSK, NONE));
        DeviceProfile deviceProfile = createLwm2mDeviceProfile("profileFor" + clientEndpoint, transportConfiguration);
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsSecure(clientCredentials, privateKey, certificate, RPK, true);
        MvcResult result = createDeviceWithMvcResult(deviceCredentials, clientEndpoint, deviceProfile.getId());
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, result.getResponse().getStatus());
        String msgExpected = "Bootstrap server client RPK secret key must be in PKCS#8 format (DER encoding, standard [RFC5958]) and then encoded to Base64 format!";
        assertTrue(result.getResponse().getContentAsString().contains(msgExpected));
    }

    // Bootstrap + Lwm2m
    @Test
    public void testWithRpkConnectBsSuccess_UpdateTwoSectionsBootstrapAndLm2m_ConnectLwm2mSuccess() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_RPK_BS;
        X509Certificate certificate = clientX509CertTrust;
        PrivateKey privateKey = clientPrivateKeyFromCertTrust;
        RPKClientCredential clientCredentials = new RPKClientCredential();
        clientCredentials.setEndpoint(clientEndpoint);
        clientCredentials.setKey(Base64.encodeBase64String(certificate.getPublicKey().getEncoded()));
        Security securityRpkBs = rpkBootstrap(SECURE_URI_BS,
                certificate.getPublicKey().getEncoded(),
                privateKey.getEncoded(),
                serverX509CertBs.getPublicKey().getEncoded());
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITHOUT_PARAMS, getBootstrapServerCredentialsSecure(RPK, BOTH));
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsSecure(clientCredentials, clientPrivateKeyFromCertTrust, certificate, RPK, false);
        this.basicTestConnection(null, securityRpkBs,
                deviceCredentials,
                clientEndpoint,
                transportConfiguration,
                "await on client state (RpkBS two section)",
                expectedStatusesRegistrationBsSuccess,
                false,
                ON_REGISTRATION_SUCCESS,
                true);
    }
}

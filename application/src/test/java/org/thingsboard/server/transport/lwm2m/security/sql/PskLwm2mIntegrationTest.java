/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.client.object.Security;
import org.eclipse.leshan.core.util.Hex;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.web.servlet.MvcResult;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MDeviceCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.PSKClientCredential;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.transport.lwm2m.security.AbstractSecurityLwM2MIntegrationTest;

import java.nio.charset.StandardCharsets;
import static org.eclipse.leshan.client.object.Security.psk;
import static org.eclipse.leshan.client.object.Security.pskBootstrap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MSecurityMode.PSK;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_REGISTRATION_SUCCESS;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.BOTH;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.NONE;

@Slf4j
public class PskLwm2mIntegrationTest extends AbstractSecurityLwM2MIntegrationTest {

    //Lwm2m only
    @Test
    public void testWithPskConnectLwm2mSuccess() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_PSK;
        String identity = CLIENT_PSK_IDENTITY;
        String keyPsk = CLIENT_PSK_KEY;
        PSKClientCredential clientCredentials = new PSKClientCredential();
        clientCredentials.setEndpoint(clientEndpoint);
        clientCredentials.setIdentity(identity);
        clientCredentials.setKey(keyPsk);
        Security security = psk(SECURE_URI,
                shortServerId,
                identity.getBytes(StandardCharsets.UTF_8),
                Hex.decodeHex(keyPsk.toCharArray()));
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITHOUT_PARAMS, getBootstrapServerCredentialsSecure(PSK, NONE));
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsSecure(clientCredentials, null, null, PSK, false);
        this.basicTestConnection(security, null,
                deviceCredentials,
                clientEndpoint,
                transportConfiguration,
                "await on client state (Psk_Lwm2m)",
                expectedStatusesRegistrationLwm2mSuccess,
                true,
                ON_REGISTRATION_SUCCESS,
                true);
    }
    @Test
    public void testWithPskConnectLwm2mOneObserveSuccessUpdateProfileManyObserveUpdateRegistrationSuccess() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_PSK;
        String identity = CLIENT_PSK_IDENTITY;
        String keyPsk = CLIENT_PSK_KEY;
        PSKClientCredential clientCredentials = new PSKClientCredential();
        clientCredentials.setEndpoint(clientEndpoint);
        clientCredentials.setIdentity(identity);
        clientCredentials.setKey(keyPsk);
        Security security = psk(SECURE_URI,
                shortServerId,
                identity.getBytes(StandardCharsets.UTF_8),
                Hex.decodeHex(keyPsk.toCharArray()));
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(TELEMETRY_WITH_ONE_OBSERVE, getBootstrapServerCredentialsSecure(PSK, NONE));
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsSecure(clientCredentials, null, null, PSK, false);
        String awaitAlias = "await on client state (Psk_Lwm2m)";
        Device lwm2mDevice = this.basicTestConnection(security, null,
                deviceCredentials,
                clientEndpoint,
                transportConfiguration,
                awaitAlias,
                expectedStatusesRegistrationLwm2mSuccess,
                false,
                ON_REGISTRATION_SUCCESS,
                true);

        awaitObserveReadAll(1, lwm2mDevice.getId().getId().toString());
        DeviceProfile foundDeviceProfile = doGet("/api/deviceProfile/" + lwm2mDevice.getDeviceProfileId().getId().toString(), DeviceProfile.class);
        transportConfiguration = getTransportConfiguration(TELEMETRY_WITH_MANY_OBSERVE, getBootstrapServerCredentialsSecure(PSK, NONE));
        foundDeviceProfile.getProfileData().setTransportConfiguration(transportConfiguration);
        DeviceProfile lwm2mDeviceProfileManyParams = doPost("/api/deviceProfile", foundDeviceProfile, DeviceProfile.class);
        Assert.assertNotNull(lwm2mDeviceProfileManyParams);
        awaitObserveReadAll(2, lwm2mDevice.getId().getId().toString());
        awaitUpdateReg(3);
    }
    @Test
    public void testWithPskConnectLwm2mSuccessObserveSuccessUnRegClientUpdateProfileObserveConnectLwm2mSuccessOWithNewObserve() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_PSK;
        String identity = CLIENT_PSK_IDENTITY;
        String keyPsk = CLIENT_PSK_KEY;
        PSKClientCredential clientCredentials = new PSKClientCredential();
        clientCredentials.setEndpoint(clientEndpoint);
        clientCredentials.setIdentity(identity);
        clientCredentials.setKey(keyPsk);
        Security security = psk(SECURE_URI,
                shortServerId,
                identity.getBytes(StandardCharsets.UTF_8),
                Hex.decodeHex(keyPsk.toCharArray()));
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(TELEMETRY_WITH_ONE_OBSERVE, getBootstrapServerCredentialsSecure(PSK, NONE));
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsSecure(clientCredentials, null, null, PSK, false);
        String awaitAlias = "await on client state (Psk_Lwm2m)";
        Device lwm2mDevice = this.basicTestConnection(security, null,
                deviceCredentials,
                clientEndpoint,
                transportConfiguration,
                awaitAlias,
                expectedStatusesRegistrationLwm2mSuccess,
                false,
                ON_REGISTRATION_SUCCESS,
                true);

        awaitObserveReadAll(1, lwm2mDevice.getId().getId().toString());
        lwM2MTestClient.stop(true);

        DeviceProfile foundDeviceProfile = doGet("/api/deviceProfile/" + lwm2mDevice.getDeviceProfileId().getId().toString(), DeviceProfile.class);
        transportConfiguration = getTransportConfiguration(TELEMETRY_WITH_MANY_OBSERVE, getBootstrapServerCredentialsSecure(PSK, NONE));
        foundDeviceProfile.getProfileData().setTransportConfiguration(transportConfiguration);
        DeviceProfile lwm2mDeviceProfileManyParams = doPost("/api/deviceProfile", foundDeviceProfile, DeviceProfile.class);
        Assert.assertNotNull(lwm2mDeviceProfileManyParams);

        lwM2MTestClient.start(true);
        awaitObserveReadAll(2, lwm2mDevice.getId().getId().toString());
        awaitUpdateReg(3);
    }

    @Test
    public void testWithPskConnectLwm2mBadPskKeyByLength_BAD_REQUEST() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_PSK;
        String identity = CLIENT_PSK_IDENTITY + "_BadLength";
        String keyPsk = CLIENT_PSK_KEY + "05AC";
        PSKClientCredential clientCredentials = new PSKClientCredential();
        clientCredentials.setEndpoint(clientEndpoint);
        clientCredentials.setIdentity(identity);
        clientCredentials.setKey(keyPsk);
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITHOUT_PARAMS, getBootstrapServerCredentialsSecure(PSK, NONE));
        DeviceProfile deviceProfile = createLwm2mDeviceProfile("profileFor" + clientEndpoint, transportConfiguration);
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsSecure(clientCredentials, null, null, PSK, false);
        MvcResult result = createDeviceWithMvcResult(deviceCredentials, clientEndpoint, deviceProfile.getId());
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, result.getResponse().getStatus());
        String msgExpected = "Key must be HexDec format: 32, 64, 128 characters!";
        assertTrue(result.getResponse().getContentAsString().contains(msgExpected));
    }


    // Bootstrap + Lwm2m
    @Test
    public void testWithPskConnectBsSuccess_UpdateTwoSectionsBootstrapAndLm2m_ConnectLwm2mSuccess() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_PSK_BS;
        String identity = CLIENT_PSK_IDENTITY_BS;
        String keyPsk = CLIENT_PSK_KEY;
        PSKClientCredential clientCredentials = new PSKClientCredential();
        clientCredentials.setEndpoint(clientEndpoint);
        clientCredentials.setIdentity(identity);
        clientCredentials.setKey(keyPsk);
        Security securityPskBs = pskBootstrap(SECURE_URI_BS,
                identity.getBytes(StandardCharsets.UTF_8),
                Hex.decodeHex(keyPsk.toCharArray()));
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITHOUT_PARAMS, getBootstrapServerCredentialsSecure(PSK, BOTH));
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsSecure(clientCredentials, null, null, PSK, false);
        this.basicTestConnection(null, securityPskBs,
                deviceCredentials,
                clientEndpoint,
                transportConfiguration,
                "await on client state (PskBS two section)",
                expectedStatusesRegistrationBsSuccess,
                false,
                ON_REGISTRATION_SUCCESS,
                false);
    }
}

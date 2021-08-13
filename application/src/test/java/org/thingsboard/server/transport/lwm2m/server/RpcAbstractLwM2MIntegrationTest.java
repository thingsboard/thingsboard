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
package org.thingsboard.server.transport.lwm2m.server;

import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.leshan.client.object.Security;
import org.eclipse.leshan.client.resource.ObjectEnabler;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MClientCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.NoSecClientCredentials;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.DisabledDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.controller.AbstractWebsocketTest;
import org.thingsboard.server.controller.TbTestWebSocketClient;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.lwm2m.client.LwM2MTestClient;
import org.thingsboard.server.transport.lwm2m.secure.credentials.LwM2MCredentials;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.eclipse.leshan.client.object.Security.noSec;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class RpcAbstractLwM2MIntegrationTest extends AbstractWebsocketTest {

    protected final String RPC_TRANSPORT_CONFIGURATION = "{\n" +
            "  \"type\": \"LWM2M\",\n" +
            "  \"observeAttr\": {\n" +
            "    \"keyName\": {\n" +
            "      \"/3_1.0/0/9\": \"batteryLevel\"\n" +
            "    },\n" +
            "    \"observe\": [],\n" +
            "    \"attribute\": [\n" +
            "    ],\n" +
            "    \"telemetry\": [\n" +
            "      \"/3_1.0/0/9\"\n" +
            "    ],\n" +
            "    \"attributeLwm2m\": {}\n" +
            "  },\n" +
            "  \"bootstrap\": {\n" +
            "    \"servers\": {\n" +
            "      \"binding\": \"U\",\n" +
            "      \"shortId\": 123,\n" +
            "      \"lifetime\": 300,\n" +
            "      \"notifIfDisabled\": true,\n" +
            "      \"defaultMinPeriod\": 1\n" +
            "    },\n" +
            "    \"lwm2mServer\": {\n" +
            "      \"host\": \"localhost\",\n" +
            "      \"port\": 5686,\n" +
            "      \"serverId\": 123,\n" +
            "      \"serverPublicKey\": \"\",\n" +
            "      \"bootstrapServerIs\": false,\n" +
            "      \"clientHoldOffTime\": 1,\n" +
            "      \"bootstrapServerAccountTimeout\": 0\n" +
            "    },\n" +
            "    \"bootstrapServer\": {\n" +
            "      \"host\": \"localhost\",\n" +
            "      \"port\": 5687,\n" +
            "      \"serverId\": 111,\n" +
            "      \"securityMode\": \"NO_SEC\",\n" +
            "      \"serverPublicKey\": \"\",\n" +
            "      \"bootstrapServerIs\": true,\n" +
            "      \"clientHoldOffTime\": 1,\n" +
            "      \"bootstrapServerAccountTimeout\": 0\n" +
            "    }\n" +
            "  },\n" +
            "  \"clientLwM2mSettings\": {\n" +
            "    \"clientOnlyObserveAfterConnect\": 1,\n" +
            "    \"fwUpdateStrategy\": 1,\n" +
            "    \"swUpdateStrategy\": 1\n" +
            "  }\n" +
            "}";

    protected DeviceProfile deviceProfile;
    protected ScheduledExecutorService executor;
    protected TbTestWebSocketClient wsClient;
    protected NoSecClientCredentials clientCredentials;
    protected LwM2MTestClient client;

    // client private key used for X509
    protected final PrivateKey clientPrivateKeyFromCert;
    // server private key used for X509
    protected final PrivateKey serverPrivateKeyFromCert;
    // client certificate signed by rootCA with a good CN (CN start by leshan_integration_test)
    protected final X509Certificate clientX509Cert;
    // client certificate signed by rootCA but with bad CN (CN does not start by leshan_integration_test)
    protected final X509Certificate clientX509CertWithBadCN;
    // client certificate self-signed with a good CN (CN start by leshan_integration_test)
    protected final X509Certificate clientX509CertSelfSigned;
    // client certificate signed by another CA (not rootCA) with a good CN (CN start by leshan_integration_test)
    protected final X509Certificate clientX509CertNotTrusted;
    // server certificate signed by rootCA
    protected final X509Certificate serverX509Cert;
    // self-signed server certificate
    protected final X509Certificate serverX509CertSelfSigned;
    // rootCA used by the server
    protected final X509Certificate rootCAX509Cert;
    // certificates trustedby the server (should contain rootCA)
    protected final Certificate[] trustedCertificates = new Certificate[1];

    protected static final int SECURE_PORT = 5686;
    protected static final NetworkConfig SECURE_COAP_CONFIG = new NetworkConfig().setString("COAP_SECURE_PORT", Integer.toString(SECURE_PORT));
    protected static final String ENDPOINT = "deviceRPCEndpoint";
    protected static final String SECURE_URI = "coaps://localhost:" + SECURE_PORT;

    protected static final int PORT = 5685;
    protected static final Security SECURITY = noSec("coap://localhost:" + PORT, 123);
    protected static final NetworkConfig COAP_CONFIG = new NetworkConfig().setString("COAP_PORT", Integer.toString(PORT));
    protected String deviceId;
    public Set expectedObjects;
    public Set expectedObjectIdVers;
    public Set expectedInstances;
    public Set expectedObjectIdVerInstances;

    public RpcAbstractLwM2MIntegrationTest() {
// create client credentials
        try {
            // Get certificates from key store
            char[] clientKeyStorePwd = "client".toCharArray();
            KeyStore clientKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            try (InputStream clientKeyStoreFile = this.getClass().getClassLoader().getResourceAsStream("lwm2m/credentials/clientKeyStore.jks")) {
                clientKeyStore.load(clientKeyStoreFile, clientKeyStorePwd);
            }

            clientPrivateKeyFromCert = (PrivateKey) clientKeyStore.getKey("client", clientKeyStorePwd);
            clientX509Cert = (X509Certificate) clientKeyStore.getCertificate("client");
            clientX509CertWithBadCN = (X509Certificate) clientKeyStore.getCertificate("client_bad_cn");
            clientX509CertSelfSigned = (X509Certificate) clientKeyStore.getCertificate("client_self_signed");
            clientX509CertNotTrusted = (X509Certificate) clientKeyStore.getCertificate("client_not_trusted");
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }

        // create server credentials
        try {
            // Get certificates from key store
            char[] serverKeyStorePwd = "server".toCharArray();
            KeyStore serverKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            try (InputStream serverKeyStoreFile = this.getClass().getClassLoader().getResourceAsStream("lwm2m/credentials/serverKeyStore.jks")) {
                serverKeyStore.load(serverKeyStoreFile, serverKeyStorePwd);
            }

            serverPrivateKeyFromCert = (PrivateKey) serverKeyStore.getKey("server", serverKeyStorePwd);
            rootCAX509Cert = (X509Certificate) serverKeyStore.getCertificate("rootCA");
            serverX509Cert = (X509Certificate) serverKeyStore.getCertificate("server");
            serverX509CertSelfSigned = (X509Certificate) serverKeyStore.getCertificate("server_self_signed");
            trustedCertificates[0] = rootCAX509Cert;
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Before
    public void beforeTest() throws Exception {
        executor = Executors.newScheduledThreadPool(10);
        loginTenantAdmin();
        wsClient = buildAndConnectWebSocketClient();
        createDeviceProfile(RPC_TRANSPORT_CONFIGURATION);
        clientCredentials = new NoSecClientCredentials();
        clientCredentials.setEndpoint(ENDPOINT);
        Device device = createDevice(clientCredentials);
        deviceId = device.getId().getId().toString();
        client = new LwM2MTestClient(executor, ENDPOINT);
        client.init(SECURITY, COAP_CONFIG, 11004);
        expectedObjects = ConcurrentHashMap.newKeySet();
        expectedObjectIdVers = ConcurrentHashMap.newKeySet();
        expectedInstances = ConcurrentHashMap.newKeySet();
        expectedObjectIdVerInstances = ConcurrentHashMap.newKeySet();
        client.getClient().getObjectTree().getObjectEnablers().forEach((key, val) -> {
            if (key > 0) {
                String objectVerId = "/" + key;
                if (!val.getObjectModel().version.equals("1.0")) {
                    objectVerId += ("_" + val.getObjectModel().version);
                }
                expectedObjects.add("/" + key);
                expectedObjectIdVers.add(objectVerId);
                String finalObjectVerId = objectVerId;
                val.getAvailableInstanceIds().forEach(inststanceId -> {
                    expectedInstances.add( "/" + key + "/" + inststanceId);
                    expectedObjectIdVerInstances.add(finalObjectVerId + "/" + inststanceId);
                });
            }
        });
    }

    protected void createDeviceProfile(String transportConfiguration) throws Exception {
        deviceProfile = new DeviceProfile();

        deviceProfile.setName("LwM2M_RPC");
        deviceProfile.setType(DeviceProfileType.DEFAULT);
        deviceProfile.setTenantId(tenantId);
        deviceProfile.setTransportType(DeviceTransportType.LWM2M);
        deviceProfile.setProvisionType(DeviceProfileProvisionType.DISABLED);
        deviceProfile.setDescription(deviceProfile.getName());

        DeviceProfileData deviceProfileData = new DeviceProfileData();
        deviceProfileData.setConfiguration(new DefaultDeviceProfileConfiguration());
        deviceProfileData.setProvisionConfiguration(new DisabledDeviceProfileProvisionConfiguration(null));
        deviceProfileData.setTransportConfiguration(JacksonUtil.fromString(transportConfiguration, Lwm2mDeviceProfileTransportConfiguration.class));
        deviceProfile.setProfileData(deviceProfileData);

        deviceProfile = doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);
        Assert.assertNotNull(deviceProfile);
    }

    protected Device createDevice(LwM2MClientCredentials clientCredentials) throws Exception {
        Device device = new Device();
        device.setName("Device RPC");
        device.setDeviceProfileId(deviceProfile.getId());
        device.setTenantId(tenantId);
        device = doPost("/api/device", device, Device.class);
        Assert.assertNotNull(device);

        DeviceCredentials deviceCredentials =
                doGet("/api/device/" + device.getId().getId().toString() + "/credentials", DeviceCredentials.class);
        Assert.assertEquals(device.getId(), deviceCredentials.getDeviceId());
        deviceCredentials.setCredentialsType(DeviceCredentialsType.LWM2M_CREDENTIALS);

        LwM2MCredentials credentials = new LwM2MCredentials();

        credentials.setClient(clientCredentials);

        deviceCredentials.setCredentialsValue(JacksonUtil.toString(credentials));
        doPost("/api/device/credentials", deviceCredentials).andExpect(status().isOk());
        return device;
    }

    protected String objectIdVerToObjectId (String objectIdVer) {
        return objectIdVer.contains("_") ? objectIdVer.split("_")[0] : objectIdVer;
    }
    protected String objectInstanceIdVerToObjectInstanceId (String objectInstanceIdVer) {
        String [] objectIdVer = objectInstanceIdVer.split("/");
        return objectIdVer[0].contains("_") ? objectIdVer[0].split("_")[0] + "/" + objectIdVer[1] : objectInstanceIdVer;
    }

    @After
    public void after() {
        if (client != null) {
            client.destroy();
        }
        executor.shutdownNow();
        if (wsClient != null) {
            wsClient.close();
        }
    }
}

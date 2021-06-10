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
package org.thingsboard.server.transport.lwm2m;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.io.IOUtils;
import org.eclipse.leshan.core.util.Hex;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.DisabledDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.controller.AbstractWebsocketTest;
import org.thingsboard.server.controller.TbTestWebSocketClient;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@DaoSqlTest
public class AbstractLwM2MIntegrationTest extends AbstractWebsocketTest {

    protected final String TRANSPORT_CONFIGURATION = "{\n" +
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
            "    \"clientOnlyObserveAfterConnect\": 1\n" +
            "  }\n" +
            "}";

    protected DeviceProfile deviceProfile;
    protected ScheduledExecutorService executor;
    protected TbTestWebSocketClient wsClient;

    protected final PublicKey clientPublicKey; // client public key used for RPK
    protected final PrivateKey clientPrivateKey; // client private key used for RPK
    protected final PublicKey serverPublicKey; // server public key used for RPK
    protected final PrivateKey serverPrivateKey; // server private key used for RPK

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

    public AbstractLwM2MIntegrationTest() {
// create client credentials
        try {
            // Get point values
            byte[] publicX = Hex
                    .decodeHex("89c048261979208666f2bfb188be1968fc9021c416ce12828c06f4e314c167b5".toCharArray());
            byte[] publicY = Hex
                    .decodeHex("cbf1eb7587f08e01688d9ada4be859137ca49f79394bad9179326b3090967b68".toCharArray());
            byte[] privateS = Hex
                    .decodeHex("e67b68d2aaeb6550f19d98cade3ad62b39532e02e6b422e1f7ea189dabaea5d2".toCharArray());

            // Get Elliptic Curve Parameter spec for secp256r1
            AlgorithmParameters algoParameters = AlgorithmParameters.getInstance("EC");
            algoParameters.init(new ECGenParameterSpec("secp256r1"));
            ECParameterSpec parameterSpec = algoParameters.getParameterSpec(ECParameterSpec.class);

            // Create key specs
            KeySpec publicKeySpec = new ECPublicKeySpec(new ECPoint(new BigInteger(publicX), new BigInteger(publicY)),
                    parameterSpec);
            KeySpec privateKeySpec = new ECPrivateKeySpec(new BigInteger(privateS), parameterSpec);

            // Get keys
            clientPublicKey = KeyFactory.getInstance("EC").generatePublic(publicKeySpec);
            clientPrivateKey = KeyFactory.getInstance("EC").generatePrivate(privateKeySpec);

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
            // Get point values
            byte[] publicX = Hex
                    .decodeHex("fcc28728c123b155be410fc1c0651da374fc6ebe7f96606e90d927d188894a73".toCharArray());
            byte[] publicY = Hex
                    .decodeHex("d2ffaa73957d76984633fc1cc54d0b763ca0559a9dff9706e9f4557dacc3f52a".toCharArray());
            byte[] privateS = Hex
                    .decodeHex("1dae121ba406802ef07c193c1ee4df91115aabd79c1ed7f4c0ef7ef6a5449400".toCharArray());

            // Get Elliptic Curve Parameter spec for secp256r1
            AlgorithmParameters algoParameters = AlgorithmParameters.getInstance("EC");
            algoParameters.init(new ECGenParameterSpec("secp256r1"));
            ECParameterSpec parameterSpec = algoParameters.getParameterSpec(ECParameterSpec.class);

            // Create key specs
            KeySpec publicKeySpec = new ECPublicKeySpec(new ECPoint(new BigInteger(publicX), new BigInteger(publicY)),
                    parameterSpec);
            KeySpec privateKeySpec = new ECPrivateKeySpec(new BigInteger(privateS), parameterSpec);

            // Get keys
            serverPublicKey = KeyFactory.getInstance("EC").generatePublic(publicKeySpec);
            serverPrivateKey = KeyFactory.getInstance("EC").generatePrivate(privateKeySpec);

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

        String[] resources = new String[]{"1.xml", "2.xml", "3.xml"};
        for (String resourceName : resources) {
            TbResource lwModel = new TbResource();
            lwModel.setResourceType(ResourceType.LWM2M_MODEL);
            lwModel.setTitle(resourceName);
            lwModel.setFileName(resourceName);
            lwModel.setTenantId(tenantId);
            byte[] bytes = IOUtils.toByteArray(AbstractLwM2MIntegrationTest.class.getClassLoader().getResourceAsStream("lwm2m/" + resourceName));
            lwModel.setData(Base64.getEncoder().encodeToString(bytes));
            lwModel = doPostWithTypedResponse("/api/resource", lwModel, new TypeReference<>() {
            });
            Assert.assertNotNull(lwModel);
        }
        wsClient = buildAndConnectWebSocketClient();
    }

    protected void createDeviceProfile(String transportConfiguration) throws Exception {
        deviceProfile = new DeviceProfile();

        deviceProfile.setName("LwM2M");
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

    @After
    public void after() {
        executor.shutdownNow();
        wsClient.close();
    }

}

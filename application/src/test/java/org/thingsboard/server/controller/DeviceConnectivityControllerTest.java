/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.device.credentials.BasicMqttCredentials;
import org.thingsboard.server.common.data.device.profile.CoapDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.MqttDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.dao.device.DeviceDao;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.COAP;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.COAPS;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.DOCKER;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.HTTP;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.HTTPS;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.MQTT;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.MQTTS;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.PEM_CERT_FILE_NAME;

@TestPropertySource(properties = {
        "device.connectivity.https.enabled=true",
        "device.connectivity.http.port=8080",
        "device.connectivity.https.port=444",
        "device.connectivity.mqtts.enabled=true",
        "device.connectivity.mqtts.pem_cert_file=/tmp/" + PEM_CERT_FILE_NAME,
        "device.connectivity.coaps.enabled=true",
})
@ContextConfiguration(classes = {DeviceConnectivityControllerTest.Config.class})
@DaoSqlTest
public class DeviceConnectivityControllerTest extends AbstractControllerTest {

    private static final String DEVICE_TELEMETRY_TOPIC = "v1/devices/customTopic";
    private static final String CHECK_DOCUMENTATION = "Check documentation";

    private static final String CERT = "-----BEGIN CERTIFICATE-----\n" +
            "MIIBfzCCASmgAwIBAgIUC1dtaskm/SJLmFE2Ae+YojArg+swDQYJKoZIhvcNAQEL\n" +
            "BQAwFDESMBAGA1UEAwwJbG9jYWxob3N0MB4XDTIzMDgyODEzMTAzM1oXDTI0MDgy\n" +
            "NzEzMTAzM1owFDESMBAGA1UEAwwJbG9jYWxob3N0MFwwDQYJKoZIhvcNAQEBBQAD\n" +
            "SwAwSAJBANpcs46MavFdv7onsxH178YgK5XbpMqzx8AKaLMP2X6UEXN0nlt5mpX5\n" +
            "uCJmSwVaFn6lwTm8ThXFYOBydOQImIsCAwEAAaNTMFEwHQYDVR0OBBYEFDvN49bI\n" +
            "LaWMmUZ+cMboWAaozfXTMB8GA1UdIwQYMBaAFDvN49bILaWMmUZ+cMboWAaozfXT\n" +
            "MA8GA1UdEwEB/wQFMAMBAf8wDQYJKoZIhvcNAQELBQADQQAhIQL8zPvIhQvHJocU\n" +
            "tnSmDAE0iR2rJVkousA+LiORE9BnuBtBUEv5SvFUv3VYUWA0eYFoyatpDHByIm6e\n" +
            "/+1c\n" +
            "-----END CERTIFICATE-----\n";
    private static final String P_KEY = "-----BEGIN PRIVATE KEY-----\n" +
            "MIIBVgIBADANBgkqhkiG9w0BAQEFAASCAUAwggE8AgEAAkEA2lyzjoxq8V2/uiez\n" +
            "EfXvxiArldukyrPHwAposw/ZfpQRc3SeW3malfm4ImZLBVoWfqXBObxOFcVg4HJ0\n" +
            "5AiYiwIDAQABAkEA1DYhPljSmc2dRcHNMphLtMWQ9iumpGRBrS2wgMzXdz2NF2+0\n" +
            "4cicaaL06/Cw6XXx43s8cn7e1xZAkGtNRQuqMQIhAPbrqrcYsropURpI5HSemeha\n" +
            "MJA3i67ZFaom39VSrNKJAiEA4mQ0qFKxFSh2xAOqDWDRkiCgdOS00J6hgrYJRPcI\n" +
            "nXMCIQDBHGjkT72gGKYkT3PUvSGTdc3bTIXDFmZ6L3MJTGJ7OQIhAKO+6r9coCy3\n" +
            "ib+ZDuSCRNK2upgR3B6Qvi020VmKfDa1AiBhCgpBlClv5OjnmC42EGxxFOaZtNQQ\n" +
            "C3swkUdrR3pezg==\n" +
            "-----END PRIVATE KEY-----\n";

    ListeningExecutorService executor;

    private Tenant savedTenant;
    private User tenantAdmin;
    private DeviceProfileId mqttDeviceProfileId;
    private DeviceProfileId coapDeviceProfileId;

    static class Config {
        @Bean
        @Primary
        public DeviceDao deviceDao(DeviceDao deviceDao) {
            return Mockito.mock(DeviceDao.class, AdditionalAnswers.delegatesTo(deviceDao));
        }
    }

    @Before
    public void beforeTest() throws Exception {
        executor = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(8, getClass()));

        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");

        DeviceProfile mqttProfile = new DeviceProfile();
        mqttProfile.setName("Mqtt device profile");
        mqttProfile.setType(DeviceProfileType.DEFAULT);
        mqttProfile.setTransportType(DeviceTransportType.MQTT);
        DeviceProfileData deviceProfileData = new DeviceProfileData();
        deviceProfileData.setConfiguration(new DefaultDeviceProfileConfiguration());
        MqttDeviceProfileTransportConfiguration transportConfiguration = new MqttDeviceProfileTransportConfiguration();
        transportConfiguration.setDeviceTelemetryTopic(DEVICE_TELEMETRY_TOPIC);
        deviceProfileData.setTransportConfiguration(transportConfiguration);
        mqttProfile.setProfileData(deviceProfileData);
        mqttProfile.setDefault(false);
        mqttProfile.setDefaultRuleChainId(null);

        mqttDeviceProfileId = doPost("/api/deviceProfile", mqttProfile, DeviceProfile.class).getId();

        DeviceProfile coapProfile = new DeviceProfile();
        coapProfile.setName("Coap device profile");
        coapProfile.setType(DeviceProfileType.DEFAULT);
        coapProfile.setTransportType(DeviceTransportType.COAP);
        DeviceProfileData deviceProfileData2 = new DeviceProfileData();
        deviceProfileData2.setConfiguration(new DefaultDeviceProfileConfiguration());
        deviceProfileData2.setTransportConfiguration(new CoapDeviceProfileTransportConfiguration());
        coapProfile.setProfileData(deviceProfileData);
        coapProfile.setDefault(false);
        coapProfile.setDefaultRuleChainId(null);

        coapDeviceProfileId = doPost("/api/deviceProfile", coapProfile, DeviceProfile.class).getId();
    }

    @After
    public void afterTest() throws Exception {
        executor.shutdownNow();

        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant.getId().getId())
                .andExpect(status().isOk());
    }

    @Test
    public void testFetchPublishTelemetryCommandsForDefaultDevice() throws Exception {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        Device savedDevice = doPost("/api/device", device, Device.class);
        JsonNode commands =
                doGetTyped("/api/device-connectivity/" + savedDevice.getId().getId(), new TypeReference<>() {
                });

        DeviceCredentials credentials =
                doGet("/api/device/" + savedDevice.getId().getId() + "/credentials", DeviceCredentials.class);

        assertThat(commands).hasSize(3);
        JsonNode httpCommands = commands.get(HTTP);
        assertThat(httpCommands.get(HTTP).asText()).isEqualTo(String.format("curl -v -X POST http://localhost:8080/api/v1/%s/telemetry " +
                        "--header Content-Type:application/json --data \"{temperature:25}\"",
                credentials.getCredentialsId()));
        assertThat(httpCommands.get(HTTPS).asText()).isEqualTo(String.format("curl -v -X POST https://localhost:444/api/v1/%s/telemetry " +
                        "--header Content-Type:application/json --data \"{temperature:25}\"",
                credentials.getCredentialsId()));


        JsonNode mqttCommands = commands.get(MQTT);
        assertThat(mqttCommands.get(MQTT).asText()).isEqualTo(String.format("mosquitto_pub -d -q 1 -h localhost -p 1883 -t v1/devices/me/telemetry " +
                        "-u %s -m \"{temperature:25}\"",
                credentials.getCredentialsId()));
        assertThat(mqttCommands.get(MQTTS).get(0).asText()).isEqualTo("curl -f -S -o tb-server-chain.pem http://localhost:80/api/device-connectivity/mqtts/certificate/download");
        assertThat(mqttCommands.get(MQTTS).get(1).asText()).isEqualTo(String.format("mosquitto_pub -d -q 1 --cafile tb-server-chain.pem -h localhost -p 8883 " +
                "-t v1/devices/me/telemetry -u %s -m \"{temperature:25}\"", credentials.getCredentialsId()));

        JsonNode dockerMqttCommands = commands.get(MQTT).get(DOCKER);
        assertThat(dockerMqttCommands.get(MQTT).asText()).isEqualTo(String.format("docker run --rm -it thingsboard/mosquitto-clients mosquitto_pub -d -q 1 -h localhost" +
                        " -p 1883 -t v1/devices/me/telemetry -u %s -m \"{temperature:25}\"",
                credentials.getCredentialsId()));
        assertThat(dockerMqttCommands.get(MQTTS).asText()).isEqualTo(String.format("docker run --rm -it thingsboard/mosquitto-clients " +
                        "/bin/sh -c \"curl -f -S -o tb-server-chain.pem http://localhost:80/api/device-connectivity/mqtts/certificate/download && " +
                        "mosquitto_pub -d -q 1 --cafile tb-server-chain.pem -h localhost -p 8883 -t v1/devices/me/telemetry -u %s -m \"{temperature:25}\"\"",
                credentials.getCredentialsId()));

        JsonNode linuxCoapCommands = commands.get(COAP);
        assertThat(linuxCoapCommands.get(COAP).asText()).isEqualTo(String.format("coap-client -m POST coap://localhost:5683/api/v1/%s/telemetry " +
                "-t json -e \"{temperature:25}\"", credentials.getCredentialsId()));
        assertThat(linuxCoapCommands.get(COAPS).asText()).isEqualTo(String.format("coap-client-openssl -m POST coaps://localhost:5684/api/v1/%s/telemetry" +
                " -t json -e \"{temperature:25}\"", credentials.getCredentialsId()));
    }

    @Test
    public void testFetchPublishTelemetryCommandsForMqttDeviceWithAccessToken() throws Exception {
        Device device = new Device();
        device.setName("My device");
        device.setDeviceProfileId(mqttDeviceProfileId);

        Device savedDevice = doPost("/api/device", device, Device.class);
        DeviceCredentials credentials =
                doGet("/api/device/" + savedDevice.getId().getId() + "/credentials", DeviceCredentials.class);

        JsonNode commands =
                doGetTyped("/api/device-connectivity/" + savedDevice.getId().getId(), new TypeReference<>() {
                });
        assertThat(commands).hasSize(1);

        JsonNode mqttCommands = commands.get(MQTT);
        assertThat(mqttCommands.get(MQTT).asText()).isEqualTo(String.format("mosquitto_pub -d -q 1 -h localhost -p 1883 -t %s " +
                "-u %s -m \"{temperature:25}\"", DEVICE_TELEMETRY_TOPIC, credentials.getCredentialsId()));
        assertThat(mqttCommands.get(MQTTS).get(0).asText()).isEqualTo("curl -f -S -o tb-server-chain.pem http://localhost:80/api/device-connectivity/mqtts/certificate/download");
        assertThat(mqttCommands.get(MQTTS).get(1).asText()).isEqualTo(String.format("mosquitto_pub -d -q 1 --cafile tb-server-chain.pem -h localhost -p 8883 " +
                "-t %s -u %s -m \"{temperature:25}\"", DEVICE_TELEMETRY_TOPIC, credentials.getCredentialsId()));

        JsonNode dockerMqttCommands = commands.get(MQTT).get(DOCKER);
        assertThat(dockerMqttCommands.get(MQTT).asText()).isEqualTo(String.format("docker run --rm -it thingsboard/mosquitto-clients mosquitto_pub -d -q 1 -h localhost" +
                        " -p 1883 -t %s -u %s -m \"{temperature:25}\"",
                DEVICE_TELEMETRY_TOPIC, credentials.getCredentialsId()));
        assertThat(dockerMqttCommands.get(MQTTS).asText()).isEqualTo(String.format("docker run --rm -it thingsboard/mosquitto-clients " +
                        "/bin/sh -c \"curl -f -S -o tb-server-chain.pem http://localhost:80/api/device-connectivity/mqtts/certificate/download && " +
                        "mosquitto_pub -d -q 1 --cafile tb-server-chain.pem -h localhost -p 8883 -t %s -u %s -m \"{temperature:25}\"\"",
                DEVICE_TELEMETRY_TOPIC, credentials.getCredentialsId()));
    }

    @Test
    public void testFetchPublishTelemetryCommandsForDeviceWithMqttBasicCreds() throws Exception {
        Device device = new Device();
        device.setName("My device");
        device.setDeviceProfileId(mqttDeviceProfileId);

        Device savedDevice = doPost("/api/device", device, Device.class);
        DeviceCredentials credentials =
                doGet("/api/device/" + savedDevice.getId().getId() + "/credentials", DeviceCredentials.class);
        credentials.setCredentialsId(null);
        credentials.setCredentialsType(DeviceCredentialsType.MQTT_BASIC);
        BasicMqttCredentials basicMqttCredentials = new BasicMqttCredentials();
        String clientId = "testClientId";
        String userName = "testUsername";
        String password = "testPassword";
        basicMqttCredentials.setClientId(clientId);
        basicMqttCredentials.setUserName(userName);
        basicMqttCredentials.setPassword(password);
        credentials.setCredentialsValue(JacksonUtil.toString(basicMqttCredentials));
        doPost("/api/device/credentials", credentials)
                .andExpect(status().isOk());

        JsonNode commands =
                doGetTyped("/api/device-connectivity/" + savedDevice.getId().getId(), new TypeReference<>() {
                });
        assertThat(commands).hasSize(1);

        JsonNode mqttCommands = commands.get(MQTT);
        assertThat(mqttCommands.get(MQTT).asText()).isEqualTo(String.format("mosquitto_pub -d -q 1 -h localhost -p 1883 -t %s " +
                "-i %s -u %s -P %s -m \"{temperature:25}\"", DEVICE_TELEMETRY_TOPIC, clientId, userName, password));
        assertThat(mqttCommands.get(MQTTS).get(0).asText()).isEqualTo("curl -f -S -o tb-server-chain.pem http://localhost:80/api/device-connectivity/mqtts/certificate/download");
        assertThat(mqttCommands.get(MQTTS).get(1).asText()).isEqualTo(String.format("mosquitto_pub -d -q 1 --cafile tb-server-chain.pem -h localhost -p 8883 " +
                "-t %s -i %s -u %s -P %s -m \"{temperature:25}\"", DEVICE_TELEMETRY_TOPIC, clientId, userName, password));

        JsonNode dockerMqttCommands = commands.get(MQTT).get(DOCKER);
        assertThat(dockerMqttCommands.get(MQTT).asText()).isEqualTo(String.format("docker run --rm -it thingsboard/mosquitto-clients mosquitto_pub -d -q 1 -h localhost" +
                        " -p 1883 -t %s -i %s -u %s -P %s -m \"{temperature:25}\"",
                DEVICE_TELEMETRY_TOPIC, clientId, userName, password));
        assertThat(dockerMqttCommands.get(MQTTS).asText()).isEqualTo(String.format("docker run --rm -it thingsboard/mosquitto-clients " +
                        "/bin/sh -c \"curl -f -S -o tb-server-chain.pem http://localhost:80/api/device-connectivity/mqtts/certificate/download && " +
                        "mosquitto_pub -d -q 1 --cafile tb-server-chain.pem -h localhost -p 8883 -t %s -i %s -u %s -P %s -m \"{temperature:25}\"\"",
                DEVICE_TELEMETRY_TOPIC, clientId, userName, password));
    }

    @Test
    public void testFetchPublishTelemetryCommandsForDeviceWithX509Creds() throws Exception {
        Device device = new Device();
        device.setName("My device");
        device.setDeviceProfileId(mqttDeviceProfileId);

        Device savedDevice = doPost("/api/device", device, Device.class);
        DeviceCredentials credentials =
                doGet("/api/device/" + savedDevice.getId().getId() + "/credentials", DeviceCredentials.class);
        credentials.setCredentialsId(null);
        credentials.setCredentialsType(DeviceCredentialsType.X509_CERTIFICATE);
        credentials.setCredentialsValue("testValue");
        doPost("/api/device/credentials", credentials)
                .andExpect(status().isOk());

        JsonNode commands =
                doGetTyped("/api/device-connectivity/" + savedDevice.getId().getId(), new TypeReference<>() {
                });
        assertThat(commands).hasSize(1);
        assertThat(commands.get(MQTT).get(MQTTS).asText()).isEqualTo(CHECK_DOCUMENTATION);
        assertThat(commands.get(MQTT).get(DOCKER)).isNull();
    }

    @Test
    public void testFetchPublishTelemetryCommandsForCoapDevice() throws Exception {
        Device device = new Device();
        device.setName("My device");
        device.setDeviceProfileId(coapDeviceProfileId);

        Device savedDevice = doPost("/api/device", device, Device.class);
        DeviceCredentials credentials =
                doGet("/api/device/" + savedDevice.getId().getId() + "/credentials", DeviceCredentials.class);

        JsonNode commands =
                doGetTyped("/api/device-connectivity/" + savedDevice.getId().getId(), new TypeReference<>() {
                });
        assertThat(commands).hasSize(1);

        JsonNode linuxCommands = commands.get(COAP);
        assertThat(linuxCommands.get(COAP).asText()).isEqualTo(String.format("coap-client -m POST coap://localhost:5683/api/v1/%s/telemetry -t json -e \"{temperature:25}\"",
                credentials.getCredentialsId()));
        assertThat(linuxCommands.get(COAPS).asText()).isEqualTo(String.format("coap-client-openssl -m POST coaps://localhost:5684/api/v1/%s/telemetry -t json -e \"{temperature:25}\"",
                credentials.getCredentialsId()));
    }

    @Test
    public void testFetchPublishTelemetryCommandsForCoapDeviceWithX509Creds() throws Exception {
        Device device = new Device();
        device.setName("My device");
        device.setDeviceProfileId(coapDeviceProfileId);

        Device savedDevice = doPost("/api/device", device, Device.class);
        DeviceCredentials credentials =
                doGet("/api/device/" + savedDevice.getId().getId() + "/credentials", DeviceCredentials.class);
        credentials.setCredentialsId(null);
        credentials.setCredentialsType(DeviceCredentialsType.X509_CERTIFICATE);
        credentials.setCredentialsValue("testValue");
        doPost("/api/device/credentials", credentials)
                .andExpect(status().isOk());

        JsonNode commands =
                doGetTyped("/api/device-connectivity/" + savedDevice.getId().getId(), new TypeReference<>() {
                });
        assertThat(commands).hasSize(1);
        assertThat(commands.get(COAP).get(COAPS).asText()).isEqualTo(CHECK_DOCUMENTATION);
    }

    @Test
    @DirtiesContext
    public void testDownloadMqttCert() throws Exception {
        Path path = Files.createFile(Path.of("/tmp/" + PEM_CERT_FILE_NAME));
        Files.writeString(path, CERT);

        try {
            String downloadedCert = doGet("/api/device-connectivity/mqtts/certificate/download", String.class);
            Assert.assertEquals(CERT, downloadedCert);
        } finally {
            Files.deleteIfExists(path);
        }
    }

    @Test
    @DirtiesContext
    public void testDownloadMqttCertFromFileWithPrivateKey() throws Exception {
        Path path = Files.createFile(Path.of("/tmp/" + PEM_CERT_FILE_NAME));
        Files.writeString(path, CERT + P_KEY);

        try {
            String downloadedCert = doGet("/api/device-connectivity/mqtts/certificate/download", String.class);
            Assert.assertEquals(CERT, downloadedCert);
        } finally {
            Files.deleteIfExists(path);
        }
    }

    @Test
    @DirtiesContext
    public void testDownloadMqttCertWithoutCertFile() throws Exception {
        doGet("/api/device-connectivity/mqtts/certificate/download").andExpect(status().isNotFound());
    }

    @Test
    @DirtiesContext
    public void testDownloadCertWithUnknownProtocol() throws Exception {
        doGet("/api/device-connectivity/unknownProtocol/certificate/download").andExpect(status().isNotFound());
    }
}

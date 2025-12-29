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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
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
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.CA_ROOT_CERT_PEM;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.COAP;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.COAPS;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.DOCKER;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.HTTP;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.HTTPS;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.MQTT;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.MQTTS;

@TestPropertySource(properties = {
        "device.connectivity.mqtts.pem_cert_file=/tmp/" + CA_ROOT_CERT_PEM
})
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

    private Tenant savedTenant;
    private User tenantAdmin;
    private DeviceProfileId mqttDeviceProfileId;
    private DeviceProfileId coapDeviceProfileId;

    @Value("${device.connectivity.gateway.image_version:3.8-stable}")
    private String gatewayImageVersion;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        ObjectNode config = JacksonUtil.newObjectNode();

        ObjectNode http = JacksonUtil.newObjectNode();
        http.put("enabled", true);
        http.put("host", "");
        http.put("port", 8080);
        config.set("http", http);

        ObjectNode https = JacksonUtil.newObjectNode();
        https.put("enabled", true);
        https.put("host", "");
        https.put("port", 444);
        config.set("https", https);

        ObjectNode mqtt = JacksonUtil.newObjectNode();
        mqtt.put("enabled", true);
        mqtt.put("host", "");
        mqtt.put("port", 1883);
        config.set("mqtt", mqtt);

        ObjectNode mqtts = JacksonUtil.newObjectNode();
        mqtts.put("enabled", true);
        mqtts.put("host", "");
        mqtts.put("port", 8883);
        config.set("mqtts", mqtts);

        ObjectNode coap = JacksonUtil.newObjectNode();
        coap.put("enabled", true);
        coap.put("host", "");
        coap.put("port", 5683);
        config.set("coap", coap);

        ObjectNode coaps = JacksonUtil.newObjectNode();
        coaps.put("enabled", true);
        coaps.put("host", "");
        coaps.put("port", 5684);
        config.set("coaps", coaps);

        AdminSettings adminSettings = doGet("/api/admin/settings/connectivity", AdminSettings.class);
        adminSettings.setJsonValue(config);
        doPost("/api/admin/settings", adminSettings).andExpect(status().isOk());

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = saveTenant(tenant);
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
        DeviceProfileData mqttProfileData = new DeviceProfileData();
        mqttProfileData.setConfiguration(new DefaultDeviceProfileConfiguration());
        MqttDeviceProfileTransportConfiguration transportConfiguration = new MqttDeviceProfileTransportConfiguration();
        transportConfiguration.setDeviceTelemetryTopic(DEVICE_TELEMETRY_TOPIC);
        mqttProfileData.setTransportConfiguration(transportConfiguration);
        mqttProfile.setProfileData(mqttProfileData);
        mqttProfile.setDefault(false);
        mqttProfile.setDefaultRuleChainId(null);

        mqttDeviceProfileId = doPost("/api/deviceProfile", mqttProfile, DeviceProfile.class).getId();

        DeviceProfile coapProfile = new DeviceProfile();
        coapProfile.setName("Coap device profile");
        coapProfile.setType(DeviceProfileType.DEFAULT);
        coapProfile.setTransportType(DeviceTransportType.COAP);
        DeviceProfileData coapProfileData = new DeviceProfileData();
        coapProfileData.setConfiguration(new DefaultDeviceProfileConfiguration());
        coapProfileData.setTransportConfiguration(new CoapDeviceProfileTransportConfiguration());
        coapProfile.setProfileData(coapProfileData);
        coapProfile.setDefault(false);
        coapProfile.setDefaultRuleChainId(null);

        coapDeviceProfileId = doPost("/api/deviceProfile", coapProfile, DeviceProfile.class).getId();
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();

        deleteTenant(savedTenant.getId());
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
                        "-u \"%s\" -m \"{temperature:25}\"",
                credentials.getCredentialsId()));
        assertThat(mqttCommands.get(MQTTS).get(0).asText()).isEqualTo("curl -f -S -o " + CA_ROOT_CERT_PEM + " http://localhost:80/api/device-connectivity/mqtts/certificate/download");
        assertThat(mqttCommands.get(MQTTS).get(1).asText()).isEqualTo(String.format("mosquitto_pub -d -q 1 --cafile " + CA_ROOT_CERT_PEM + " -h localhost -p 8883 " +
                "-t v1/devices/me/telemetry -u \"%s\" -m \"{temperature:25}\"", credentials.getCredentialsId()));

        JsonNode dockerMqttCommands = commands.get(MQTT).get(DOCKER);
        assertThat(dockerMqttCommands.get(MQTT).asText()).isEqualTo(String.format("docker run --rm -it --add-host=host.docker.internal:host-gateway thingsboard/mosquitto-clients mosquitto_pub -d -q 1 -h host.docker.internal" +
                        " -p 1883 -t v1/devices/me/telemetry -u \"%s\" -m \"{temperature:25}\"",
                credentials.getCredentialsId()));
        assertThat(dockerMqttCommands.get(MQTTS).asText()).isEqualTo(String.format("docker run --rm -it --add-host=host.docker.internal:host-gateway thingsboard/mosquitto-clients " +
                        "/bin/sh -c \"curl -f -S -o " + CA_ROOT_CERT_PEM + " http://localhost:80/api/device-connectivity/mqtts/certificate/download && " +
                        "mosquitto_pub -d -q 1 --cafile " + CA_ROOT_CERT_PEM + " -h host.docker.internal -p 8883 -t v1/devices/me/telemetry -u \"%s\" -m \"{temperature:25}\"\"",
                credentials.getCredentialsId()));

        JsonNode linuxCoapCommands = commands.get(COAP);
        assertThat(linuxCoapCommands.get(COAP).asText()).isEqualTo(String.format("coap-client -v 6 -m POST " +
                "-t \"application/json\" -e \"{temperature:25}\" coap://localhost:5683/api/v1/%s/telemetry", credentials.getCredentialsId()));

        assertThat(linuxCoapCommands.get(COAPS).get(1).asText()).isEqualTo(String.format("coap-client-openssl -v 6 -m POST " +
                "-R " + CA_ROOT_CERT_PEM + " -t \"application/json\" -e \"{temperature:25}\" coaps://localhost:5684/api/v1/%s/telemetry", credentials.getCredentialsId()));

        JsonNode dockerCoapCommands = commands.get(COAP).get(DOCKER);
        assertThat(dockerCoapCommands.get(COAP).asText()).isEqualTo(String.format("docker run --rm -it --add-host=host.docker.internal:host-gateway" +
                " thingsboard/coap-clients coap-client -v 6 -m POST -t \"application/json\" -e \"{temperature:25}\" coap://host.docker.internal:5683/api/v1/%s/telemetry", credentials.getCredentialsId()));
        assertThat(dockerCoapCommands.get(COAPS).asText()).isEqualTo(String.format("docker run --rm -it --add-host=host.docker.internal:host-gateway" +
                " thingsboard/coap-clients " +
                "/bin/sh -c \"curl -f -S -o " + CA_ROOT_CERT_PEM + " http://localhost:80/api/device-connectivity/coaps/certificate/download && " +
                "coap-client-openssl -v 6 -m POST -R " + CA_ROOT_CERT_PEM + " -t \"application/json\" -e \"{temperature:25}\" coaps://host.docker.internal:5684/api/v1/%s/telemetry\"", credentials.getCredentialsId()));
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
                "-u \"%s\" -m \"{temperature:25}\"", DEVICE_TELEMETRY_TOPIC, credentials.getCredentialsId()));
        assertThat(mqttCommands.get(MQTTS).get(0).asText()).isEqualTo("curl -f -S -o " + CA_ROOT_CERT_PEM + " http://localhost:80/api/device-connectivity/mqtts/certificate/download");
        assertThat(mqttCommands.get(MQTTS).get(1).asText()).isEqualTo(String.format("mosquitto_pub -d -q 1 --cafile " + CA_ROOT_CERT_PEM + " -h localhost -p 8883 " +
                "-t %s -u \"%s\" -m \"{temperature:25}\"", DEVICE_TELEMETRY_TOPIC, credentials.getCredentialsId()));

        JsonNode dockerMqttCommands = commands.get(MQTT).get(DOCKER);
        assertThat(dockerMqttCommands.get(MQTT).asText()).isEqualTo(String.format("docker run --rm -it --add-host=host.docker.internal:host-gateway thingsboard/mosquitto-clients mosquitto_pub -d -q 1 -h host.docker.internal" +
                        " -p 1883 -t %s -u \"%s\" -m \"{temperature:25}\"",
                DEVICE_TELEMETRY_TOPIC, credentials.getCredentialsId()));
        assertThat(dockerMqttCommands.get(MQTTS).asText()).isEqualTo(String.format("docker run --rm -it --add-host=host.docker.internal:host-gateway thingsboard/mosquitto-clients " +
                        "/bin/sh -c \"curl -f -S -o " + CA_ROOT_CERT_PEM + " http://localhost:80/api/device-connectivity/mqtts/certificate/download && " +
                        "mosquitto_pub -d -q 1 --cafile " + CA_ROOT_CERT_PEM + " -h host.docker.internal -p 8883 -t %s -u \"%s\" -m \"{temperature:25}\"\"",
                DEVICE_TELEMETRY_TOPIC, credentials.getCredentialsId()));
    }

    @Test
    public void testFetchGatewayDockerComposeFile() throws Exception {
        String deviceName = "My device";
        Device device = new Device();
        device.setName(deviceName);
        device.setType("default");
        ObjectNode additionalInfo = JacksonUtil.newObjectNode();
        additionalInfo.put("gateway", true);
        device.setAdditionalInfo(additionalInfo);
        Device savedDevice = doPost("/api/device", device, Device.class);
        DeviceCredentials credentials =
                doGet("/api/device/" + savedDevice.getId().getId() + "/credentials", DeviceCredentials.class);

        String commands =
                doGet("/api/device-connectivity/gateway-launch/" + savedDevice.getId().getId() + "/docker-compose/download", String.class);

        assertThat(commands).isEqualTo(String.format("version: '3.4'\n" +
                "services:\n" +
                "  # ThingsBoard IoT Gateway Service Configuration\n" +
                "  tb-gateway:\n" +
                "    image: thingsboard/tb-gateway:" + gatewayImageVersion + "\n" +
                "    container_name: tb-gateway\n" +
                "    restart: always\n" +
                "\n" +
                "    # Ports bindings - required by some connectors\n" +
                "    ports:\n" +
                "        - \"5000:5000\" # Comment if you don't use REST connector and change if you use another port\n" +
                "        # Uncomment and modify the following ports based on connector usage:\n" +
                "#        - \"1052:1052\" # BACnet connector\n" +
                "#        - \"5026:5026\" # Modbus TCP connector (Modbus Slave)\n" +
                "#        - \"50000:50000/tcp\" # Socket connector with type TCP\n" +
                "#        - \"50000:50000/udp\" # Socket connector with type UDP\n" +
                "\n" +
                "    # Necessary mapping for Linux\n" +
                "    extra_hosts:\n" +
                "      - \"host.docker.internal:host-gateway\"\n" +
                "\n" +
                "    # Environment variables\n" +
                "    environment:\n" +
                "      - TB_GW_HOST=host.docker.internal\n" +
                "      - TB_GW_PORT=1883\n" +
                "      - TB_GW_SECURITY_TYPE=accessToken\n" +
                "      - TB_GW_ACCESS_TOKEN=" + credentials.getCredentialsId() + "\n" +
                "\n" +
                "    # Volumes bind\n" +
                "    volumes:\n" +
                "      - tb-gw-config:/thingsboard_gateway/config\n" +
                "      - tb-gw-logs:/thingsboard_gateway/logs\n" +
                "      - tb-gw-extensions:/thingsboard_gateway/extensions\n" +
                "\n" +
                "# Volumes declaration for configurations, extensions and configuration\n" +
                "volumes:\n" +
                "  tb-gw-config:\n" +
                "    name: tb-gw-config\n" +
                "  tb-gw-logs:\n" +
                "    name: tb-gw-logs\n" +
                "  tb-gw-extensions:\n" +
                "    name: tb-gw-extensions\n"));
    }

    @Test
    public void testFetchPublishTelemetryCommandsForDeviceWithIpV6LocalhostAddress() throws Exception {
        loginSysAdmin();
        setConnectivityHost("::1");
        loginTenantAdmin();

        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        Device savedDevice = doPost("/api/device", device, Device.class);
        DeviceCredentials credentials =
                doGet("/api/device/" + savedDevice.getId().getId() + "/credentials", DeviceCredentials.class);

        JsonNode commands =
                doGetTyped("/api/device-connectivity/" + savedDevice.getId().getId(), new TypeReference<>() {
                });

        assertThat(commands).hasSize(3);
        JsonNode httpCommands = commands.get(HTTP);
        assertThat(httpCommands.get(HTTP).asText()).isEqualTo(String.format("curl -v -X POST http://[::1]:8080/api/v1/%s/telemetry " +
                        "--header Content-Type:application/json --data \"{temperature:25}\"",
                credentials.getCredentialsId()));
        assertThat(httpCommands.get(HTTPS).asText()).isEqualTo(String.format("curl -v -X POST https://[::1]/api/v1/%s/telemetry " +
                        "--header Content-Type:application/json --data \"{temperature:25}\"",
                credentials.getCredentialsId()));

        JsonNode mqttCommands = commands.get(MQTT);
        assertThat(mqttCommands.get(MQTT).asText()).isEqualTo(String.format("mosquitto_pub -d -q 1 -h ::1 -p 1883 -t v1/devices/me/telemetry " +
                "-u \"%s\" -m \"{temperature:25}\"", credentials.getCredentialsId()));
        assertThat(mqttCommands.get(MQTTS).get(0).asText()).isEqualTo("curl -f -S -o " + CA_ROOT_CERT_PEM + " http://localhost:80/api/device-connectivity/mqtts/certificate/download");
        assertThat(mqttCommands.get(MQTTS).get(1).asText()).isEqualTo(String.format("mosquitto_pub -d -q 1 --cafile " + CA_ROOT_CERT_PEM + " -h ::1 -p 8883 " +
                "-t v1/devices/me/telemetry -u \"%s\" -m \"{temperature:25}\"", credentials.getCredentialsId()));

        JsonNode dockerMqttCommands = commands.get(MQTT).get(DOCKER);
        assertThat(dockerMqttCommands.get(MQTT).asText()).isEqualTo(String.format("docker run --rm -it --add-host=host.docker.internal:host-gateway thingsboard/mosquitto-clients mosquitto_pub -d -q 1 -h host.docker.internal" +
                " -p 1883 -t v1/devices/me/telemetry -u \"%s\" -m \"{temperature:25}\"", credentials.getCredentialsId()));
        assertThat(dockerMqttCommands.get(MQTTS).asText()).isEqualTo(String.format("docker run --rm -it --add-host=host.docker.internal:host-gateway thingsboard/mosquitto-clients " +
                        "/bin/sh -c \"curl -f -S -o " + CA_ROOT_CERT_PEM + " http://localhost:80/api/device-connectivity/mqtts/certificate/download && " +
                        "mosquitto_pub -d -q 1 --cafile " + CA_ROOT_CERT_PEM + " -h host.docker.internal -p 8883 -t v1/devices/me/telemetry -u \"%s\" -m \"{temperature:25}\"\"",
                credentials.getCredentialsId()));

        JsonNode linuxCoapCommands = commands.get(COAP);
        assertThat(linuxCoapCommands.get(COAP).asText()).isEqualTo(String.format("coap-client -v 6 -m POST " +
                "-t \"application/json\" -e \"{temperature:25}\" coap://[::1]:5683/api/v1/%s/telemetry", credentials.getCredentialsId()));
        assertThat(linuxCoapCommands.get(COAPS).get(0).asText()).isEqualTo("curl -f -S -o " + CA_ROOT_CERT_PEM + " http://localhost:80/api/device-connectivity/coaps/certificate/download");
        assertThat(linuxCoapCommands.get(COAPS).get(1).asText()).isEqualTo(String.format("coap-client-openssl -v 6 -m POST " +
                "-R " + CA_ROOT_CERT_PEM + " -t \"application/json\" -e \"{temperature:25}\" coaps://[::1]:5684/api/v1/%s/telemetry", credentials.getCredentialsId()));

        JsonNode dockerCoapCommands = commands.get(COAP).get(DOCKER);
        assertThat(dockerCoapCommands.get(COAP).asText()).isEqualTo(String.format("docker run --rm -it --add-host=host.docker.internal:host-gateway" +
                " thingsboard/coap-clients coap-client -v 6 -m POST -t \"application/json\" -e \"{temperature:25}\" coap://host.docker.internal:5683/api/v1/%s/telemetry", credentials.getCredentialsId()));
        assertThat(dockerCoapCommands.get(COAPS).asText()).isEqualTo(String.format("docker run --rm -it --add-host=host.docker.internal:host-gateway" +
                " thingsboard/coap-clients " +
                "/bin/sh -c \"curl -f -S -o " + CA_ROOT_CERT_PEM + " http://localhost:80/api/device-connectivity/coaps/certificate/download && " +
                "coap-client-openssl -v 6 -m POST -R " + CA_ROOT_CERT_PEM + " -t \"application/json\" -e \"{temperature:25}\" coaps://host.docker.internal:5684/api/v1/%s/telemetry\"", credentials.getCredentialsId()));
    }

    @Test
    public void testFetchPublishTelemetryCommandsForDeviceWithIpV6Address() throws Exception {

        loginSysAdmin();
        setConnectivityHost("1:1:1:1:1:1:1:1");
        loginTenantAdmin();

        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        Device savedDevice = doPost("/api/device", device, Device.class);
        DeviceCredentials credentials =
                doGet("/api/device/" + savedDevice.getId().getId() + "/credentials", DeviceCredentials.class);

        JsonNode commands =
                doGetTyped("/api/device-connectivity/" + savedDevice.getId().getId(), new TypeReference<>() {
                });
        assertThat(commands).hasSize(3);
        JsonNode httpCommands = commands.get(HTTP);
        assertThat(httpCommands.get(HTTP).asText()).isEqualTo(String.format("curl -v -X POST http://[1:1:1:1:1:1:1:1]:8080/api/v1/%s/telemetry " +
                        "--header Content-Type:application/json --data \"{temperature:25}\"",
                credentials.getCredentialsId()));
        assertThat(httpCommands.get(HTTPS).asText()).isEqualTo(String.format("curl -v -X POST https://[1:1:1:1:1:1:1:1]/api/v1/%s/telemetry " +
                        "--header Content-Type:application/json --data \"{temperature:25}\"",
                credentials.getCredentialsId()));

        JsonNode mqttCommands = commands.get(MQTT);
        assertThat(mqttCommands.get(MQTT).asText()).isEqualTo(String.format("mosquitto_pub -d -q 1 -h 1:1:1:1:1:1:1:1 -p 1883 -t v1/devices/me/telemetry " +
                "-u \"%s\" -m \"{temperature:25}\"", credentials.getCredentialsId()));
        assertThat(mqttCommands.get(MQTTS).get(0).asText()).isEqualTo("curl -f -S -o " + CA_ROOT_CERT_PEM + " http://localhost:80/api/device-connectivity/mqtts/certificate/download");
        assertThat(mqttCommands.get(MQTTS).get(1).asText()).isEqualTo(String.format("mosquitto_pub -d -q 1 --cafile " + CA_ROOT_CERT_PEM + " -h 1:1:1:1:1:1:1:1 -p 8883 " +
                "-t v1/devices/me/telemetry -u \"%s\" -m \"{temperature:25}\"", credentials.getCredentialsId()));

        JsonNode dockerMqttCommands = commands.get(MQTT).get(DOCKER);
        assertThat(dockerMqttCommands.get(MQTT).asText()).isEqualTo(String.format("docker run --rm -it thingsboard/mosquitto-clients mosquitto_pub -d -q 1 -h 1:1:1:1:1:1:1:1" +
                " -p 1883 -t v1/devices/me/telemetry -u \"%s\" -m \"{temperature:25}\"", credentials.getCredentialsId()));
        assertThat(dockerMqttCommands.get(MQTTS).asText()).isEqualTo(String.format("docker run --rm -it thingsboard/mosquitto-clients " +
                        "/bin/sh -c \"curl -f -S -o " + CA_ROOT_CERT_PEM + " http://localhost:80/api/device-connectivity/mqtts/certificate/download && " +
                        "mosquitto_pub -d -q 1 --cafile " + CA_ROOT_CERT_PEM + " -h 1:1:1:1:1:1:1:1 -p 8883 -t v1/devices/me/telemetry -u \"%s\" -m \"{temperature:25}\"\"",
                credentials.getCredentialsId()));

        JsonNode linuxCoapCommands = commands.get(COAP);
        assertThat(linuxCoapCommands.get(COAP).asText()).isEqualTo(String.format("coap-client -v 6 -m POST " +
                "-t \"application/json\" -e \"{temperature:25}\" coap://[1:1:1:1:1:1:1:1]:5683/api/v1/%s/telemetry", credentials.getCredentialsId()));
        assertThat(linuxCoapCommands.get(COAPS).get(0).asText()).isEqualTo("curl -f -S -o " + CA_ROOT_CERT_PEM + " http://localhost:80/api/device-connectivity/coaps/certificate/download");
        assertThat(linuxCoapCommands.get(COAPS).get(1).asText()).isEqualTo(String.format("coap-client-openssl -v 6 -m POST -R " + CA_ROOT_CERT_PEM +
                " -t \"application/json\" -e \"{temperature:25}\" coaps://[1:1:1:1:1:1:1:1]:5684/api/v1/%s/telemetry", credentials.getCredentialsId()));

        JsonNode dockerCoapCommands = commands.get(COAP).get(DOCKER);
        assertThat(dockerCoapCommands.get(COAP).asText()).isEqualTo(String.format("docker run --rm -it" +
                " thingsboard/coap-clients coap-client -v 6 -m POST -t \"application/json\" -e \"{temperature:25}\" coap://[1:1:1:1:1:1:1:1]:5683/api/v1/%s/telemetry", credentials.getCredentialsId()));
        assertThat(dockerCoapCommands.get(COAPS).asText()).isEqualTo(String.format("docker run --rm -it" +
                " thingsboard/coap-clients " +
                "/bin/sh -c \"curl -f -S -o " + CA_ROOT_CERT_PEM + " http://localhost:80/api/device-connectivity/coaps/certificate/download && " +
                "coap-client-openssl -v 6 -m POST -R " + CA_ROOT_CERT_PEM + " -t \"application/json\" -e \"{temperature:25}\" coaps://[1:1:1:1:1:1:1:1]:5684/api/v1/%s/telemetry\"", credentials.getCredentialsId()));

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
        credentials = doPost("/api/device/credentials", credentials, DeviceCredentials.class);

        JsonNode commands =
                doGetTyped("/api/device-connectivity/" + savedDevice.getId().getId(), new TypeReference<>() {
                });
        assertThat(commands).hasSize(1);

        JsonNode mqttCommands = commands.get(MQTT);
        assertThat(mqttCommands.get(MQTT).asText()).isEqualTo(String.format("mosquitto_pub -d -q 1 -h localhost -p 1883 -t %s " +
                "-i \"%s\" -u \"%s\" -P \"%s\" -m \"{temperature:25}\"", DEVICE_TELEMETRY_TOPIC, clientId, userName, password));
        assertThat(mqttCommands.get(MQTTS).get(0).asText()).isEqualTo("curl -f -S -o " + CA_ROOT_CERT_PEM + " http://localhost:80/api/device-connectivity/mqtts/certificate/download");
        assertThat(mqttCommands.get(MQTTS).get(1).asText()).isEqualTo(String.format("mosquitto_pub -d -q 1 --cafile " + CA_ROOT_CERT_PEM + " -h localhost -p 8883 " +
                "-t %s -i \"%s\" -u \"%s\" -P \"%s\" -m \"{temperature:25}\"", DEVICE_TELEMETRY_TOPIC, clientId, userName, password));

        JsonNode dockerMqttCommands = commands.get(MQTT).get(DOCKER);
        assertThat(dockerMqttCommands.get(MQTT).asText()).isEqualTo(String.format("docker run --rm -it --add-host=host.docker.internal:host-gateway thingsboard/mosquitto-clients mosquitto_pub -d -q 1 -h host.docker.internal" +
                        " -p 1883 -t %s -i \"%s\" -u \"%s\" -P \"%s\" -m \"{temperature:25}\"",
                DEVICE_TELEMETRY_TOPIC, clientId, userName, password));
        assertThat(dockerMqttCommands.get(MQTTS).asText()).isEqualTo(String.format("docker run --rm -it --add-host=host.docker.internal:host-gateway thingsboard/mosquitto-clients " +
                        "/bin/sh -c \"curl -f -S -o " + CA_ROOT_CERT_PEM + " http://localhost:80/api/device-connectivity/mqtts/certificate/download && " +
                        "mosquitto_pub -d -q 1 --cafile " + CA_ROOT_CERT_PEM + " -h host.docker.internal -p 8883 -t %s -i \"%s\" -u \"%s\" -P \"%s\" -m \"{temperature:25}\"\"",
                DEVICE_TELEMETRY_TOPIC, clientId, userName, password));

        basicMqttCredentials.setClientId("");
        credentials.setCredentialsValue(JacksonUtil.toString(basicMqttCredentials));

        doPost("/api/device/credentials", credentials)
                .andExpect(status().isOk());

        commands =
                doGetTyped("/api/device-connectivity/" + savedDevice.getId().getId(), new TypeReference<>() {
                });
        assertThat(commands).hasSize(1);

        mqttCommands = commands.get(MQTT);
        assertThat(mqttCommands.get(MQTT).asText()).isEqualTo(String.format("mosquitto_pub -d -q 1 -h localhost -p 1883 -t %s " +
                "-u \"%s\" -P \"%s\" -m \"{temperature:25}\"", DEVICE_TELEMETRY_TOPIC, userName, password));
        assertThat(mqttCommands.get(MQTTS).get(0).asText()).isEqualTo("curl -f -S -o " + CA_ROOT_CERT_PEM + " http://localhost:80/api/device-connectivity/mqtts/certificate/download");
        assertThat(mqttCommands.get(MQTTS).get(1).asText()).isEqualTo(String.format("mosquitto_pub -d -q 1 --cafile " + CA_ROOT_CERT_PEM + " -h localhost -p 8883 " +
                "-t %s -u \"%s\" -P \"%s\" -m \"{temperature:25}\"", DEVICE_TELEMETRY_TOPIC, userName, password));

        dockerMqttCommands = commands.get(MQTT).get(DOCKER);
        assertThat(dockerMqttCommands.get(MQTT).asText()).isEqualTo(String.format("docker run --rm -it --add-host=host.docker.internal:host-gateway thingsboard/mosquitto-clients mosquitto_pub -d -q 1 -h host.docker.internal" +
                        " -p 1883 -t %s -u \"%s\" -P \"%s\" -m \"{temperature:25}\"",
                DEVICE_TELEMETRY_TOPIC, userName, password));
        assertThat(dockerMqttCommands.get(MQTTS).asText()).isEqualTo(String.format("docker run --rm -it --add-host=host.docker.internal:host-gateway thingsboard/mosquitto-clients " +
                        "/bin/sh -c \"curl -f -S -o " + CA_ROOT_CERT_PEM + " http://localhost:80/api/device-connectivity/mqtts/certificate/download && " +
                        "mosquitto_pub -d -q 1 --cafile " + CA_ROOT_CERT_PEM + " -h host.docker.internal -p 8883 -t %s -u \"%s\" -P \"%s\" -m \"{temperature:25}\"\"",
                DEVICE_TELEMETRY_TOPIC, userName, password));
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
        assertThat(linuxCommands.get(COAP).asText()).isEqualTo(String.format("coap-client -v 6 -m POST -t \"application/json\" -e \"{temperature:25}\" coap://localhost:5683/api/v1/%s/telemetry",
                credentials.getCredentialsId()));
        assertThat(linuxCommands.get(COAPS).get(0).asText()).isEqualTo("curl -f -S -o " + CA_ROOT_CERT_PEM + " http://localhost:80/api/device-connectivity/coaps/certificate/download");
        assertThat(linuxCommands.get(COAPS).get(1).asText()).isEqualTo(String.format("coap-client-openssl -v 6 -m POST -R " + CA_ROOT_CERT_PEM + " -t \"application/json\" -e \"{temperature:25}\" coaps://localhost:5684/api/v1/%s/telemetry",
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
        Path path = Files.createFile(Path.of("/tmp/" + CA_ROOT_CERT_PEM));
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
        Path path = Files.createFile(Path.of("/tmp/" + CA_ROOT_CERT_PEM));
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

    @Test
    public void testFetchPublishTelemetryCommandsForDefaultDeviceIfPortsSetToDefault() throws Exception {
        loginSysAdmin();

        ObjectNode config = JacksonUtil.newObjectNode();

        ObjectNode http = JacksonUtil.newObjectNode();
        http.put("enabled", true);
        http.put("host", "");
        http.put("port", 80);
        config.set("http", http);

        ObjectNode https = JacksonUtil.newObjectNode();
        https.put("enabled", true);
        https.put("host", "");
        https.put("port", 443);
        config.set("https", https);

        ObjectNode mqtt = JacksonUtil.newObjectNode();
        mqtt.put("enabled", false);
        mqtt.put("host", "");
        mqtt.put("port", 1883);
        config.set("mqtt", mqtt);

        ObjectNode mqtts = JacksonUtil.newObjectNode();
        mqtts.put("enabled", false);
        mqtts.put("host", "");
        mqtts.put("port", 8883);
        config.set("mqtts", mqtts);

        ObjectNode coap = JacksonUtil.newObjectNode();
        coap.put("enabled", false);
        coap.put("host", "");
        coap.put("port", 5683);
        config.set("coap", coap);

        ObjectNode coaps = JacksonUtil.newObjectNode();
        coaps.put("enabled", false);
        coaps.put("host", "");
        coaps.put("port", 5684);
        config.set("coaps", coaps);

        AdminSettings adminSettings = doGet("/api/admin/settings/connectivity", AdminSettings.class);
        adminSettings.setJsonValue(config);
        doPost("/api/admin/settings", adminSettings).andExpect(status().isOk());

        login("tenant2@thingsboard.org", "testPassword1");

        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        Device savedDevice = doPost("/api/device", device, Device.class);
        JsonNode commands =
                doGetTyped("/api/device-connectivity/" + savedDevice.getId().getId(), new TypeReference<>() {
                });

        DeviceCredentials credentials =
                doGet("/api/device/" + savedDevice.getId().getId() + "/credentials", DeviceCredentials.class);

        assertThat(commands).hasSize(1);
        JsonNode httpCommands = commands.get(HTTP);
        assertThat(httpCommands.get(HTTP).asText()).isEqualTo(String.format("curl -v -X POST http://localhost/api/v1/%s/telemetry " +
                        "--header Content-Type:application/json --data \"{temperature:25}\"",
                credentials.getCredentialsId()));
        assertThat(httpCommands.get(HTTPS).asText()).isEqualTo(String.format("curl -v -X POST https://localhost/api/v1/%s/telemetry " +
                        "--header Content-Type:application/json --data \"{temperature:25}\"",
                credentials.getCredentialsId()));
    }

    @Test
    public void testFetchPublishTelemetryCommandsForDefaultDeviceIfPortsAndHostsNull() throws Exception {
        loginSysAdmin();

        ObjectNode config = JacksonUtil.newObjectNode();

        ObjectNode http = JacksonUtil.newObjectNode();
        http.put("enabled", true);
        http.set("host", null);
        http.set("port", null);
        config.set("http", http);

        ObjectNode https = JacksonUtil.newObjectNode();
        https.put("enabled", true);
        https.set("host", null);
        https.set("port", null);
        config.set("https", https);

        ObjectNode mqtt = JacksonUtil.newObjectNode();
        mqtt.put("enabled", true);
        mqtt.set("host", null);
        mqtt.set("port", null);
        config.set("mqtt", mqtt);

        ObjectNode mqtts = JacksonUtil.newObjectNode();
        mqtts.put("enabled", true);
        mqtts.set("host", null);
        mqtts.set("port", null);
        config.set("mqtts", mqtts);

        ObjectNode coap = JacksonUtil.newObjectNode();
        coap.put("enabled", true);
        coap.set("host", null);
        coap.set("port", null);
        config.set("coap", coap);

        ObjectNode coaps = JacksonUtil.newObjectNode();
        coaps.put("enabled", true);
        coaps.set("host", null);
        coaps.set("port", null);
        config.set("coaps", coaps);

        AdminSettings adminSettings = doGet("/api/admin/settings/connectivity", AdminSettings.class);
        adminSettings.setJsonValue(config);
        doPost("/api/admin/settings", adminSettings).andExpect(status().isOk());

        login("tenant2@thingsboard.org", "testPassword1");

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
        assertThat(httpCommands.get(HTTP).asText()).isEqualTo(String.format("curl -v -X POST http://localhost/api/v1/%s/telemetry " +
                        "--header Content-Type:application/json --data \"{temperature:25}\"",
                credentials.getCredentialsId()));
        assertThat(httpCommands.get(HTTPS).asText()).isEqualTo(String.format("curl -v -X POST https://localhost/api/v1/%s/telemetry " +
                        "--header Content-Type:application/json --data \"{temperature:25}\"",
                credentials.getCredentialsId()));

        JsonNode mqttCommands = commands.get(MQTT);
        assertThat(mqttCommands.get(MQTT).asText()).isEqualTo(String.format("mosquitto_pub -d -q 1 -h localhost -t v1/devices/me/telemetry " +
                "-u \"%s\" -m \"{temperature:25}\"", credentials.getCredentialsId()));
        assertThat(mqttCommands.get(MQTTS).get(0).asText()).isEqualTo("curl -f -S -o " + CA_ROOT_CERT_PEM + " http://localhost:80/api/device-connectivity/mqtts/certificate/download");
        assertThat(mqttCommands.get(MQTTS).get(1).asText()).isEqualTo(String.format("mosquitto_pub -d -q 1 --cafile " + CA_ROOT_CERT_PEM + " -h localhost " +
                "-t v1/devices/me/telemetry -u \"%s\" -m \"{temperature:25}\"", credentials.getCredentialsId()));

        JsonNode dockerMqttCommands = commands.get(MQTT).get(DOCKER);
        assertThat(dockerMqttCommands.get(MQTT).asText()).isEqualTo(String.format("docker run --rm -it --add-host=host.docker.internal:host-gateway thingsboard/mosquitto-clients mosquitto_pub -d -q 1 -h host.docker.internal" +
                " -t v1/devices/me/telemetry -u \"%s\" -m \"{temperature:25}\"", credentials.getCredentialsId()));
        assertThat(dockerMqttCommands.get(MQTTS).asText()).isEqualTo(String.format("docker run --rm -it --add-host=host.docker.internal:host-gateway thingsboard/mosquitto-clients " +
                "/bin/sh -c \"curl -f -S -o " + CA_ROOT_CERT_PEM + " http://localhost:80/api/device-connectivity/mqtts/certificate/download && " +
                "mosquitto_pub -d -q 1 --cafile " + CA_ROOT_CERT_PEM + " -h host.docker.internal -t v1/devices/me/telemetry -u \"%s\" -m \"{temperature:25}\"\"", credentials.getCredentialsId()));

        JsonNode linuxCoapCommands = commands.get(COAP);
        assertThat(linuxCoapCommands.get(COAP).asText()).isEqualTo(String.format("coap-client -v 6 -m POST " +
                "-t \"application/json\" -e \"{temperature:25}\" coap://localhost/api/v1/%s/telemetry", credentials.getCredentialsId()));
        assertThat(linuxCoapCommands.get(COAPS).get(0).asText()).isEqualTo("curl -f -S -o " + CA_ROOT_CERT_PEM + " http://localhost:80/api/device-connectivity/coaps/certificate/download");
        assertThat(linuxCoapCommands.get(COAPS).get(1).asText()).isEqualTo(String.format("coap-client-openssl -v 6 -m POST " +
                "-R " + CA_ROOT_CERT_PEM + " -t \"application/json\" -e \"{temperature:25}\" coaps://localhost/api/v1/%s/telemetry", credentials.getCredentialsId()));

        JsonNode dockerCoapCommands = commands.get(COAP).get(DOCKER);
        assertThat(dockerCoapCommands.get(COAP).asText()).isEqualTo(String.format("docker run --rm -it --add-host=host.docker.internal:host-gateway" +
                " thingsboard/coap-clients coap-client -v 6 -m POST -t \"application/json\" -e \"{temperature:25}\" coap://host.docker.internal/api/v1/%s/telemetry", credentials.getCredentialsId()));
        assertThat(dockerCoapCommands.get(COAPS).asText()).isEqualTo(String.format("docker run --rm -it --add-host=host.docker.internal:host-gateway" +
                " thingsboard/coap-clients /bin/sh -c \"curl -f -S -o " + CA_ROOT_CERT_PEM + " http://localhost:80/api/device-connectivity/coaps/certificate/download && " +
                "coap-client-openssl -v 6 -m POST -R " + CA_ROOT_CERT_PEM + " -t \"application/json\" -e \"{temperature:25}\" coaps://host.docker.internal/api/v1/%s/telemetry\"", credentials.getCredentialsId()));
    }

    @Test
    public void testFetchPublishTelemetryCommandsForDefaultDeviceIfHostIsNotLocalhost() throws Exception {
        loginSysAdmin();

        setConnectivityHost("test.domain");

        login("tenant2@thingsboard.org", "testPassword1");

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
        assertThat(httpCommands.get(HTTP).asText()).isEqualTo(String.format("curl -v -X POST http://test.domain:8080/api/v1/%s/telemetry " +
                        "--header Content-Type:application/json --data \"{temperature:25}\"",
                credentials.getCredentialsId()));
        assertThat(httpCommands.get(HTTPS).asText()).isEqualTo(String.format("curl -v -X POST https://test.domain/api/v1/%s/telemetry " +
                        "--header Content-Type:application/json --data \"{temperature:25}\"",
                credentials.getCredentialsId()));


        JsonNode mqttCommands = commands.get(MQTT);
        assertThat(mqttCommands.get(MQTT).asText()).isEqualTo(String.format("mosquitto_pub -d -q 1 -h test.domain -p 1883 -t v1/devices/me/telemetry " +
                        "-u \"%s\" -m \"{temperature:25}\"",
                credentials.getCredentialsId()));
        assertThat(mqttCommands.get(MQTTS).get(0).asText()).isEqualTo("curl -f -S -o " + CA_ROOT_CERT_PEM + " http://localhost:80/api/device-connectivity/mqtts/certificate/download");
        assertThat(mqttCommands.get(MQTTS).get(1).asText()).isEqualTo(String.format("mosquitto_pub -d -q 1 --cafile " + CA_ROOT_CERT_PEM + " -h test.domain -p 8883 " +
                "-t v1/devices/me/telemetry -u \"%s\" -m \"{temperature:25}\"", credentials.getCredentialsId()));

        JsonNode dockerMqttCommands = commands.get(MQTT).get(DOCKER);
        assertThat(dockerMqttCommands.get(MQTT).asText()).isEqualTo(String.format("docker run --rm -it thingsboard/mosquitto-clients mosquitto_pub -d -q 1 -h test.domain" +
                        " -p 1883 -t v1/devices/me/telemetry -u \"%s\" -m \"{temperature:25}\"",
                credentials.getCredentialsId()));
        assertThat(dockerMqttCommands.get(MQTTS).asText()).isEqualTo(String.format("docker run --rm -it thingsboard/mosquitto-clients " +
                        "/bin/sh -c \"curl -f -S -o " + CA_ROOT_CERT_PEM + " http://localhost:80/api/device-connectivity/mqtts/certificate/download && " +
                        "mosquitto_pub -d -q 1 --cafile " + CA_ROOT_CERT_PEM + " -h test.domain -p 8883 -t v1/devices/me/telemetry -u \"%s\" -m \"{temperature:25}\"\"",
                credentials.getCredentialsId()));

        JsonNode linuxCoapCommands = commands.get(COAP);
        assertThat(linuxCoapCommands.get(COAP).asText()).isEqualTo(String.format("coap-client -v 6 -m POST " +
                "-t \"application/json\" -e \"{temperature:25}\" coap://test.domain:5683/api/v1/%s/telemetry", credentials.getCredentialsId()));
        assertThat(linuxCoapCommands.get(COAPS).get(0).asText()).isEqualTo("curl -f -S -o " + CA_ROOT_CERT_PEM + " http://localhost:80/api/device-connectivity/coaps/certificate/download");
        assertThat(linuxCoapCommands.get(COAPS).get(1).asText()).isEqualTo(String.format("coap-client-openssl -v 6 -m POST " +
                "-R " + CA_ROOT_CERT_PEM + " -t \"application/json\" -e \"{temperature:25}\" coaps://test.domain:5684/api/v1/%s/telemetry", credentials.getCredentialsId()));

        JsonNode dockerCoapCommands = commands.get(COAP).get(DOCKER);
        assertThat(dockerCoapCommands.get(COAP).asText()).isEqualTo(String.format("docker run --rm -it " +
                "thingsboard/coap-clients coap-client -v 6 -m POST -t \"application/json\" -e \"{temperature:25}\" coap://test.domain:5683/api/v1/%s/telemetry", credentials.getCredentialsId()));
        assertThat(dockerCoapCommands.get(COAPS).asText()).isEqualTo(String.format("docker run --rm -it " +
                "thingsboard/coap-clients /bin/sh -c \"curl -f -S -o " + CA_ROOT_CERT_PEM + " http://localhost:80/api/device-connectivity/coaps/certificate/download && " +
                "coap-client-openssl -v 6 -m POST -R " + CA_ROOT_CERT_PEM + " -t \"application/json\" -e \"{temperature:25}\" coaps://test.domain:5684/api/v1/%s/telemetry\"", credentials.getCredentialsId()));
    }

    @Test
    public void testFetchPublishTelemetryCommandsForDeviceWhenHostSetToNullInSettings() throws Exception {
        loginSysAdmin();
        ObjectNode config = JacksonUtil.newObjectNode();

        ObjectNode http = JacksonUtil.newObjectNode();
        http.put("enabled", true);
        http.put("host", "  ");
        http.put("port", 8080);
        config.set("http", http);

        ObjectNode https = JacksonUtil.newObjectNode();
        https.put("enabled", true);
        https.put("host", "");
        https.put("port", 443);
        config.set("https", https);

        ObjectNode mqtt = JacksonUtil.newObjectNode();
        mqtt.put("enabled", true);
        mqtt.set("host", NullNode.getInstance());
        mqtt.set("port", NullNode.getInstance());
        config.set("mqtt", mqtt);

        ObjectNode mqtts = JacksonUtil.newObjectNode();
        mqtts.put("enabled", true);
        mqtts.put("host", "");
        mqtts.set("port", NullNode.getInstance());
        config.set("mqtts", mqtts);

        ObjectNode coap = JacksonUtil.newObjectNode();
        coap.put("enabled", true);
        coap.set("host", NullNode.getInstance());
        coap.put("port", "");
        config.set("coap", coap);

        ObjectNode coaps = JacksonUtil.newObjectNode();
        coaps.put("enabled", true);
        coaps.set("host", NullNode.getInstance());
        coaps.set("port", NullNode.getInstance());
        config.set("coaps", coaps);

        AdminSettings adminSettings = doGet("/api/admin/settings/connectivity", AdminSettings.class);
        adminSettings.setJsonValue(config);
        doPost("/api/admin/settings", adminSettings).andExpect(status().isOk());


        login("tenant2@thingsboard.org", "testPassword1");

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
        assertThat(httpCommands.get(HTTP).asText()).isEqualTo(String.format("curl -v -X POST http://localhost:8080/api/v1/%s/telemetry --header Content-Type:application/json --data \"{temperature:25}\"", credentials.getCredentialsId()));
        assertThat(httpCommands.get(HTTPS).asText()).isEqualTo(String.format("curl -v -X POST https://localhost/api/v1/%s/telemetry --header Content-Type:application/json --data \"{temperature:25}\"", credentials.getCredentialsId()));

        JsonNode mqttCommands = commands.get(MQTT);
        assertThat(mqttCommands.get(MQTT).asText()).isEqualTo(String.format("mosquitto_pub -d -q 1 -h localhost -t v1/devices/me/telemetry -u \"%s\" -m \"{temperature:25}\"", credentials.getCredentialsId()));
        assertThat(mqttCommands.get(MQTTS).get(0).asText()).isEqualTo("curl -f -S -o " + CA_ROOT_CERT_PEM + " http://localhost:80/api/device-connectivity/mqtts/certificate/download");
        assertThat(mqttCommands.get(MQTTS).get(1).asText()).isEqualTo(String.format("mosquitto_pub -d -q 1 --cafile " + CA_ROOT_CERT_PEM + " -h localhost -t v1/devices/me/telemetry -u \"%s\" -m \"{temperature:25}\"", credentials.getCredentialsId()));

        JsonNode dockerMqttCommands = mqttCommands.get(DOCKER);
        assertThat(dockerMqttCommands.get(MQTT).asText()).isEqualTo(String.format("docker run --rm -it --add-host=host.docker.internal:host-gateway thingsboard/mosquitto-clients mosquitto_pub -d -q 1 -h host.docker.internal -t v1/devices/me/telemetry -u \"%s\" -m \"{temperature:25}\"", credentials.getCredentialsId()));
        assertThat(dockerMqttCommands.get(MQTTS).asText()).isEqualTo(String.format("docker run --rm -it --add-host=host.docker.internal:host-gateway thingsboard/mosquitto-clients /bin/sh -c \"curl -f -S -o " + CA_ROOT_CERT_PEM + " http://localhost:80/api/device-connectivity/mqtts/certificate/download && mosquitto_pub -d -q 1 --cafile " + CA_ROOT_CERT_PEM + " -h host.docker.internal -t v1/devices/me/telemetry -u \"%s\" -m \"{temperature:25}\"\"", credentials.getCredentialsId()));

        JsonNode coapCommands = commands.get(COAP);
        assertThat(coapCommands.get(COAP).asText()).isEqualTo(String.format("coap-client -v 6 -m POST -t \"application/json\" -e \"{temperature:25}\" coap://localhost/api/v1/%s/telemetry", credentials.getCredentialsId()));
        assertThat(coapCommands.get(COAPS).get(0).asText()).isEqualTo("curl -f -S -o " + CA_ROOT_CERT_PEM + " http://localhost:80/api/device-connectivity/coaps/certificate/download");
        assertThat(coapCommands.get(COAPS).get(1).asText()).isEqualTo(String.format("coap-client-openssl -v 6 -m POST " +
                "-R " + CA_ROOT_CERT_PEM + " -t \"application/json\" -e \"{temperature:25}\" coaps://localhost/api/v1/%s/telemetry", credentials.getCredentialsId()));

        JsonNode dockerCoapCommands = coapCommands.get(DOCKER);
        assertThat(dockerCoapCommands.get(COAP).asText()).isEqualTo(String.format("docker run --rm -it --add-host=host.docker.internal:host-gateway " +
                "thingsboard/coap-clients coap-client -v 6 -m POST -t \"application/json\" -e \"{temperature:25}\" coap://host.docker.internal/api/v1/%s/telemetry", credentials.getCredentialsId()));
        assertThat(dockerCoapCommands.get(COAPS).asText()).isEqualTo(String.format("docker run --rm -it --add-host=host.docker.internal:host-gateway " +
                "thingsboard/coap-clients /bin/sh -c \"curl -f -S -o " + CA_ROOT_CERT_PEM + " http://localhost:80/api/device-connectivity/coaps/certificate/download && " +
                "coap-client-openssl -v 6 -m POST -R " + CA_ROOT_CERT_PEM + " -t \"application/json\" -e \"{temperature:25}\" coaps://host.docker.internal/api/v1/%s/telemetry\"", credentials.getCredentialsId()));
    }


    private void setConnectivityHost(String host) throws Exception {
        ObjectNode config = JacksonUtil.newObjectNode();

        ObjectNode http = JacksonUtil.newObjectNode();
        http.put("enabled", true);
        http.put("host", host);
        http.put("port", 8080);
        config.set("http", http);

        ObjectNode https = JacksonUtil.newObjectNode();
        https.put("enabled", true);
        https.put("host", host);
        https.put("port", 443);
        config.set("https", https);

        ObjectNode mqtt = JacksonUtil.newObjectNode();
        mqtt.put("enabled", true);
        mqtt.put("host", host);
        mqtt.put("port", 1883);
        config.set("mqtt", mqtt);

        ObjectNode mqtts = JacksonUtil.newObjectNode();
        mqtts.put("enabled", true);
        mqtts.put("host", host);
        mqtts.put("port", 8883);
        config.set("mqtts", mqtts);

        ObjectNode coap = JacksonUtil.newObjectNode();
        coap.put("enabled", true);
        coap.put("host", host);
        coap.put("port", 5683);
        config.set("coap", coap);

        ObjectNode coaps = JacksonUtil.newObjectNode();
        coaps.put("enabled", true);
        coaps.put("host", host);
        coaps.put("port", 5684);
        config.set("coaps", coaps);

        AdminSettings adminSettings = doGet("/api/admin/settings/connectivity", AdminSettings.class);
        adminSettings.setJsonValue(config);
        doPost("/api/admin/settings", adminSettings).andExpect(status().isOk());
    }
}

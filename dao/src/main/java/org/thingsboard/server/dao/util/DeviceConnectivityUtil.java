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
package org.thingsboard.server.dao.util;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.device.credentials.BasicMqttCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.dao.device.DeviceConnectivityInfo;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class DeviceConnectivityUtil {

    public static final String HTTP = "http";
    public static final String HTTPS = "https";
    public static final String MQTT = "mqtt";
    public static final String DOCKER = "docker";
    public static final String MQTTS = "mqtts";
    public static final String COAP = "coap";
    public static final String COAPS = "coaps";
    public static final String CA_ROOT_CERT_PEM = "ca-root.pem";
    public static final String DOCKER_COMPOSE_YML = "docker-compose.yml";
    public static final String CHECK_DOCUMENTATION = "Check documentation";
    public static final String JSON_EXAMPLE_PAYLOAD = "\"{temperature:25}\"";
    public static final String DOCKER_RUN = "docker run --rm -it ";
    public static final String HOST_DOCKER_INTERNAL = "host.docker.internal";
    public static final String ADD_DOCKER_INTERNAL_HOST = "--add-host=" + HOST_DOCKER_INTERNAL + ":host-gateway ";
    public static final String MQTT_IMAGE = "thingsboard/mosquitto-clients ";
    public static final String COAP_IMAGE = "thingsboard/coap-clients ";
    private final static Pattern VALID_URL_PATTERN = Pattern.compile("^(https?)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");

    public static String getHttpPublishCommand(String protocol, String host, String port, DeviceCredentials deviceCredentials) {
        return String.format("curl -v -X POST %s://%s%s/api/v1/%s/telemetry --header Content-Type:application/json --data " + JSON_EXAMPLE_PAYLOAD,
                protocol, host, port, deviceCredentials.getCredentialsId());
    }

    public static String getMqttPublishCommand(String protocol, String host, String port, String deviceTelemetryTopic, DeviceCredentials deviceCredentials) {
        StringBuilder command = new StringBuilder("mosquitto_pub -d -q 1");
        if (MQTTS.equals(protocol)) {
            command.append(" --cafile ").append(CA_ROOT_CERT_PEM);
        }
        command.append(" -h ").append(host).append(StringUtils.isBlank(port) ? "" : " -p " + port);
        command.append(" -t ").append(deviceTelemetryTopic);

        switch (deviceCredentials.getCredentialsType()) {
            case ACCESS_TOKEN:
                command.append(" -u \"").append(deviceCredentials.getCredentialsId()).append("\"");
                break;
            case MQTT_BASIC:
                BasicMqttCredentials credentials = JacksonUtil.fromString(deviceCredentials.getCredentialsValue(),
                        BasicMqttCredentials.class);
                if (credentials != null) {
                    if (StringUtils.isNotEmpty(credentials.getClientId())) {
                        command.append(" -i \"").append(credentials.getClientId()).append("\"");
                    }
                    if (StringUtils.isNotEmpty(credentials.getUserName())) {
                        command.append(" -u \"").append(credentials.getUserName()).append("\"");
                    }
                    if (StringUtils.isNotEmpty(credentials.getPassword())) {
                        command.append(" -P \"").append(credentials.getPassword()).append("\"");
                    }
                } else {
                    return null;
                }
                break;
            default:
                return null;
        }
        command.append(" -m " + JSON_EXAMPLE_PAYLOAD);
        return command.toString();
    }

    public static Resource getGatewayDockerComposeFile(String host, String gatewayImageVersion, DeviceCredentials deviceCredentials) {
        StringBuilder dockerComposeBuilder = new StringBuilder();
        dockerComposeBuilder.append("version: '3.4'\n");
        dockerComposeBuilder.append("services:\n");
        dockerComposeBuilder.append("  # ThingsBoard IoT Gateway Service Configuration\n");
        dockerComposeBuilder.append("  tb-gateway:\n");
        dockerComposeBuilder.append("    image: thingsboard/tb-gateway:").append(gatewayImageVersion).append("\n");
        dockerComposeBuilder.append("    container_name: tb-gateway\n");
        dockerComposeBuilder.append("    restart: always\n");
        dockerComposeBuilder.append("\n");
        dockerComposeBuilder.append("    # Ports bindings - required by some connectors\n");
        dockerComposeBuilder.append("    ports:\n");
        dockerComposeBuilder.append("        - \"5000:5000\" # Comment if you don't use REST connector and change if you use another port\n");
        dockerComposeBuilder.append("        # Uncomment and modify the following ports based on connector usage:\n");
        dockerComposeBuilder.append("#        - \"1052:1052\" # BACnet connector\n");
        dockerComposeBuilder.append("#        - \"5026:5026\" # Modbus TCP connector (Modbus Slave)\n");
        dockerComposeBuilder.append("#        - \"50000:50000/tcp\" # Socket connector with type TCP\n");
        dockerComposeBuilder.append("#        - \"50000:50000/udp\" # Socket connector with type UDP\n");
        dockerComposeBuilder.append("\n");
        dockerComposeBuilder.append("    # Necessary mapping for Linux\n");
        dockerComposeBuilder.append("    extra_hosts:\n");
        dockerComposeBuilder.append("      - \"host.docker.internal:host-gateway\"\n");
        dockerComposeBuilder.append("\n");
        dockerComposeBuilder.append("    # Environment variables\n");
        dockerComposeBuilder.append("    environment:\n");
        dockerComposeBuilder.append("      - TB_GW_HOST=").append(isLocalhost(host) ? HOST_DOCKER_INTERNAL : host).append("\n");
        dockerComposeBuilder.append("      - TB_GW_PORT=1883\n");
        switch (deviceCredentials.getCredentialsType()) {
            case ACCESS_TOKEN:
                dockerComposeBuilder.append("      - TB_GW_SECURITY_TYPE=accessToken\n");
                dockerComposeBuilder.append("      - TB_GW_ACCESS_TOKEN=").append(deviceCredentials.getCredentialsId()).append("\n");
                break;
            case MQTT_BASIC:
                dockerComposeBuilder.append("      - TB_GW_SECURITY_TYPE=usernamePassword\n");
                BasicMqttCredentials credentials = JacksonUtil.fromString(deviceCredentials.getCredentialsValue(),
                        BasicMqttCredentials.class);
                if (credentials != null) {
                    if (StringUtils.isNotEmpty(credentials.getClientId())) {
                        dockerComposeBuilder.append("      - TB_GW_CLIENT_ID=").append(credentials.getClientId()).append("\n");
                    }
                    if (StringUtils.isNotEmpty(credentials.getUserName())) {
                        dockerComposeBuilder.append("      - TB_GW_USERNAME=").append(credentials.getUserName()).append("\n");
                    }
                    if (StringUtils.isNotEmpty(credentials.getPassword())) {
                        dockerComposeBuilder.append("      - TB_GW_PASSWORD=").append(credentials.getPassword()).append("\n");
                    }
                }
                break;
        }
        dockerComposeBuilder.append("\n");
        dockerComposeBuilder.append("    # Volumes bind\n");
        dockerComposeBuilder.append("    volumes:\n");
        dockerComposeBuilder.append("      - tb-gw-config:/thingsboard_gateway/config\n");
        dockerComposeBuilder.append("      - tb-gw-logs:/thingsboard_gateway/logs\n");
        dockerComposeBuilder.append("      - tb-gw-extensions:/thingsboard_gateway/extensions\n");
        dockerComposeBuilder.append("\n");
        dockerComposeBuilder.append("# Volumes declaration for configurations, extensions and configuration\n");
        dockerComposeBuilder.append("volumes:\n");
        dockerComposeBuilder.append("  tb-gw-config:\n");
        dockerComposeBuilder.append("    name: tb-gw-config\n");
        dockerComposeBuilder.append("  tb-gw-logs:\n");
        dockerComposeBuilder.append("    name: tb-gw-logs\n");
        dockerComposeBuilder.append("  tb-gw-extensions:\n");
        dockerComposeBuilder.append("    name: tb-gw-extensions\n");

        return new ByteArrayResource(dockerComposeBuilder.toString().getBytes(StandardCharsets.UTF_8));
    }

    public static String getDockerMqttPublishCommand(String protocol, String baseUrl, String host, String port, String deviceTelemetryTopic, DeviceCredentials deviceCredentials) {
        String mqttCommand = getMqttPublishCommand(protocol, host, port, deviceTelemetryTopic, deviceCredentials);

        if (mqttCommand == null) {
            return null;
        }

        StringBuilder mqttDockerCommand = new StringBuilder();
        mqttDockerCommand.append(DOCKER_RUN).append(isLocalhost(host) ? ADD_DOCKER_INTERNAL_HOST : "").append(MQTT_IMAGE);

        if (isLocalhost(host)) {
            mqttCommand = mqttCommand.replace(host, HOST_DOCKER_INTERNAL);
        }

        if (MQTTS.equals(protocol)) {
            mqttDockerCommand.append("/bin/sh -c \"")
                    .append(getCurlPemCertCommand(baseUrl, protocol))
                    .append(" && ")
                    .append(mqttCommand)
                    .append("\"");
        } else {
            mqttDockerCommand.append(mqttCommand);
        }

        return mqttDockerCommand.toString();
    }

    public static String getCurlPemCertCommand(String baseUrl, String protocol) {
        return getCurlPemCertCommand(baseUrl, protocol, CA_ROOT_CERT_PEM);
    }

    public static String getCurlPemCertCommand(String baseUrl, String protocol, String caCertFilePath) {
        return String.format("curl -f -S -o %s %s/api/device-connectivity/%s/certificate/download", caCertFilePath, baseUrl, protocol);
    }

    public static String getCoapPublishCommand(String protocol, String host, String port, DeviceCredentials deviceCredentials) {
        switch (deviceCredentials.getCredentialsType()) {
            case ACCESS_TOKEN:
                String client = COAPS.equals(protocol) ? "coap-client-openssl" : "coap-client";
                String certificate = COAPS.equals(protocol) ? " -R " + CA_ROOT_CERT_PEM : "";
                return String.format("%s -v 6 -m POST%s -t \"application/json\" -e %s %s://%s%s/api/v1/%s/telemetry",
                        client, certificate, JSON_EXAMPLE_PAYLOAD, protocol, host, port, deviceCredentials.getCredentialsId());
            default:
                return null;
        }
    }

    public static String getDockerCoapPublishCommand(String protocol, String baseUrl, String host, String port, DeviceCredentials deviceCredentials) {
        String coapCommand = getCoapPublishCommand(protocol, host, port, deviceCredentials);

        if (coapCommand == null) {
            return null;
        }

        StringBuilder coapDockerCommand = new StringBuilder();
        coapDockerCommand.append(DOCKER_RUN).append(isLocalhost(host) ? ADD_DOCKER_INTERNAL_HOST : "").append(COAP_IMAGE);

        if (isLocalhost(host)) {
            coapCommand = coapCommand.replace(host, HOST_DOCKER_INTERNAL);
        }

        if (COAPS.equals(protocol)) {
            coapDockerCommand.append("/bin/sh -c \"")
                    .append(getCurlPemCertCommand(baseUrl, protocol))
                    .append(" && ")
                    .append(coapCommand)
                    .append("\"");
        } else {
            coapDockerCommand.append(coapCommand);
        }

        return coapDockerCommand.toString();
    }

    public static String getHost(String baseUrl, DeviceConnectivityInfo properties, String protocol) throws URISyntaxException {
        String initialHost = StringUtils.isBlank(properties.getHost()) ? baseUrl : properties.getHost();
        InetAddress inetAddress;
        String host = null;
        if (VALID_URL_PATTERN.matcher(initialHost).matches()) {
            host = new URI(initialHost).getHost();
        }
        if (host == null) {
            host = initialHost;
        }
        try {
            host = host.replaceAll("^https?://", "");
            inetAddress = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            return host;
        }
        if (inetAddress instanceof Inet6Address) {
            host = host.replaceAll("[\\[\\]]", "");
            if (!MQTT.equals(protocol) && !MQTTS.equals(protocol)) {
                host = "[" + host + "]";
            }
        }
        return host;
    }

    public static String getPort(DeviceConnectivityInfo properties) {
        return StringUtils.isBlank(properties.getPort()) ? "" : properties.getPort();
    }

    public static boolean isLocalhost(String host) {
        try {
            InetAddress inetAddress = InetAddress.getByName(host);
            return inetAddress.isLoopbackAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }
}

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
package org.thingsboard.server.dao.util;

import org.apache.commons.lang3.StringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.device.credentials.BasicMqttCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentials;

public class DeviceConnectivityUtil {

    public static final String HTTP = "http";
    public static final String HTTPS = "https";
    public static final String MQTT = "mqtt";
    public static final String LINUX = "linux";
    public static final String WINDOWS = "windows";
    public static final String DOCKER = "docker";
    public static final String MQTTS = "mqtts";
    public static final String COAP = "coap";
    public static final String COAPS = "coaps";
    public static final String PEM_CERT_FILE_NAME = "tb-server-chain.pem";
    public static final String CHECK_DOCUMENTATION = "Check documentation";
    public static final String JSON_EXAMPLE_PAYLOAD = "\"{temperature:25}\"";
    public static final String DOCKER_RUN = "docker run --rm -it ";
    public static final String GATEWAY_DOCKER_RUN = "docker run -it ";
    public static final String MQTT_IMAGE = "thingsboard/mosquitto-clients ";
    public static final String COAP_IMAGE = "thingsboard/coap-clients ";

    public static String getHttpPublishCommand(String protocol, String host, String port, DeviceCredentials deviceCredentials) {
        return String.format("curl -v -X POST %s://%s%s/api/v1/%s/telemetry --header Content-Type:application/json --data " + JSON_EXAMPLE_PAYLOAD,
                protocol, host, port, deviceCredentials.getCredentialsId());
    }

    public static String getMqttPublishCommand(String protocol, String host, String port, String deviceTelemetryTopic, DeviceCredentials deviceCredentials) {
        StringBuilder command = new StringBuilder("mosquitto_pub -d -q 1");
        if (MQTTS.equals(protocol)) {
            command.append(" --cafile ").append(PEM_CERT_FILE_NAME);
        }
        command.append(" -h ").append(host).append(port == null ? "" : " -p " + port);
        command.append(" -t ").append(deviceTelemetryTopic);

        switch (deviceCredentials.getCredentialsType()) {
            case ACCESS_TOKEN:
                command.append(" -u ").append(deviceCredentials.getCredentialsId());
                break;
            case MQTT_BASIC:
                BasicMqttCredentials credentials = JacksonUtil.fromString(deviceCredentials.getCredentialsValue(),
                        BasicMqttCredentials.class);
                if (credentials != null) {
                    if (credentials.getClientId() != null) {
                        command.append(" -i ").append(credentials.getClientId());
                    }
                    if (credentials.getUserName() != null) {
                        command.append(" -u ").append(credentials.getUserName());
                    }
                    if (credentials.getPassword() != null) {
                        command.append(" -P ").append(credentials.getPassword());
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

    public static String getGatewayLaunchCommand(String os, String host, String port, DeviceCredentials deviceCredentials) {
        String gatewayVolumePathPrefix = "~/.tb-gateway";
        if (WINDOWS.equals(os)) {
            gatewayVolumePathPrefix = "%HOMEPATH%/tb-gateway";
        }

        String gatewayContainerName = "tbGateway" + StringUtils.capitalize(host.replace(".", ""));

        StringBuilder command = new StringBuilder(GATEWAY_DOCKER_RUN);
        command.append("-v {gatewayVolumePathPrefix}/logs:/thingsboard_gateway/logs".replace("{gatewayVolumePathPrefix}", gatewayVolumePathPrefix));
        command.append(" -v {gatewayVolumePathPrefix}/extensions:/thingsboard_gateway/extensions".replace("{gatewayVolumePathPrefix}", gatewayVolumePathPrefix));
        command.append(" -v {gatewayVolumePathPrefix}/config:/thingsboard_gateway/config".replace("{gatewayVolumePathPrefix}", gatewayVolumePathPrefix));
        command.append(" --name ").append(gatewayContainerName);
        command.append(" -e host=").append(host);
        command.append(" -e port=").append(port);

        switch(deviceCredentials.getCredentialsType()) {
            case ACCESS_TOKEN:
                command.append(" -e accessToken=").append(deviceCredentials.getCredentialsId());
                break;
            case MQTT_BASIC:
                BasicMqttCredentials credentials = JacksonUtil.fromString(deviceCredentials.getCredentialsValue(),
                        BasicMqttCredentials.class);
                if (credentials != null) {
                    if (credentials.getClientId() != null) {
                        command.append(" -e clientId=").append(credentials.getClientId());
                    }
                    if (credentials.getUserName() != null) {
                        command.append(" -e username=").append(credentials.getUserName());
                    }
                    if (credentials.getPassword() != null) {
                        command.append(" -e password=").append(credentials.getPassword());
                    }
                } else {
                    return null;
                }
                break;
            default:
                return null;
        }

        command.append(" --restart always");
        command.append(" thingsboard/tb-gateway");

        return command.toString();
    }

    public static String getDockerMqttPublishCommand(String protocol, String baseUrl, String host, String port, String deviceTelemetryTopic, DeviceCredentials deviceCredentials) {
        String mqttCommand = getMqttPublishCommand(protocol, host, port, deviceTelemetryTopic, deviceCredentials);

        if (mqttCommand == null) {
            return null;
        }

        StringBuilder mqttDockerCommand = new StringBuilder();
        mqttDockerCommand.append(DOCKER_RUN).append(MQTT_IMAGE);

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
        return String.format("curl -f -S -o %s %s/api/device-connectivity/%s/certificate/download", PEM_CERT_FILE_NAME, baseUrl, protocol);
    }

    public static String getCoapPublishCommand(String protocol, String host, String port, DeviceCredentials deviceCredentials) {
        switch (deviceCredentials.getCredentialsType()) {
            case ACCESS_TOKEN:
                String client = COAPS.equals(protocol) ? "coap-client-openssl" : "coap-client";
                return String.format("%s -m POST %s://%s%s/api/v1/%s/telemetry -t json -e %s",
                        client, protocol, host, port, deviceCredentials.getCredentialsId(), JSON_EXAMPLE_PAYLOAD);
            default:
                return null;
        }
    }

    public static String getDockerCoapPublishCommand(String protocol, String host, String port, DeviceCredentials deviceCredentials) {
        String coapCommand = getCoapPublishCommand(protocol, host, port, deviceCredentials);
        return coapCommand != null ? String.format("%s%s%s", DOCKER_RUN, COAP_IMAGE, coapCommand) : null;
    }
}

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
package org.thingsboard.server.dao.device;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.ResourceUtils;
import org.thingsboard.server.common.data.device.profile.MqttDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.CHECK_DOCUMENTATION;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.COAP;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.COAPS;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.DOCKER;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.HTTP;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.HTTPS;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.LINUX;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.MQTT;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.MQTTS;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.getCoapClientCommand;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.getCurlCommand;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.getDockerMosquittoClientsPublishCommand;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.getMosquittoPubPublishCommand;

@Service("DeviceConnectivityDaoService")
@Slf4j
public class DeviceСonnectivityServiceImpl implements DeviceConnectivityService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_DEVICE_ID = "Incorrect deviceId ";
    public static final String DEFAULT_DEVICE_TELEMETRY_TOPIC = "v1/devices/me/telemetry";

    @Autowired
    private DeviceCredentialsService deviceCredentialsService;

    @Autowired
    private DeviceProfileService deviceProfileService;

    @Autowired
    private DeviceConnectivityConfiguration deviceConnectivityConfiguration;

    @Override
    public JsonNode findDevicePublishTelemetryCommands(String baseUrl, Device device) throws URISyntaxException {
        DeviceId deviceId = device.getId();
        log.trace("Executing findDevicePublishTelemetryCommands [{}]", deviceId);
        validateId(deviceId, INCORRECT_DEVICE_ID + deviceId);

        DeviceCredentials creds = deviceCredentialsService.findDeviceCredentialsByDeviceId(device.getTenantId(), deviceId);
        DeviceProfile deviceProfile = deviceProfileService.findDeviceProfileById(device.getTenantId(), device.getDeviceProfileId());
        DeviceTransportType transportType = deviceProfile.getTransportType();

        ObjectNode commands = JacksonUtil.newObjectNode();
        switch (transportType) {
            case DEFAULT:
                Optional.ofNullable(getHttpTransportPublishCommands(baseUrl, creds))
                        .ifPresent(v -> commands.set(HTTP, v));
                Optional.ofNullable(getMqttTransportPublishCommands(baseUrl, creds))
                        .ifPresent(v -> commands.set(MQTT, v));
                Optional.ofNullable(getCoapTransportPublishCommands(baseUrl, creds))
                        .ifPresent(v -> commands.set(COAP, v));
                break;
            case MQTT:
                MqttDeviceProfileTransportConfiguration transportConfiguration =
                        (MqttDeviceProfileTransportConfiguration) deviceProfile.getProfileData().getTransportConfiguration();
                String topicName = transportConfiguration.getDeviceTelemetryTopic();

                Optional.ofNullable(getMqttTransportPublishCommands(baseUrl, topicName, creds))
                        .ifPresent(v -> commands.set(MQTT, v));
                break;
            case COAP:
                Optional.ofNullable(getCoapTransportPublishCommands(baseUrl, creds))
                        .ifPresent(v -> commands.set(COAP, v));
                break;
            default:
                commands.put(transportType.name(), CHECK_DOCUMENTATION);
        }
        return commands;
    }

    @Override
    public String getSslServerChain(String protocol) throws IOException {
        String mqttSslPemPath = deviceConnectivityConfiguration.getConnectivity()
                .get(protocol)
                .getSslServerPemPath();
        if (!mqttSslPemPath.isEmpty() && ResourceUtils.resourceExists(this, mqttSslPemPath)) {
            return FileUtils.readFileToString(new File(mqttSslPemPath), StandardCharsets.UTF_8);
        } else {
            return null;
        }
    }

    private JsonNode getHttpTransportPublishCommands(String defaultHostname, DeviceCredentials deviceCredentials) throws URISyntaxException {
        ObjectNode httpCommands = JacksonUtil.newObjectNode();
        Optional.ofNullable(getHttpPublishCommand(HTTP, defaultHostname, deviceCredentials))
                .ifPresent(v -> httpCommands.put(HTTP, v));
        Optional.ofNullable(getHttpPublishCommand(HTTPS, defaultHostname, deviceCredentials))
                .ifPresent(v -> httpCommands.put(HTTPS, v));
        return httpCommands.isEmpty() ? null : httpCommands;
    }

    private String getHttpPublishCommand(String protocol, String baseUrl, DeviceCredentials deviceCredentials) throws URISyntaxException {
        DeviceConnectivityInfo httpProps = deviceConnectivityConfiguration.getConnectivity().get(protocol);
        if (httpProps == null || !httpProps.getEnabled() ||
                deviceCredentials.getCredentialsType() != DeviceCredentialsType.ACCESS_TOKEN) {
            return null;
        }
        String hostName = httpProps.getHost().isEmpty() ? new URI(baseUrl).getHost() : httpProps.getHost();
        String port = httpProps.getPort().isEmpty() ? "" : ":" + httpProps.getPort();

        return getCurlCommand(protocol, hostName, port, deviceCredentials);
    }

    private JsonNode getMqttTransportPublishCommands(String baseUrl, DeviceCredentials deviceCredentials) throws URISyntaxException {
        return getMqttTransportPublishCommands(baseUrl, DEFAULT_DEVICE_TELEMETRY_TOPIC, deviceCredentials);
    }

    private JsonNode getMqttTransportPublishCommands(String baseUrl, String topic, DeviceCredentials deviceCredentials) throws URISyntaxException {
        ObjectNode mqttCommands = JacksonUtil.newObjectNode();

        Optional.ofNullable(getMqttPublishCommand(baseUrl, topic, deviceCredentials))
                .ifPresent(v -> mqttCommands.put(MQTT, v));
        List<String> mqttsPublishCommand = getMqttsPublishCommand(baseUrl, topic, deviceCredentials);
        if (mqttsPublishCommand != null){
            if (mqttsPublishCommand.size() > 1) {
                ArrayNode arrayNode = mqttCommands.putArray(MQTTS);
                mqttsPublishCommand.forEach(arrayNode::add);
            } else {
                mqttCommands.put(MQTTS, mqttsPublishCommand.get(0));
            }
        }

        ObjectNode dockerMqttCommands = JacksonUtil.newObjectNode();
        Optional.ofNullable(getDockerMqttPublishCommand(MQTT,baseUrl, topic, deviceCredentials))
                .ifPresent(v -> dockerMqttCommands.put(MQTT, v));
        Optional.ofNullable(getDockerMqttPublishCommand(MQTTS, baseUrl, topic, deviceCredentials))
                .ifPresent(v -> dockerMqttCommands.put(MQTTS, v));

        if (!dockerMqttCommands.isEmpty()) {
            mqttCommands.set(DOCKER, dockerMqttCommands);
        }
        return mqttCommands.isEmpty() ? null : mqttCommands;
    }

    private String getMqttPublishCommand(String baseUrl, String deviceTelemetryTopic, DeviceCredentials deviceCredentials) throws URISyntaxException {
        DeviceConnectivityInfo properties = deviceConnectivityConfiguration.getConnectivity().get(MQTT);
        if (properties == null || !properties.getEnabled()) {
            return null;
        }
        String mqttHost = properties.getHost().isEmpty() ? new URI(baseUrl).getHost() : properties.getHost();
        String mqttPort = properties.getPort().isEmpty() ? null : properties.getPort();
        return getMosquittoPubPublishCommand(MQTT, mqttHost, mqttPort, deviceTelemetryTopic, deviceCredentials);
    }

    private List<String> getMqttsPublishCommand(String baseUrl, String deviceTelemetryTopic, DeviceCredentials deviceCredentials) throws URISyntaxException {
        String pubCommand;
        if (deviceCredentials.getCredentialsType() == DeviceCredentialsType.X509_CERTIFICATE) {
            return List.of(CHECK_DOCUMENTATION);
        } else {
            DeviceConnectivityInfo properties = deviceConnectivityConfiguration.getConnectivity().get(MQTTS);
            if (properties == null || !properties.getEnabled()) {
                return null;
            }
            String mqttHost = properties.getHost().isEmpty() ? new URI(baseUrl).getHost() : properties.getHost();
            String mqttPort = properties.getPort().isEmpty() ? null : properties.getPort();
            pubCommand = getMosquittoPubPublishCommand(MQTTS, mqttHost, mqttPort, deviceTelemetryTopic, deviceCredentials);
        }

        ArrayList<String> commands = new ArrayList<>();
        if (pubCommand != null) {
            commands.add("curl " + baseUrl + "/api/device-connectivity/mqtts/certificate/download -o /tmp/tb-server-chain.pem");
            commands.add(pubCommand);
            return commands;
        }
        return null;
    }


    private String getDockerMqttPublishCommand(String protocol, String baseUrl, String deviceTelemetryTopic, DeviceCredentials deviceCredentials) throws URISyntaxException {
        DeviceConnectivityInfo properties = deviceConnectivityConfiguration.getConnectivity().get(protocol);
        if (properties == null || !properties.getEnabled()) {
            return null;
        }
        String mqttHost = properties.getHost().isEmpty() ? new URI(baseUrl).getHost() : properties.getHost();
        String mqttPort = properties.getPort().isEmpty() ? null : properties.getPort();
        return getDockerMosquittoClientsPublishCommand(protocol, baseUrl, mqttHost, mqttPort, deviceTelemetryTopic, deviceCredentials);
    }

    private JsonNode getCoapTransportPublishCommands(String baseUrl, DeviceCredentials deviceCredentials) throws URISyntaxException {
        ObjectNode coapCommands = JacksonUtil.newObjectNode();

        Optional.ofNullable(getCoapPublishCommand(COAP, baseUrl, deviceCredentials))
                .ifPresent(v -> coapCommands.put(COAP, v));
        Optional.ofNullable(getCoapPublishCommand(COAPS, baseUrl, deviceCredentials))
                .ifPresent(v -> coapCommands.put(COAPS, v));

        return coapCommands.isEmpty() ? null : coapCommands;
    }

    private String getCoapPublishCommand(String protocol, String baseUrl, DeviceCredentials deviceCredentials) throws URISyntaxException {
        if (COAPS.equals(protocol) && deviceCredentials.getCredentialsType() == DeviceCredentialsType.X509_CERTIFICATE) {
            return CHECK_DOCUMENTATION;
        }
        DeviceConnectivityInfo properties = deviceConnectivityConfiguration.getConnectivity().get(protocol);
        if (properties == null || !properties.getEnabled()) {
            return null;
        }
        String hostName = properties.getHost().isEmpty() ? new URI(baseUrl).getHost() : properties.getHost();
        String port = properties.getPort().isEmpty() ? "" : ":" + properties.getPort();

        return getCoapClientCommand(protocol, hostName, port, deviceCredentials);
    }
}

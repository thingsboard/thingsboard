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
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.WINDOWS;
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

        String defaultHostname = new URI(baseUrl).getHost();
        DeviceCredentials creds = deviceCredentialsService.findDeviceCredentialsByDeviceId(device.getTenantId(), deviceId);
        DeviceProfile deviceProfile = deviceProfileService.findDeviceProfileById(device.getTenantId(), device.getDeviceProfileId());
        DeviceTransportType transportType = deviceProfile.getTransportType();

        ObjectNode commands = JacksonUtil.newObjectNode();
        switch (transportType) {
            case DEFAULT:
                commands.set(HTTP, getHttpTransportPublishCommands(defaultHostname, creds));
                commands.set(MQTT, getMqttTransportPublishCommands(defaultHostname, creds));
                commands.set(COAP, getCoapTransportPublishCommands(defaultHostname, creds));
                break;
            case MQTT:
                MqttDeviceProfileTransportConfiguration transportConfiguration =
                        (MqttDeviceProfileTransportConfiguration) deviceProfile.getProfileData().getTransportConfiguration();
                String topicName = transportConfiguration.getDeviceTelemetryTopic();

                commands.set(MQTT, getMqttTransportPublishCommands(defaultHostname, topicName, creds));
                break;
            case COAP:
                commands.set(COAP, getCoapTransportPublishCommands(defaultHostname, creds));
                break;
            default:
                commands.set(transportType.name(), JacksonUtil.toJsonNode(CHECK_DOCUMENTATION));
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

    private JsonNode getHttpTransportPublishCommands(String defaultHostname, DeviceCredentials deviceCredentials) {
        ObjectNode httpCommands = JacksonUtil.newObjectNode();
        httpCommands.put(HTTP, getHttpPublishCommand(HTTP, defaultHostname, deviceCredentials));
        httpCommands.put(HTTPS, getHttpPublishCommand(HTTPS, defaultHostname, deviceCredentials));
        return httpCommands;
    }

    private String getHttpPublishCommand(String protocol, String defaultHostname, DeviceCredentials deviceCredentials) {
        DeviceConnectivityInfo httpProps = deviceConnectivityConfiguration.getConnectivity().get(protocol);
        if (httpProps == null || !httpProps.getEnabled() ||
                deviceCredentials.getCredentialsType() != DeviceCredentialsType.ACCESS_TOKEN) {
            return null;
        }
        String hostName = httpProps.getHost().isEmpty() ? defaultHostname : httpProps.getHost();
        String port = httpProps.getPort().isEmpty() ? "" : ":" + httpProps.getPort();

        return getCurlCommand(protocol, hostName, port, deviceCredentials);
    }

    private JsonNode getMqttTransportPublishCommands(String defaultHostname, DeviceCredentials deviceCredentials) {
        return getMqttTransportPublishCommands(defaultHostname, DEFAULT_DEVICE_TELEMETRY_TOPIC, deviceCredentials);
    }

    private JsonNode getMqttTransportPublishCommands(String defaultHostname, String topic, DeviceCredentials deviceCredentials) {
        ObjectNode mqttCommands = JacksonUtil.newObjectNode();

        ObjectNode linuxMqttCommands = JacksonUtil.newObjectNode();
        Optional.ofNullable(getMqttPublishCommand(LINUX, MQTT, defaultHostname, topic, deviceCredentials))
                .ifPresent(v -> linuxMqttCommands.put(MQTT, v));
        Optional.ofNullable(getMqttPublishCommand(LINUX, MQTTS, defaultHostname, topic, deviceCredentials))
                .ifPresent(v -> linuxMqttCommands.put(MQTTS, v));

        ObjectNode windowsMqttCommands = JacksonUtil.newObjectNode();
        Optional.ofNullable(getMqttPublishCommand(WINDOWS, MQTT, defaultHostname, topic, deviceCredentials))
                .ifPresent(v -> windowsMqttCommands.put(MQTT, v));
        Optional.ofNullable(getMqttPublishCommand(WINDOWS, MQTTS, defaultHostname, topic, deviceCredentials))
                .ifPresent(v -> windowsMqttCommands.put(MQTTS, v));

        ObjectNode dockerMqttCommands = JacksonUtil.newObjectNode();
        Optional.ofNullable(getMqttPublishCommand(DOCKER, MQTT, defaultHostname, topic, deviceCredentials))
                .ifPresent(v -> dockerMqttCommands.put(MQTT, v));
        Optional.ofNullable(getMqttPublishCommand(DOCKER, MQTTS, defaultHostname, topic, deviceCredentials))
                .ifPresent(v -> dockerMqttCommands.put(MQTTS, v));

        mqttCommands.set(LINUX, linuxMqttCommands);
        mqttCommands.set(WINDOWS, windowsMqttCommands);
        mqttCommands.set(DOCKER, dockerMqttCommands);

        return mqttCommands;
    }

    private String getMqttPublishCommand(String os, String protocol, String defaultHostname, String deviceTelemetryTopic, DeviceCredentials deviceCredentials) {
        if (MQTTS.equals(protocol) && deviceCredentials.getCredentialsType() == DeviceCredentialsType.X509_CERTIFICATE) {
            return CHECK_DOCUMENTATION;
        }
        DeviceConnectivityInfo properties = deviceConnectivityConfiguration.getConnectivity().get(protocol);
        if (properties == null || !properties.getEnabled()) {
            return null;
        }
        String mqttHost = properties.getHost().isEmpty() ? defaultHostname : properties.getHost();
        String mqttPort = properties.getPort().isEmpty() ? null : properties.getPort();
        switch (os) {
            case LINUX:
                return getMosquittoPubPublishCommand(protocol, mqttHost, mqttPort, deviceTelemetryTopic, deviceCredentials);
            case WINDOWS:
                return getMosquittoPubPublishCommand(protocol, mqttHost, mqttPort, deviceTelemetryTopic, deviceCredentials);
            case DOCKER:
                return getDockerMosquittoClientsPublishCommand(protocol, mqttHost, mqttPort, deviceTelemetryTopic, deviceCredentials);
            default:
                throw new IllegalArgumentException("Unsupported operating system: " + os);
        }
    }

    private JsonNode getCoapTransportPublishCommands(String defaultHostname, DeviceCredentials deviceCredentials) {
        ObjectNode coapCommands = JacksonUtil.newObjectNode();

        ObjectNode linuxCoapCommands = JacksonUtil.newObjectNode();
        Optional.ofNullable(getCoapPublishCommand(LINUX, COAP, defaultHostname, deviceCredentials))
                .ifPresent(v -> linuxCoapCommands.put(COAP, v));
        Optional.ofNullable(getCoapPublishCommand(LINUX, COAPS, defaultHostname, deviceCredentials))
                .ifPresent(v -> linuxCoapCommands.put(COAPS, v));

        coapCommands.set(LINUX, linuxCoapCommands);
        return coapCommands;
    }

    private String getCoapPublishCommand(String os, String protocol, String defaultHostname, DeviceCredentials deviceCredentials) {
        if (COAPS.equals(protocol) && deviceCredentials.getCredentialsType() == DeviceCredentialsType.X509_CERTIFICATE) {
            return CHECK_DOCUMENTATION;
        }
        DeviceConnectivityInfo properties = deviceConnectivityConfiguration.getConnectivity().get(protocol);
        if (properties == null || !properties.getEnabled()) {
            return null;
        }
        String hostName = properties.getHost().isEmpty() ? defaultHostname : properties.getHost();
        String port = properties.getPort().isEmpty() ? "" : ":" + properties.getPort();

        switch (os) {
            case LINUX:
                return getCoapClientCommand(protocol, hostName, port, deviceCredentials);
            default:
                throw new IllegalArgumentException("Unsupported operating system: " + os);
        }
    }
}

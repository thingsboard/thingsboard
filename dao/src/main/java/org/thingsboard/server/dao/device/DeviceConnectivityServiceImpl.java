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
package org.thingsboard.server.dao.device;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.openssl.PEMParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.ResourceUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.device.profile.MqttDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.util.DeviceConnectivityUtil;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.CHECK_DOCUMENTATION;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.COAP;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.COAPS;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.DOCKER;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.HTTP;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.HTTPS;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.MQTT;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.MQTTS;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.getHost;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.getPort;

@Service("DeviceConnectivityDaoService")
@Slf4j
@RequiredArgsConstructor
public class DeviceConnectivityServiceImpl implements DeviceConnectivityService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_DEVICE_ID = "Incorrect deviceId ";
    public static final String DEFAULT_DEVICE_TELEMETRY_TOPIC = "v1/devices/me/telemetry";
    public static final String HTTP_DEFAULT_PORT = "80";
    public static final String HTTPS_DEFAULT_PORT = "443";

    private final Map<String, Resource> certs = new ConcurrentHashMap<>();

    private final DeviceCredentialsService deviceCredentialsService;
    private final DeviceProfileService deviceProfileService;
    private final AdminSettingsService adminSettingsService;

    @Value("${device.connectivity.mqtts.pem_cert_file:}")
    private String mqttsPemCertFile;
    @Value("${device.connectivity.coaps.pem_cert_file:}")
    private String coapsPemCertFile;
    @Value("${device.connectivity.gateway.image_version:3.7-stable}")
    private String gatewayImageVersion;

    @Override
    public JsonNode findDevicePublishTelemetryCommands(String baseUrl, Device device) throws URISyntaxException {
        DeviceId deviceId = device.getId();
        log.trace("Executing findDevicePublishTelemetryCommands [{}]", deviceId);
        validateId(deviceId, id -> INCORRECT_DEVICE_ID + id);

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
                //TODO: add sparkplug command with emulator (check SSL)
                if (transportConfiguration.isSparkplug()) {
                    ObjectNode sparkplug = JacksonUtil.newObjectNode();
                    sparkplug.put("sparkplug", CHECK_DOCUMENTATION);
                    commands.set(MQTT, sparkplug);
                } else {
                    String topicName = transportConfiguration.getDeviceTelemetryTopic();

                    Optional.ofNullable(getMqttTransportPublishCommands(baseUrl, topicName, creds))
                            .ifPresent(v -> commands.set(MQTT, v));
                }
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
    public Resource getPemCertFile(String protocol) {
        return certs.computeIfAbsent(protocol, key -> {
            DeviceConnectivityInfo connectivity = getConnectivity(protocol);
            if (connectivity == null) {
                log.warn("Unknown connectivity protocol: {}", protocol);
                return null;
            }

            return switch (protocol) {
                case COAPS -> getCert(coapsPemCertFile);
                case MQTTS -> getCert(mqttsPemCertFile);
                default -> {
                    log.warn("Unsupported secure protocol: {}", protocol);
                    yield null;
                }
            };
        });
    }

    @Override
    public Resource createGatewayDockerComposeFile(String baseUrl, Device device) throws URISyntaxException {
        String mqttType = isEnabled(MQTTS) ? MQTTS : MQTT;
        DeviceConnectivityInfo properties = getConnectivity(mqttType);
        DeviceCredentials creds = deviceCredentialsService.findDeviceCredentialsByDeviceId(device.getTenantId(), device.getId());
        String host = getHost(baseUrl, properties, mqttType);
        return DeviceConnectivityUtil.getGatewayDockerComposeFile(host, gatewayImageVersion, creds);
    }

    private DeviceConnectivityInfo getConnectivity(String protocol) {
        AdminSettings connectivitySettings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "connectivity");
        JsonNode connectivity;
        if (connectivitySettings != null && (connectivity = connectivitySettings.getJsonValue()) != null) {
            return JacksonUtil.convertValue(connectivity.get(protocol), DeviceConnectivityInfo.class);
        }
        return null;
    }

    public boolean isEnabled(String protocol) {
        var info = getConnectivity(protocol);
        return info != null && info.isEnabled();
    }

    private Resource getCert(String path) {
        if (StringUtils.isBlank(path) || !ResourceUtils.resourceExists(this, path)) {
           return null;
        }

        StringBuilder pemContentBuilder = new StringBuilder();

        try (InputStream inStream = ResourceUtils.getInputStream(this, path);
             PEMParser pemParser = new PEMParser(new InputStreamReader(inStream))) {

            Object object;

            while ((object = pemParser.readObject()) != null) {
                if (object instanceof X509CertificateHolder) {
                    var certHolder = (X509CertificateHolder) object;
                    String certBase64 = Base64.getEncoder().encodeToString(certHolder.getEncoded());

                    pemContentBuilder.append("-----BEGIN CERTIFICATE-----\n");
                    int index = 0;
                    while (index < certBase64.length()) {
                        pemContentBuilder.append(certBase64, index, Math.min(index + 64, certBase64.length()));
                        pemContentBuilder.append("\n");
                        index += 64;
                    }
                    pemContentBuilder.append("-----END CERTIFICATE-----\n");
                }
            }
        } catch (Exception e) {
            String msg = String.format("Failed to read %s server certificate!", path);
            log.warn(msg);
            throw new RuntimeException(msg, e);
        }

        return new ByteArrayResource(pemContentBuilder.toString().getBytes(StandardCharsets.UTF_8));
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
        DeviceConnectivityInfo properties = getConnectivity(protocol);
        if (properties == null || !properties.isEnabled() ||
                deviceCredentials.getCredentialsType() != DeviceCredentialsType.ACCESS_TOKEN) {
            return null;
        }
        String hostName = getHost(baseUrl, properties, protocol);
        String propertiesPort = getPort(properties);
        String port = (propertiesPort.isEmpty() || HTTP_DEFAULT_PORT.equals(propertiesPort) || HTTPS_DEFAULT_PORT.equals(propertiesPort))
                ? "" : ":" + propertiesPort;
        return DeviceConnectivityUtil.getHttpPublishCommand(protocol, hostName, port, deviceCredentials);
    }

    private JsonNode getMqttTransportPublishCommands(String baseUrl, DeviceCredentials deviceCredentials) throws URISyntaxException {
        return getMqttTransportPublishCommands(baseUrl, DEFAULT_DEVICE_TELEMETRY_TOPIC, deviceCredentials);
    }

    private JsonNode getMqttTransportPublishCommands(String baseUrl, String topic, DeviceCredentials deviceCredentials) throws URISyntaxException {
        ObjectNode mqttCommands = JacksonUtil.newObjectNode();

        if (deviceCredentials.getCredentialsType() == DeviceCredentialsType.X509_CERTIFICATE) {
            mqttCommands.put(MQTTS, CHECK_DOCUMENTATION);
            return mqttCommands;
        }

        ObjectNode dockerMqttCommands = JacksonUtil.newObjectNode();

        if (isEnabled(MQTT)) {
            Optional.ofNullable(getMqttPublishCommand(baseUrl, topic, deviceCredentials)).
                    ifPresent(v -> mqttCommands.put(MQTT, v));

            Optional.ofNullable(getDockerMqttPublishCommand(MQTT, baseUrl, topic, deviceCredentials))
                    .ifPresent(v -> dockerMqttCommands.put(MQTT, v));
        }

        if (isEnabled(MQTTS)) {
            List<String> mqttsPublishCommand = getMqttsPublishCommand(baseUrl, topic, deviceCredentials);
            if (mqttsPublishCommand != null) {
                ArrayNode arrayNode = mqttCommands.putArray(MQTTS);
                mqttsPublishCommand.forEach(arrayNode::add);
            }

            Optional.ofNullable(getDockerMqttPublishCommand(MQTTS, baseUrl, topic, deviceCredentials))
                    .ifPresent(v -> dockerMqttCommands.put(MQTTS, v));
        }

        if (!dockerMqttCommands.isEmpty()) {
            mqttCommands.set(DOCKER, dockerMqttCommands);
        }
        return mqttCommands.isEmpty() ? null : mqttCommands;
    }

    private String getMqttPublishCommand(String baseUrl, String deviceTelemetryTopic, DeviceCredentials deviceCredentials) throws URISyntaxException {
        DeviceConnectivityInfo properties = getConnectivity(MQTT);
        String mqttHost = getHost(baseUrl, properties, MQTT);
        String mqttPort = getPort(properties);
        return DeviceConnectivityUtil.getMqttPublishCommand(MQTT, mqttHost, mqttPort, deviceTelemetryTopic, deviceCredentials);
    }

    private List<String> getMqttsPublishCommand(String baseUrl, String deviceTelemetryTopic, DeviceCredentials deviceCredentials) throws URISyntaxException {
        DeviceConnectivityInfo properties = getConnectivity(MQTTS);
        String mqttHost = getHost(baseUrl, properties, MQTTS);
        String mqttPort = getPort(properties);
        String pubCommand = DeviceConnectivityUtil.getMqttPublishCommand(MQTTS, mqttHost, mqttPort, deviceTelemetryTopic, deviceCredentials);

        ArrayList<String> commands = new ArrayList<>();
        if (pubCommand != null) {
            commands.add(DeviceConnectivityUtil.getCurlPemCertCommand(baseUrl, MQTTS));
            commands.add(pubCommand);
            return commands;
        }
        return null;
    }

    private String getDockerMqttPublishCommand(String protocol, String baseUrl, String deviceTelemetryTopic, DeviceCredentials deviceCredentials) throws URISyntaxException {
        DeviceConnectivityInfo properties = getConnectivity(protocol);
        String mqttHost = getHost(baseUrl, properties, protocol);
        String mqttPort = getPort(properties);
        return DeviceConnectivityUtil.getDockerMqttPublishCommand(protocol, baseUrl, mqttHost, mqttPort, deviceTelemetryTopic, deviceCredentials);
    }

    private JsonNode getCoapTransportPublishCommands(String baseUrl, DeviceCredentials deviceCredentials) throws URISyntaxException {
        ObjectNode coapCommands = JacksonUtil.newObjectNode();

        if (deviceCredentials.getCredentialsType() == DeviceCredentialsType.X509_CERTIFICATE) {
            coapCommands.put(COAPS, CHECK_DOCUMENTATION);
            return coapCommands;
        }

        ObjectNode dockerCoapCommands = JacksonUtil.newObjectNode();

        if (isEnabled(COAP)) {
            Optional.ofNullable(getCoapPublishCommand(COAP, baseUrl, deviceCredentials))
                    .ifPresent(v -> coapCommands.put(COAP, v));

            Optional.ofNullable(getDockerCoapPublishCommand(COAP, baseUrl, deviceCredentials))
                    .ifPresent(v -> dockerCoapCommands.put(COAP, v));
        }

        if (isEnabled(COAPS)) {
            ArrayNode coapsCommands = coapCommands.putArray(COAPS);
            Optional.ofNullable(DeviceConnectivityUtil.getCurlPemCertCommand(baseUrl, COAPS))
                    .ifPresent(coapsCommands::add);
            Optional.ofNullable(getCoapPublishCommand(COAPS, baseUrl, deviceCredentials))
                    .ifPresent(coapsCommands::add);

            Optional.ofNullable(getDockerCoapPublishCommand(COAPS, baseUrl, deviceCredentials))
                    .ifPresent(v -> dockerCoapCommands.put(COAPS, v));
        }

        if (!dockerCoapCommands.isEmpty()) {
            coapCommands.set(DOCKER, dockerCoapCommands);
        }

        return coapCommands.isEmpty() ? null : coapCommands;
    }

    private String getCoapPublishCommand(String protocol, String baseUrl, DeviceCredentials deviceCredentials) throws URISyntaxException {
        DeviceConnectivityInfo properties = getConnectivity(protocol);
        String hostName = getHost(baseUrl, properties, protocol);
        String port = StringUtils.isBlank(properties.getPort()) ? "" : ":" + properties.getPort();
        return DeviceConnectivityUtil.getCoapPublishCommand(protocol, hostName, port, deviceCredentials);
    }

    private String getDockerCoapPublishCommand(String protocol, String baseUrl, DeviceCredentials deviceCredentials) throws URISyntaxException {
        DeviceConnectivityInfo properties = getConnectivity(protocol);
        String host = getHost(baseUrl, properties, protocol);
        String port = StringUtils.isBlank(properties.getPort()) ? "" : ":" + properties.getPort();
        return DeviceConnectivityUtil.getDockerCoapPublishCommand(protocol, baseUrl, host, port, deviceCredentials);
    }

}

/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.mqtt.telemetry;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.gen.transport.TransportApiProtos;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Slf4j
public abstract class AbstractMqttTelemetryIntegrationTest extends AbstractControllerTest {

    protected static final String PAYLOAD_VALUES_STR_V_1 = "{\"key1\":\"value1\", \"key2\":true, \"key3\": 3.0, \"key4\": 4," +
            " \"key5\": {\"someNumber\": 42, \"someArray\": [1,2,3], \"someNestedObject\": {\"key\": \"value\"}}}";

    protected static final String PAYLOAD_VALUES_STR_V_2 = "{\"key6\":\"value1\", \"key7\":true, \"key8\": 3.0, \"key9\": 4, \"key10\":" +
            " {\"someNumber\": 42, \"someArray\": [1,2,3], \"someNestedObject\": {\"key\": \"value\"}}}";

    protected static final String MQTT_URL = "tcp://localhost:1883";

    protected Device savedDevice;
    protected String accessToken;

    protected Device savedGateway;
    protected String gatewayAccessToken;

    protected void processBeforeTest(String deviceName, String gatewayName) throws Exception {
        loginTenantAdmin();

        Device device = new Device();
        device.setName(deviceName);
        device.setType("default");
        savedDevice = doPost("/api/device", device, Device.class);

        DeviceCredentials deviceCredentials =
                doGet("/api/device/" + savedDevice.getId().getId().toString() + "/credentials", DeviceCredentials.class);

        Device gateway = new Device();
        gateway.setName(gatewayName);
        gateway.setType("default");
        ObjectNode additionalInfo = mapper.createObjectNode();
        additionalInfo.put("gateway", true);
        gateway.setAdditionalInfo(additionalInfo);
        savedGateway = doPost("/api/device", gateway, Device.class);

        DeviceCredentials gatewayCredentials =
                doGet("/api/device/" + savedGateway.getId().getId().toString() + "/credentials", DeviceCredentials.class);

        assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
        accessToken = deviceCredentials.getCredentialsId();
        assertNotNull(accessToken);

        assertEquals(savedGateway.getId(), gatewayCredentials.getDeviceId());
        gatewayAccessToken = gatewayCredentials.getCredentialsId();
        assertNotNull(gatewayAccessToken);
    }

    protected MqttAsyncClient getMqttAsyncClient(String token) throws MqttException {
        String clientId = MqttAsyncClient.generateClientId();
        MqttAsyncClient client = new MqttAsyncClient(MQTT_URL, clientId);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(token);
        client.connect(options);
        return client;
    }

    protected void publishMqttMsg(MqttAsyncClient client, byte[] payload, String topic) throws MqttException {
        MqttMessage message = new MqttMessage();
        message.setPayload(payload);
        client.publish(topic, message);
    }

    protected List<TransportApiProtos.KeyValueProto> getKvProtos(List<String> expectedKeys) {
        List<TransportApiProtos.KeyValueProto> keyValueProtos = new ArrayList<>();
        TransportApiProtos.KeyValueProto strKeyValueProto = getKeyValueProto(expectedKeys.get(0), TransportApiProtos.KeyValueType.STRING_V, "value1");
        TransportApiProtos.KeyValueProto boolKeyValueProto = getKeyValueProto(expectedKeys.get(1), TransportApiProtos.KeyValueType.BOOLEAN_V, "true");
        TransportApiProtos.KeyValueProto dblKeyValueProto = getKeyValueProto(expectedKeys.get(2), TransportApiProtos.KeyValueType.DOUBLE_V, "3.0");
        TransportApiProtos.KeyValueProto longKeyValueProto = getKeyValueProto(expectedKeys.get(3), TransportApiProtos.KeyValueType.LONG_V, "4");
        TransportApiProtos.KeyValueProto jsonKeyValueProto = getKeyValueProto(expectedKeys.get(4), TransportApiProtos.KeyValueType.JSON_V,
                "{\"someNumber\": 42, \"someArray\": [1,2,3], \"someNestedObject\": {\"key\": \"value\"}}");
        keyValueProtos.add(strKeyValueProto);
        keyValueProtos.add(boolKeyValueProto);
        keyValueProtos.add(dblKeyValueProto);
        keyValueProtos.add(longKeyValueProto);
        keyValueProtos.add(jsonKeyValueProto);
        return keyValueProtos;
    }

    protected TransportApiProtos.KeyValueProto getKeyValueProto(String key, TransportApiProtos.KeyValueType type, String strValue) {
        TransportApiProtos.KeyValueProto.Builder keyValueProtoBuilder = TransportApiProtos.KeyValueProto.newBuilder();
        keyValueProtoBuilder.setKey(key);
        keyValueProtoBuilder.setType(type);
        switch (type) {
            case BOOLEAN_V:
                keyValueProtoBuilder.setBoolV(Boolean.parseBoolean(strValue));
                break;
            case LONG_V:
                keyValueProtoBuilder.setLongV(Long.parseLong(strValue));
                break;
            case DOUBLE_V:
                keyValueProtoBuilder.setDoubleV(Double.parseDouble(strValue));
                break;
            case STRING_V:
                keyValueProtoBuilder.setStringV(strValue);
                break;
            case JSON_V:
                keyValueProtoBuilder.setJsonV(strValue);
                break;
        }
        return keyValueProtoBuilder.build();
    }

}

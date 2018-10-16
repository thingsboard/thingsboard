/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.server.msa.connectivity;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Data;
import org.junit.*;
import org.thingsboard.mqtt.MqttClient;
import org.thingsboard.mqtt.MqttClientConfig;
import org.thingsboard.mqtt.MqttHandler;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.msa.AbstractContainerTest;
import org.thingsboard.server.msa.WsClient;
import org.thingsboard.server.msa.WsTelemetryResponse;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class MqttClientTest extends AbstractContainerTest {

    @Test
    public void telemetryUpload() throws Exception {
        restClient.login("tenant@thingsboard.org", "tenant");
        Device device = createDevice("mqtt_");
        DeviceCredentials deviceCredentials = restClient.getCredentials(device.getId());

        WsClient mWs = subscribeToTelemetryWebSocket(device.getId());
        MqttClient mqttClient = getMqttClient(deviceCredentials);
        mqttClient.publish("v1/devices/me/telemetry", Unpooled.wrappedBuffer(createPayload().toString().getBytes()));
        TimeUnit.SECONDS.sleep(1);
        WsTelemetryResponse actualLatestTelemetry = mapper.readValue(mWs.getLastMessage(), WsTelemetryResponse.class);

        Assert.assertEquals(getExpectedLatestValues(123456789L).keySet(), actualLatestTelemetry.getLatestValues().keySet());

        Assert.assertTrue(verify(actualLatestTelemetry, "booleanKey", Boolean.TRUE.toString()));
        Assert.assertTrue(verify(actualLatestTelemetry, "stringKey", "value1"));
        Assert.assertTrue(verify(actualLatestTelemetry, "doubleKey", Double.toString(42.0)));
        Assert.assertTrue(verify(actualLatestTelemetry, "longKey", Long.toString(73)));

        restClient.getRestTemplate().delete(httpUrl + "/api/device/" + device.getId());
    }

    @Test
    public void telemetryUploadWithTs() throws Exception {
        long ts = 1451649600512L;

        restClient.login("tenant@thingsboard.org", "tenant");
        Device device = createDevice("mqtt_");
        DeviceCredentials deviceCredentials = restClient.getCredentials(device.getId());

        WsClient mWs = subscribeToTelemetryWebSocket(device.getId());
        MqttClient mqttClient = getMqttClient(deviceCredentials);
        mqttClient.publish("v1/devices/me/telemetry", Unpooled.wrappedBuffer(createPayload(ts).toString().getBytes()));
        TimeUnit.SECONDS.sleep(1);
        WsTelemetryResponse actualLatestTelemetry = mapper.readValue(mWs.getLastMessage(), WsTelemetryResponse.class);

        Assert.assertEquals(getExpectedLatestValues(ts), actualLatestTelemetry.getLatestValues());

        Assert.assertTrue(verify(actualLatestTelemetry, "booleanKey", ts, Boolean.TRUE.toString()));
        Assert.assertTrue(verify(actualLatestTelemetry, "stringKey", ts, "value1"));
        Assert.assertTrue(verify(actualLatestTelemetry, "doubleKey", ts, Double.toString(42.0)));
        Assert.assertTrue(verify(actualLatestTelemetry, "longKey", ts, Long.toString(73)));

        restClient.getRestTemplate().delete(httpUrl + "/api/device/" + device.getId());
    }

    private MqttClient getMqttClient(DeviceCredentials deviceCredentials) throws InterruptedException {
        MqttMessageListener queue = new MqttMessageListener();
        MqttClientConfig clientConfig = new MqttClientConfig();
        clientConfig.setClientId("MQTT client from test");
        clientConfig.setUsername(deviceCredentials.getCredentialsId());
        MqttClient mqttClient = MqttClient.create(clientConfig, queue);
        mqttClient.connect("localhost", 1883).sync();
        return mqttClient;
    }

    @Data
    private class MqttMessageListener implements MqttHandler {
        private final BlockingQueue<MqttEvent> events;

        private MqttMessageListener() {
            events = new ArrayBlockingQueue<>(100);
        }

        @Override
        public void onMessage(String topic, ByteBuf message) {
            events.add(new MqttEvent(topic, message.toString(StandardCharsets.UTF_8)));
        }
    }

    @Data
    private class MqttEvent {
        private final String topic;
        private final String message;
    }
}

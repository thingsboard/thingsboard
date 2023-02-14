/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.transport.mqtt.sparkplug.rpc;

import com.nimbusds.jose.util.StandardCharset;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.thingsboard.server.transport.mqtt.AbstractMqttIntegrationTest;
import org.thingsboard.server.transport.mqtt.mqttv5.MqttV5TestCallback;
import org.thingsboard.server.transport.mqtt.mqttv5.MqttV5TestClient;

@Slf4j
public abstract class AbstractMqttV5RpcSparkplugTest  extends AbstractMqttIntegrationTest {

    private static final String DEVICE_RESPONSE = "{\"value1\":\"A\",\"value2\":\"B\"}";
    private static final String setSparklpugRpcNodeRequest = "{\"method\": \"NCMD\", \"params\": {\"MyNodeMetric05_String\":\"MyNodeMetric05_String_Value\"}}";
    private static final String setSparklpugRpcDeviceRequest = "{\"method\": \"DCMD\", \"params\": {\"MyDeviceMetric05_String\":{\"MyDeviceMetric05_String_Value\"}}";

    protected class MqttV5TestRpcCallback extends MqttV5TestCallback {

        private final MqttV5TestClient client;

        public MqttV5TestRpcCallback(MqttV5TestClient client, String awaitSubTopic) {
            super(awaitSubTopic);
            this.client = client;
        }

        @Override
        protected void messageArrivedOnAwaitSubTopic(String requestTopic, MqttMessage mqttMessage) {
            log.warn("messageArrived on topic: {}, awaitSubTopic: {}", requestTopic, awaitSubTopic);
            if (awaitSubTopic.equals(requestTopic)) {
                qoS = mqttMessage.getQos();
                payloadBytes = mqttMessage.getPayload();
                String responseTopic = requestTopic.replace("request", "response");
                try {
                    client.publish(responseTopic, DEVICE_RESPONSE.getBytes(StandardCharset.UTF_8));
                } catch (MqttException e) {
                    log.warn("Failed to publish response on topic: {} due to: ", responseTopic, e);
                }
                subscribeLatch.countDown();
            }
        }
    }

}

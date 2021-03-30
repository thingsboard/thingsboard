/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.transport.mqtt.attributes;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.mqtt.AbstractMqttIntegrationTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@Slf4j
public abstract class AbstractMqttAttributesIntegrationTest extends AbstractMqttIntegrationTest {

    protected static final String POST_ATTRIBUTES_PAYLOAD = "{\"attribute1\":\"value1\",\"attribute2\":true,\"attribute3\":42.0,\"attribute4\":73," +
            "\"attribute5\":{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}}";

    protected void processBeforeTest(String deviceName, String gatewayName, TransportPayloadType payloadType, String telemetryTopic, String attributesTopic) throws Exception {
        super.processBeforeTest(deviceName, gatewayName, payloadType, telemetryTopic, attributesTopic);
    }

    protected void processAfterTest() throws Exception {
        super.processAfterTest();
    }

    protected List<TransportProtos.TsKvProto> getTsKvProtoList() {
        TransportProtos.TsKvProto tsKvProtoAttribute1 = getTsKvProto("attribute1", "value1", TransportProtos.KeyValueType.STRING_V);
        TransportProtos.TsKvProto tsKvProtoAttribute2 = getTsKvProto("attribute2", "true", TransportProtos.KeyValueType.BOOLEAN_V);
        TransportProtos.TsKvProto tsKvProtoAttribute3 = getTsKvProto("attribute3", "42.0", TransportProtos.KeyValueType.DOUBLE_V);
        TransportProtos.TsKvProto tsKvProtoAttribute4 = getTsKvProto("attribute4", "73", TransportProtos.KeyValueType.LONG_V);
        TransportProtos.TsKvProto tsKvProtoAttribute5 = getTsKvProto("attribute5", "{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}", TransportProtos.KeyValueType.JSON_V);
        List<TransportProtos.TsKvProto> tsKvProtoList = new ArrayList<>();
        tsKvProtoList.add(tsKvProtoAttribute1);
        tsKvProtoList.add(tsKvProtoAttribute2);
        tsKvProtoList.add(tsKvProtoAttribute3);
        tsKvProtoList.add(tsKvProtoAttribute4);
        tsKvProtoList.add(tsKvProtoAttribute5);
        return tsKvProtoList;
    }

    protected TransportProtos.TsKvProto getTsKvProto(String key, String value, TransportProtos.KeyValueType keyValueType) {
        TransportProtos.TsKvProto.Builder tsKvProtoBuilder = TransportProtos.TsKvProto.newBuilder();
        TransportProtos.KeyValueProto keyValueProto = getKeyValueProto(key, value, keyValueType);
        tsKvProtoBuilder.setKv(keyValueProto);
        return tsKvProtoBuilder.build();
    }

    protected TestMqttCallback getTestMqttCallback() {
        CountDownLatch latch = new CountDownLatch(1);
        return new TestMqttCallback(latch);
    }

    protected static class TestMqttCallback implements MqttCallback {

        private final CountDownLatch latch;
        private Integer qoS;
        private byte[] payloadBytes;

        TestMqttCallback(CountDownLatch latch) {
            this.latch = latch;
        }

        public int getQoS() {
            return qoS;
        }

        public byte[] getPayloadBytes() {
            return payloadBytes;
        }

        public CountDownLatch getLatch() {
            return latch;
        }

        @Override
        public void connectionLost(Throwable throwable) {
        }

        @Override
        public void messageArrived(String requestTopic, MqttMessage mqttMessage) throws Exception {
            qoS = mqttMessage.getQos();
            payloadBytes = mqttMessage.getPayload();
            latch.countDown();
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

        }
    }

}

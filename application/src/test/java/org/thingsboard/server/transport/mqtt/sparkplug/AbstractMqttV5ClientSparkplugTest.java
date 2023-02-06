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
package org.thingsboard.server.transport.mqtt.sparkplug;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttConnAck;
import org.eclipse.paho.mqttv5.common.packet.MqttReturnCode;
import org.eclipse.paho.mqttv5.common.packet.MqttWireMessage;
import org.junit.Assert;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.gen.transport.mqtt.SparkplugBProto;
import org.thingsboard.server.transport.mqtt.AbstractMqttIntegrationTest;
import org.thingsboard.server.transport.mqtt.MqttTestConfigProperties;
import org.thingsboard.server.transport.mqtt.mqttv5.MqttV5TestClient;
import org.thingsboard.server.transport.mqtt.util.sparkplug.MetricDataType;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType;

import java.util.Calendar;

import static org.eclipse.paho.mqttv5.common.packet.MqttWireMessage.MESSAGE_TYPE_CONNACK;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.MetricDataType.Int64;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMetricUtil.createMetric;

/**
 * Created by nickAS21 on 12.01.23
 */
@Slf4j
public abstract class AbstractMqttV5ClientSparkplugTest extends AbstractMqttIntegrationTest {

    protected MqttV5TestClient client;
    protected Calendar calendar = Calendar.getInstance();

    protected static final String NAMESPACE = "spBv1.0";
    protected static final String groupId = "SparkplugBGroupId";
    protected static final String edgeNode = "SparkpluBNode";
    protected static final String keysBdSeq = "bdSeq";
    protected static final String alias = "Failed Post Telemetry node proto payload. SparkplugMessageType ";
    protected String deviceId = "Test Sparkplug B Device";
    protected int bdSeq = 0;
    protected int seq = 0;
    protected static final long PUBLISH_TS_DELTA_MS = 86400000;// Publish start TS <-> 24h

    public void beforeSparkplugTest() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .gatewayName("Test Connect Sparkplug client node")
                .isSparkPlug(true)
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .build();
        processBeforeTest(configProperties);
    }

    public void clientWithCorrectNodeAccessTokenWithNDEATH() throws Exception {
        long ts = calendar.getTimeInMillis();
        long value = bdSeq = 0;
        clientWithCorrectNodeAccessTokenWithNDEATH(ts, value);
    }

    public void clientWithCorrectNodeAccessTokenWithNDEATH(long ts, long value) throws Exception {
        String key = keysBdSeq;
        MetricDataType metricDataType = Int64;
        SparkplugBProto.Payload.Builder deathPayload = SparkplugBProto.Payload.newBuilder()
                .setTimestamp(calendar.getTimeInMillis());
        deathPayload.addMetrics(createMetric(value, ts, key, metricDataType));
        byte[] deathBytes = deathPayload.build().toByteArray();
        this.client = new MqttV5TestClient();
        MqttConnectionOptions options = new MqttConnectionOptions();
        options.setUserName(gatewayAccessToken);
        String topic = NAMESPACE + "/" + groupId + "/" + SparkplugMessageType.NDEATH.name() + "/" + edgeNode;
        MqttMessage msg = new MqttMessage();
        msg.setId(0);
        msg.setPayload(deathBytes);
        options.setWill(topic, msg);
        IMqttToken connectionResult = client.connect(options);

        MqttWireMessage response = connectionResult.getResponse();
        Assert.assertEquals(MESSAGE_TYPE_CONNACK, response.getType());
        MqttConnAck connAckMsg = (MqttConnAck) response;
        Assert.assertEquals(MqttReturnCode.RETURN_CODE_SUCCESS, connAckMsg.getReturnCode());
    }

    protected long getBdSeqNum() throws Exception {
        if (bdSeq == 256) {
            bdSeq = 0;
        }
        return bdSeq++;
    }

    protected long getSeqNum() throws Exception {
        if (seq == 256) {
            seq = 0;
        }
        return seq++;
    }

}

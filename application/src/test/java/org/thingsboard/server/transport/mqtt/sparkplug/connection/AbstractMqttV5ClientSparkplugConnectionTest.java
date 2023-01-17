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
package org.thingsboard.server.transport.mqtt.sparkplug.connection;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttConnAck;
import org.eclipse.paho.mqttv5.common.packet.MqttReturnCode;
import org.eclipse.paho.mqttv5.common.packet.MqttWireMessage;
import org.junit.Assert;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.gen.transport.mqtt.SparkplugBProto;
import org.thingsboard.server.transport.mqtt.AbstractMqttIntegrationTest;
import org.thingsboard.server.transport.mqtt.mqttv5.MqttV5TestClient;
import org.thingsboard.server.transport.mqtt.sparkplug.AbstractMqttV5ClientSparkplugTest;
import org.thingsboard.server.transport.mqtt.util.sparkplug.MetricDataType;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMetricUtil;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.awaitility.Awaitility.await;
import static org.eclipse.paho.mqttv5.common.packet.MqttWireMessage.MESSAGE_TYPE_CONNACK;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.MetricDataType.Int64;

/**
 * Created by nickAS21 on 12.01.23
 */
@Slf4j
public  abstract class AbstractMqttV5ClientSparkplugConnectionTest extends AbstractMqttV5ClientSparkplugTest {

    protected void processClientWithCorrectNodeAccessTokenTest() throws Exception {
        processClientWithCorrectNodeAccess();
    }

    protected void processClientWithCorrectNodeAccessTokenWithNdeathTest() throws Exception {
        long ts = calendar.getTimeInMillis()-PUBLISH_TS_DELTA_MS;
        int value = bdSeq;
        MetricDataType metricDataType = Int64;
        TsKvEntry expectedTsKvEntryOriginal = new BasicTsKvEntry(ts, new LongDataEntry(keysBdSeq, Integer.toUnsignedLong(value)));

        SparkplugBProto.Payload.Builder deathPayload = SparkplugBProto.Payload.newBuilder()
                .setTimestamp(calendar.getTimeInMillis());
        deathPayload.addMetrics(createMetric(expectedTsKvEntryOriginal, metricDataType));

        byte[] deathBytes = deathPayload.build().toByteArray();

        MqttWireMessage response = clientWithCorrectNodeAccessTokenWithNdeath(deathBytes);

        Assert.assertEquals(MESSAGE_TYPE_CONNACK, response.getType());

        MqttConnAck connAckMsg = (MqttConnAck) response;

        Assert.assertEquals(MqttReturnCode.RETURN_CODE_SUCCESS, connAckMsg.getReturnCode());

        String keys = SparkplugMessageType.NDEATH.name() + " " + keysBdSeq;
        TsKvEntry expectedTsKvEntry = new BasicTsKvEntry(ts, new LongDataEntry(keys, Integer.toUnsignedLong(value)));
        ListenableFuture<Optional<TsKvEntry>> future = tsService.findLatest(tenantId, savedGateway.getId(), keys);
        AtomicReference<ListenableFuture<Optional<TsKvEntry>>> finalFuture = new AtomicReference<>(future);
        await("Failed Post Telemetry node proto payload. SparkplugMessageType NDEATH")
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    finalFuture.set(tsService.findLatest(tenantId, savedGateway.getId(), keys));
                    return finalFuture.get().get().isPresent();
                });
        TsKvEntry actualTsKvEntry = finalFuture.get().get().get();
        Assert.assertEquals(expectedTsKvEntry, actualTsKvEntry);
        client.disconnect();
    }

    protected void processClientWithCorrectAccessTokenCreatedDevices(int cntDevices) throws Exception {
        processClientWithCorrectNodeAccess();



    }

    private MqttWireMessage clientWithCorrectNodeAccessTokenWithNdeath(byte[] deathBytes) throws Exception {
        this.client = new MqttV5TestClient();
        MqttConnectionOptions options = new MqttConnectionOptions();
        options.setUserName(gatewayAccessToken);
        if (deathBytes != null) {
            String topic = NAMESPACE + "/" + groupId + "/" + SparkplugMessageType.NDEATH.name() + "/" + edgeNode;
            MqttMessage msg = new MqttMessage();
            msg.setId(0);
            msg.setPayload(deathBytes);
            options.setWill(topic, msg);
        }
        IMqttToken connectionResult = client.connect(options);
        return connectionResult.getResponse();
    }

}

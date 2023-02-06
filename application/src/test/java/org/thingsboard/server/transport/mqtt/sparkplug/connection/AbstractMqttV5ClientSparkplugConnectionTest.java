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
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.junit.Assert;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.gen.transport.mqtt.SparkplugBProto;
import org.thingsboard.server.transport.mqtt.mqttv5.MqttV5TestClient;
import org.thingsboard.server.transport.mqtt.sparkplug.AbstractMqttV5ClientSparkplugTest;
import org.thingsboard.server.transport.mqtt.util.sparkplug.MetricDataType;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.awaitility.Awaitility.await;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.MetricDataType.Int32;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMetricUtil.createMetric;

/**
 * Created by nickAS21 on 12.01.23
 */
@Slf4j
public  abstract class AbstractMqttV5ClientSparkplugConnectionTest extends AbstractMqttV5ClientSparkplugTest {

    protected void processClientWithCorrectNodeAccessTokenWithNDEATH_Test() throws Exception {
        long ts = calendar.getTimeInMillis()-PUBLISH_TS_DELTA_MS;
        long value = bdSeq = 0;
        clientWithCorrectNodeAccessTokenWithNDEATH(ts, value);

        String keys = SparkplugMessageType.NDEATH.name() + " " + keysBdSeq;
        TsKvEntry expectedTsKvEntry = new BasicTsKvEntry(ts, new LongDataEntry(keys, value));
        AtomicReference<ListenableFuture<Optional<TsKvEntry>>> finalFuture = new AtomicReference<>();
        await(alias + SparkplugMessageType.NDEATH.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    finalFuture.set(tsService.findLatest(tenantId, savedGateway.getId(), keys));
                    return finalFuture.get().get().isPresent();
                });
        TsKvEntry actualTsKvEntry = finalFuture.get().get().get();
        Assert.assertEquals(expectedTsKvEntry, actualTsKvEntry);
    }

    protected void processClientWithCorrectNodeAccessTokenWithoutNDEATH_Test() throws Exception {
        this.client = new MqttV5TestClient();
        MqttException actualException = Assert.assertThrows(MqttException.class, () -> client.connectAndWait(gatewayAccessToken));
        String expectedMessage = "Server unavailable.";
        int expectedReasonCode = 136;
        Assert.assertEquals(expectedMessage, actualException.getMessage());
        Assert.assertEquals( expectedReasonCode, actualException.getReasonCode());
    }

    protected void processClientWithCorrectAccessTokenWithNDEATHCreatedDevices(int cntDevices) throws Exception {
        clientWithCorrectNodeAccessTokenWithNDEATH();
        long ts = calendar.getTimeInMillis();
        MetricDataType metricDataType = Int32;
        Set<String> deviceIds = new HashSet<>();
        String key = "Device Metric int32";
        int valueDeviceInt32 = 1024;
        SparkplugBProto.Payload.Metric metric = createMetric(valueDeviceInt32, ts, key, metricDataType);
        for (int i=0; i < cntDevices; i++ ) {
            SparkplugBProto.Payload.Builder payloadBirthDevice = SparkplugBProto.Payload.newBuilder()
                    .setTimestamp(calendar.getTimeInMillis())
                    .setSeq(getSeqNum());
            String deviceName = deviceId + "_" + i;

            payloadBirthDevice.addMetrics(metric);
            if (client.isConnected()) {
                client.publish(NAMESPACE + "/" + groupId + "/" + SparkplugMessageType.DBIRTH.name() + "/" + edgeNode + "/" + deviceName,
                        payloadBirthDevice.build().toByteArray(), 0, false);
                AtomicReference<Device> device = new AtomicReference<>();
                await(alias + "find device [" + deviceName + "] after created")
                        .atMost(200, TimeUnit.SECONDS)
                        .until(() -> {
                            device.set(doGet("/api/tenant/devices?deviceName=" + deviceName, Device.class));
                            return device.get() != null;
                        });
            }
            deviceIds.add(deviceName);
        }
        Assert.assertEquals(cntDevices, deviceIds.size());
    }

}

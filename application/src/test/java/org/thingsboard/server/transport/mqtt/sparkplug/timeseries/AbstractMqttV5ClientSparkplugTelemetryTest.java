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
package org.thingsboard.server.transport.mqtt.sparkplug.timeseries;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.gen.transport.mqtt.SparkplugBProto;
import org.thingsboard.server.transport.mqtt.sparkplug.AbstractMqttV5ClientSparkplugTest;
import org.thingsboard.server.transport.mqtt.util.sparkplug.MetricDataType;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.awaitility.Awaitility.await;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.MetricDataType.Int16;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.MetricDataType.Int32;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.MetricDataType.Int64;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.MetricDataType.Int8;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.MetricDataType.UInt16;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.MetricDataType.UInt32;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.MetricDataType.UInt64;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.MetricDataType.UInt8;

/**
 * Created by nickAS21 on 12.01.23
 */
@Slf4j
public  abstract class AbstractMqttV5ClientSparkplugTelemetryTest extends AbstractMqttV5ClientSparkplugTest {

    protected void processClientWithCorrectAccessTokenPushDeviceMetricBuildSimple() throws Exception {
        processClientWithCorrectNodeAccess();
        Random random = new Random();
        String deviceName = deviceId + "_" + 10;
        String messageTypeName = SparkplugMessageType.DDATA.name();
        List<String> listKeys = new ArrayList<>();

        SparkplugBProto.Payload.Builder ddataPayload = SparkplugBProto.Payload.newBuilder()
                .setTimestamp(calendar.getTimeInMillis())
                .setSeq(getSeqNum());
        long ts = calendar.getTimeInMillis()-PUBLISH_TS_DELTA_MS;

        String keys = "MyInt8";
        MetricDataType metricDataType = Int8;
        TsKvEntry tsKvEntry = new BasicTsKvEntry(ts, new LongDataEntry(keys, (long)((byte)random.nextInt())));
        ddataPayload.addMetrics(createMetric(tsKvEntry, metricDataType));
        listKeys.add(keys);

        keys = "MyInt16";
        metricDataType = Int16;
        tsKvEntry = new BasicTsKvEntry(ts, new LongDataEntry(keys, (long)((short)random.nextInt())));
        ddataPayload.addMetrics(createMetric(tsKvEntry, metricDataType));
        listKeys.add(keys);

        keys = "MyInt32";
        metricDataType = Int32;
        tsKvEntry = new BasicTsKvEntry(ts, new LongDataEntry(keys, (long)(random.nextInt())));
        ddataPayload.addMetrics(createMetric(tsKvEntry, metricDataType));
        listKeys.add(keys);

        keys = "MyInt64";
        metricDataType = UInt64;
        tsKvEntry = new BasicTsKvEntry(ts, new LongDataEntry(keys, random.nextLong()));
        ddataPayload.addMetrics(createMetric(tsKvEntry, metricDataType));
        listKeys.add(keys);

        keys = "MyUInt8";
        metricDataType = UInt8;
        tsKvEntry = new BasicTsKvEntry(ts, new LongDataEntry(keys, (long)((short)random.nextInt())));
        ddataPayload.addMetrics(createMetric(tsKvEntry, metricDataType));
        listKeys.add(keys);

        keys = "MyUInt16";
        metricDataType = UInt16;
        tsKvEntry = new BasicTsKvEntry(ts, new LongDataEntry(keys, (long)random.nextInt()));
        ddataPayload.addMetrics(createMetric(tsKvEntry, metricDataType));
        listKeys.add(keys);

        keys = "MyUInt32";
        metricDataType = UInt32;
        tsKvEntry = new BasicTsKvEntry(ts, new LongDataEntry(keys, random.nextLong()));
        ddataPayload.addMetrics(createMetric(tsKvEntry, metricDataType));
        listKeys.add(keys);

        keys = "MyUInt64";
        metricDataType = UInt64;
        tsKvEntry = new BasicTsKvEntry(ts, new LongDataEntry(keys, BigInteger.valueOf(random.nextLong()).longValue()));
        ddataPayload.addMetrics(createMetric(tsKvEntry, metricDataType));
        listKeys.add(keys);

        keys = "MyFloat";
        metricDataType = MetricDataType.Float;
        tsKvEntry = new BasicTsKvEntry(ts, new LongDataEntry(keys, (long)random.nextFloat()));
        ddataPayload.addMetrics(createMetric(tsKvEntry, metricDataType));
        listKeys.add(keys);

        keys = "MyDateTime";
        metricDataType = MetricDataType.DateTime;
        tsKvEntry = new BasicTsKvEntry(ts, new LongDataEntry(keys, ts));
        ddataPayload.addMetrics(createMetric(tsKvEntry, metricDataType));
        listKeys.add(keys);

        keys = "MyDouble";
        metricDataType = MetricDataType.Double;
        tsKvEntry = new BasicTsKvEntry(ts, new LongDataEntry(keys, (long) random.nextDouble()));
        ddataPayload.addMetrics(createMetric(tsKvEntry, metricDataType));
        listKeys.add(keys);

        keys = "MyBoolean";
        metricDataType = MetricDataType.Boolean;
        tsKvEntry = new BasicTsKvEntry(ts, new BooleanDataEntry(keys, random.nextBoolean()));
        ddataPayload.addMetrics(createMetric(tsKvEntry, metricDataType));
        listKeys.add(keys);

        keys = "MyString";
        metricDataType = MetricDataType.String;
        tsKvEntry = new BasicTsKvEntry(ts, new StringDataEntry(keys, newUUID()));
        ddataPayload.addMetrics(createMetric(tsKvEntry, metricDataType));
        listKeys.add(keys);

        keys = "MyText";
        metricDataType = MetricDataType.Text;
        tsKvEntry = new BasicTsKvEntry(ts, new StringDataEntry(keys, newUUID()));
        ddataPayload.addMetrics(createMetric(tsKvEntry, metricDataType));
        listKeys.add(keys);

        keys = "MyUUID";
        metricDataType = MetricDataType.Text;
        tsKvEntry = new BasicTsKvEntry(ts, new StringDataEntry(keys, newUUID()));
        ddataPayload.addMetrics(createMetric(tsKvEntry, metricDataType));
        listKeys.add(keys);

        if (client.isConnected()) {
            client.publish(NAMESPACE + "/" + groupId + "/" + messageTypeName + "/" + edgeNode + "/" + deviceName,
                    ddataPayload.build().toByteArray(), 0, false);
        }

        AtomicReference<ListenableFuture<List<TsKvEntry>>> finalFuture = new AtomicReference<>();
        await(alias + SparkplugMessageType.NCMD.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    finalFuture.set(tsService.findLatest(tenantId, savedGateway.getId(), listKeys));
                    return !finalFuture.get().get().isEmpty();
                });
        Assert.assertEquals(listKeys.size(), finalFuture.get().get().size());
    }

    protected void processClientWithCorrectAccessTokenPublishNBIRTH() throws Exception {
        processClientWithCorrectNodeAccess();
        List<String> listKeys = new ArrayList<>();
        SparkplugBProto.Payload.Builder payloadBirthNode = SparkplugBProto.Payload.newBuilder()
                .setTimestamp(calendar.getTimeInMillis());
        long ts = calendar.getTimeInMillis()-PUBLISH_TS_DELTA_MS;
        long valueBdSec = getBdSeqNum();
        MetricDataType metricDataType = Int64;
        TsKvEntry tsKvEntryBdSecOriginal = new BasicTsKvEntry(ts, new LongDataEntry(keysBdSeq, valueBdSec));
        payloadBirthNode.addMetrics(createMetric(tsKvEntryBdSecOriginal, metricDataType));
        listKeys.add(SparkplugMessageType.NBIRTH.name() + " " + keysBdSeq);

        String keys  = "Node Control/Rebirth";
        boolean valueRebirth = false;
        metricDataType = MetricDataType.Boolean;
        TsKvEntry expectedSsKvEntryRebirth = new BasicTsKvEntry(ts, new BooleanDataEntry(keys, valueRebirth));
        payloadBirthNode.addMetrics(createMetric(expectedSsKvEntryRebirth , metricDataType));
        listKeys.add(keys);

        keys  = "Node Metric int32";
        int valueNodeInt32 = 1024;
        metricDataType = MetricDataType.Boolean;
        TsKvEntry expectedSsKvEntryNodeInt32 = new BasicTsKvEntry(ts, new LongDataEntry(keys, Integer.toUnsignedLong(valueNodeInt32)));
        payloadBirthNode.addMetrics(createMetric(expectedSsKvEntryNodeInt32 , metricDataType));
        listKeys.add(keys);

        client.publish(NAMESPACE + "/" + groupId + "/" + SparkplugMessageType.NBIRTH.name() + "/" + edgeNode,
                payloadBirthNode.build().toByteArray(), 0, false);

        AtomicReference<ListenableFuture<List<TsKvEntry>>> finalFuture = new AtomicReference<>();
        await(alias + SparkplugMessageType.NBIRTH.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    finalFuture.set(tsService.findLatest(tenantId, savedGateway.getId(), listKeys));
                    return !finalFuture.get().get().isEmpty();
                });
        Assert.assertEquals(listKeys.size(), finalFuture.get().get().size());
    }

    protected void processClientWithCorrectAccessTokenPublishNCMDReBirth() throws Exception {
        processClientWithCorrectNodeAccess();
        SparkplugBProto.Payload.Builder payloadBirthNode = SparkplugBProto.Payload.newBuilder()
                .setTimestamp(calendar.getTimeInMillis());
        List<String> listKeys = new ArrayList<>();
        long ts = calendar.getTimeInMillis()-PUBLISH_TS_DELTA_MS;
        long valueBdSec = getBdSeqNum();
        MetricDataType metricDataType = Int64;
        TsKvEntry tsKvEntryBdSecOriginal = new BasicTsKvEntry(ts, new LongDataEntry(keysBdSeq, valueBdSec));
        payloadBirthNode.addMetrics(createMetric(tsKvEntryBdSecOriginal, metricDataType));
        listKeys.add(SparkplugMessageType.NCMD.name() + " " + keysBdSeq);

        String keys  = "Node Control/Rebirth";
        boolean valueRebirth = true;
        metricDataType = MetricDataType.Boolean;
        TsKvEntry expectedSsKvEntryRebirth = new BasicTsKvEntry(ts, new BooleanDataEntry(keys, valueRebirth));
        payloadBirthNode.addMetrics(createMetric(expectedSsKvEntryRebirth , metricDataType));
        listKeys.add(keys);

        client.publish(NAMESPACE + "/" + groupId + "/" + SparkplugMessageType.NCMD.name() + "/" + edgeNode,
                payloadBirthNode.build().toByteArray(), 0, false);

        AtomicReference<ListenableFuture<List<TsKvEntry>>> finalFuture = new AtomicReference<>();
        await(alias + SparkplugMessageType.NCMD.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    finalFuture.set(tsService.findLatest(tenantId, savedGateway.getId(), listKeys));
                    return !finalFuture.get().get().isEmpty();
                });
        Assert.assertEquals(listKeys.size(), finalFuture.get().get().size());
    }

    private String newUUID() {
        return java.util.UUID.randomUUID().toString();
    }

}

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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.gen.transport.mqtt.SparkplugBProto;
import org.thingsboard.server.transport.mqtt.sparkplug.AbstractMqttV5ClientSparkplugTest;
import org.thingsboard.server.transport.mqtt.util.sparkplug.MetricDataType;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
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
public abstract class AbstractMqttV5ClientSparkplugTelemetryTest extends AbstractMqttV5ClientSparkplugTest {

    protected ThreadLocalRandom random = ThreadLocalRandom.current();

    protected void processClientWithCorrectAccessTokenPublishNBIRTH() throws Exception {
        processClientWithCorrectNodeAccess();
        List<String> listKeys = new ArrayList<>();
        SparkplugBProto.Payload.Builder payloadBirthNode = SparkplugBProto.Payload.newBuilder()
                .setTimestamp(calendar.getTimeInMillis());
        long ts = calendar.getTimeInMillis() - PUBLISH_TS_DELTA_MS;
        long valueBdSec = getBdSeqNum();
        MetricDataType metricDataType = Int64;
        TsKvEntry tsKvEntryBdSecOriginal = new BasicTsKvEntry(ts, new LongDataEntry(keysBdSeq, valueBdSec));
        payloadBirthNode.addMetrics(createMetric(valueBdSec, tsKvEntryBdSecOriginal, metricDataType));
        listKeys.add(SparkplugMessageType.NBIRTH.name() + " " + keysBdSeq);

        String keys = "Node Control/Rebirth";
        boolean valueRebirth = false;
        metricDataType = MetricDataType.Boolean;
        TsKvEntry expectedSsKvEntryRebirth = new BasicTsKvEntry(ts, new BooleanDataEntry(keys, valueRebirth));
        payloadBirthNode.addMetrics(createMetric(valueRebirth, expectedSsKvEntryRebirth, metricDataType));
        listKeys.add(keys);

        keys = "Node Metric int32";
        int valueNodeInt32 = 1024;
        metricDataType = Int32;
        TsKvEntry expectedSsKvEntryNodeInt32 = new BasicTsKvEntry(ts, new LongDataEntry(keys, Integer.toUnsignedLong(valueNodeInt32)));
        payloadBirthNode.addMetrics(createMetric(valueNodeInt32, expectedSsKvEntryNodeInt32, metricDataType));
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
        long ts = calendar.getTimeInMillis() - PUBLISH_TS_DELTA_MS;
        long valueBdSec = getBdSeqNum();
        MetricDataType metricDataType = Int64;
        TsKvEntry tsKvEntryBdSecOriginal = new BasicTsKvEntry(ts, new LongDataEntry(keysBdSeq, valueBdSec));
        payloadBirthNode.addMetrics(createMetric(valueBdSec, tsKvEntryBdSecOriginal, metricDataType));
        listKeys.add(SparkplugMessageType.NCMD.name() + " " + keysBdSeq);

        String keys = "Node Control/Rebirth";
        boolean valueRebirth = true;
        metricDataType = MetricDataType.Boolean;
        TsKvEntry expectedSsKvEntryRebirth = new BasicTsKvEntry(ts, new BooleanDataEntry(keys, valueRebirth));
        payloadBirthNode.addMetrics(createMetric(valueRebirth, expectedSsKvEntryRebirth, metricDataType));
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

    protected void processClientWithCorrectAccessTokenPushNodeMetricBuildPrimitiveSimple() throws Exception {
        processClientWithCorrectNodeAccess();

        String deviceName = deviceId + "_" + 10;
        String messageTypeName = SparkplugMessageType.NDATA.name();
        List<String> listKeys = new ArrayList<>();
        List<TsKvEntry> listTsKvEntry = new ArrayList<>();

        SparkplugBProto.Payload.Builder ndataPayload = SparkplugBProto.Payload.newBuilder()
                .setTimestamp(calendar.getTimeInMillis())
                .setSeq(getSeqNum());
        long ts = calendar.getTimeInMillis() - PUBLISH_TS_DELTA_MS;

        createdAddMetricTsKv(listTsKvEntry, listKeys, ndataPayload, ts);

        if (client.isConnected()) {
            client.publish(NAMESPACE + "/" + groupId + "/" + messageTypeName + "/" + edgeNode,
                    ndataPayload.build().toByteArray(), 0, false);
        }

        AtomicReference<ListenableFuture<List<TsKvEntry>>> finalFuture = new AtomicReference<>();
        await(alias + SparkplugMessageType.NCMD.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    finalFuture.set(tsService.findAllLatest(tenantId, savedGateway.getId()));
                    return finalFuture.get().get().size() == listTsKvEntry.size();
                });
        Assert.assertTrue("Expected tsKvEntrys is not equal Actual tsKvEntrys", listTsKvEntry.containsAll(finalFuture.get().get()));
        Assert.assertTrue("Actual tsKvEntrys is not equal Expected tsKvEntrys", finalFuture.get().get().containsAll(listTsKvEntry));
    }

    private void createdAddMetricTsKv(List<TsKvEntry> listTsKvEntry, List<String> listKeys,
                                      SparkplugBProto.Payload.Builder dataPayload, long ts) throws ThingsboardException {

        String keys = "MyInt8";
        listTsKvEntry.add(createdAddMetricTsKvLong(dataPayload, keys, nextInt8(), ts, Int8));
        listKeys.add(keys);

        keys = "MyInt16";
        listTsKvEntry.add(createdAddMetricTsKvLong(dataPayload, keys, nextInt16(), ts, Int16));
        listKeys.add(keys);

        keys = "MyInt32";
        listTsKvEntry.add(createdAddMetricTsKvLong(dataPayload, keys, nextInt32(), ts, Int32));
        listKeys.add(keys);

        keys = "MyInt64";
        listTsKvEntry.add(createdAddMetricTsKvLong(dataPayload, keys, nextInt64(), ts, Int64));
        listKeys.add(keys);

        keys = "MyUInt8";
        listTsKvEntry.add(createdAddMetricTsKvLong(dataPayload, keys, nextUInt8(), ts, UInt8));
        listKeys.add(keys);

        keys = "MyUInt16";
        listTsKvEntry.add(createdAddMetricTsKvLong(dataPayload, keys, nextUInt16(), ts, UInt16));
        listKeys.add(keys);

        keys = "MyUInt32I";
        listTsKvEntry.add(createdAddMetricTsKvLong(dataPayload, keys, nextUInt32I(), ts, UInt32));
        listKeys.add(keys);

        keys = "MyUInt32L";
        listTsKvEntry.add(createdAddMetricTsKvLong(dataPayload, keys, nextUInt32L(), ts, UInt32));
        listKeys.add(keys);

        keys = "MyUInt64";
        listTsKvEntry.add(createdAddMetricTsKvLong(dataPayload, keys, nextUInt64(), ts, UInt64));
        listKeys.add(keys);

        keys = "MyFloat";
        listTsKvEntry.add(createdAddMetricTsKvFloat(dataPayload, keys, nextFloat(0, 100), ts, MetricDataType.Float));
        listKeys.add(keys);

        keys = "MyDateTime";
        listTsKvEntry.add(createdAddMetricTsKvLong(dataPayload, keys, ts, ts, MetricDataType.DateTime));
        listKeys.add(keys);

        keys = "MyDouble";
        listTsKvEntry.add(createdAddMetricTsKvDouble(dataPayload, keys, nextDouble(), ts, MetricDataType.Double));
        listKeys.add(keys);

        keys = "MyBoolean";
        listTsKvEntry.add(createdAddMetricTsKvBoolean(dataPayload, keys, nextBoolean(), ts, MetricDataType.Boolean));
        listKeys.add(keys);

        keys = "MyString";
        listTsKvEntry.add(createdAddMetricTsKvString(dataPayload, keys, nexString(), ts, MetricDataType.String));
        listKeys.add(keys);

        keys = "MyText";
        listTsKvEntry.add(createdAddMetricTsKvString(dataPayload, keys, nexString(), ts, MetricDataType.Text));
        listKeys.add(keys);

        keys = "MyUUID";
        listTsKvEntry.add(createdAddMetricTsKvString(dataPayload, keys, nexString(), ts, MetricDataType.UUID));
        listKeys.add(keys);

    }

    private TsKvEntry createdAddMetricTsKvLong(SparkplugBProto.Payload.Builder dataPayload, String keys, Object value,
                                               long ts, MetricDataType metricDataType) throws ThingsboardException {
        TsKvEntry tsKvEntry = new BasicTsKvEntry(ts, new LongDataEntry(keys, Long.valueOf(String.valueOf(value))));

        dataPayload.addMetrics(createMetric(value, tsKvEntry, metricDataType));
        return tsKvEntry;
    }

    private TsKvEntry createdAddMetricTsKvFloat(SparkplugBProto.Payload.Builder dataPayload, String keys, Object value,
                                                long ts, MetricDataType metricDataType) throws ThingsboardException {
        var f = new BigDecimal(String.valueOf(value));
        TsKvEntry tsKvEntry = new BasicTsKvEntry(ts, new DoubleDataEntry(keys, f.doubleValue()));
        dataPayload.addMetrics(createMetric(value, tsKvEntry, metricDataType));
        return tsKvEntry;
    }

    private TsKvEntry createdAddMetricTsKvDouble(SparkplugBProto.Payload.Builder dataPayload, String keys, double value,
                                                 long ts, MetricDataType metricDataType) throws ThingsboardException {
        var d = new BigDecimal(String.valueOf(value));
        TsKvEntry tsKvEntry = new BasicTsKvEntry(ts, new LongDataEntry(keys, d.longValueExact()));
        dataPayload.addMetrics(createMetric(value, tsKvEntry, metricDataType));
        return tsKvEntry;
    }

    private TsKvEntry createdAddMetricTsKvBoolean(SparkplugBProto.Payload.Builder dataPayload, String keys, boolean value,
                                                  long ts, MetricDataType metricDataType) throws ThingsboardException {
        TsKvEntry tsKvEntry = new BasicTsKvEntry(ts, new BooleanDataEntry(keys, value));
        dataPayload.addMetrics(createMetric(value, tsKvEntry, metricDataType));
        return tsKvEntry;
    }

    private TsKvEntry createdAddMetricTsKvString(SparkplugBProto.Payload.Builder dataPayload, String keys, String value,
                                                 long ts, MetricDataType metricDataType) throws ThingsboardException {
        TsKvEntry tsKvEntry = new BasicTsKvEntry(ts, new StringDataEntry(keys, value));
        dataPayload.addMetrics(createMetric(value, tsKvEntry, metricDataType));
        return tsKvEntry;
    }

    private TsKvEntry createdAddMetricTsKvJson(SparkplugBProto.Payload.Builder dataPayload, String keys, String value,
                                               long ts, MetricDataType metricDataType) throws ThingsboardException {
        TsKvEntry tsKvEntry = new BasicTsKvEntry(ts, new JsonDataEntry(keys, value));
        dataPayload.addMetrics(createMetric(value, tsKvEntry, metricDataType));
        return tsKvEntry;
    }

    private byte nextInt8() {
        return (byte) random.nextInt(Byte.MIN_VALUE, Byte.MAX_VALUE);
    }

    private short nextUInt8() {
        return (short) random.nextInt(0, Byte.MAX_VALUE * 2 + 1);
    }

    private short nextInt16() {
        return (short) random.nextInt(Short.MIN_VALUE, Short.MAX_VALUE);
    }

    private short nextUInt16() {
        return (short) random.nextInt(0, Short.MAX_VALUE * 2 + 1);
    }

    private int nextInt32() {
        return random.nextInt(Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    private int nextUInt32I() {
        return random.nextInt(0, Integer.MAX_VALUE);
    }

    private long nextUInt32L() {
        long l = Integer.MAX_VALUE;
        return random.nextLong(0, l * 2 + 1);
    }

    private long nextInt64() {
        return random.nextLong(Long.MIN_VALUE, Long.MAX_VALUE);
    }

    private long nextUInt64() {
        double d = Long.MAX_VALUE;
        return random.nextLong(0, (long) (d * 2 + 1));
    }

    private double nextDouble() {
        return random.nextDouble(Long.MIN_VALUE, Long.MAX_VALUE);
    }

    private float nextFloat(float min, float max) {
        if (min >= max)
            throw new IllegalArgumentException("max must be greater than min");
        float result = ThreadLocalRandom.current().nextFloat() * (max - min) + min;
        if (result >= max) // correct for rounding
            result = Float.intBitsToFloat(Float.floatToIntBits(max) - 1);
        return result;
    }


    private boolean nextBoolean() {
        return random.nextBoolean();
    }

    private String nexString() {
        return java.util.UUID.randomUUID().toString();
    }

    private List<String> getActualKeysList(DeviceId deviceId, List<String> expectedKeys, long start) throws Exception {
//        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis() + 3000;

        List<String> actualKeys = null;
        while (start <= end) {
            actualKeys = doGetAsyncTyped("/api/plugins/telemetry/DEVICE/" + deviceId + "/keys/timeseries", new TypeReference<>() {
            });

            Map<String, List<JsonNode>> timeseries = doGetAsyncTyped("/api/plugins/telemetry/DEVICE/" +
                    savedGateway.getId().getId() + "/values/timeseries?keys=" + expectedKeys.get(6), new TypeReference<>() {
            });
            if (actualKeys.size() == expectedKeys.size()) {
                break;
            }
            Thread.sleep(300);
            start += 100;
        }
        return actualKeys;
    }

}

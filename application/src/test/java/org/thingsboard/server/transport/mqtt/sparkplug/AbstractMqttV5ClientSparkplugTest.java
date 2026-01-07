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
package org.thingsboard.server.transport.mqtt.sparkplug;

import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttConnAck;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.eclipse.paho.mqttv5.common.packet.MqttReturnCode;
import org.eclipse.paho.mqttv5.common.packet.MqttWireMessage;
import org.junit.Assert;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.gen.transport.mqtt.SparkplugBProto;
import org.thingsboard.server.transport.mqtt.AbstractMqttIntegrationTest;
import org.thingsboard.server.transport.mqtt.MqttTestConfigProperties;
import org.thingsboard.server.transport.mqtt.mqttv5.MqttV5TestClient;
import org.thingsboard.server.transport.mqtt.util.sparkplug.MetricDataType;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.awaitility.Awaitility.await;
import static org.eclipse.paho.mqttv5.common.packet.MqttWireMessage.MESSAGE_TYPE_CONNACK;
import static org.thingsboard.common.util.JacksonUtil.newArrayNode;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.MetricDataType.Bytes;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.MetricDataType.Int16;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.MetricDataType.Int32;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.MetricDataType.Int64;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.MetricDataType.Int8;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.MetricDataType.UInt16;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.MetricDataType.UInt32;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.MetricDataType.UInt64;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.MetricDataType.UInt8;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMetricUtil.createMetric;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugTopicService.TOPIC_ROOT_SPB_V_1_0;

/**
 * Created by nickAS21 on 12.01.23
 */
@Slf4j
public abstract class AbstractMqttV5ClientSparkplugTest extends AbstractMqttIntegrationTest {

    protected MqttV5TestClient client;
    protected SparkplugMqttCallback mqttCallback;
    protected Calendar calendar = Calendar.getInstance();
    protected ThreadLocalRandom random = ThreadLocalRandom.current();

    protected static final String groupId = "SparkplugBGroupId";
    protected static final String edgeNode = "SparkpluBNode";
    protected static final String keysBdSeq = "bdSeq";
    protected static final String alias = "Failed Telemetry/Attribute proto sparkplug payload. SparkplugMessageType ";
    protected String deviceId = "Test Sparkplug B Device";
    protected int bdSeq = 0;
    protected int seq = 0;
    protected static final long PUBLISH_TS_DELTA_MS = 86400000;// Publish start TS <-> 24h

    // NBIRTH
    protected static final String keyNodeRebirth = "Node Control/Rebirth";

    //*BIRTH
    protected static final MetricDataType metricBirthDataType_Int32 = Int32;
    protected static final String metricBirthName_Int32 = "Device Metric int32";
    protected Set<String> sparkplugAttributesMetricNames;

    public void beforeSparkplugTest() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .gatewayName("Test Connect Sparkplug client node")
                .isSparkplug(true)
                .sparkplugAttributesMetricNames(sparkplugAttributesMetricNames)
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .build();
        processBeforeTest(configProperties);
    }

    public void clientWithCorrectNodeAccessTokenWithNDEATH() throws Exception {
        long ts = calendar.getTimeInMillis();
        long value = bdSeq = 0;
        clientWithCorrectNodeAccessTokenWithNDEATH(ts, value);
    }
    public void clientWithCorrectNodeAccessTokenWithNDEATH(Long alias) throws Exception {
        long ts = calendar.getTimeInMillis();
        long value = bdSeq = 0;
        clientMqttV5ConnectWithNDEATH(ts, value,alias);
    }

    public void clientWithCorrectNodeAccessTokenWithNDEATH(long ts, long value) throws Exception {
        IMqttToken connectionResult = clientMqttV5ConnectWithNDEATH(ts, value, -1L);
        MqttWireMessage response = connectionResult.getResponse();
        Assert.assertEquals(MESSAGE_TYPE_CONNACK, response.getType());
        MqttConnAck connAckMsg = (MqttConnAck) response;
        Assert.assertEquals(MqttReturnCode.RETURN_CODE_SUCCESS, connAckMsg.getReturnCode());
    }

    public IMqttToken clientMqttV5ConnectWithNDEATH(long ts, long value, Long alias, String... nameSpaceBad) throws Exception {
        return clientMqttV5ConnectWithNDEATH(ts, value, null, alias, nameSpaceBad);
    }
    public IMqttToken clientMqttV5ConnectWithNDEATH(long ts, long value, String metricName, Long alias, String... nameSpaceBad) throws Exception {
        String key = metricName == null ? keysBdSeq : metricName;
        MetricDataType metricDataType = Int64;
        SparkplugBProto.Payload.Builder deathPayload = SparkplugBProto.Payload.newBuilder()
                .setTimestamp(calendar.getTimeInMillis());
        deathPayload.addMetrics(createMetric(value, ts, key, metricDataType, alias));
        byte[] deathBytes = deathPayload.build().toByteArray();
        this.client = new MqttV5TestClient();
        this.mqttCallback = new SparkplugMqttCallback();
        this.client.setCallback(this.mqttCallback);
        MqttConnectionOptions options = new MqttConnectionOptions();
        // If the MQTT client is using MQTT v5.0, the Edge Node’s MQTT CONNECT packet MUST set the Clean Start flag to true and the Session Expiry Interval to 0
        options.setCleanStart(true);
        options.setSessionExpiryInterval(0L);
        options.setUserName(gatewayAccessToken);
        String nameSpace = nameSpaceBad.length == 0 ? TOPIC_ROOT_SPB_V_1_0 : nameSpaceBad[0];
        String topic = nameSpace + "/" + groupId + "/" + SparkplugMessageType.NDEATH.name() + "/" + edgeNode;
        // The NDEATH message MUST set the MQTT Will QoS to 1 and Retained flag to false
        MqttMessage msg = new MqttMessage();
        msg.setId(0);
        msg.setQos(1);
        msg.setPayload(deathBytes);
        options.setWill(topic, msg);
        return client.connect(options);
    }

    protected List<Device> connectClientWithCorrectAccessTokenWithNDEATHCreatedDevices(int cntDevices, long ts) throws Exception {
        List<Device> devices = new ArrayList<>();
        clientWithCorrectNodeAccessTokenWithNDEATH();
        MetricDataType metricDataType = Int32;
        String key = "Node Metric int32";
        int valueDeviceInt32 = 1024;
        SparkplugBProto.Payload.Metric metric = createMetric(valueDeviceInt32, ts, key, metricDataType, -1L);
        SparkplugBProto.Payload.Builder payloadBirthNode = SparkplugBProto.Payload.newBuilder()
                .setTimestamp(ts)
                .setSeq(getBdSeqNum());
        payloadBirthNode.addMetrics(metric);
        payloadBirthNode.setTimestamp(ts);
        if (client.isConnected()) {
            client.publish(TOPIC_ROOT_SPB_V_1_0 + "/" + groupId + "/" + SparkplugMessageType.NBIRTH.name() + "/" + edgeNode,
                    payloadBirthNode.build().toByteArray(), 0, false);
        }

        valueDeviceInt32 = 4024;
        metric = createMetric(valueDeviceInt32, ts, metricBirthName_Int32, metricBirthDataType_Int32, -1L);
        for (int i = 0; i < cntDevices; i++) {
            SparkplugBProto.Payload.Builder payloadBirthDevice = SparkplugBProto.Payload.newBuilder()
                    .setTimestamp(ts)
                    .setSeq(getSeqNum());
            String deviceName = deviceId + "_" + i;

            payloadBirthDevice.addMetrics(metric);
            if (client.isConnected()) {
                client.publish(TOPIC_ROOT_SPB_V_1_0 + "/" + groupId + "/" + SparkplugMessageType.DBIRTH.name() + "/" + edgeNode + "/" + deviceName,
                        payloadBirthDevice.build().toByteArray(), 0, false);
                AtomicReference<Device> device = new AtomicReference<>();
                await(alias + "find device [" + deviceName + "] after created")
                        .atMost(200, TimeUnit.SECONDS)
                        .until(() -> {
                            device.set(doGet("/api/tenant/devices?deviceName=" + deviceName, Device.class));
                            return device.get() != null;
                        });
                devices.add(device.get());
            }

        }

        Assert.assertEquals(cntDevices, devices.size());
        return devices;
    }

    protected List<Device> connectClientWithCorrectAccessTokenWithNDEATHWithAliasCreatedDevices(long ts) throws Exception {
        List<Device> devices = new ArrayList<>();
        Long alias = 0L;
        clientWithCorrectNodeAccessTokenWithNDEATH(alias++);
        MetricDataType metricDataType = Int32;
        String key = "Node Metric int32";
        int valueDeviceInt32 = 1024;
        SparkplugBProto.Payload.Metric metric = createMetric(valueDeviceInt32, ts, key, metricDataType, alias++);
        SparkplugBProto.Payload.Builder payloadBirthNode = SparkplugBProto.Payload.newBuilder()
                .setTimestamp(ts)
                .setSeq(getBdSeqNum());
        payloadBirthNode.addMetrics(metric);
        payloadBirthNode.setTimestamp(ts);
        if (client.isConnected()) {
            client.publish(TOPIC_ROOT_SPB_V_1_0 + "/" + groupId + "/" + SparkplugMessageType.NBIRTH.name() + "/" + edgeNode,
                    payloadBirthNode.build().toByteArray(), 0, false);
        }

        valueDeviceInt32 = 4024;
        metric = createMetric(valueDeviceInt32, ts, metricBirthName_Int32, metricBirthDataType_Int32, alias++);
        SparkplugBProto.Payload.Builder payloadBirthDevice = SparkplugBProto.Payload.newBuilder()
                .setTimestamp(ts)
                .setSeq(getSeqNum());
        String deviceName = deviceId + "_" + 1;

        payloadBirthDevice.addMetrics(metric);
        if (client.isConnected()) {
            client.publish(TOPIC_ROOT_SPB_V_1_0 + "/" + groupId + "/" + SparkplugMessageType.DBIRTH.name() + "/" + edgeNode + "/" + deviceName,
                    payloadBirthDevice.build().toByteArray(), 0, false);
            AtomicReference<Device> device = new AtomicReference<>();
            await(alias + "find device [" + deviceName + "] after created")
                    .atMost(200, TimeUnit.SECONDS)
                    .until(() -> {
                        device.set(doGet("/api/tenant/devices?deviceName=" + deviceName, Device.class));
                        return device.get() != null;
                    });
            devices.add(device.get());
        }

        Assert.assertEquals(1, devices.size());
        return devices;
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

    protected List<String> connectionWithNBirth(MetricDataType metricDataType, String metricKey, Object metricValue) throws Exception {
        List<String> listKeys = new ArrayList<>();
        SparkplugBProto.Payload.Builder payloadBirthNode = SparkplugBProto.Payload.newBuilder()
                .setTimestamp(calendar.getTimeInMillis());
        long ts = calendar.getTimeInMillis() - PUBLISH_TS_DELTA_MS;
        long valueBdSec = getBdSeqNum();
        payloadBirthNode.addMetrics(createMetric(valueBdSec, ts, keysBdSeq, Int64, -1L));
        listKeys.add(SparkplugMessageType.NBIRTH.name() + " " + keysBdSeq);
        payloadBirthNode.addMetrics(createMetric(false, ts, keyNodeRebirth, MetricDataType.Boolean, -1L));
        listKeys.add(keyNodeRebirth);

        if (StringUtils.isNotBlank(metricKey)) {
            payloadBirthNode.addMetrics(createMetric(metricValue, ts, metricKey, metricDataType, -1L));
        } else {
            payloadBirthNode.addMetrics(createMetric(metricValue, ts, metricKey, metricDataType, 4L));
        }
        listKeys.add(metricKey);

        if (client.isConnected()) {
            client.publish(TOPIC_ROOT_SPB_V_1_0 + "/" + groupId + "/" + SparkplugMessageType.NBIRTH.name() + "/" + edgeNode,
                    payloadBirthNode.build().toByteArray(), 0, false);
        }
        return listKeys;
    }

    protected void createdAddMetricValuePrimitiveTsKv(List<TsKvEntry> listTsKvEntry, List<String> listKeys,
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

        keys = "MyUInt32";
        listTsKvEntry.add(createdAddMetricTsKvLong(dataPayload, keys, nextUInt32(), ts, UInt32));
        listKeys.add(keys);

        keys = "MyUInt64";
        listTsKvEntry.add(createdAddMetricTsKvLong(dataPayload, keys, nextUInt64(), ts, UInt64));
        listKeys.add(keys);

        keys = "MyFloat";
        listTsKvEntry.add(createdAddMetricTsKvFloat(dataPayload, keys, nextFloat(0, 100), ts, MetricDataType.Float));
        listKeys.add(keys);

        keys = "MyDateTime";
        listTsKvEntry.add(createdAddMetricTsKvLong(dataPayload, keys, nextDateTime(), ts, MetricDataType.DateTime));
        listKeys.add(keys);

        keys = "MyDouble";
        listTsKvEntry.add(createdAddMetricTsKvDouble(dataPayload, keys, nextDouble(), ts, MetricDataType.Double));
        listKeys.add(keys);

        keys = "MyBoolean";
        listTsKvEntry.add(createdAddMetricTsKvBoolean(dataPayload, keys, nextBoolean(), ts, MetricDataType.Boolean));
        listKeys.add(keys);

        keys = "MyString";
        listTsKvEntry.add(createdAddMetricTsKvString(dataPayload, keys, nextString(), ts, MetricDataType.String));
        listKeys.add(keys);

        keys = "MyText";
        listTsKvEntry.add(createdAddMetricTsKvString(dataPayload, keys, nextString(), ts, MetricDataType.Text));
        listKeys.add(keys);

        keys = "MyUUID";
        listTsKvEntry.add(createdAddMetricTsKvString(dataPayload, keys, nextString(), ts, MetricDataType.UUID));
        listKeys.add(keys);

    }

    protected void createdAddMetricValueArraysPrimitiveTsKv(List<TsKvEntry> listTsKvEntry, List<String> listKeys,
                                                            SparkplugBProto.Payload.Builder dataPayload, long ts) throws ThingsboardException {
        String keys = "MyBytesArray";
        byte[] bytes = {nextInt8(), nextInt8(), nextInt8()};
        createdAddMetricTsKvJson(dataPayload, keys, bytes, ts, Bytes, listTsKvEntry, listKeys);
    }

    private TsKvEntry createdAddMetricTsKvLong(SparkplugBProto.Payload.Builder dataPayload, String key, Object value,
                                               long ts, MetricDataType metricDataType) throws ThingsboardException {
        TsKvEntry tsKvEntry = new BasicTsKvEntry(ts, new LongDataEntry(key, Long.valueOf(String.valueOf(value))));
        dataPayload.addMetrics(createMetric(value, ts, key, metricDataType, -1L));
        return tsKvEntry;
    }

    private TsKvEntry createdAddMetricTsKvFloat(SparkplugBProto.Payload.Builder dataPayload, String key, float value,
                                                long ts, MetricDataType metricDataType) throws ThingsboardException {
        Double dd = Double.parseDouble(Float.toString(value));
        TsKvEntry tsKvEntry = new BasicTsKvEntry(ts, new DoubleDataEntry(key, dd));
        dataPayload.addMetrics(createMetric(value, ts, key, metricDataType, -1L));
        return tsKvEntry;
    }

    private TsKvEntry createdAddMetricTsKvDouble(SparkplugBProto.Payload.Builder dataPayload, String key, double value,
                                                 long ts, MetricDataType metricDataType) throws ThingsboardException {
        Long l = Double.valueOf(value).longValue();
        TsKvEntry tsKvEntry = new BasicTsKvEntry(ts, new LongDataEntry(key, l));
        dataPayload.addMetrics(createMetric(value, ts, key, metricDataType, -1L));
        return tsKvEntry;
    }

    private TsKvEntry createdAddMetricTsKvBoolean(SparkplugBProto.Payload.Builder dataPayload, String key, boolean value,
                                                  long ts, MetricDataType metricDataType) throws ThingsboardException {
        TsKvEntry tsKvEntry = new BasicTsKvEntry(ts, new BooleanDataEntry(key, value));
        dataPayload.addMetrics(createMetric(value, ts, key, metricDataType, -1L));
        return tsKvEntry;
    }

    private TsKvEntry createdAddMetricTsKvString(SparkplugBProto.Payload.Builder dataPayload, String key, String value,
                                                 long ts, MetricDataType metricDataType) throws ThingsboardException {
        TsKvEntry tsKvEntry = new BasicTsKvEntry(ts, new StringDataEntry(key, value));
        dataPayload.addMetrics(createMetric(value, ts, key, metricDataType, -1L));
        return tsKvEntry;
    }

    private void createdAddMetricTsKvJson(SparkplugBProto.Payload.Builder dataPayload, String key,
                                          Object values, long ts, MetricDataType metricDataType,
                                          List<TsKvEntry> listTsKvEntry,
                                          List<String> listKeys) throws ThingsboardException {
        ArrayNode nodeArray = newArrayNode();
        switch (metricDataType) {
            case Bytes:
                for (byte b : (byte[]) values) {
                    nodeArray.add(b);
                }
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + metricDataType);
        }
        if (nodeArray.size() > 0) {
            Optional<TsKvEntry> tsKvEntryOptional = Optional.of(new BasicTsKvEntry(ts, new JsonDataEntry(key, nodeArray.toString())));
            if (tsKvEntryOptional.isPresent()) {
                dataPayload.addMetrics(createMetric(values, ts, key, metricDataType, -1L));
                listTsKvEntry.add(tsKvEntryOptional.get());
                listKeys.add(key);
            }
        }
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

    private int nextUInt16() {
        return random.nextInt(0, Short.MAX_VALUE * 2 + 1);
    }

    protected int nextInt32() {
        return random.nextInt(Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    protected long nextUInt32() {
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

    protected double nextDouble() {
        return random.nextDouble(Long.MIN_VALUE, Long.MAX_VALUE);
    }

    private long nextDateTime() {
        long min = calendar.getTimeInMillis() - PUBLISH_TS_DELTA_MS;
        long max = calendar.getTimeInMillis();
        return random.nextLong(min, max);
    }

    protected float nextFloat(float min, float max) {
        if (min >= max)
            throw new IllegalArgumentException("max must be greater than min");
        float result = ThreadLocalRandom.current().nextFloat() * (max - min) + min;
        if (result >= max) // correct for rounding
            result = Float.intBitsToFloat(Float.floatToIntBits(max) - 1);
        return result;
    }

    protected boolean nextBoolean() {
        return random.nextBoolean();
    }

    protected String nextString() {
        return java.util.UUID.randomUUID().toString();
    }

    public class SparkplugMqttCallback implements MqttCallback {
        private final List<SparkplugBProto.Payload.Metric> messageArrivedMetrics = new ArrayList<>();

        @Override
        public void disconnected(MqttDisconnectResponse mqttDisconnectResponse) {

        }

        @Override
        public void mqttErrorOccurred(MqttException e) {

        }

        @Override
        public void messageArrived(String topic, MqttMessage mqttMsg) throws Exception {
            SparkplugBProto.Payload sparkplugBProtoNode = SparkplugBProto.Payload.parseFrom(mqttMsg.getPayload());
            messageArrivedMetrics.addAll(sparkplugBProtoNode.getMetricsList());
        }

        @Override
        public void deliveryComplete(IMqttToken iMqttToken) {

        }

        @Override
        public void connectComplete(boolean b, String s) {

        }

        @Override
        public void authPacketArrived(int i, MqttProperties mqttProperties) {

        }

        public List<SparkplugBProto.Payload.Metric> getMessageArrivedMetrics() {
            return messageArrivedMetrics;
        }

        public void deleteMessageArrivedMetrics(int id) {
            messageArrivedMetrics.remove(id);
        }
    }

}

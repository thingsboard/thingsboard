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
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.relation.EntityRelation;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.awaitility.Awaitility.await;
import static org.eclipse.paho.mqttv5.common.packet.MqttWireMessage.MESSAGE_TYPE_CONNACK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
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
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugConnectionState.ONLINE;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType.STATE;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType.messageName;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMetricUtil.createMetric;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugTopicService.DEVICE_NAME_SPLIT_SEPARATOR;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugTopicService.TOPIC_ROOT_SPB_V_1_0;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugTopicService.TOPIC_SPLIT_SEPARATOR;

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
    protected static final String edgeNodeDeviceName = "Test Connect Sparkplug client node";
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
                .gatewayName(edgeNodeDeviceName)
                .isSparkplug(true)
                .sparkplugAttributesMetricNames(sparkplugAttributesMetricNames)
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .build();
        processBeforeTest(configProperties);
    }

    public void seedLegacyAndFullPathDevices() throws Exception {
        // 1. Create the first device with a short name (legacy style)
        String deviceName1 = deviceId + "_1";
        Device device1 = createDevice(deviceName1, deviceProfile.getName(), false);

        // 2. Establish 'Created' relation so the transport identifies this gateway as the owner
        String relationType = "Created";
        EntityRelation relation1 = createFromRelation(savedGateway, device1, relationType);
        doPost("/api/relation", relation1).andExpect(status().isOk());

        // 3. Create the second device with a full-path name
        String deviceName2 = groupId + DEVICE_NAME_SPLIT_SEPARATOR + edgeNode + DEVICE_NAME_SPLIT_SEPARATOR + deviceId + "_2";
        Device device2 = createDevice(deviceName2, deviceProfile.getName(), false);

        // 4. Establish 'Created' relation for the second device as well
        EntityRelation relation2 = createFromRelation(savedGateway, device2, relationType);
        doPost("/api/relation", relation2).andExpect(status().isOk());

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
        assertEquals(MESSAGE_TYPE_CONNACK, response.getType());
        MqttConnAck connAckMsg = (MqttConnAck) response;
        assertEquals(MqttReturnCode.RETURN_CODE_SUCCESS, connAckMsg.getReturnCode());
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
        String topic = nameSpace + TOPIC_SPLIT_SEPARATOR + groupId + TOPIC_SPLIT_SEPARATOR + SparkplugMessageType.NDEATH.name() + TOPIC_SPLIT_SEPARATOR + edgeNode;
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
            client.publish(TOPIC_ROOT_SPB_V_1_0 + TOPIC_SPLIT_SEPARATOR + groupId + TOPIC_SPLIT_SEPARATOR + SparkplugMessageType.NBIRTH.name() + TOPIC_SPLIT_SEPARATOR + edgeNode,
                    payloadBirthNode.build().toByteArray(), 0, false);
        }

        valueDeviceInt32 = 4024;
        metric = createMetric(valueDeviceInt32, ts, metricBirthName_Int32, metricBirthDataType_Int32, -1L);
        for (int i = 0; i < cntDevices; i++) {
            SparkplugBProto.Payload.Builder payloadBirthDevice = SparkplugBProto.Payload.newBuilder()
                    .setTimestamp(ts)
                    .setSeq(getSeqNum());
            String deviceIdName = deviceId + "_" + i;
            String deviceName = groupId +  DEVICE_NAME_SPLIT_SEPARATOR + edgeNode +  DEVICE_NAME_SPLIT_SEPARATOR + deviceIdName;
            payloadBirthDevice.addMetrics(metric);
            if (client.isConnected()) {
                client.publish(TOPIC_ROOT_SPB_V_1_0 + TOPIC_SPLIT_SEPARATOR + groupId + TOPIC_SPLIT_SEPARATOR + SparkplugMessageType.DBIRTH.name() + TOPIC_SPLIT_SEPARATOR + edgeNode + TOPIC_SPLIT_SEPARATOR + deviceIdName,
                        payloadBirthDevice.build().toByteArray(), 0, false);
                AtomicReference<Device> device = new AtomicReference<>();
                await(alias + "find device [" + deviceIdName + "] after created")
                        .atMost(40, TimeUnit.SECONDS)
                        .ignoreExceptions()
                        .until(() -> {
                            device.set(doGet("/api/tenant/devices?deviceName=" + deviceName, Device.class));
                            return device.get() != null;
                        });
                devices.add(device.get());
            }
        }

        assertEquals(cntDevices, devices.size());
        return devices;
    }

    protected void connectClientWithCorrectAccessTokenWithNDEATHDevicesCreatingBefore_Test(int cntDevices) throws Exception {
        long ts = calendar.getTimeInMillis();
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
            client.publish(TOPIC_ROOT_SPB_V_1_0 + TOPIC_SPLIT_SEPARATOR + groupId + TOPIC_SPLIT_SEPARATOR + SparkplugMessageType.NBIRTH.name() + TOPIC_SPLIT_SEPARATOR + edgeNode,
                    payloadBirthNode.build().toByteArray(), 0, false);
        }

        valueDeviceInt32 = 4024;
        metric = createMetric(valueDeviceInt32, ts, metricBirthName_Int32, metricBirthDataType_Int32, -1L);
        // as old device name -> deviceId
        String deviceIdNameLabel1 = deviceId + "_1";

        if (client.isConnected()) {
            SparkplugBProto.Payload.Builder payloadBirthDevice1 = SparkplugBProto.Payload.newBuilder()
                    .setTimestamp(ts)
                    .setSeq(getSeqNum());
            payloadBirthDevice1.addMetrics(metric);
            client.publish(TOPIC_ROOT_SPB_V_1_0 + TOPIC_SPLIT_SEPARATOR + groupId + TOPIC_SPLIT_SEPARATOR + SparkplugMessageType.DBIRTH.name() + TOPIC_SPLIT_SEPARATOR + edgeNode + TOPIC_SPLIT_SEPARATOR + deviceIdNameLabel1,
                    payloadBirthDevice1.build().toByteArray(), 0, false);

        }
        String deviceName1 = groupId + DEVICE_NAME_SPLIT_SEPARATOR + edgeNode + DEVICE_NAME_SPLIT_SEPARATOR + deviceIdNameLabel1;
        AtomicReference<Device> device1 = new AtomicReference<>();
        await(alias + "find device [" + deviceName1 + "] before connecting")
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    device1.set(doGet("/api/tenant/devices?deviceName=" + deviceName1, Device.class));
                    return device1.get() != null;
                });
        devices.add(device1.get());

        // as new device name ->  groupId +  ":" + edgeNode +  ":" + deviceId;
        String deviceIdName2 = deviceId + "_2";
        if (client.isConnected()) {
            SparkplugBProto.Payload.Builder payloadBirthDevice2 = SparkplugBProto.Payload.newBuilder()
                    .setTimestamp(ts)
                    .setSeq(getSeqNum());
            payloadBirthDevice2.addMetrics(metric);
            client.publish(TOPIC_ROOT_SPB_V_1_0 + TOPIC_SPLIT_SEPARATOR + groupId + TOPIC_SPLIT_SEPARATOR + SparkplugMessageType.DBIRTH.name() + TOPIC_SPLIT_SEPARATOR + edgeNode + TOPIC_SPLIT_SEPARATOR + deviceIdName2,
                    payloadBirthDevice2.build().toByteArray(), 0, false);
        }
        String deviceName2 = groupId + DEVICE_NAME_SPLIT_SEPARATOR + edgeNode + DEVICE_NAME_SPLIT_SEPARATOR + deviceIdName2;
        AtomicReference<Device> device2 = new AtomicReference<>();
        await(alias + "find device [" + deviceName2 + "] before connecting")
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    device2.set(doGet("/api/tenant/devices?deviceName=" + deviceName2, Device.class));
                    return device2.get() != null;
                });
        devices.add(device2.get());
        assertEquals(cntDevices, devices.size());
        state_ONLINE_ALL (devices, calendar.getTimeInMillis());
        // Without full topic: as it was in the old version. When deviceId is updated to full theme, Label is also updated to old deviceId
        assertEquals(deviceIdNameLabel1, device1.get().getLabel());
        // // With a full topic: if new. When creating a device by a client to a full topic, if the Label was not filled in - we do not touch it.
        Assert.assertNull(device2.get().getLabel());
    }

    /**
     * Coverage: Rename when a device with the target full-path name already exists (collision).
     */
    protected void renameCollisionWhenTargetNameAlreadyExists_Test() throws Exception {
        long ts = calendar.getTimeInMillis();
        String shortName = deviceId + "_1"; // Created in beforeTest
        String fullPathName = groupId + DEVICE_NAME_SPLIT_SEPARATOR + edgeNode + DEVICE_NAME_SPLIT_SEPARATOR + shortName;

        // Manually create a device that already has the "new" full-path name to trigger a collision
        createDevice(fullPathName, deviceProfile.getName(), false);

        clientWithCorrectNodeAccessTokenWithNDEATH();

        SparkplugBProto.Payload.Builder payload = SparkplugBProto.Payload.newBuilder()
                .setTimestamp(ts)
                .setSeq(getSeqNum());
        payload.addMetrics(createMetric(123, ts, metricBirthName_Int32, metricBirthDataType_Int32, -1L));

        // Gateway sends DBIRTH for the short name.
        // Transport will try to rename it but should find a conflict and handle it gracefully.
        client.publish(TOPIC_ROOT_SPB_V_1_0 + TOPIC_SPLIT_SEPARATOR + groupId + "/DBIRTH/" + edgeNode + TOPIC_SPLIT_SEPARATOR + shortName,
                payload.build().toByteArray(), 0, false);

        await("Checking stability after collision")
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    Device oldDevice = doGet("/api/tenant/devices?deviceName=" + shortName, Device.class);
                    Device conflictDevice = doGet("/api/tenant/devices?deviceName=" + fullPathName, Device.class);
                    // Both devices must still exist, proving no exception crashed the process
                    return oldDevice != null && conflictDevice != null;
                });
    }

    /**
     * Coverage: The privilege concern — attempt to rename a device not owned by the gateway.
     * This test verifies that the original device's ID remains unchanged, meaning it was not hijacked.
     */
    protected void unauthorizedRenameAttemptBad_Test() throws Exception {
        long ts = calendar.getTimeInMillis();
        String strangerName = "unauthorized_device_rename";

        // 1. Create a "stranger" device via API (it has no 'Created' relation to the gateway)
        Device stranger = new Device();
        stranger.setName(strangerName);
        stranger.setType("default");
        doPost("/api/device", stranger);
        final DeviceId originalStrangerId = stranger.getId();

        clientWithCorrectNodeAccessTokenWithNDEATH();

        SparkplugBProto.Payload.Builder payload = SparkplugBProto.Payload.newBuilder()
                .setTimestamp(ts).setSeq(getSeqNum());

        // 2. Unauthorized gateway attempts to rename this device via Sparkplug topic path
        client.publish(TOPIC_ROOT_SPB_V_1_0 + TOPIC_SPLIT_SEPARATOR + groupId + "/DBIRTH/" + edgeNode + TOPIC_SPLIT_SEPARATOR + strangerName,
                payload.build().toByteArray(), 0, false);

        String expectedFullPath = groupId + DEVICE_NAME_SPLIT_SEPARATOR + edgeNode + DEVICE_NAME_SPLIT_SEPARATOR + strangerName;

        // 3. Verify security: the original device must still be linked to its short name with the same ID
        await("Verify original device was not hijacked")
                .atMost(40, TimeUnit.SECONDS)
                .pollDelay(2, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    // Check if the original device still exists with its original ID
                    Device currentStranger = doGet("/api/tenant/devices?deviceName=" + strangerName, Device.class);
                    assertNotNull("Original device disappeared!", currentStranger);
                    assertEquals("Security breach: Original device ID changed!", originalStrangerId, currentStranger.getId());

                    // Even if the gateway created a NEW device with a full path, it must have a different ID
                    Device newDevice = doGet("/api/tenant/devices?deviceName=" + expectedFullPath, Device.class);
                    if (newDevice != null) {
                        Assert.assertNotEquals("Stranger device was successfully hijacked (IDs match)!", originalStrangerId, newDevice.getId());
                    }
                });
    }

    /**
     * Coverage: The privilege concern — attempt to rename a device not owned by the gateway.
     */
    protected void unauthorizedRenameAttempt_Test() throws Exception {
        long ts = calendar.getTimeInMillis();
        String strangerName = "unauthorized_device_rename";

        // Create a device without a "Created" relation to the gateway
        Device stranger = new Device();
        stranger.setName(strangerName);
        stranger.setType("default");
        doPost("/api/device", stranger);
        Device originalStrangerDevice =
                doGet("/api/tenant/devices?deviceName=" + strangerName, Device.class);

        clientWithCorrectNodeAccessTokenWithNDEATH();

        SparkplugBProto.Payload.Builder payload = SparkplugBProto.Payload.newBuilder()
                .setTimestamp(ts).setSeq(getSeqNum());

        // Unauthorized gateway attempts to rename the device via Sparkplug topic
        client.publish(TOPIC_ROOT_SPB_V_1_0 + TOPIC_SPLIT_SEPARATOR + groupId + "/DBIRTH/" + edgeNode + TOPIC_SPLIT_SEPARATOR + strangerName,
                payload.build().toByteArray(), 0, false);

        String expectedFullPath = groupId + DEVICE_NAME_SPLIT_SEPARATOR + edgeNode + DEVICE_NAME_SPLIT_SEPARATOR + strangerName;
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() ->
                doGet("/api/tenant/devices?deviceName=" + expectedFullPath, Device.class, status().isOk())
        );

        Device strangerDevice =
                doGet("/api/tenant/devices?deviceName=" + strangerName, Device.class);
        assertNotNull(strangerDevice);
        assertEquals(originalStrangerDevice.getId(), strangerDevice.getId());
    }

    protected void state_ONLINE_ALL (List<Device> devices, long ts) {
        TsKvEntry tsKvEntry = new BasicTsKvEntry(ts, new StringDataEntry(messageName(STATE), ONLINE.name()));
        await(alias + messageName(STATE) + ", device: " + savedGateway.getName())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    var foundEntry = tsService.findAllLatest(tenantId, savedGateway.getId()).get().stream()
                            .filter(tsKv -> tsKv.getKey().equals(tsKvEntry.getKey()))
                            .filter(tsKv -> tsKv.getValue().equals(tsKvEntry.getValue()))
                            .filter(tsKv -> tsKv.getTs() == tsKvEntry.getTs())
                            .findFirst();
                    return foundEntry.isPresent();
                });

        for (Device device : devices) {
            await(alias + messageName(STATE) + ", device: " + device.getName())
                    .atMost(40, TimeUnit.SECONDS)
                    .until(() -> {
                        var foundEntry = tsService.findAllLatest(tenantId, device.getId()).get().stream()
                                .filter(tsKv -> tsKv.getKey().equals(tsKvEntry.getKey()))
                                .filter(tsKv -> tsKv.getValue().equals(tsKvEntry.getValue()))
                                .filter(tsKv -> tsKv.getTs() == tsKvEntry.getTs())
                                .findFirst();
                        return foundEntry.isPresent();
                    });
        }
    }

    /**
     * Coverage: Concurrent first-message registration with the lock mechanism.
     */
    protected void concurrentFirstMessageRegistration_Test() throws Exception {
        int threadCount = 5;
        String concurrentDeviceName = "concurrent_device";
        clientWithCorrectNodeAccessTokenWithNDEATH();

        ExecutorService executor = newFixedThreadPool(threadCount);
        long ts = calendar.getTimeInMillis();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    SparkplugBProto.Payload.Builder payload = SparkplugBProto.Payload.newBuilder()
                            .setTimestamp(ts).setSeq(0);
                    client.publish(TOPIC_ROOT_SPB_V_1_0 + TOPIC_SPLIT_SEPARATOR + groupId + "/DBIRTH/" + edgeNode + TOPIC_SPLIT_SEPARATOR + concurrentDeviceName,
                            payload.build().toByteArray(), 0, false);
                } catch (Exception e) {
                    log.error("Concurrent publish failed", e);
                }
            });
        }

        String expectedName = groupId + DEVICE_NAME_SPLIT_SEPARATOR + edgeNode + DEVICE_NAME_SPLIT_SEPARATOR + concurrentDeviceName;
        await("Wait for concurrent registration result")
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> doGet("/api/tenant/devices?deviceName=" + expectedName, Device.class) != null);

        executor.shutdown();
    }

    /**
     * Coverage: Sparkplug-message handling when msgId <= 0 (#7).
     * Verifies that the transport does not close the session for Sparkplug clients using msgId 0.
     */
    protected void sparkplugSessionStaysAliveWithZeroMsgId_Test() throws Exception {
        // clientMqttV5ConnectWithNDEATH internally sets msgId = 0 for the Will message.
        // This validates that the connection is accepted despite msgId being 0.
        IMqttToken connectionResult = clientMqttV5ConnectWithNDEATH(calendar.getTimeInMillis(), 0, -1L);
        Assert.assertTrue("Sparkplug connection should be successful with msgId=0", client.isConnected());

        // Publish NBIRTH message which usually goes through the aggregate callback.
        // This verifies that msgId=0 in the callback does not trigger closeDeviceSession.
        connectionWithNBirth(Int32, "test_metric_msgId_0", 555);

        // Awaitility to ensure the session remains open after processing.
        await("Verify Sparkplug session remains open after receiving msgId=0")
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> client.isConnected());
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
            client.publish(TOPIC_ROOT_SPB_V_1_0 + TOPIC_SPLIT_SEPARATOR + groupId + TOPIC_SPLIT_SEPARATOR + SparkplugMessageType.NBIRTH.name() + TOPIC_SPLIT_SEPARATOR + edgeNode,
                    payloadBirthNode.build().toByteArray(), 0, false);
        }

        valueDeviceInt32 = 4024;
        metric = createMetric(valueDeviceInt32, ts, metricBirthName_Int32, metricBirthDataType_Int32, alias++);
        SparkplugBProto.Payload.Builder payloadBirthDevice = SparkplugBProto.Payload.newBuilder()
                .setTimestamp(ts)
                .setSeq(getSeqNum());
        String deviceIdName = deviceId + "_1";
        String deviceName = groupId +  DEVICE_NAME_SPLIT_SEPARATOR + edgeNode +  DEVICE_NAME_SPLIT_SEPARATOR + deviceIdName;

        payloadBirthDevice.addMetrics(metric);
        if (client.isConnected()) {
            client.publish(TOPIC_ROOT_SPB_V_1_0 + TOPIC_SPLIT_SEPARATOR + groupId + TOPIC_SPLIT_SEPARATOR + SparkplugMessageType.DBIRTH.name() + TOPIC_SPLIT_SEPARATOR + edgeNode + TOPIC_SPLIT_SEPARATOR + deviceIdName,
                    payloadBirthDevice.build().toByteArray(), 0, false);
            AtomicReference<Device> device = new AtomicReference<>();
            await(alias + "find device [" + deviceName + "] after created")
                    .atMost(40, TimeUnit.SECONDS)
                    .ignoreExceptions()
                    .until(() -> {
                        device.set(doGet("/api/tenant/devices?deviceName=" + deviceName, Device.class));
                        return device.get() != null;
                    });
            devices.add(device.get());
        }

        assertEquals(1, devices.size());
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
            client.publish(TOPIC_ROOT_SPB_V_1_0 + TOPIC_SPLIT_SEPARATOR + groupId + TOPIC_SPLIT_SEPARATOR + SparkplugMessageType.NBIRTH.name() + TOPIC_SPLIT_SEPARATOR + edgeNode,
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

    private EntityRelation createFromRelation(Device mainDevice, Device device, String relationType) {
        return new EntityRelation(mainDevice.getId(), device.getId(), relationType);
    }


}

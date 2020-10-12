/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.mqtt.telemetry.timeseries;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.device.profile.MqttTopics;
import org.thingsboard.server.gen.transport.TransportApiProtos;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Slf4j
public abstract class AbstractMqttTimeseriesProtoIntegrationTest extends AbstractMqttTimeseriesIntegrationTest {

    private static final String POST_DATA_TELEMETRY_TOPIC = "proto/telemetry";

    @Before
    public void beforeTest() throws Exception {
        processBeforeTest("Test Post Telemetry device proto payload", "Test Post Telemetry gateway proto payload", TransportPayloadType.PROTOBUF, POST_DATA_TELEMETRY_TOPIC, null);
    }

    @After
    public void afterTest() throws Exception {
        processAfterTest();
    }

    @Test
    @Ignore
    public void testPushMqttTelemetry() throws Exception {
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        TransportProtos.TsKvListProto tsKvListProto = getTsKvListProto(expectedKeys, 0);
        processTelemetryTest(POST_DATA_TELEMETRY_TOPIC, expectedKeys, tsKvListProto.toByteArray(), false);
    }

    @Test
    @Ignore
    public void testPushMqttTelemetryWithTs() throws Exception {
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        TransportProtos.TsKvListProto tsKvListProto = getTsKvListProto(expectedKeys, 10000);
        processTelemetryTest(POST_DATA_TELEMETRY_TOPIC, expectedKeys, tsKvListProto.toByteArray(), true);
    }

    @Test
    @Ignore
    public void testPushMqttTelemetryGateway() throws Exception {
        TransportApiProtos.GatewayTelemetryMsg.Builder gatewayTelemetryMsgProtoBuilder = TransportApiProtos.GatewayTelemetryMsg.newBuilder();
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        String deviceName1 = "Device A";
        String deviceName2 = "Device B";
        TransportApiProtos.TelemetryMsg deviceATelemetryMsgProto = getDeviceTelemetryMsgProto(deviceName1, expectedKeys, 10000, 20000);
        TransportApiProtos.TelemetryMsg deviceBTelemetryMsgProto = getDeviceTelemetryMsgProto(deviceName2, expectedKeys, 10000, 20000);
        gatewayTelemetryMsgProtoBuilder.addAllMsg(Arrays.asList(deviceATelemetryMsgProto, deviceBTelemetryMsgProto));
        TransportApiProtos.GatewayTelemetryMsg gatewayTelemetryMsg = gatewayTelemetryMsgProtoBuilder.build();
        processGatewayTelemetryTest(MqttTopics.GATEWAY_TELEMETRY_TOPIC, expectedKeys, gatewayTelemetryMsg.toByteArray(), deviceName1, deviceName2);
    }

    @Test
    @Ignore
    public void testGatewayConnect() throws Exception {
        String deviceName = "Device A";
        TransportApiProtos.ConnectMsg connectMsgProto = getConnectProto(deviceName);
        MqttAsyncClient client = getMqttAsyncClient(gatewayAccessToken);
        publishMqttMsg(client, connectMsgProto.toByteArray(), MqttTopics.GATEWAY_CONNECT_TOPIC);

        Thread.sleep(2000);

        Device device = doGet("/api/tenant/devices?deviceName=" + deviceName, Device.class);
        assertNotNull(device);
    }

    private TransportApiProtos.ConnectMsg getConnectProto(String deviceName) {
        TransportApiProtos.ConnectMsg.Builder builder = TransportApiProtos.ConnectMsg.newBuilder();
        builder.setDeviceName(deviceName);
        builder.setDeviceType(TransportPayloadType.PROTOBUF.name());
        return builder.build();
    }

    private TransportApiProtos.TelemetryMsg getDeviceTelemetryMsgProto(String deviceName, List<String> expectedKeys, long firstTs, long secondTs) {
        TransportApiProtos.TelemetryMsg.Builder deviceTelemetryMsgBuilder = TransportApiProtos.TelemetryMsg.newBuilder();
        TransportProtos.TsKvListProto tsKvListProto1 = getTsKvListProto(expectedKeys, firstTs);
        TransportProtos.TsKvListProto tsKvListProto2 = getTsKvListProto(expectedKeys, secondTs);
        TransportProtos.PostTelemetryMsg.Builder msg = TransportProtos.PostTelemetryMsg.newBuilder();
        msg.addAllTsKvList(Arrays.asList(tsKvListProto1, tsKvListProto2));
        deviceTelemetryMsgBuilder.setDeviceName(deviceName);
        deviceTelemetryMsgBuilder.setMsg(msg);
        return deviceTelemetryMsgBuilder.build();
    }

    private TransportProtos.TsKvListProto getTsKvListProto(List<String> expectedKeys, long ts) {
        List<TransportProtos.KeyValueProto> kvProtos = getKvProtos(expectedKeys);
        TransportProtos.TsKvListProto.Builder builder = TransportProtos.TsKvListProto.newBuilder();
        builder.addAllKv(kvProtos);
        builder.setTs(ts);
        return builder.build();
    }
}

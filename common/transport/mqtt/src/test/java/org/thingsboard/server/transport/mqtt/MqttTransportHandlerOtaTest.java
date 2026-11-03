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
package org.thingsboard.server.transport.mqtt;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.ssl.SslHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.JsonTransportPayloadConfiguration;
import org.thingsboard.server.common.data.device.profile.MqttDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.gen.transport.TransportProtos.GetOtaPackageRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.SessionInfoProto;
import org.thingsboard.server.transport.mqtt.adaptors.JsonMqttAdaptor;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MqttTransportHandlerOtaTest {

    static final InetSocketAddress IP_ADDR = new InetSocketAddress("127.0.0.1", 9876);

    @Mock
    MqttTransportContext context;
    @Mock
    SslHandler sslHandler;
    @Mock
    ChannelHandlerContext ctx;
    @Spy
    TransportService transportService;

    AtomicInteger packedId = new AtomicInteger();
    MqttTransportHandler handler;

    @BeforeEach
    public void setUp() {
        lenient().doReturn(10).when(context).getMessageQueueSizePerDeviceLimit();
        lenient().doReturn(transportService).when(context).getTransportService();

        handler = spy(new MqttTransportHandler(context, sslHandler));
        lenient().doReturn(IP_ADDR).when(handler).getAddress(any());

        when(context.getJsonMqttAdaptor()).thenReturn(new JsonMqttAdaptor());

        DeviceProfile deviceProfile = new DeviceProfile();
        DeviceProfileData deviceProfileData = new DeviceProfileData();
        MqttDeviceProfileTransportConfiguration mqttConfig = new MqttDeviceProfileTransportConfiguration();
        mqttConfig.setTransportPayloadTypeConfiguration(new JsonTransportPayloadConfiguration());
        deviceProfileData.setTransportConfiguration(mqttConfig);
        deviceProfile.setProfileData(deviceProfileData);
        deviceProfile.setTransportType(DeviceTransportType.MQTT);

        handler.deviceSessionCtx.setDeviceProfile(deviceProfile);
        handler.deviceSessionCtx.setConnected(true);
        handler.deviceSessionCtx.setChannel(ctx);
        handler.deviceSessionCtx.setSessionInfo(SessionInfoProto.newBuilder().build());

        doNothing().when(transportService).process(any(SessionInfoProto.class), any(GetOtaPackageRequestMsg.class), any());
    }

    @Test
    public void givenFwChunkRequestWithChunkSizeAboveMaxPayloadSize_whenProcessRegularSessionMsg_thenNoDisconnect() {
        // chunkSize > 65536 must NOT disconnect the client
        handler.processRegularSessionMsg(ctx, otaRequest("v2/fw/request/1/chunk/0", "100000"));

        verify(ctx, never()).close();
    }

    @Test
    public void givenFwChunkRequestWithChunkSizeWithinBounds_whenProcessRegularSessionMsg_thenNoDisconnect() {
        handler.processRegularSessionMsg(ctx, otaRequest("v2/fw/request/1/chunk/0", "32768"));

        verify(ctx, never()).close();
        verify(transportService, times(1)).process(any(SessionInfoProto.class), any(GetOtaPackageRequestMsg.class), any());
    }

    @Test
    public void givenFwChunkRequestWithEmptyPayload_whenProcessRegularSessionMsg_thenNoDisconnect() {
        // empty payload → chunkSize=0 → server sends the full firmware in one response
        handler.processRegularSessionMsg(ctx, otaRequest("v2/fw/request/1/chunk/0", null));

        verify(ctx, never()).close();
        verify(transportService, times(1)).process(any(SessionInfoProto.class), any(GetOtaPackageRequestMsg.class), any());
    }

    @Test
    public void givenFwChunkRequestsWithChunkSizeOnFirstAndEmptyOnSubsequent_whenProcessRegularSessionMsg_thenNoDisconnect() {
        // chunkSize is stored from the first request and reused for subsequent empty-payload requests
        handler.processRegularSessionMsg(ctx, otaRequest("v2/fw/request/1/chunk/0", "100000"));
        handler.processRegularSessionMsg(ctx, otaRequest("v2/fw/request/1/chunk/1", null));

        verify(ctx, never()).close();
        verify(transportService, times(2)).process(any(SessionInfoProto.class), any(GetOtaPackageRequestMsg.class), any());
    }

    @Test
    public void givenSwChunkRequestWithChunkSizeAboveMaxPayloadSize_whenProcessRegularSessionMsg_thenNoDisconnect() {
        // same fix applies to software OTA (SW) requests
        handler.processRegularSessionMsg(ctx, otaRequest("v2/sw/request/1/chunk/0", "100000"));

        verify(ctx, never()).close();
        verify(transportService, times(1)).process(any(SessionInfoProto.class), any(GetOtaPackageRequestMsg.class), any());
    }

    private MqttPublishMessage otaRequest(String topic, String chunkSizePayload) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.AT_MOST_ONCE, false, 0);
        MqttPublishVariableHeader varHeader = new MqttPublishVariableHeader(topic, packedId.incrementAndGet());
        ByteBuf payload = chunkSizePayload == null
                ? Unpooled.EMPTY_BUFFER
                : Unpooled.wrappedBuffer(chunkSizePayload.getBytes(StandardCharsets.UTF_8));
        return new MqttPublishMessage(fixedHeader, varHeader, payload);
    }
}

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
package org.thingsboard.server.mqtt.attributes.updates;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.device.profile.MqttTopics;
import org.thingsboard.server.gen.transport.TransportApiProtos;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@Slf4j
public abstract class AbstractMqttAttributesUpdatesProtoIntegrationTest extends AbstractMqttAttributesUpdatesIntegrationTest {

    @Before
    public void beforeTest() throws Exception {
        processBeforeTest("Test Subscribe to attribute updates", "Gateway Test Subscribe to attribute updates", TransportPayloadType.PROTOBUF);
    }

    @After
    public void afterTest() throws Exception {
        processAfterTest();
    }

    @Test
    public void testSubscribeToAttributesUpdatesFromTheServer() throws Exception {
        processTestSubscribeToAttributesUpdates();
    }

    @Test
    public void testSubscribeToAttributesUpdatesFromTheServerGateway() throws Exception {
        processGatewayTestSubscribeToAttributesUpdates();
    }

    protected void validateUpdateAttributesResponse(TestMqttCallback callback) throws InvalidProtocolBufferException {
        assertNotNull(callback.getPayloadBytes());
        TransportProtos.AttributeUpdateNotificationMsg.Builder attributeUpdateNotificationMsgBuilder = TransportProtos.AttributeUpdateNotificationMsg.newBuilder();
        List<TransportProtos.TsKvProto> tsKvProtoList = getTsKvProtoList();
        attributeUpdateNotificationMsgBuilder.addAllSharedUpdated(tsKvProtoList);

        TransportProtos.AttributeUpdateNotificationMsg expectedAttributeUpdateNotificationMsg = attributeUpdateNotificationMsgBuilder.build();
        TransportProtos.AttributeUpdateNotificationMsg actualAttributeUpdateNotificationMsg = TransportProtos.AttributeUpdateNotificationMsg.parseFrom(callback.getPayloadBytes());

        List<TransportProtos.KeyValueProto> actualSharedUpdatedList = actualAttributeUpdateNotificationMsg.getSharedUpdatedList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());
        List<TransportProtos.KeyValueProto> expectedSharedUpdatedList = expectedAttributeUpdateNotificationMsg.getSharedUpdatedList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());

        assertEquals(expectedSharedUpdatedList.size(), actualSharedUpdatedList.size());
        assertTrue(actualSharedUpdatedList.containsAll(expectedSharedUpdatedList));

    }

    protected void validateDeleteAttributesResponse(TestMqttCallback callback) throws InvalidProtocolBufferException {
        assertNotNull(callback.getPayloadBytes());
        TransportProtos.AttributeUpdateNotificationMsg.Builder attributeUpdateNotificationMsgBuilder = TransportProtos.AttributeUpdateNotificationMsg.newBuilder();
        attributeUpdateNotificationMsgBuilder.addSharedDeleted("attribute5");

        TransportProtos.AttributeUpdateNotificationMsg expectedAttributeUpdateNotificationMsg = attributeUpdateNotificationMsgBuilder.build();
        TransportProtos.AttributeUpdateNotificationMsg actualAttributeUpdateNotificationMsg = TransportProtos.AttributeUpdateNotificationMsg.parseFrom(callback.getPayloadBytes());

        assertEquals(expectedAttributeUpdateNotificationMsg.getSharedDeletedList().size(), actualAttributeUpdateNotificationMsg.getSharedDeletedList().size());
        assertEquals("attribute5", actualAttributeUpdateNotificationMsg.getSharedDeletedList().get(0));

    }

    protected void validateGatewayUpdateAttributesResponse(TestMqttCallback callback) throws InvalidProtocolBufferException {
        assertNotNull(callback.getPayloadBytes());

        TransportProtos.AttributeUpdateNotificationMsg.Builder attributeUpdateNotificationMsgBuilder = TransportProtos.AttributeUpdateNotificationMsg.newBuilder();
        List<TransportProtos.TsKvProto> tsKvProtoList = getTsKvProtoList();
        attributeUpdateNotificationMsgBuilder.addAllSharedUpdated(tsKvProtoList);
        TransportProtos.AttributeUpdateNotificationMsg expectedAttributeUpdateNotificationMsg = attributeUpdateNotificationMsgBuilder.build();

        TransportApiProtos.GatewayAttributeUpdateNotificationMsg.Builder gatewayAttributeUpdateNotificationMsgBuilder = TransportApiProtos.GatewayAttributeUpdateNotificationMsg.newBuilder();
        gatewayAttributeUpdateNotificationMsgBuilder.setDeviceName("Gateway Device Subscribe to attribute updates");
        gatewayAttributeUpdateNotificationMsgBuilder.setNotificationMsg(expectedAttributeUpdateNotificationMsg);

        TransportApiProtos.GatewayAttributeUpdateNotificationMsg expectedGatewayAttributeUpdateNotificationMsg = gatewayAttributeUpdateNotificationMsgBuilder.build();
        TransportApiProtos.GatewayAttributeUpdateNotificationMsg actualGatewayAttributeUpdateNotificationMsg = TransportApiProtos.GatewayAttributeUpdateNotificationMsg.parseFrom(callback.getPayloadBytes());

        assertEquals(expectedGatewayAttributeUpdateNotificationMsg.getDeviceName(), actualGatewayAttributeUpdateNotificationMsg.getDeviceName());

        List<TransportProtos.KeyValueProto> actualSharedUpdatedList = actualGatewayAttributeUpdateNotificationMsg.getNotificationMsg().getSharedUpdatedList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());
        List<TransportProtos.KeyValueProto> expectedSharedUpdatedList = expectedGatewayAttributeUpdateNotificationMsg.getNotificationMsg().getSharedUpdatedList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());

        assertEquals(expectedSharedUpdatedList.size(), actualSharedUpdatedList.size());
        assertTrue(actualSharedUpdatedList.containsAll(expectedSharedUpdatedList));

    }

    protected void validateGatewayDeleteAttributesResponse(TestMqttCallback callback) throws InvalidProtocolBufferException {
        assertNotNull(callback.getPayloadBytes());
        TransportProtos.AttributeUpdateNotificationMsg.Builder attributeUpdateNotificationMsgBuilder = TransportProtos.AttributeUpdateNotificationMsg.newBuilder();
        attributeUpdateNotificationMsgBuilder.addSharedDeleted("attribute5");
        TransportProtos.AttributeUpdateNotificationMsg attributeUpdateNotificationMsg = attributeUpdateNotificationMsgBuilder.build();

        TransportApiProtos.GatewayAttributeUpdateNotificationMsg.Builder gatewayAttributeUpdateNotificationMsgBuilder = TransportApiProtos.GatewayAttributeUpdateNotificationMsg.newBuilder();
        gatewayAttributeUpdateNotificationMsgBuilder.setDeviceName("Gateway Device Subscribe to attribute updates");
        gatewayAttributeUpdateNotificationMsgBuilder.setNotificationMsg(attributeUpdateNotificationMsg);

        TransportApiProtos.GatewayAttributeUpdateNotificationMsg expectedGatewayAttributeUpdateNotificationMsg = gatewayAttributeUpdateNotificationMsgBuilder.build();
        TransportApiProtos.GatewayAttributeUpdateNotificationMsg actualGatewayAttributeUpdateNotificationMsg = TransportApiProtos.GatewayAttributeUpdateNotificationMsg.parseFrom(callback.getPayloadBytes());

        assertEquals(expectedGatewayAttributeUpdateNotificationMsg.getDeviceName(), actualGatewayAttributeUpdateNotificationMsg.getDeviceName());

        TransportProtos.AttributeUpdateNotificationMsg expectedAttributeUpdateNotificationMsg = expectedGatewayAttributeUpdateNotificationMsg.getNotificationMsg();
        TransportProtos.AttributeUpdateNotificationMsg actualAttributeUpdateNotificationMsg = actualGatewayAttributeUpdateNotificationMsg.getNotificationMsg();

        assertEquals(expectedAttributeUpdateNotificationMsg.getSharedDeletedList().size(), actualAttributeUpdateNotificationMsg.getSharedDeletedList().size());
        assertEquals("attribute5", actualAttributeUpdateNotificationMsg.getSharedDeletedList().get(0));

    }

    protected byte[] getConnectPayloadBytes() {
        TransportApiProtos.ConnectMsg connectProto = getConnectProto();
        return connectProto.toByteArray();
    }

    private TransportApiProtos.ConnectMsg getConnectProto() {
        TransportApiProtos.ConnectMsg.Builder builder = TransportApiProtos.ConnectMsg.newBuilder();
        builder.setDeviceName("Gateway Device Subscribe to attribute updates");
        return builder.build();
    }

}

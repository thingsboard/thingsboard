/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.rule.engine.geo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.relation.*;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.relation.RelationService;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class TbGpsMultiGeofencingActionNodeTest {

    private TbGpsMultiGeofencingActionNode node;
    private TbGpsMultiGeofencingActionNodeConfiguration nodeConfiguration;
    private TbContext ctx;
    private AttributesService attributesService;
    private DeviceId deviceId;
    private AssetId zoneId;
    private String geofenceStateAttributeKey;

    @BeforeEach
    public void setUp() throws TbNodeException {
        node = new TbGpsMultiGeofencingActionNode();
        ctx = mock(TbContext.class);
        when(ctx.getSelfId()).thenReturn(new RuleNodeId(UUID.randomUUID()));
        geofenceStateAttributeKey = "geofenceState_" + ctx.getSelfId();

        TbGpsMultiGeofencingActionNodeConfiguration configuration = new TbGpsMultiGeofencingActionNodeConfiguration();
        configuration.setPerimeterAttributeKey("perimeter");
        configuration.setLatitudeKeyName("latitude");
        configuration.setLongitudeKeyName("longitude");
        configuration.setRelationField(EntitySearchDirection.TO);
        configuration.setInsideDurationMs(10L);
        configuration.setOutsideDurationMs(10L);
        EntityRelationsQuery entityRelationsQuery = new EntityRelationsQuery();
        entityRelationsQuery.setParameters(new RelationsSearchParameters(new DeviceId(UUID.randomUUID()), EntitySearchDirection.FROM, 1, true));
        entityRelationsQuery.setFilters(Collections.emptyList());
        configuration.setEntityRelationsQuery(entityRelationsQuery);

        nodeConfiguration = configuration;

        JsonNode jsonNode = JacksonUtil.valueToTree(configuration);
        TbNodeConfiguration tbNodeConfiguration = new TbNodeConfiguration(jsonNode);

        node.init(ctx, tbNodeConfiguration);

        AttributesService attributesService = mock(AttributesService.class);
        this.attributesService = attributesService;
        RelationService relationService = mock(RelationService.class);
        DeviceService deviceService = mock(DeviceService.class);

        when(ctx.getAttributesService()).thenReturn(attributesService);
        when(ctx.getRelationService()).thenReturn(relationService);
        when(ctx.getDeviceService()).thenReturn(deviceService);

        deviceId = (DeviceId) EntityIdFactory.getByTypeAndUuid(EntityType.DEVICE, UUID.randomUUID().toString());
        zoneId = (AssetId) EntityIdFactory.getByTypeAndUuid(EntityType.ASSET, UUID.randomUUID().toString());

        EntityRelation entityRelation = new EntityRelation(deviceId, zoneId, "DeviceToZone", RelationTypeGroup.COMMON);
        when(relationService.findByQuery(any(), any())).thenReturn(Futures.immediateFuture(List.of(entityRelation)));

        BaseAttributeKvEntry perimeterAttribute = new BaseAttributeKvEntry(new JsonDataEntry(configuration.getPerimeterAttributeKey(), "{\n" +
                "  \"perimeterType\": \"CIRCLE\",\n" +
                "  \"centerLatitude\": 48.8566,\n" +
                "  \"centerLongitude\": 2.3522,\n" +
                "  \"range\": 5,\n" +
                "  \"rangeUnit\": \"KILOMETER\"\n" +
                "}"), System.currentTimeMillis());
        when(attributesService.find(any(), any(), any(AttributeScope.class), eq(configuration.getPerimeterAttributeKey()))).thenReturn(Futures.immediateFuture(Optional.of(perimeterAttribute)));

        when(attributesService.find(any(), any(), any(AttributeScope.class), eq(geofenceStateAttributeKey))).thenReturn(Futures.immediateFuture(Optional.empty()));

        Device device = new Device(deviceId);
        device.setName("container");
        when(deviceService.findDeviceById(any(), any())).thenReturn(device);
    }

    @Test
    public void testEnteredEvent() throws Exception {
        TbMsg msg = createTbMsgWithCoordinates(deviceId, 48.8566, 2.3522);

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<String> labelCaptor = ArgumentCaptor.forClass(String.class);

        node.onMsg(ctx, msg);

        verify(ctx, atLeastOnce()).tellNext(msgCaptor.capture(), labelCaptor.capture());

        assertCapturedEvent(msgCaptor, labelCaptor, "Entered");
    }

    @Test
    public void testInsideEvent() throws Exception {
        TbMsg msgEntered = createTbMsgWithCoordinates(deviceId, 48.8566, 2.3522);

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<String> labelCaptor = ArgumentCaptor.forClass(String.class);

        node.onMsg(ctx, msgEntered);

        BaseAttributeKvEntry attributeKvEntry = captureStateAttribute();
        when(attributesService.find(any(), any(), any(AttributeScope.class), eq(geofenceStateAttributeKey))).thenReturn(Futures.immediateFuture(Optional.of(attributeKvEntry)));

        Thread.sleep(10L);

        TbMsg msgInside = createTbMsgWithCoordinates(deviceId, 48.8566, 2.3522);

        node.onMsg(ctx, msgInside);

        verify(ctx, atLeastOnce()).tellNext(msgCaptor.capture(), labelCaptor.capture());
        assertCapturedEvent(msgCaptor, labelCaptor, "Inside");
    }

    @Test
    public void testLeftEvent() throws Exception {
        TbMsg msgInside = createTbMsgWithCoordinates(deviceId, 48.8566, 2.3522);
        TbMsg msgOutside = createTbMsgWithCoordinates(deviceId, 40.7128, -74.0060);

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<String> labelCaptor = ArgumentCaptor.forClass(String.class);

        node.onMsg(ctx, msgInside);

        BaseAttributeKvEntry attributeKvEntry = captureStateAttribute();
        when(attributesService.find(any(), any(), any(AttributeScope.class), eq(geofenceStateAttributeKey))).thenReturn(Futures.immediateFuture(Optional.of(attributeKvEntry)));

        node.onMsg(ctx, msgOutside);

        verify(ctx, atLeastOnce()).tellNext(msgCaptor.capture(), labelCaptor.capture());
        assertCapturedEvent(msgCaptor, labelCaptor, "Left");
    }

    @Test
    public void testOutsideEvent() throws Exception {
        TbMsg msgInside = createTbMsgWithCoordinates(deviceId, 48.8566, 2.3522);

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<String> labelCaptor = ArgumentCaptor.forClass(String.class);

        node.onMsg(ctx, msgInside);

        BaseAttributeKvEntry attributeKvEntry = captureStateAttribute();
        when(attributesService.find(any(), any(), any(AttributeScope.class), eq(geofenceStateAttributeKey))).thenReturn(Futures.immediateFuture(Optional.of(attributeKvEntry)));

        TbMsg msgLeft = createTbMsgWithCoordinates(deviceId, 40.7128, -74.0060);

        node.onMsg(ctx, msgLeft);

        attributeKvEntry = captureStateAttribute();
        when(attributesService.find(any(), any(), any(AttributeScope.class), eq(geofenceStateAttributeKey))).thenReturn(Futures.immediateFuture(Optional.of(attributeKvEntry)));

        Thread.sleep(10L);

        TbMsg msgOutside = createTbMsgWithCoordinates(deviceId, 40.7128, -74.0060);

        node.onMsg(ctx, msgOutside);

        verify(ctx, atLeastOnce()).tellNext(msgCaptor.capture(), labelCaptor.capture());
        assertCapturedEvent(msgCaptor, labelCaptor, "Outside");
    }

    private BaseAttributeKvEntry captureStateAttribute() {
        ArgumentCaptor<AttributeKvEntry> attributeCaptor = ArgumentCaptor.forClass(AttributeKvEntry.class);
        verify(ctx.getAttributesService(), atLeastOnce()).save(eq(ctx.getTenantId()), eq(deviceId), eq(AttributeScope.SERVER_SCOPE), attributeCaptor.capture());
        return (BaseAttributeKvEntry) attributeCaptor.getValue();
    }

    private TbMsg createTbMsgWithCoordinates(DeviceId deviceId, double latitude, double longitude) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode jsonNode = objectMapper.createObjectNode();
        jsonNode.put("latitude", latitude);
        jsonNode.put("longitude", longitude);

        String data = objectMapper.writeValueAsString(jsonNode);
        TbMsgMetaData metaData = new TbMsgMetaData();
        return TbMsg.newMsg(TbMsgType.NA, deviceId, metaData, data);
    }

    private void assertCapturedEvent(ArgumentCaptor<TbMsg> msgCaptor, ArgumentCaptor<String> relationTypeCaptor, String expectedRelationType) {
        assertEquals(expectedRelationType, relationTypeCaptor.getValue());
        assertEquals(zoneId, msgCaptor.getValue().getOriginator());
        assertEquals("container", msgCaptor.getValue().getMetaData().getValue("originatorName"));
    }

}
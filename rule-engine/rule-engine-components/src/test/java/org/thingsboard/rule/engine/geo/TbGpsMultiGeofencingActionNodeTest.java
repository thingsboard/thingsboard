/**
 * Copyright © 2016-2024 The Thingsboard Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
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
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.TestDbCallbackExecutor;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.data.RelationsQuery;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationEntityTypeFilter;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.relation.RelationService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TbGpsMultiGeofencingActionNodeTest {

    private static final String PERIMETER_DEFINITION_JSON = """
            {
              "perimeterType": "CIRCLE",
              "centerLatitude": 48.8566,
              "centerLongitude": 2.3522,
              "range": 5,
              "rangeUnit": "KILOMETER"
            }
            """;
    private static final RuleNodeId RULE_NODE_ID = new RuleNodeId(RuleNodeId.NULL_UUID);
    private static final String ASSET_GEOFENCE_STATE_KEY_ATTR = "geofenceState_" + RULE_NODE_ID;
    private static final DeviceId DEVICE_ID = new DeviceId(DeviceId.NULL_UUID);
    private static final AssetId ZONE_ID = new AssetId(AssetId.NULL_UUID);
    private static final ListeningExecutor DB_EXECUTOR = new TestDbCallbackExecutor();
    private static final int VERIFY_TIMEOUT_MS = 5000;
    private static final int MIN_INSIDE_DURATION_MS = 500;
    private static final int MIN_OUTSIDE_DURATION_MS = 500;

    private TbGpsMultiGeofencingActionNode node;
    private TbContext ctx;
    private AttributesService attributesService;

    @BeforeEach
    public void setUp() throws TbNodeException {
        node = new TbGpsMultiGeofencingActionNode();
        ctx = mock(TbContext.class);
        when(ctx.getSelfId()).thenReturn(RULE_NODE_ID);

        TbGpsMultiGeofencingActionNodeConfiguration geoConfig = createGeoConfig(createRelationsQuery());

        node.init(ctx, createNodeConfiguration(geoConfig));

        AttributesService attributesService = mock(AttributesService.class);
        this.attributesService = attributesService;
        RelationService relationService = mock(RelationService.class);

        when(ctx.getAttributesService()).thenReturn(attributesService);
        when(ctx.getRelationService()).thenReturn(relationService);

        when(ctx.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);

        EntityRelation entityRelation = new EntityRelation(DEVICE_ID, ZONE_ID, "DeviceToZone", RelationTypeGroup.COMMON);
        when(relationService.findByQuery(any(), any())).thenReturn(Futures.immediateFuture(List.of(entityRelation)));

        BaseAttributeKvEntry perimeterAttribute = new BaseAttributeKvEntry(new JsonDataEntry(geoConfig.getPerimeterKeyName(), PERIMETER_DEFINITION_JSON), System.currentTimeMillis());
        when(attributesService.find(any(), any(), any(AttributeScope.class), eq(geoConfig.getPerimeterKeyName()))).thenReturn(Futures.immediateFuture(Optional.of(perimeterAttribute)));
        when(attributesService.find(any(), any(), any(AttributeScope.class), eq(ASSET_GEOFENCE_STATE_KEY_ATTR))).thenReturn(Futures.immediateFuture(Optional.empty()));
    }

    @Test
    public void testEnteredEvent() throws Exception {
        TbMsg msg = createTbMsgWithCoordinates(DEVICE_ID, 48.8566, 2.3522);

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<String> labelCaptor = ArgumentCaptor.forClass(String.class);

        node.onMsg(ctx, msg);

        verify(ctx, timeout(VERIFY_TIMEOUT_MS).times(1)).enqueueForTellNext(msgCaptor.capture(), labelCaptor.capture());

        assertCapturedEvent(msgCaptor, labelCaptor, "Entered");
    }

    @Test
    public void testInsideEvent() throws Exception {
        TbMsg msgEntered = createTbMsgWithCoordinates(DEVICE_ID, 48.8566, 2.3522);

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<String> labelCaptor = ArgumentCaptor.forClass(String.class);

        node.onMsg(ctx, msgEntered);

        verify(ctx, timeout(VERIFY_TIMEOUT_MS).times(1)).enqueueForTellNext(msgCaptor.capture(), labelCaptor.capture());

        BaseAttributeKvEntry attributeKvEntry = captureStateAttribute();
        when(attributesService.find(any(), any(), any(AttributeScope.class), eq(ASSET_GEOFENCE_STATE_KEY_ATTR))).thenReturn(Futures.immediateFuture(Optional.of(attributeKvEntry)));

        sleep(MIN_INSIDE_DURATION_MS);

        TbMsg msgInside = createTbMsgWithCoordinates(DEVICE_ID, 48.8566, 2.3522);

        node.onMsg(ctx, msgInside);

        verify(ctx, timeout(VERIFY_TIMEOUT_MS).times(2)).enqueueForTellNext(msgCaptor.capture(), labelCaptor.capture());

        assertCapturedEvent(msgCaptor, labelCaptor, "Inside");
    }

    @Test
    public void testLeftEvent() throws Exception {
        TbMsg msgEntered = createTbMsgWithCoordinates(DEVICE_ID, 48.8566, 2.3522);
        TbMsg msgLeft = createTbMsgWithCoordinates(DEVICE_ID, 40.7128, -74.0060);

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<String> labelCaptor = ArgumentCaptor.forClass(String.class);

        node.onMsg(ctx, msgEntered);

        verify(ctx, timeout(VERIFY_TIMEOUT_MS).times(1)).enqueueForTellNext(msgCaptor.capture(), labelCaptor.capture());

        BaseAttributeKvEntry attributeKvEntry = captureStateAttribute();
        when(attributesService.find(any(), any(), any(AttributeScope.class), eq(ASSET_GEOFENCE_STATE_KEY_ATTR))).thenReturn(Futures.immediateFuture(Optional.of(attributeKvEntry)));

        node.onMsg(ctx, msgLeft);

        verify(ctx, timeout(VERIFY_TIMEOUT_MS).times(2)).enqueueForTellNext(msgCaptor.capture(), labelCaptor.capture());

        assertCapturedEvent(msgCaptor, labelCaptor, "Left");
    }

    @Test
    public void testOutsideEvent() throws Exception {
        TbMsg msgInside = createTbMsgWithCoordinates(DEVICE_ID, 48.8566, 2.3522);

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<String> labelCaptor = ArgumentCaptor.forClass(String.class);

        node.onMsg(ctx, msgInside);

        verify(ctx, timeout(VERIFY_TIMEOUT_MS).times(1)).enqueueForTellNext(msgCaptor.capture(), labelCaptor.capture());

        BaseAttributeKvEntry attributeKvEntry = captureStateAttribute();
        when(attributesService.find(any(), any(), any(AttributeScope.class), eq(ASSET_GEOFENCE_STATE_KEY_ATTR))).thenReturn(Futures.immediateFuture(Optional.of(attributeKvEntry)));

        TbMsg msgLeft = createTbMsgWithCoordinates(DEVICE_ID, 40.7128, -74.0060);

        node.onMsg(ctx, msgLeft);

        verify(ctx, timeout(VERIFY_TIMEOUT_MS).times(2)).enqueueForTellNext(msgCaptor.capture(), labelCaptor.capture());

        sleep(MIN_OUTSIDE_DURATION_MS);

        attributeKvEntry = captureStateAttribute();
        when(attributesService.find(any(), any(), any(AttributeScope.class), eq(ASSET_GEOFENCE_STATE_KEY_ATTR))).thenReturn(Futures.immediateFuture(Optional.of(attributeKvEntry)));

        TbMsg msgOutside = createTbMsgWithCoordinates(DEVICE_ID, 40.7128, -74.0060);

        node.onMsg(ctx, msgOutside);

        verify(ctx, timeout(VERIFY_TIMEOUT_MS).times(3)).enqueueForTellNext(msgCaptor.capture(), labelCaptor.capture());

        assertCapturedEvent(msgCaptor, labelCaptor, "Outside");
    }

    private BaseAttributeKvEntry captureStateAttribute() {
        ArgumentCaptor<AttributeKvEntry> attributeCaptor = ArgumentCaptor.forClass(AttributeKvEntry.class);
        verify(ctx.getAttributesService(), atLeastOnce()).save(eq(ctx.getTenantId()), eq(DEVICE_ID), eq(AttributeScope.SERVER_SCOPE), attributeCaptor.capture());
        return (BaseAttributeKvEntry) attributeCaptor.getValue();
    }

    private TbMsg createTbMsgWithCoordinates(DeviceId deviceId, double latitude, double longitude) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode jsonNode = objectMapper.createObjectNode();
        jsonNode.put("latitude", latitude);
        jsonNode.put("longitude", longitude);

        String data = objectMapper.writeValueAsString(jsonNode);
        TbMsgMetaData metaData = new TbMsgMetaData();
        TbMsg tbMsg = TbMsg.newMsg(TbMsgType.NA, deviceId, metaData, data);
        return tbMsg;
    }

    private void assertCapturedEvent(ArgumentCaptor<TbMsg> msgCaptor, ArgumentCaptor<String> relationTypeCaptor, String expectedRelationType) {
        assertEquals(expectedRelationType, relationTypeCaptor.getValue());
        assertEquals(ZONE_ID, msgCaptor.getValue().getOriginator());
        assertEquals(DEVICE_ID, new DeviceId(UUID.fromString(msgCaptor.getValue().getMetaData().getValue("originatorId"))));
    }

    private TbNodeConfiguration createNodeConfiguration(TbGpsMultiGeofencingActionNodeConfiguration geoConfig) {
        JsonNode jsonNode = JacksonUtil.valueToTree(geoConfig);
        return new TbNodeConfiguration(jsonNode);
    }

    private RelationsQuery createRelationsQuery() {
        RelationsQuery relationsQuery = new RelationsQuery();
        relationsQuery.setDirection(EntitySearchDirection.FROM);
        relationsQuery.setMaxLevel(1);
        RelationEntityTypeFilter filter = new RelationEntityTypeFilter("DeviceToZone", List.of(EntityType.ASSET));
        relationsQuery.setFilters(List.of(filter));
        return relationsQuery;
    }

    private TbGpsMultiGeofencingActionNodeConfiguration createGeoConfig(RelationsQuery relationsQuery) {
        TbGpsMultiGeofencingActionNodeConfiguration geoConfig = new TbGpsMultiGeofencingActionNodeConfiguration();
        geoConfig.setPerimeterKeyName("perimeter");
        geoConfig.setLatitudeKeyName("latitude");
        geoConfig.setLongitudeKeyName("longitude");
        geoConfig.setMinInsideDuration(MIN_INSIDE_DURATION_MS);
        geoConfig.setMinOutsideDuration(MIN_OUTSIDE_DURATION_MS);
        geoConfig.setMinInsideDurationTimeUnit(TimeUnit.MILLISECONDS.name());
        geoConfig.setMinOutsideDurationTimeUnit(TimeUnit.MILLISECONDS.name());
        geoConfig.setRelationsQuery(relationsQuery);
        return geoConfig;
    }

}

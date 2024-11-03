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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TbGpsMultiGeofencingActionNodeTest {

    private static final String PERIMETER_DEFINITION_JSON_1 = """
            {
              "perimeterType": "CIRCLE",
              "centerLatitude": 48.8566,
              "centerLongitude": 2.3522,
              "range": 5,
              "rangeUnit": "KILOMETER"
            }
            """;
    private static final String PERIMETER_DEFINITION_JSON_2 = """
            {
              "perimeterType": "CIRCLE",
              "centerLatitude": 48.8510,
              "centerLongitude": 2.3470,
              "range": 5,
              "rangeUnit": "KILOMETER"
            }
            """;

    private static final RuleNodeId RULE_NODE_ID = new RuleNodeId(RuleNodeId.NULL_UUID);
    private static final String ASSET_GEOFENCE_STATE_KEY_ATTR = "geofenceState_" + RULE_NODE_ID;
    private static final DeviceId DEVICE_ID = new DeviceId(DeviceId.NULL_UUID);
    private static final AssetId ZONE_ID_1 = new AssetId(UUID.randomUUID());
    private static final AssetId ZONE_ID_2 = new AssetId(UUID.randomUUID());

    private static final ListeningExecutor DB_EXECUTOR = new TestDbCallbackExecutor();
    private static final int VERIFY_TIMEOUT_MS = 5000;
    private static final int MIN_INSIDE_DURATION_MS = 500;
    private static final int MIN_OUTSIDE_DURATION_MS = 500;
    private static final long MIN_INSIDE_DURATION_METADATA_CONFIG_MS = 3000L;
    private static final long MIN_OUTSIDE_DURATION_METADATA_CONFIG_MS = 3000L;

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

        EntityRelation toZoneRelation1 = new EntityRelation(DEVICE_ID, ZONE_ID_1, "DeviceToZone", RelationTypeGroup.COMMON);
        EntityRelation toZoneRelation2 = new EntityRelation(DEVICE_ID, ZONE_ID_2, "DeviceToZone", RelationTypeGroup.COMMON);
        when(relationService.findByQuery(any(), any())).thenReturn(Futures.immediateFuture(List.of(toZoneRelation1, toZoneRelation2)));

        BaseAttributeKvEntry perimeterAttribute1 = new BaseAttributeKvEntry(new JsonDataEntry(geoConfig.getPerimeterKeyName(), PERIMETER_DEFINITION_JSON_1), System.currentTimeMillis());
        BaseAttributeKvEntry perimeterAttribute2 = new BaseAttributeKvEntry(new JsonDataEntry(geoConfig.getPerimeterKeyName(), PERIMETER_DEFINITION_JSON_2), System.currentTimeMillis());
        when(attributesService.find(any(), eq(ZONE_ID_1), any(AttributeScope.class), eq(geoConfig.getPerimeterKeyName()))).thenReturn(Futures.immediateFuture(Optional.of(perimeterAttribute1)));
        when(attributesService.find(any(), eq(ZONE_ID_2), any(AttributeScope.class), eq(geoConfig.getPerimeterKeyName()))).thenReturn(Futures.immediateFuture(Optional.of(perimeterAttribute2)));
        when(attributesService.find(any(), any(), any(AttributeScope.class), eq(ASSET_GEOFENCE_STATE_KEY_ATTR))).thenReturn(Futures.immediateFuture(Optional.empty()));
        when(attributesService.save(any(), any(), any(AttributeScope.class), any(AttributeKvEntry.class))).thenReturn(Futures.immediateFuture("test"));
        when(attributesService.save(any(), any(), any(AttributeScope.class), anyList())).thenReturn(Futures.immediateFuture(List.of("test")));
    }

    @Test
    public void testGeofenceEvents() throws Exception {
        double latInside = 48.8566;
        double longInside = 2.3522;
        double latOutside = 40.7128;
        double longOutside = -74.0060;

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<String> connectionCaptor = ArgumentCaptor.forClass(String.class);

        TbMsg insideMsg = createTbMsgWithCoordinates(latInside, longInside);

        testEvents("Entered", insideMsg, msgCaptor, connectionCaptor);

        msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        connectionCaptor = ArgumentCaptor.forClass(String.class);
        clearInvocations(ctx);

        sleep(MIN_INSIDE_DURATION_MS);
        insideMsg = createTbMsgWithCoordinates(latInside, longInside);

        testEvents("Inside", insideMsg, msgCaptor, connectionCaptor);

        msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        connectionCaptor = ArgumentCaptor.forClass(String.class);
        clearInvocations(ctx);

        TbMsg outsideMsg = createTbMsgWithCoordinates(latOutside, longOutside);

        testEvents("Left", outsideMsg, msgCaptor, connectionCaptor);

        msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        connectionCaptor = ArgumentCaptor.forClass(String.class);
        clearInvocations(ctx);

        sleep(MIN_OUTSIDE_DURATION_MS);
        outsideMsg = createTbMsgWithCoordinates(latOutside, longOutside);

        testEvents("Outside", outsideMsg, msgCaptor, connectionCaptor);
    }

    @Test
    public void testGeofenceEventsWithDurationConfig() throws Exception {
        double latInside = 48.8566;
        double longInside = 2.3522;
        double latOutside = 40.7128;
        double longOutside = -74.0060;

        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("durationConfig", JacksonUtil.toString(createGeofenceDurationConfig().getGeofenceDurationMap()));

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<String> connectionCaptor = ArgumentCaptor.forClass(String.class);

        TbMsg insideMsg = createTbMsgWithCoordinates(latInside, longInside, metaData);

        testEvents("Entered", insideMsg, msgCaptor, connectionCaptor);

        msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        connectionCaptor = ArgumentCaptor.forClass(String.class);
        clearInvocations(ctx);

        sleep(MIN_INSIDE_DURATION_METADATA_CONFIG_MS);
        insideMsg = createTbMsgWithCoordinates(latInside, longInside, metaData);

        testEvents("Inside", insideMsg, msgCaptor, connectionCaptor);

        msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        connectionCaptor = ArgumentCaptor.forClass(String.class);
        clearInvocations(ctx);

        TbMsg outsideMsg = createTbMsgWithCoordinates(latOutside, longOutside, metaData);

        testEvents("Left", outsideMsg, msgCaptor, connectionCaptor);

        msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        connectionCaptor = ArgumentCaptor.forClass(String.class);
        clearInvocations(ctx);

        sleep(MIN_OUTSIDE_DURATION_METADATA_CONFIG_MS);
        outsideMsg = createTbMsgWithCoordinates(latOutside, longOutside, metaData);

        testEvents("Outside", outsideMsg, msgCaptor, connectionCaptor);
    }

    @Test
    public void testNoEventsWhenOutsideZone() throws Exception {
        TbMsg msg = createTbMsgWithCoordinates(20.8566, 21.3522);

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<String> connectionCaptor = ArgumentCaptor.forClass(String.class);

        node.onMsg(ctx, msg);

        verify(ctx, timeout(VERIFY_TIMEOUT_MS).times(0))
                .enqueueForTellNext(msgCaptor.capture(), connectionCaptor.capture(), any(), any());

        verify(ctx, timeout(VERIFY_TIMEOUT_MS).times(1)).tellSuccess(msg);
    }

    private GeofenceDurationConfig createGeofenceDurationConfig() {
        Map<UUID, GeofenceDuration> durationMap = new HashMap<>();
        GeofenceDuration duration = new GeofenceDuration();
        duration.setMinInsideDuration(MIN_INSIDE_DURATION_METADATA_CONFIG_MS);
        duration.setMinOutsideDuration(MIN_OUTSIDE_DURATION_METADATA_CONFIG_MS);
        durationMap.put(ZONE_ID_1.getId(), duration);
        durationMap.put(ZONE_ID_2.getId(), duration);
        return new GeofenceDurationConfig(durationMap);
    }

    private void testEvents(String expectedConnectionType, TbMsg msg, ArgumentCaptor<TbMsg> msgCaptor, ArgumentCaptor<String> connectionCaptor) throws Exception {
        node.onMsg(ctx, msg);

        verify(ctx, timeout(VERIFY_TIMEOUT_MS).times(2))
                .enqueueForTellNext(msgCaptor.capture(), connectionCaptor.capture(), any(), any());

        assertEquals(Arrays.asList(expectedConnectionType, expectedConnectionType), connectionCaptor.getAllValues());

        for (AssetId zoneId : Arrays.asList(ZONE_ID_1, ZONE_ID_2)) {
            assertMessageForZone(msgCaptor.getAllValues(), zoneId);
        }

        updateStateAttribute();
    }

    private void updateStateAttribute() {
        BaseAttributeKvEntry attributeKvEntry = captureStateAttribute();
        when(attributesService.find(any(), any(), any(AttributeScope.class), eq(ASSET_GEOFENCE_STATE_KEY_ATTR)))
                .thenReturn(Futures.immediateFuture(Optional.of(attributeKvEntry)));
    }

    private void assertMessageForZone(List<TbMsg> messages, AssetId zoneId) {
        TbMsg zoneMsg = messages.stream()
                .filter(msg -> msg.getOriginator().equals(zoneId))
                .findFirst()
                .orElse(null);
        assertNotNull(zoneMsg, "Message for zone ID " + zoneId + " should not be null");
        assertEquals(DEVICE_ID.toString(), zoneMsg.getMetaData().getValue("originatorId"));
    }

    private BaseAttributeKvEntry captureStateAttribute() {
        ArgumentCaptor<AttributeKvEntry> attributeCaptor = ArgumentCaptor.forClass(AttributeKvEntry.class);
        verify(ctx.getAttributesService(), atLeastOnce()).save(eq(ctx.getTenantId()), eq(DEVICE_ID), eq(AttributeScope.SERVER_SCOPE), attributeCaptor.capture());
        return (BaseAttributeKvEntry) attributeCaptor.getValue();
    }

    private TbMsg createTbMsgWithCoordinates(double latitude, double longitude) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode jsonNode = objectMapper.createObjectNode();
        jsonNode.put("latitude", latitude);
        jsonNode.put("longitude", longitude);

        String data = objectMapper.writeValueAsString(jsonNode);
        TbMsgMetaData metaData = new TbMsgMetaData();
        return TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, metaData, data);
    }

    private TbMsg createTbMsgWithCoordinates(double latitude, double longitude, TbMsgMetaData metaData) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode jsonNode = objectMapper.createObjectNode();
        jsonNode.put("latitude", latitude);
        jsonNode.put("longitude", longitude);

        String data = objectMapper.writeValueAsString(jsonNode);
        return TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, metaData, data);
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
        geoConfig.setMetadataDurationConfigKey("durationConfig");
        return geoConfig;
    }

}

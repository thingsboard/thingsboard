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
package org.thingsboard.rule.engine.geo;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.geo.Coordinates;
import org.thingsboard.common.util.geo.PerimeterType;
import org.thingsboard.common.util.geo.RangeUnit;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class TbGpsGeofencingFilterNodeTest {

    private static final double CIRCLE_RANGE = 1.0;
    private static final Coordinates CIRCLE_CENTER = new Coordinates(49.0384, 31.4513);
    private static final Coordinates POINT_INSIDE_CIRCLE = new Coordinates(49.0354, 31.4513); // distance from center: 0.334 km
    private static final Coordinates POINT_OUTSIDE_CIRCLE = new Coordinates(49.0284, 31.4513); // distance from center: 1.112 km

    private TbContext ctx;
    private TbGpsGeofencingFilterNode node;

    @BeforeEach
    void setUp() {
        ctx = mock(TbContext.class);
        node = new TbGpsGeofencingFilterNode();
    }

    @AfterEach
    void tearDown() {
        node.destroy();
    }

    // Exception tests

    @Test
    void givenDefaultConfig_whenOnMsg_thenExceptionInvalidMsg() throws TbNodeException {
        // GIVEN
        var config = new TbGpsGeofencingFilterNodeConfiguration().defaultConfiguration();
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        TbMsg msg = getEmptyArrayTbMsg(deviceId);

        // WHEN
        var exception = assertThrows(TbNodeException.class, () -> node.onMsg(ctx, msg));

        // THEN
        assertThat(exception.getMessage()).isEqualTo("Incoming Message is not a valid JSON object!");
    }

    @Test
    void givenDefaultConfig_whenOnMsg_thenExceptionMissingPerimeterDefinitionNewVersion() throws TbNodeException {
        // GIVEN
        var config = new TbGpsGeofencingFilterNodeConfiguration().defaultConfiguration();
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        TbMsg msg = getTbMsg(deviceId, TbMsgMetaData.EMPTY,
                GeoUtilTest.POINT_INSIDE_SIMPLE_RECT_CENTER.getLatitude(), GeoUtilTest.POINT_INSIDE_SIMPLE_RECT_CENTER.getLongitude());

        // WHEN
        var exception = assertThrows(TbNodeException.class, () -> node.onMsg(ctx, msg));

        // THEN
        assertThat(exception.getMessage()).isEqualTo("Missing perimeter definition!");
    }

    @Test
    void givenTypePolygonAndConfigWithoutPerimeterKeyName_whenOnMsg_thenExceptionMissingPerimeterDefinitionOldVersion() throws TbNodeException {
        // GIVEN
        var config = new TbGpsGeofencingFilterNodeConfiguration().defaultConfiguration();
        config.setPerimeterKeyName(null);
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        TbMsg msg = getTbMsg(deviceId, TbMsgMetaData.EMPTY,
                GeoUtilTest.POINT_INSIDE_SIMPLE_RECT_CENTER.getLatitude(), GeoUtilTest.POINT_INSIDE_SIMPLE_RECT_CENTER.getLongitude());

        // WHEN
        var exception = assertThrows(TbNodeException.class, () -> node.onMsg(ctx, msg));

        // THEN
        assertThat(exception.getMessage()).isEqualTo("Missing perimeter definition!");
    }

    // Polygon tests

    @Test
    void givenTypePolygonAndConfigWithoutPerimeterKeyName_whenOnMsg_thenTrue() throws TbNodeException {
        // GIVEN
        var config = new TbGpsGeofencingFilterNodeConfiguration().defaultConfiguration();
        config.setPerimeterKeyName(null);
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        TbMsgMetaData metadata = getMetadataForOldVersionPolygonPerimeter();
        TbMsg msg = getTbMsg(deviceId, metadata,
                GeoUtilTest.POINT_INSIDE_SIMPLE_RECT_CENTER.getLatitude(), GeoUtilTest.POINT_INSIDE_SIMPLE_RECT_CENTER.getLongitude());

        // WHEN
        node.onMsg(ctx, msg);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.TRUE));
        verify(ctx, never()).tellFailure(any(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(msg);
    }

    @Test
    void givenTypePolygonAndConfigWithoutPerimeterKeyName_whenOnMsg_thenFalse() throws TbNodeException {
        // GIVEN
        var config = new TbGpsGeofencingFilterNodeConfiguration().defaultConfiguration();
        config.setPerimeterKeyName(null);
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        TbMsgMetaData metadata = getMetadataForOldVersionPolygonPerimeter();
        TbMsg msg = getTbMsg(deviceId, metadata,
                GeoUtilTest.POINT_OUTSIDE_SIMPLE_RECT.getLatitude(), GeoUtilTest.POINT_OUTSIDE_SIMPLE_RECT.getLongitude());

        // WHEN
        node.onMsg(ctx, msg);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.FALSE));
        verify(ctx, never()).tellFailure(any(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(msg);
    }

    @Test
    void givenDefaultConfig_whenOnMsg_thenTrue() throws TbNodeException {
        // GIVEN
        var config = new TbGpsGeofencingFilterNodeConfiguration().defaultConfiguration();
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        TbMsgMetaData metadata = getMetadataForNewVersionPolygonPerimeter();
        TbMsg msg = getTbMsg(deviceId, metadata,
                GeoUtilTest.POINT_INSIDE_SIMPLE_RECT_CENTER.getLatitude(), GeoUtilTest.POINT_INSIDE_SIMPLE_RECT_CENTER.getLongitude());

        // WHEN
        node.onMsg(ctx, msg);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.TRUE));
        verify(ctx, never()).tellFailure(any(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(msg);
    }

    @Test
    void givenDefaultConfig_whenOnMsg_thenFalse() throws TbNodeException {
        // GIVEN
        var config = new TbGpsGeofencingFilterNodeConfiguration().defaultConfiguration();
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        TbMsgMetaData metadata = getMetadataForNewVersionPolygonPerimeter();
        TbMsg msg = getTbMsg(deviceId, metadata,
                GeoUtilTest.POINT_OUTSIDE_SIMPLE_RECT.getLatitude(), GeoUtilTest.POINT_OUTSIDE_SIMPLE_RECT.getLongitude());

        // WHEN
        node.onMsg(ctx, msg);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.FALSE));
        verify(ctx, never()).tellFailure(any(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(msg);
    }

    @Test
    void givenTypePolygonAndConfigWithPolygonDefined_whenOnMsg_thenTrue() throws TbNodeException {
        // GIVEN
        var config = new TbGpsGeofencingFilterNodeConfiguration().defaultConfiguration();
        config.setFetchPerimeterInfoFromMessageMetadata(false);
        config.setPolygonsDefinition(GeoUtilTest.SIMPLE_RECT);
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        TbMsg msg = getTbMsg(deviceId, TbMsgMetaData.EMPTY,
                GeoUtilTest.POINT_INSIDE_SIMPLE_RECT_CENTER.getLatitude(), GeoUtilTest.POINT_INSIDE_SIMPLE_RECT_CENTER.getLongitude());

        // WHEN
        node.onMsg(ctx, msg);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.TRUE));
        verify(ctx, never()).tellFailure(any(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(msg);
    }

    @Test
    void givenTypePolygonAndConfigWithPolygonDefined_whenOnMsg_thenFalse() throws TbNodeException {
        // GIVEN
        var config = new TbGpsGeofencingFilterNodeConfiguration().defaultConfiguration();
        config.setFetchPerimeterInfoFromMessageMetadata(false);
        config.setPolygonsDefinition(GeoUtilTest.SIMPLE_RECT);
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        TbMsg msg = getTbMsg(deviceId, TbMsgMetaData.EMPTY,
                GeoUtilTest.POINT_OUTSIDE_SIMPLE_RECT.getLatitude(), GeoUtilTest.POINT_OUTSIDE_SIMPLE_RECT.getLongitude());

        // WHEN
        node.onMsg(ctx, msg);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.FALSE));
        verify(ctx, never()).tellFailure(any(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(msg);
    }

    private TbMsgMetaData getMetadataForOldVersionPolygonPerimeter() {
        var metadata = new TbMsgMetaData();
        metadata.putValue("perimeter", GeoUtilTest.SIMPLE_RECT);
        return metadata;
    }

    private TbMsgMetaData getMetadataForNewVersionPolygonPerimeter() {
        var metadata = new TbMsgMetaData();
        metadata.putValue("ss_perimeter", GeoUtilTest.SIMPLE_RECT);
        return metadata;
    }

    // Circle tests

    @Test
    void givenTypeCircleAndConfigWithoutPerimeterKeyName_whenOnMsg_thenTrue() throws TbNodeException {
        // GIVEN
        var config = new TbGpsGeofencingFilterNodeConfiguration().defaultConfiguration();
        config.setPerimeterKeyName(null);
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        TbMsgMetaData metadata = getMetadataForOldVersionCirclePerimeter();
        TbMsg msg = getTbMsg(deviceId, metadata,
                POINT_INSIDE_CIRCLE.getLatitude(), POINT_INSIDE_CIRCLE.getLongitude());

        // WHEN
        node.onMsg(ctx, msg);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.TRUE));
        verify(ctx, never()).tellFailure(any(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(msg);
    }

    @Test
    void givenTypeCircleAndConfigWithoutPerimeterKeyName_whenOnMsg_thenFalse() throws TbNodeException {
        // GIVEN
        var config = new TbGpsGeofencingFilterNodeConfiguration().defaultConfiguration();
        config.setPerimeterKeyName(null);
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        TbMsgMetaData metadata = getMetadataForOldVersionCirclePerimeter();
        TbMsg msg = getTbMsg(deviceId, metadata,
                POINT_OUTSIDE_CIRCLE.getLatitude(), POINT_OUTSIDE_CIRCLE.getLongitude());

        // WHEN
        node.onMsg(ctx, msg);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.FALSE));
        verify(ctx, never()).tellFailure(any(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(msg);
    }

    @Test
    void givenTypeCircle_whenOnMsg_thenTrue() throws TbNodeException {
        // GIVEN
        var config = new TbGpsGeofencingFilterNodeConfiguration().defaultConfiguration();
        config.setPerimeterType(PerimeterType.CIRCLE);
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        TbMsgMetaData metadata = getMetadataForNewVersionCirclePerimeter();
        TbMsg msg = getTbMsg(deviceId, metadata,
                POINT_INSIDE_CIRCLE.getLatitude(), POINT_INSIDE_CIRCLE.getLongitude());

        // WHEN
        node.onMsg(ctx, msg);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.TRUE));
        verify(ctx, never()).tellFailure(any(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(msg);
    }

    @Test
    void givenTypeCircle_whenOnMsg_thenFalse() throws TbNodeException {
        // GIVEN
        var config = new TbGpsGeofencingFilterNodeConfiguration().defaultConfiguration();
        config.setPerimeterType(PerimeterType.CIRCLE);
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        TbMsgMetaData metadata = getMetadataForNewVersionCirclePerimeter();
        TbMsg msg = getTbMsg(deviceId, metadata,
                POINT_OUTSIDE_CIRCLE.getLatitude(), POINT_OUTSIDE_CIRCLE.getLongitude());

        // WHEN
        node.onMsg(ctx, msg);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.FALSE));
        verify(ctx, never()).tellFailure(any(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(msg);
    }

    @Test
    void givenTypeCircleAndConfigWithCircleDefined_whenOnMsg_thenTrue() throws TbNodeException {
        // GIVEN
        var config = new TbGpsGeofencingFilterNodeConfiguration().defaultConfiguration();
        config.setFetchPerimeterInfoFromMessageMetadata(false);
        config.setPerimeterType(PerimeterType.CIRCLE);
        config.setCenterLatitude(CIRCLE_CENTER.getLatitude());
        config.setCenterLongitude(CIRCLE_CENTER.getLongitude());
        config.setRange(CIRCLE_RANGE);
        config.setRangeUnit(RangeUnit.KILOMETER);
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        TbMsg msg = getTbMsg(deviceId, TbMsgMetaData.EMPTY,
                POINT_INSIDE_CIRCLE.getLatitude(), POINT_INSIDE_CIRCLE.getLongitude());

        // WHEN
        node.onMsg(ctx, msg);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.TRUE));
        verify(ctx, never()).tellFailure(any(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(msg);
    }

    @Test
    void givenTypeCircleAndConfigWithCircleDefined_whenOnMsg_thenFalse() throws TbNodeException {
        // GIVEN
        var config = new TbGpsGeofencingFilterNodeConfiguration().defaultConfiguration();
        config.setFetchPerimeterInfoFromMessageMetadata(false);
        config.setPerimeterType(PerimeterType.CIRCLE);
        config.setCenterLatitude(CIRCLE_CENTER.getLatitude());
        config.setCenterLongitude(CIRCLE_CENTER.getLongitude());
        config.setRange(CIRCLE_RANGE);
        config.setRangeUnit(RangeUnit.KILOMETER);
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        TbMsg msg = getTbMsg(deviceId, TbMsgMetaData.EMPTY,
                POINT_OUTSIDE_CIRCLE.getLatitude(), POINT_OUTSIDE_CIRCLE.getLongitude());

        // WHEN
        node.onMsg(ctx, msg);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.FALSE));
        verify(ctx, never()).tellFailure(any(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(msg);
    }

    private TbMsgMetaData getMetadataForOldVersionCirclePerimeter() {
        var metadata = new TbMsgMetaData();
        metadata.putValue("centerLatitude", String.valueOf(CIRCLE_CENTER.getLatitude()));
        metadata.putValue("centerLongitude", String.valueOf(CIRCLE_CENTER.getLongitude()));
        metadata.putValue("range", String.valueOf(CIRCLE_RANGE));
        metadata.putValue("rangeUnit", String.valueOf(RangeUnit.KILOMETER));
        return metadata;
    }

    private TbMsgMetaData getMetadataForNewVersionCirclePerimeter() {
        ObjectNode perimeter = JacksonUtil.newObjectNode();
        perimeter.put("latitude", CIRCLE_CENTER.getLatitude());
        perimeter.put("longitude", CIRCLE_CENTER.getLongitude());
        perimeter.put("radius", CIRCLE_RANGE);
        perimeter.put("radiusUnit", String.valueOf(RangeUnit.KILOMETER));
        var metadata = new TbMsgMetaData();
        metadata.putValue("ss_perimeter", JacksonUtil.toString(perimeter));
        return metadata;
    }

    private TbMsg getTbMsg(EntityId entityId, TbMsgMetaData metadata, double latitude, double longitude) {
        String data = "{\"latitude\": " + latitude + ", \"longitude\": " + longitude + "}";
        return TbMsg.newMsg()
                .type(TbMsgType.POST_ATTRIBUTES_REQUEST)
                .originator(entityId)
                .copyMetaData(metadata)
                .data(data)
                .build();
    }

    private TbMsg getEmptyArrayTbMsg(EntityId entityId) {
        return TbMsg.newMsg()
                .type(TbMsgType.POST_ATTRIBUTES_REQUEST)
                .originator(entityId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_ARRAY)
                .build();
    }

}

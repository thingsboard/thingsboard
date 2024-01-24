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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.attributes.AttributesService;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.thingsboard.rule.engine.util.GpsGeofencingEvents.ENTERED;
import static org.thingsboard.rule.engine.util.GpsGeofencingEvents.INSIDE;
import static org.thingsboard.rule.engine.util.GpsGeofencingEvents.LEFT;
import static org.thingsboard.rule.engine.util.GpsGeofencingEvents.OUTSIDE;
import static org.thingsboard.server.common.data.msg.TbNodeConnectionType.SUCCESS;

class TbGpsGeofencingActionNodeTest {
    private TbContext ctx;
    private TbGpsGeofencingActionNode node;
    private AttributesService attributesService;

    @BeforeEach
    void setUp() {
        ctx = mock(TbContext.class);
        attributesService = mock(AttributesService.class);
        node = new TbGpsGeofencingActionNode();
    }

    @AfterEach
    void tearDown() {
        node.destroy();
    }

    private static Stream<Arguments> givenReportPresenceStatusOnEachMessage_whenOnMsg_thenVerifyOutputMsgType() {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        long tsNow = System.currentTimeMillis();
        long tsNowMinusMinuteAndMillis = tsNow - Duration.ofMinutes(1).plusMillis(1).toMillis();
        return Stream.of(
                // default config with presenceMonitoringStrategyOnEachMessage false and msgInside true
                Arguments.of(new GpsGeofencingActionTestCase(deviceId, true, false, new EntityGeofencingState(false, 0, false)), ENTERED),
                Arguments.of(new GpsGeofencingActionTestCase(deviceId, true, false, new EntityGeofencingState(true, tsNow, false)), SUCCESS),
                Arguments.of(new GpsGeofencingActionTestCase(deviceId, true, false, new EntityGeofencingState(true, tsNowMinusMinuteAndMillis, false)), INSIDE),
                Arguments.of(new GpsGeofencingActionTestCase(deviceId, true, false, new EntityGeofencingState(true, tsNow, true)), SUCCESS),
                // default config with presenceMonitoringStrategyOnEachMessage false and msgInside false
                Arguments.of(new GpsGeofencingActionTestCase(deviceId, false, false, new EntityGeofencingState(false, 0, false)), LEFT),
                Arguments.of(new GpsGeofencingActionTestCase(deviceId, false, false, new EntityGeofencingState(false, tsNow, false)), SUCCESS),
                Arguments.of(new GpsGeofencingActionTestCase(deviceId, false, false, new EntityGeofencingState(false, tsNowMinusMinuteAndMillis, false)), OUTSIDE),
                Arguments.of(new GpsGeofencingActionTestCase(deviceId, false, false, new EntityGeofencingState(false, tsNow, true)), SUCCESS),
                // default config with presenceMonitoringStrategyOnEachMessage true and msgInside true
                Arguments.of(new GpsGeofencingActionTestCase(deviceId, true, true, new EntityGeofencingState(false, 0, false)), ENTERED),
                Arguments.of(new GpsGeofencingActionTestCase(deviceId, true, true, new EntityGeofencingState(true, tsNow, false)), INSIDE),
                Arguments.of(new GpsGeofencingActionTestCase(deviceId, true, true, new EntityGeofencingState(true, tsNowMinusMinuteAndMillis, false)), INSIDE),
                // default config with presenceMonitoringStrategyOnEachMessage true and msgInside false
                Arguments.of(new GpsGeofencingActionTestCase(deviceId, false, true, new EntityGeofencingState(false, 0, false)), LEFT),
                Arguments.of(new GpsGeofencingActionTestCase(deviceId, false, true, new EntityGeofencingState(false, tsNow, false)), OUTSIDE),
                Arguments.of(new GpsGeofencingActionTestCase(deviceId, false, true, new EntityGeofencingState(false, tsNowMinusMinuteAndMillis, false)), OUTSIDE)
        );
    }

    @ParameterizedTest
    @MethodSource
    void givenReportPresenceStatusOnEachMessage_whenOnMsg_thenVerifyOutputMsgType(
            GpsGeofencingActionTestCase gpsGeofencingActionTestCase,
            String expectedOutput
    ) throws TbNodeException {
        // GIVEN
        var config = new TbGpsGeofencingActionNodeConfiguration().defaultConfiguration();
        config.setReportPresenceStatusOnEachMessage(gpsGeofencingActionTestCase.isReportPresenceStatusOnEachMessage());

        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        TbMsg msg = gpsGeofencingActionTestCase.isMsgInside() ?
                getInsideRectangleTbMsg(gpsGeofencingActionTestCase.getEntityId()) :
                getOutsideRectangleTbMsg(gpsGeofencingActionTestCase.getEntityId());

        when(ctx.getAttributesService()).thenReturn(attributesService);
        when(ctx
                .getAttributesService()
                .find(ctx.getTenantId(), msg.getOriginator(), DataConstants.SERVER_SCOPE, ctx.getServiceId()))
                .thenReturn(Futures.immediateFuture(Optional.empty()));

        ReflectionTestUtils.setField(node, "entityStates", gpsGeofencingActionTestCase.getEntityStates());

        // WHEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        node.onMsg(ctx, msg);

        // THEN
        if (SUCCESS.equals(expectedOutput)) {
            verify(ctx, times(1)).tellSuccess(newMsgCaptor.capture());
        } else {
            verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(expectedOutput));
        }
    }

    private TbMsg getOutsideRectangleTbMsg(EntityId entityId) {
        return getTbMsg(entityId, getMetadataForNewVersionPolygonPerimeter(),
                GeoUtilTest.POINT_OUTSIDE_SIMPLE_RECT.getLatitude(),
                GeoUtilTest.POINT_OUTSIDE_SIMPLE_RECT.getLongitude());
    }

    private TbMsg getInsideRectangleTbMsg(EntityId entityId) {
        return getTbMsg(entityId, getMetadataForNewVersionPolygonPerimeter(),
                GeoUtilTest.POINT_INSIDE_SIMPLE_RECT_CENTER.getLatitude(),
                GeoUtilTest.POINT_INSIDE_SIMPLE_RECT_CENTER.getLongitude());
    }

    private TbMsg getTbMsg(EntityId entityId, TbMsgMetaData metadata, double latitude, double longitude) {
        String data = "{\"latitude\": " + latitude + ", \"longitude\": " + longitude + "}";
        return TbMsg.newMsg(TbMsgType.POST_ATTRIBUTES_REQUEST, entityId, metadata, data);
    }

    private TbMsgMetaData getMetadataForNewVersionPolygonPerimeter() {
        var metadata = new TbMsgMetaData();
        metadata.putValue("ss_perimeter", GeoUtilTest.SIMPLE_RECT);
        return metadata;
    }

    // Rule nodes upgrade
    private static Stream<Arguments> givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig() {
        return Stream.of(
                // default config for version 0
                Arguments.of(0,
                        "{\n" +
                                "  \"minInsideDuration\": 1,\n" +
                                "  \"minOutsideDuration\": 1,\n" +
                                "  \"minInsideDurationTimeUnit\": \"MINUTES\",\n" +
                                "  \"minOutsideDurationTimeUnit\": \"MINUTES\",\n" +
                                "  \"latitudeKeyName\": \"latitude\",\n" +
                                "  \"longitudeKeyName\": \"longitude\",\n" +
                                "  \"perimeterType\": \"POLYGON\",\n" +
                                "  \"fetchPerimeterInfoFromMessageMetadata\": true,\n" +
                                "  \"perimeterKeyName\": \"ss_perimeter\",\n" +
                                "  \"polygonsDefinition\": null,\n" +
                                "  \"centerLatitude\": null,\n" +
                                "  \"centerLongitude\": null,\n" +
                                "  \"range\": null,\n" +
                                "  \"rangeUnit\": null\n" +
                                "}\n",
                        true,
                        "{\n" +
                                "  \"minInsideDuration\": 1,\n" +
                                "  \"minOutsideDuration\": 1,\n" +
                                "  \"minInsideDurationTimeUnit\": \"MINUTES\",\n" +
                                "  \"minOutsideDurationTimeUnit\": \"MINUTES\",\n" +
                                "  \"reportPresenceStatusOnEachMessage\": false,\n" +
                                "  \"latitudeKeyName\": \"latitude\",\n" +
                                "  \"longitudeKeyName\": \"longitude\",\n" +
                                "  \"perimeterType\": \"POLYGON\",\n" +
                                "  \"fetchPerimeterInfoFromMessageMetadata\": true,\n" +
                                "  \"perimeterKeyName\": \"ss_perimeter\",\n" +
                                "  \"polygonsDefinition\": null,\n" +
                                "  \"centerLatitude\": null,\n" +
                                "  \"centerLongitude\": null,\n" +
                                "  \"range\": null,\n" +
                                "  \"rangeUnit\": null\n" +
                                "}\n"),
                // default config for version 1 with upgrade from version 0
                Arguments.of(0,
                        "{\n" +
                                "  \"minInsideDuration\": 1,\n" +
                                "  \"minOutsideDuration\": 1,\n" +
                                "  \"minInsideDurationTimeUnit\": \"MINUTES\",\n" +
                                "  \"minOutsideDurationTimeUnit\": \"MINUTES\",\n" +
                                "  \"reportPresenceStatusOnEachMessage\": false,\n" +
                                "  \"latitudeKeyName\": \"latitude\",\n" +
                                "  \"longitudeKeyName\": \"longitude\",\n" +
                                "  \"perimeterType\": \"POLYGON\",\n" +
                                "  \"fetchPerimeterInfoFromMessageMetadata\": true,\n" +
                                "  \"perimeterKeyName\": \"ss_perimeter\",\n" +
                                "  \"polygonsDefinition\": null,\n" +
                                "  \"centerLatitude\": null,\n" +
                                "  \"centerLongitude\": null,\n" +
                                "  \"range\": null,\n" +
                                "  \"rangeUnit\": null\n" +
                                "}\n",
                        false,
                        "{\n" +
                                "  \"minInsideDuration\": 1,\n" +
                                "  \"minOutsideDuration\": 1,\n" +
                                "  \"minInsideDurationTimeUnit\": \"MINUTES\",\n" +
                                "  \"minOutsideDurationTimeUnit\": \"MINUTES\",\n" +
                                "  \"reportPresenceStatusOnEachMessage\": false,\n" +
                                "  \"latitudeKeyName\": \"latitude\",\n" +
                                "  \"longitudeKeyName\": \"longitude\",\n" +
                                "  \"perimeterType\": \"POLYGON\",\n" +
                                "  \"fetchPerimeterInfoFromMessageMetadata\": true,\n" +
                                "  \"perimeterKeyName\": \"ss_perimeter\",\n" +
                                "  \"polygonsDefinition\": null,\n" +
                                "  \"centerLatitude\": null,\n" +
                                "  \"centerLongitude\": null,\n" +
                                "  \"range\": null,\n" +
                                "  \"rangeUnit\": null\n" +
                                "}\n")
        );
    }

    @ParameterizedTest
    @MethodSource
    void givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig(int givenVersion, String givenConfigStr, boolean hasChanges, String expectedConfigStr) throws TbNodeException {
        // GIVEN
        JsonNode givenConfig = JacksonUtil.toJsonNode(givenConfigStr);
        JsonNode expectedConfig = JacksonUtil.toJsonNode(expectedConfigStr);

        // WHEN
        TbPair<Boolean, JsonNode> upgradeResult = node.upgrade(givenVersion, givenConfig);

        // THEN
        assertThat(upgradeResult.getFirst()).isEqualTo(hasChanges);
        ObjectNode upgradedConfig = (ObjectNode) upgradeResult.getSecond();
        assertThat(upgradedConfig).isEqualTo(expectedConfig);
    }

}

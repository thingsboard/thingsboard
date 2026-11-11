// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.analytics.incoming;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.AbstractRuleNodeUpgradeTest;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;

import java.util.UUID;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

public class TbSimpleAggMsgNodeTest extends AbstractRuleNodeUpgradeTest {

    TbSimpleAggMsgNode node;
    TbSimpleAggMsgNodeConfiguration config;
    TbNodeConfiguration nodeConfiguration;
    TbContext ctx;
    TbMsgCallback callback;

    @BeforeEach
    public void setUp() throws TbNodeException {
        ctx = mock(TbContext.class);
        callback = mock(TbMsgCallback.class);

        config = new TbSimpleAggMsgNodeConfiguration().defaultConfiguration();
        config.setTimeZoneId("UTC");
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        node = spy(new TbSimpleAggMsgNode());

        doNothing().when(node).scheduleReportTickMsg(any(), any());

        node.init(ctx, nodeConfiguration);
    }

    @Test
    public void givenNullValue_whenOnMsg_thenThrowException() {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        TbMsgMetaData metaData = new TbMsgMetaData();

        JsonObject jsonObject = new JsonObject();
        jsonObject.add("temperature", null);

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(deviceId)
                .copyMetaData(metaData)
                .data(jsonObject.toString())
                .callback(callback)
                .build();

        Assertions.assertThrows(IllegalArgumentException.class, () -> node.onMsg(ctx, msg));
    }

    // Rule nodes upgrade

    public static final String EXPECTED_CONFIG = "{\"parentEntitiesQuery\":{\"type\":\"group\",\"entityGroupId\":null}," +
            "\"periodTimeUnit\":\"MINUTES\",\"periodValue\":5,\"mathFunction\":\"AVG\"," +
            "\"aggIntervalType\":\"HOUR\",\"timeZoneId\":\"UTC\",\"aggIntervalTimeUnit\":\"HOURS\"," +
            "\"aggIntervalValue\":1,\"autoCreateIntervals\":false,\"intervalPersistencePolicy\":\"ON_EACH_CHECK_AFTER_INTERVAL_END\"," +
            "\"intervalCheckTimeUnit\":\"MINUTES\",\"intervalCheckValue\":1,\"inputValueKey\":\"temperature\"," +
            "\"outputValueKey\":\"avgHourlyTemperature\",\"statePersistencePolicy\":\"ON_EACH_CHANGE\"," +
            "\"statePersistenceTimeUnit\":\"MINUTES\",\"statePersistenceValue\":1, \"outMsgType\": \"POST_TELEMETRY_REQUEST\"}";

    private static Stream<Arguments> givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig() {
        return Stream.of(
                // default config for version 0
                Arguments.of(0,
                        "{\"parentEntitiesQuery\":{\"type\":\"group\",\"entityGroupId\":null}," +
                                "\"periodTimeUnit\":\"MINUTES\",\"periodValue\":5,\"queueName\":null,\"mathFunction\":\"AVG\"," +
                                "\"aggIntervalType\":\"HOUR\",\"timeZoneId\":\"UTC\",\"aggIntervalTimeUnit\":\"HOURS\"," +
                                "\"aggIntervalValue\":1,\"autoCreateIntervals\":false,\"intervalPersistencePolicy\":\"ON_EACH_CHECK_AFTER_INTERVAL_END\"," +
                                "\"intervalCheckTimeUnit\":\"MINUTES\",\"intervalCheckValue\":1,\"inputValueKey\":\"temperature\"," +
                                "\"outputValueKey\":\"avgHourlyTemperature\",\"statePersistencePolicy\":\"ON_EACH_CHANGE\"," +
                                "\"statePersistenceTimeUnit\":\"MINUTES\",\"statePersistenceValue\":1}",
                        true,
                        EXPECTED_CONFIG),
                // default config for version 0 with queueName
                Arguments.of(0,
                        "{\"parentEntitiesQuery\":{\"type\":\"group\",\"entityGroupId\":null}," +
                                "\"periodTimeUnit\":\"MINUTES\",\"periodValue\":5,\"queueName\":\"Main\",\"mathFunction\":\"AVG\"," +
                                "\"aggIntervalType\":\"HOUR\",\"timeZoneId\":\"UTC\",\"aggIntervalTimeUnit\":\"HOURS\"," +
                                "\"aggIntervalValue\":1,\"autoCreateIntervals\":false,\"intervalPersistencePolicy\":\"ON_EACH_CHECK_AFTER_INTERVAL_END\"," +
                                "\"intervalCheckTimeUnit\":\"MINUTES\",\"intervalCheckValue\":1,\"inputValueKey\":\"temperature\"," +
                                "\"outputValueKey\":\"avgHourlyTemperature\",\"statePersistencePolicy\":\"ON_EACH_CHANGE\"," +
                                "\"statePersistenceTimeUnit\":\"MINUTES\",\"statePersistenceValue\":1}",
                        true,
                        EXPECTED_CONFIG),
                // default config for version 1 with upgrade from version 1
                Arguments.of(1,
                        "{\"parentEntitiesQuery\":{\"type\":\"group\",\"entityGroupId\":null}," +
                                "\"periodTimeUnit\":\"MINUTES\",\"periodValue\":5,\"queueName\":\"Main\",\"mathFunction\":\"AVG\"," +
                                "\"aggIntervalType\":\"HOUR\",\"timeZoneId\":\"UTC\",\"aggIntervalTimeUnit\":\"HOURS\"," +
                                "\"aggIntervalValue\":1,\"autoCreateIntervals\":false,\"intervalPersistencePolicy\":\"ON_EACH_CHECK_AFTER_INTERVAL_END\"," +
                                "\"intervalCheckTimeUnit\":\"MINUTES\",\"intervalCheckValue\":1,\"inputValueKey\":\"temperature\"," +
                                "\"outputValueKey\":\"avgHourlyTemperature\",\"statePersistencePolicy\":\"ON_EACH_CHANGE\"," +
                                "\"statePersistenceTimeUnit\":\"MINUTES\",\"statePersistenceValue\":1, \"outMsgType\": \"POST_TELEMETRY_REQUEST\"}",
                        true,
                        EXPECTED_CONFIG),
                // default config for version 2 with upgrade from version 0
                Arguments.of(0, EXPECTED_CONFIG, false, EXPECTED_CONFIG)
        );
    }

    @Override
    protected TbNode getTestNode() {
        return node;
    }
}

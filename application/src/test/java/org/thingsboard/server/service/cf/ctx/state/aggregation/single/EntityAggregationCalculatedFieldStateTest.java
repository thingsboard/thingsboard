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
package org.thingsboard.server.service.cf.ctx.state.aggregation.single;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.cf.configuration.TimeSeriesOutput;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggFunction;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggKeyInput;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggMetric;
import org.thingsboard.server.common.data.cf.configuration.aggregation.single.EntityAggregationCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.aggregation.single.interval.CustomInterval;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicKvEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;
import org.thingsboard.server.service.cf.ctx.state.SingleValueArgumentEntry;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EntityAggregationCalculatedFieldStateTest {

    private static final long INTERVAL_START_TS = 1_000L;
    private static final long INTERVAL_END_TS = 2_000L;

    private final TenantId TENANT_ID = TenantId.fromUUID(UUID.fromString("80ee80ef-019f-46b1-80ba-22f3ef1b094c"));
    private final DeviceId DEVICE_ID = new DeviceId(UUID.fromString("fc83d188-9cf5-4919-a774-d5c56bba2d27"));

    private EntityAggregationCalculatedFieldState state;
    private CalculatedFieldCtx ctx;

    @Mock
    private TenantProfile tenantProfile;
    @Mock
    private TbTenantProfileCache tenantProfileCache;
    @InjectMocks
    private ActorSystemContext systemContext;

    @BeforeEach
    void setUp() {
        when(tenantProfileCache.get(any(TenantId.class))).thenReturn(tenantProfile);
        when(tenantProfile.getProfileConfiguration()).thenReturn(Optional.of(new DefaultTenantProfileConfiguration()));

        ctx = new CalculatedFieldCtx(getCalculatedField(), systemContext);
        ctx.init();
        state = new EntityAggregationCalculatedFieldState(DEVICE_ID);
        state.setCtx(ctx, null);
        state.init(false);
    }

    @Test
    void testType() {
        assertThat(state.getType()).isEqualTo(CalculatedFieldType.ENTITY_AGGREGATION);
    }

    // A numeric aggregation result (SUM/AVG/COUNT/..., numeric MIN/MAX) must be serialized as a numeric
    // JSON node; a genuine string result (lexical MIN/MAX over string telemetry, e.g. a zero-padded code)
    // must stay a string node. The node type is asserted explicitly, so asText() is only used to verify the
    // value once the type is already pinned - it is not relied on to distinguish the types (that blindness
    // is what hid the bug).
    @ParameterizedTest(name = "{0} (precision {2}) -> numeric={3}")
    @MethodSource("toResultSerializationCases")
    void toResultSerializesResultWithTypePreservingNode(String metricName, BasicKvEntry kvEntry, Integer precision,
                                                        boolean expectNumeric, String expectedText) {
        JsonNode value = toResultValue(metricName, kvEntry, precision);

        assertThat(value.isNumber()).isEqualTo(expectNumeric);
        assertThat(value.isTextual()).isEqualTo(!expectNumeric);
        assertThat(value.asText()).isEqualTo(expectedText);
    }

    private static Stream<Arguments> toResultSerializationCases() {
        return Stream.of(
                // SUM: Number result, precision 0 -> whole-number (long) node
                arguments("consumption", new DoubleDataEntry("consumption", 400.0), 0, true, "400"),
                // AVG: Number result, precision 2 -> half-up rounded double node
                arguments("avgConsumption", new DoubleDataEntry("avgConsumption", 133.335), 2, true, "133.34"),
                // MAX over string telemetry: a zero-padded code is a genuine String result and must stay a
                // string node - as a number it would lose its padding ("0009" -> 9).
                arguments("maxCode", new StringDataEntry("maxCode", "0009"), 0, false, "0009")
        );
    }

    private JsonNode toResultValue(String metricName, BasicKvEntry kvEntry, Integer precision) {
        AggIntervalEntry interval = new AggIntervalEntry(INTERVAL_START_TS, INTERVAL_END_TS);
        ArgumentEntry argumentEntry = new SingleValueArgumentEntry(INTERVAL_START_TS, kvEntry, SingleValueArgumentEntry.DEFAULT_VERSION);
        Map<AggIntervalEntry, Map<String, ArgumentEntry>> results = new HashMap<>();
        results.put(interval, Map.of(metricName, argumentEntry));

        ArrayNode result = state.toResult(results, precision);

        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0).get("ts").asLong()).isEqualTo(INTERVAL_START_TS);
        return result.get(0).get("values").get(metricName);
    }

    private CalculatedField getCalculatedField() {
        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setTenantId(TENANT_ID);
        calculatedField.setEntityId(DEVICE_ID);
        calculatedField.setType(CalculatedFieldType.ENTITY_AGGREGATION);
        calculatedField.setName("Test Entity Aggregation CF");
        calculatedField.setConfigurationVersion(1);
        calculatedField.setConfiguration(getConfiguration());
        calculatedField.setVersion(1L);
        return calculatedField;
    }

    private EntityAggregationCalculatedFieldConfiguration getConfiguration() {
        EntityAggregationCalculatedFieldConfiguration configuration = new EntityAggregationCalculatedFieldConfiguration();

        Argument energy = new Argument();
        energy.setRefEntityKey(new ReferencedEntityKey("energy", ArgumentType.TS_LATEST, null));
        configuration.setArguments(Map.of("en", energy));

        Map<String, AggMetric> metrics = new HashMap<>();
        AggMetric consumption = new AggMetric();
        consumption.setFunction(AggFunction.SUM);
        consumption.setInput(new AggKeyInput("en"));
        metrics.put("consumption", consumption);

        AggMetric avgConsumption = new AggMetric();
        avgConsumption.setFunction(AggFunction.AVG);
        avgConsumption.setInput(new AggKeyInput("en"));
        metrics.put("avgConsumption", avgConsumption);

        AggMetric maxCode = new AggMetric();
        maxCode.setFunction(AggFunction.MAX);
        maxCode.setInput(new AggKeyInput("en"));
        metrics.put("maxCode", maxCode);
        configuration.setMetrics(metrics);

        configuration.setInterval(new CustomInterval("UTC", 0L, 5L));

        TimeSeriesOutput output = new TimeSeriesOutput();
        output.setDecimalsByDefault(0);
        configuration.setOutput(output);

        return configuration;
    }

}

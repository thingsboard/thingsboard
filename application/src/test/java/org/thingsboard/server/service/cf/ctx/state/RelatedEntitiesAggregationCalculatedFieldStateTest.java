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
package org.thingsboard.server.service.cf.ctx.state;

import com.fasterxml.jackson.databind.JsonNode;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.thingsboard.script.api.tbel.DefaultTbelInvokeService;
import org.thingsboard.script.api.tbel.TbelInvokeService;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.OutputType;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.cf.configuration.TimeSeriesOutput;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggFunction;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggFunctionInput;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggKeyInput;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggMetric;
import org.thingsboard.server.common.data.cf.configuration.aggregation.RelatedEntitiesAggregationCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationPathLevel;
import org.thingsboard.server.common.stats.DefaultStatsFactory;
import org.thingsboard.server.dao.usagerecord.ApiLimitService;
import org.thingsboard.server.service.cf.TelemetryCalculatedFieldResult;
import org.thingsboard.server.service.cf.ctx.state.aggregation.RelatedEntitiesAggregationCalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.aggregation.RelatedEntitiesArgumentEntry;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {SimpleMeterRegistry.class, DefaultStatsFactory.class, DefaultTbelInvokeService.class})
public class RelatedEntitiesAggregationCalculatedFieldStateTest {

    private final long ts = System.currentTimeMillis();

    private final TenantId tenantId = TenantId.fromUUID(UUID.fromString("cceba360-71e9-44ab-8596-c600d60ee0d0"));
    private final AssetProfileId assetProfileId = new AssetProfileId(UUID.fromString("ccced83b-e5f6-4978-bba0-c2df46de9a35"));
    private final AssetId assetId = new AssetId(UUID.fromString("982dfdee-b2bc-4f04-a49d-7ffd70940c69"));
    private final DeviceId device1 = new DeviceId(UUID.fromString("47f1fef5-a3b7-46e7-9732-b7669d3ef885"));
    private final DeviceId device2 = new DeviceId(UUID.fromString("3b097e5e-9eef-46f4-8997-937075e6f342"));

    private RelatedEntitiesAggregationCalculatedFieldState state;
    private CalculatedFieldCtx ctx;

    @Autowired
    private TbelInvokeService tbelInvokeService;

    @MockitoBean
    private ApiLimitService apiLimitService;

    @MockitoBean
    private ActorSystemContext actorSystemContext;

    @BeforeEach
    void setUp() {
        when(actorSystemContext.getTbelInvokeService()).thenReturn(tbelInvokeService);
        when(actorSystemContext.getApiLimitService()).thenReturn(apiLimitService);
        when(apiLimitService.getLimit(any(), any())).thenReturn(1000L);

        initCtxAndState();
    }

    void initCtxAndState() {
        ctx = new CalculatedFieldCtx(getCalculatedField(), actorSystemContext);
        ctx.init();

        state = new RelatedEntitiesAggregationCalculatedFieldState(assetId);
        state.setCtx(ctx, null);
        state.init(false);
    }

    @Test
    void testType() {
        assertThat(state.getType()).isEqualTo(CalculatedFieldType.RELATED_ENTITIES_AGGREGATION);
    }

    @Test
    void testInitAddsRequiredArgument() {
        assertThat(state.getRequiredArguments()).contains("oc");
    }

    @Test
    void testIsReadyWhenNoRelatedEntities() {
        assertThat(state.isReady()).isFalse();
        assertThat(state.getReadinessStatus().errorMsg())
                .isEqualTo("No entities found via 'Aggregation path to related entities'. Verify the configured relation type and direction.");
    }

    @Test
    void testUpdateArguments() {
        assertThat(state.getLastArgsRefreshTs()).isEqualTo(-1);

        state.update(arguments(), ctx);

        assertThat(state.getLastArgsRefreshTs()).isGreaterThan(-1);
    }

    @Test
    void testPerformCalculationWhenArgsUpdatedButIntervalDidNotPass() throws Exception {
        // deduplication interval 60 sec
        state.setLastMetricsEvalTs(ts - TimeUnit.SECONDS.toMillis(10));
        state.setLastArgsRefreshTs(ts - TimeUnit.SECONDS.toMillis(5));

        assertThat(state.performCalculation(arguments(), ctx).get()).isEqualTo(TelemetryCalculatedFieldResult.EMPTY);
    }

    @Test
    void testPerformCalculationWhenIntervalPassedButNoArgsUpdated() throws Exception {
        state.setLastMetricsEvalTs(ts - TimeUnit.SECONDS.toMillis(90));
        state.setLastArgsRefreshTs(ts - TimeUnit.SECONDS.toMillis(90));

        assertThat(state.performCalculation(arguments(), ctx).get()).isEqualTo(TelemetryCalculatedFieldResult.EMPTY);
    }

    @Test
    void testPerformCalculationWhenIntervalPassedAndArgsUpdated() throws Exception {
        state.setLastMetricsEvalTs(ts - TimeUnit.SECONDS.toMillis(70));
        state.setLastArgsRefreshTs(ts - TimeUnit.SECONDS.toMillis(10));

        Map<String, ArgumentEntry> arguments = arguments();
        state.update(arguments, ctx);

        TelemetryCalculatedFieldResult calculatedFieldResult = (TelemetryCalculatedFieldResult) state.performCalculation(arguments, ctx).get();
        assertThat(calculatedFieldResult).isNotNull();
        assertThat(calculatedFieldResult.isEmpty()).isFalse();
        assertThat(calculatedFieldResult.getType()).isEqualTo(OutputType.TIME_SERIES);

        JsonNode result = calculatedFieldResult.getResult();
        assertThat(result).isNotNull();
        safeAssert(result.get("ts"), String.valueOf(state.getLatestTimestamp()));
        JsonNode values = result.get("values");
        safeAssert(values.get("freeSpaces"), "1");
        safeAssert(values.get("occupiedSpaces"), "1");
        safeAssert(values.get("totalSpaces"), "2");
    }

    private void safeAssert(JsonNode node, String expected) {
        assertThat(node).isNotNull();
        assertThat(node.asText()).isEqualTo(expected);
    }

    private Map<String, ArgumentEntry> arguments() {
        Map<String, ArgumentEntry> arguments = new HashMap<>();
        Map<EntityId, ArgumentEntry> entityArguments = new HashMap<>();
        entityArguments.put(device1, new SingleValueArgumentEntry(ts - 100, new BooleanDataEntry("occupied", true), 23L));
        entityArguments.put(device2, new SingleValueArgumentEntry(ts - 80, new BooleanDataEntry("occupied", false), 26L));
        RelatedEntitiesArgumentEntry entry = new RelatedEntitiesArgumentEntry(entityArguments, false);
        arguments.put("oc", entry);
        return arguments;
    }

    private CalculatedField getCalculatedField() {
        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setTenantId(tenantId);
        calculatedField.setEntityId(assetProfileId);
        calculatedField.setType(CalculatedFieldType.RELATED_ENTITIES_AGGREGATION);
        calculatedField.setName("Test CF");
        calculatedField.setConfigurationVersion(1);

        var config = new RelatedEntitiesAggregationCalculatedFieldConfiguration();

        config.setRelation(new RelationPathLevel(EntitySearchDirection.FROM, EntityRelation.CONTAINS_TYPE));

        Argument oc = new Argument();
        ReferencedEntityKey refKey = new ReferencedEntityKey("occupied", ArgumentType.TS_LATEST, null);
        oc.setRefEntityKey(refKey);
        config.setArguments(Map.of("oc", oc));

        Map<String, AggMetric> aggMetrics = new HashMap<>();

        AggMetric freeSpaces = new AggMetric();
        freeSpaces.setFunction(AggFunction.COUNT);
        freeSpaces.setFilter("return oc == false;");
        freeSpaces.setInput(new AggKeyInput("oc"));
        aggMetrics.put("freeSpaces", freeSpaces);

        AggMetric occupiedSpaces = new AggMetric();
        occupiedSpaces.setFunction(AggFunction.COUNT);
        occupiedSpaces.setFilter("return oc == true;");
        occupiedSpaces.setInput(new AggKeyInput("oc"));
        aggMetrics.put("occupiedSpaces", occupiedSpaces);

        AggMetric totalSpaces = new AggMetric();
        totalSpaces.setFunction(AggFunction.COUNT);
        totalSpaces.setInput(new AggFunctionInput("return 1;"));
        aggMetrics.put("totalSpaces", totalSpaces);
        config.setMetrics(aggMetrics);

        config.setDeduplicationIntervalInSec(60);

        TimeSeriesOutput output = new TimeSeriesOutput();
        output.setDecimalsByDefault(0);
        config.setOutput(output);

        config.setUseLatestTs(true);

        calculatedField.setConfiguration(config);
        calculatedField.setVersion(1L);
        return calculatedField;
    }

}

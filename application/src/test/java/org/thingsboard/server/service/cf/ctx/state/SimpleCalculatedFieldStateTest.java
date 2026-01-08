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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.AttributesOutput;
import org.thingsboard.server.common.data.cf.configuration.CalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.cf.configuration.SimpleCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.dao.usagerecord.ApiLimitService;
import org.thingsboard.server.service.cf.TelemetryCalculatedFieldResult;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SimpleCalculatedFieldStateTest {

    private final TenantId TENANT_ID = TenantId.fromUUID(UUID.fromString("5b18e321-3327-4290-b996-d72a65e90382"));
    private final DeviceId DEVICE_ID = new DeviceId(UUID.fromString("5512071d-5abc-411d-a907-4cdb6539c2eb"));
    private final AssetId ASSET_ID = new AssetId(UUID.fromString("5bc010ae-bcfd-46c8-98b9-8ee8c8955a76"));

    private final SingleValueArgumentEntry key1ArgEntry = new SingleValueArgumentEntry(System.currentTimeMillis() - 10, new LongDataEntry("key1", 11L), 145L);
    private final SingleValueArgumentEntry key2ArgEntry = new SingleValueArgumentEntry(System.currentTimeMillis() - 6, new LongDataEntry("key2", 15L), 165L);
    private final SingleValueArgumentEntry key3ArgEntry = new SingleValueArgumentEntry(System.currentTimeMillis() - 3, new LongDataEntry("key3", 23L), 184L);

    private SimpleCalculatedFieldState state;
    private CalculatedFieldCtx ctx;

    @Mock
    private ApiLimitService apiLimitService;
    @InjectMocks
    private ActorSystemContext systemContext;

    @BeforeEach
    void setUp() {
        when(apiLimitService.getLimit(any(), any())).thenReturn(1000L);
        ctx = new CalculatedFieldCtx(getCalculatedField(), systemContext);
        ctx.init();
        state = new SimpleCalculatedFieldState(ctx.getEntityId());
        state.setCtx(ctx, null);
        state.init(false);
    }

    @Test
    void testType() {
        assertThat(state.getType()).isEqualTo(CalculatedFieldType.SIMPLE);
    }

    @Test
    void testUpdateState() {
        state.arguments = new HashMap<>(Map.of(
                "key1", key1ArgEntry,
                "key2", key2ArgEntry
        ));

        Map<String, ArgumentEntry> newArgs = Map.of("key3", key3ArgEntry);
        boolean stateUpdated = !state.update(newArgs, ctx).isEmpty();

        assertThat(stateUpdated).isTrue();
        assertThat(state.getArguments()).containsExactlyInAnyOrderEntriesOf(
                Map.of(
                        "key1", key1ArgEntry,
                        "key2", key2ArgEntry,
                        "key3", key3ArgEntry
                )
        );
    }

    @Test
    void testUpdateStateWhenUpdateExistingEntry() {
        state.arguments = new HashMap<>(Map.of("key1", key1ArgEntry));

        SingleValueArgumentEntry newArgEntry = new SingleValueArgumentEntry(System.currentTimeMillis(), new LongDataEntry("key1", 18L), 190L);
        Map<String, ArgumentEntry> newArgs = Map.of("key1", newArgEntry);
        boolean stateUpdated = !state.update(newArgs, ctx).isEmpty();

        assertThat(stateUpdated).isTrue();
        assertThat(state.getArguments()).containsExactlyInAnyOrderEntriesOf(Map.of("key1", newArgEntry));
    }

    @Test
    void testUpdateStateWhenRollingEntryPassed() {
        state.arguments = new HashMap<>(Map.of(
                "key1", key1ArgEntry,
                "key2", key2ArgEntry
        ));

        Map<String, ArgumentEntry> newArgs = Map.of("key3", new TsRollingArgumentEntry(10, 30000L));
        assertThatThrownBy(() -> state.update(newArgs, ctx))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported argument type detected for argument: key3. " +
                            "Rolling argument entry is not supported for simple calculated fields.");
    }

    @Test
    void testPerformCalculation() throws ExecutionException, InterruptedException {
        state.arguments = new HashMap<>(Map.of(
                "key1", key1ArgEntry,
                "key2", key2ArgEntry,
                "key3", key3ArgEntry
        ));

        TelemetryCalculatedFieldResult result = performCalculation();

        assertThat(result).isNotNull();
        Output output = getCalculatedFieldConfig().getOutput();
        assertThat(result.getType()).isEqualTo(output.getType());
        assertThat(result.getScope()).isEqualTo(output.getScope());
        assertThat(result.getResult()).isEqualTo(JacksonUtil.valueToTree(Map.of("output", 49)));
    }

    @Test
    void testPerformCalculationWhenPassedString() {
        state.arguments = new HashMap<>(Map.of(
                "key1", key1ArgEntry,
                "key2", new SingleValueArgumentEntry(System.currentTimeMillis() - 9, new StringDataEntry("key2", "string"), 124L),
                "key3", key3ArgEntry
        ));

        assertThatThrownBy(() -> state.performCalculation(Collections.emptyMap(), ctx))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Argument 'key2' is not a number.");
    }

    @Test
    void testPerformCalculationWhenPassedBoolean() throws ExecutionException, InterruptedException {
        state.arguments = new HashMap<>(Map.of(
                "key1", key1ArgEntry,
                "key2", new SingleValueArgumentEntry(System.currentTimeMillis() - 9, new BooleanDataEntry("key2", true), 124L),// true is parsed as 1
                "key3", key3ArgEntry
        ));

        TelemetryCalculatedFieldResult result = performCalculation();

        assertThat(result).isNotNull();
        Output output = getCalculatedFieldConfig().getOutput();
        assertThat(result.getType()).isEqualTo(output.getType());
        assertThat(result.getScope()).isEqualTo(output.getScope());
        assertThat(result.getResult()).isEqualTo(JacksonUtil.valueToTree(Map.of("output", 35)));
    }

    @Test
    void testPerformCalculationWhenDecimalsByDefault() throws ExecutionException, InterruptedException {
        state.arguments = new HashMap<>(Map.of(
                "key1", new SingleValueArgumentEntry(System.currentTimeMillis() - 10, new DoubleDataEntry("key1", 11.3456), 145L),
                "key2", new SingleValueArgumentEntry(System.currentTimeMillis() - 6, new DoubleDataEntry("key2", 15.1), 165L),
                "key3", new SingleValueArgumentEntry(System.currentTimeMillis() - 3, new DoubleDataEntry("key3", 23.1), 184L)
        ));

        Output output = getCalculatedFieldConfig().getOutput();
        output.setDecimalsByDefault(3);
        ctx.setOutput(output);

        TelemetryCalculatedFieldResult result = performCalculation();

        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(output.getType());
        assertThat(result.getScope()).isEqualTo(output.getScope());
        assertThat(result.getResult()).isEqualTo(JacksonUtil.valueToTree(Map.of("output", 49.546)));
    }

    @Test
    void testIsReadyWhenNotAllArgPresent() {
        assertThat(state.isReady()).isFalse();
        assertThat(state.getReadinessStatus().errorMsg()).contains(state.getRequiredArguments());
    }

    @Test
    void testIsReadyWhenAllArgPresent() {
        state.update(Map.of(
                "key1", key1ArgEntry,
                "key2", key2ArgEntry,
                "key3", key3ArgEntry
        ), ctx);
        assertThat(state.isReady()).isTrue();
        assertThat(state.getReadinessStatus().errorMsg()).isNull();
    }

    @Test
    void testIsReadyWhenEmptyEntryPresents() {
        state.update(Map.of(
                "key1", key1ArgEntry,
                "key2", key2ArgEntry,
                "key3", new SingleValueArgumentEntry()
        ), ctx);
        assertThat(state.isReady()).isFalse();
        assertThat(state.getReadinessStatus().errorMsg()).contains("key3");
    }

    private CalculatedField getCalculatedField() {
        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setTenantId(TENANT_ID);
        calculatedField.setEntityId(DEVICE_ID);
        calculatedField.setType(CalculatedFieldType.SIMPLE);
        calculatedField.setName("Test Calculated Field");
        calculatedField.setConfigurationVersion(1);
        calculatedField.setConfiguration(getCalculatedFieldConfig());
        calculatedField.setVersion(1L);
        return calculatedField;
    }

    private CalculatedFieldConfiguration getCalculatedFieldConfig() {
        SimpleCalculatedFieldConfiguration config = new SimpleCalculatedFieldConfiguration();

        Argument argument1 = new Argument();
        argument1.setRefEntityId(ASSET_ID);
        ReferencedEntityKey refEntityKey1 = new ReferencedEntityKey("temp1", ArgumentType.TS_LATEST, null);
        argument1.setRefEntityKey(refEntityKey1);

        Argument argument2 = new Argument();
        argument2.setRefEntityId(ASSET_ID);
        ReferencedEntityKey refEntityKey2 = new ReferencedEntityKey("temp2", ArgumentType.ATTRIBUTE, null);
        argument2.setRefEntityKey(refEntityKey2);

        Argument argument3 = new Argument();
        argument3.setRefEntityId(ASSET_ID);
        ReferencedEntityKey refEntityKey3 = new ReferencedEntityKey("temp3", ArgumentType.TS_LATEST, null);
        argument3.setRefEntityKey(refEntityKey3);

        config.setArguments(Map.of("key1", argument1, "key2", argument2, "key3", argument3));

        config.setExpression("key1 + key2 + key3");

        AttributesOutput output = new AttributesOutput();
        output.setName("output");
        output.setScope(AttributeScope.SERVER_SCOPE);
        output.setDecimalsByDefault(0);

        config.setOutput(output);

        return config;
    }

    private TelemetryCalculatedFieldResult performCalculation() throws InterruptedException, ExecutionException {
        return (TelemetryCalculatedFieldResult) state.performCalculation(Collections.emptyMap(), ctx).get();
    }

}
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

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.script.api.tbel.DefaultTbelInvokeService;
import org.thingsboard.script.api.tbel.TbelInvokeService;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.CalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.common.data.cf.configuration.OutputType;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.cf.configuration.SimpleCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.stats.DefaultStatsFactory;
import org.thingsboard.server.dao.usagerecord.ApiLimitService;
import org.thingsboard.server.service.cf.CalculatedFieldResult;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {SimpleMeterRegistry.class, DefaultStatsFactory.class, DefaultTbelInvokeService.class})
public class ScriptCalculatedFieldStateTest {

    private final TenantId TENANT_ID = TenantId.fromUUID(UUID.fromString("5b18e321-3327-4290-b996-d72a65e90382"));
    private final DeviceId DEVICE_ID = new DeviceId(UUID.fromString("5512071d-5abc-411d-a907-4cdb6539c2eb"));
    private final AssetId ASSET_ID = new AssetId(UUID.fromString("5bc010ae-bcfd-46c8-98b9-8ee8c8955a76"));

    private final SingleValueArgumentEntry assetHumidityArgEntry = new SingleValueArgumentEntry(System.currentTimeMillis() - 10, new DoubleDataEntry("assetHumidity", 86.0), 122L);
    private final TsRollingArgumentEntry deviceTemperatureArgEntry = createRollingArgEntry();

    private final long ts = System.currentTimeMillis();

    private ScriptCalculatedFieldState state;
    private CalculatedFieldCtx ctx;

    @Autowired
    private TbelInvokeService tbelInvokeService;

    @MockitoBean
    private ApiLimitService apiLimitService;

    @BeforeEach
    void setUp() {
        when(apiLimitService.getLimit(any(), any())).thenReturn(1000L);
        ctx = new CalculatedFieldCtx(getCalculatedField(), tbelInvokeService, apiLimitService);
        ctx.init();
        state = new ScriptCalculatedFieldState(ctx.getArgNames());
    }

    @Test
    void testType() {
        assertThat(state.getType()).isEqualTo(CalculatedFieldType.SCRIPT);
    }

    @Test
    void testUpdateState() {
        state.arguments = new HashMap<>(Map.of("assetHumidity", assetHumidityArgEntry));

        Map<String, ArgumentEntry> newArgs = Map.of("deviceTemperature", deviceTemperatureArgEntry);
        boolean stateUpdated = state.updateState(ctx, newArgs);

        assertThat(stateUpdated).isTrue();
        assertThat(state.getArguments()).containsExactlyInAnyOrderEntriesOf(
                Map.of(
                        "assetHumidity", assetHumidityArgEntry,
                        "deviceTemperature", deviceTemperatureArgEntry
                )
        );
    }

    @Test
    void testUpdateStateWhenUpdateExistingEntry() {
        state.arguments = new HashMap<>(Map.of("deviceTemperature", deviceTemperatureArgEntry, "assetHumidity", assetHumidityArgEntry));

        SingleValueArgumentEntry newArgEntry = new SingleValueArgumentEntry(ts, new LongDataEntry("assetHumidity", 41L), 349L);
        Map<String, ArgumentEntry> newArgs = Map.of("assetHumidity", newArgEntry);
        boolean stateUpdated = state.updateState(ctx, newArgs);

        assertThat(stateUpdated).isTrue();
        assertThat(state.getArguments()).containsExactlyInAnyOrderEntriesOf(
                Map.of(
                        "assetHumidity", newArgEntry,
                        "deviceTemperature", deviceTemperatureArgEntry
                )
        );
    }

    @Test
    void testPerformCalculation() throws ExecutionException, InterruptedException {
        state.arguments = new HashMap<>(Map.of("deviceTemperature", deviceTemperatureArgEntry, "assetHumidity", assetHumidityArgEntry));

        CalculatedFieldResult result = state.performCalculation(ctx).get();

        assertThat(result).isNotNull();
        Output output = getCalculatedFieldConfig().getOutput();
        assertThat(result.getType()).isEqualTo(output.getType());
        assertThat(result.getScope()).isEqualTo(output.getScope());
        assertThat(result.getResult()).isEqualTo(JacksonUtil.valueToTree(Map.of("maxDeviceTemperature", 17.0, "assetHumidity", 43.0)));
    }

    @Test
    void testPerformCalculationWithLongEntry() throws ExecutionException, InterruptedException {
        state.arguments = new HashMap<>(Map.of(
                "deviceTemperature", deviceTemperatureArgEntry,
                "assetHumidity", new SingleValueArgumentEntry(System.currentTimeMillis() - 10, new LongDataEntry("a", 45L), 10L)
        ));

        CalculatedFieldResult result = state.performCalculation(ctx).get();

        assertThat(result).isNotNull();
        Output output = getCalculatedFieldConfig().getOutput();
        assertThat(result.getType()).isEqualTo(output.getType());
        assertThat(result.getScope()).isEqualTo(output.getScope());
        assertThat(result.getResult()).isEqualTo(JacksonUtil.valueToTree(Map.of("maxDeviceTemperature", 17.0, "assetHumidity", 22.5)));
    }

    @Test
    void testIsReadyWhenNotAllArgPresent() {
        assertThat(state.isReady()).isFalse();
    }

    @Test
    void testIsReadyWhenAllArgPresent() {
        state.arguments = new HashMap<>(Map.of("deviceTemperature", deviceTemperatureArgEntry, "assetHumidity", assetHumidityArgEntry));

        assertThat(state.isReady()).isTrue();
    }

    @Test
    void testIsReadyWhenEmptyEntryPresents() {
        state.arguments = new HashMap<>(Map.of("deviceTemperature", new TsRollingArgumentEntry(5, 30000L), "assetHumidity", assetHumidityArgEntry));

        assertThat(state.isReady()).isFalse();
    }

    private TsRollingArgumentEntry createRollingArgEntry() {
        TsRollingArgumentEntry argumentEntry = new TsRollingArgumentEntry(5, 30000L);
        long ts = System.currentTimeMillis();

        TreeMap<Long, Double> values = new TreeMap<>();
        values.put(ts - 40, 10.0);
        values.put(ts - 30, 12.0);
        values.put(ts - 20, 17.0);

        argumentEntry.setTsRecords(values);
        return argumentEntry;
    }

    private CalculatedField getCalculatedField() {
        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setTenantId(TENANT_ID);
        calculatedField.setEntityId(ASSET_ID);
        calculatedField.setType(CalculatedFieldType.SCRIPT);
        calculatedField.setName("Test Calculated Field");
        calculatedField.setConfigurationVersion(1);
        calculatedField.setConfiguration(getCalculatedFieldConfig());
        calculatedField.setVersion(1L);
        return calculatedField;
    }

    private CalculatedFieldConfiguration getCalculatedFieldConfig() {
        SimpleCalculatedFieldConfiguration config = new SimpleCalculatedFieldConfiguration();

        Argument argument1 = new Argument();
        argument1.setRefEntityId(DEVICE_ID);
        ReferencedEntityKey refEntityKey1 = new ReferencedEntityKey("temperature", ArgumentType.TS_ROLLING, null);
        argument1.setRefEntityKey(refEntityKey1);
        argument1.setLimit(5);
        argument1.setTimeWindow(30000L);

        Argument argument2 = new Argument();
        ReferencedEntityKey refEntityKey2 = new ReferencedEntityKey("humidity", ArgumentType.TS_LATEST, null);
        argument1.setRefEntityKey(refEntityKey2);

        config.setArguments(Map.of("deviceTemperature", argument1, "assetHumidity", argument2));

        config.setExpression("return {\"maxDeviceTemperature\": deviceTemperature.max(), \"assetHumidity\": assetHumidity / 2 }");

        Output output = new Output();
        output.setType(OutputType.ATTRIBUTES);
        output.setScope(AttributeScope.SERVER_SCOPE);

        config.setOutput(output);

        return config;
    }

}
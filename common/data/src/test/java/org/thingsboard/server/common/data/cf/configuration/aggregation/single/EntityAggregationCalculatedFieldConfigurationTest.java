/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.common.data.cf.configuration.aggregation.single;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.cf.configuration.TimeSeriesOutput;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggFunctionInput;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggKeyInput;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggMetric;
import org.thingsboard.server.common.data.cf.configuration.aggregation.single.interval.HourInterval;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class EntityAggregationCalculatedFieldConfigurationTest {

    @Test
    void typeShouldBeEntityAggregation() {
        var cfg = new EntityAggregationCalculatedFieldConfiguration();
        assertThat(cfg.getType()).isEqualTo(CalculatedFieldType.ENTITY_AGGREGATION);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ATTRIBUTE", "TS_ROLLING"})
    void validateShouldThrowWhenNotTsLatestArgumentUsed(String argumentType) {
        var cfg = new EntityAggregationCalculatedFieldConfiguration();
        cfg.setArguments(Map.of("k", validArgument(ArgumentType.valueOf(argumentType))));
        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Calculated field with type: '" + cfg.getType() + "' support only TS_LATEST arguments.");
    }

    @Test
    void validateShouldThrowWhenMetricMapIsEmpty() {
        var cfg = new EntityAggregationCalculatedFieldConfiguration();

        cfg.setArguments(Map.of("k", validArgument(ArgumentType.TS_LATEST)));
        cfg.setMetrics(Map.of());

        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Metrics map cannot be empty.");
    }

    @Test
    void validateShouldThrowWhenMetricInputIsNotAggKeyInput() {
        var cfg = new EntityAggregationCalculatedFieldConfiguration();

        cfg.setArguments(Map.of("k", validArgument(ArgumentType.TS_LATEST)));

        AggMetric metric = new AggMetric();
        metric.setInput(new AggFunctionInput()); // cannot be function
        cfg.setMetrics(Map.of("m", metric));

        cfg.setInterval(new HourInterval("Europe/Kiev", null));
        cfg.setOutput(new TimeSeriesOutput());

        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Metric key can only refer to argument.");
    }

    @Test
    void validateShouldThrowWhenMetricReferencesUnknownArgument() {
        var cfg = new EntityAggregationCalculatedFieldConfiguration();

        cfg.setArguments(Map.of("k", validArgument(ArgumentType.TS_LATEST)));

        AggMetric metric = new AggMetric();
        metric.setInput(new AggKeyInput("unknown"));
        cfg.setMetrics(Map.of("m", metric));

        cfg.setInterval(new HourInterval("Europe/Kiev", null));
        cfg.setOutput(new TimeSeriesOutput());

        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Metric references unknown argument: 'unknown'.");
    }

    @Test
    void validateShouldThrowWhenIntervalIsNull() {
        var cfg = new EntityAggregationCalculatedFieldConfiguration();

        cfg.setArguments(Map.of("k", validArgument(ArgumentType.TS_LATEST)));
        cfg.setMetrics(Map.of("m", validMetric()));
        cfg.setInterval(null);
        cfg.setOutput(new TimeSeriesOutput());

        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Interval must be defined.");
    }

    private Argument validArgument(ArgumentType type) {
        Argument a = new Argument();
        a.setRefEntityKey(new ReferencedEntityKey("key", type, null));
        return a;
    }

    private AggMetric validMetric() {
        AggMetric metric = new AggMetric();
        metric.setInput(new AggKeyInput("k"));
        return metric;
    }

}

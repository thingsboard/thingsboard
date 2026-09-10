// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.cf.configuration.aggregation.single;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.ArgumentsBasedCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.CalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggKeyInput;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggMetric;
import org.thingsboard.server.common.data.cf.configuration.aggregation.single.interval.AggInterval;
import org.thingsboard.server.common.data.cf.configuration.aggregation.single.interval.Watermark;

import java.util.Map;

@Schema
@Data
public class EntityAggregationCalculatedFieldConfiguration implements ArgumentsBasedCalculatedFieldConfiguration {

    private Map<String, Argument> arguments;
    @Valid
    @NotEmpty
    private Map<String, AggMetric> metrics;
    @Valid
    @NotNull
    private AggInterval interval;
    @Valid
    private Watermark watermark;
    private boolean produceIntermediateResult;
    @Valid
    @NotNull
    private Output output;

    @Override
    public CalculatedFieldType getType() {
        return CalculatedFieldType.ENTITY_AGGREGATION;
    }

    @Override
    public void validate() {
        validateArguments();
        validateMetrics();
        validateInterval();
    }

    private void validateArguments() {
        if (arguments.containsKey("ctx")) {
            throw new IllegalArgumentException("Argument name 'ctx' is reserved and cannot be used.");
        }
        if (arguments.values().stream().anyMatch(argument -> !ArgumentType.TS_LATEST.equals(argument.getRefEntityKey().getType()))) {
            throw new IllegalArgumentException("Calculated field with type: '" + getType() + "' support only TS_LATEST arguments.");
        }
    }

    private void validateMetrics() {
        if (metrics == null || metrics.isEmpty()) {
            throw new IllegalArgumentException("Metrics map cannot be empty.");
        }

        for (AggMetric metric : metrics.values()) {
            if (metric.getInput() instanceof AggKeyInput aggKeyInput) {
                if (!arguments.containsKey(aggKeyInput.getKey())) {
                    throw new IllegalArgumentException(
                            "Metric references unknown argument: '" + aggKeyInput.getKey() + "'."
                    );
                }
            } else {
                throw new IllegalArgumentException("Metric key can only refer to argument.");
            }
        }
    }

    private void validateInterval() {
        if (interval == null) {
            throw new IllegalArgumentException("Interval must be defined.");
        }
        interval.validate();
    }

}

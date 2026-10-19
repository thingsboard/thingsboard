// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.cf.configuration.aggregation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentsBasedCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.CalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.HasRelationPathLevel;
import org.thingsboard.server.common.data.cf.configuration.HasUseLatestTsConfig;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.common.data.cf.configuration.ScheduledUpdateSupportedCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.relation.RelationPathLevel;

import java.util.Map;

@Schema
@Data
public class RelatedEntitiesAggregationCalculatedFieldConfiguration implements ArgumentsBasedCalculatedFieldConfiguration, ScheduledUpdateSupportedCalculatedFieldConfiguration, HasRelationPathLevel, HasUseLatestTsConfig {

    @NotNull
    private RelationPathLevel relation;
    private Map<String, Argument> arguments;
    private long deduplicationIntervalInSec;
    @Valid
    @NotEmpty
    private Map<String, AggMetric> metrics;
    @Valid
    @NotNull
    private Output output;
    private boolean useLatestTs;

    private Integer scheduledUpdateInterval;

    @Override
    public CalculatedFieldType getType() {
        return CalculatedFieldType.RELATED_ENTITIES_AGGREGATION;
    }

    @Override
    public boolean isScheduledUpdateEnabled() {
        return true;
    }

    @Override
    public void validate() {
        validateRelation();
        validateArguments();
        validateMetrics();
    }

    private void validateRelation() {
        if (relation == null) {
            throw new IllegalArgumentException("Relation must be specified!");
        }
        relation.validate();
    }

    private void validateArguments() {
        if (arguments == null || arguments.isEmpty()) {
            throw new IllegalArgumentException("Arguments map cannot be empty.");
        }
        if (arguments.containsKey("ctx")) {
            throw new IllegalArgumentException("Argument name 'ctx' is reserved and cannot be used.");
        }
        if (arguments.values().stream().anyMatch(Argument::hasTsRollingArgument)) {
            throw new IllegalArgumentException("Calculated field with type: '" + getType() + "' doesn't support TS_ROLLING arguments.");
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
            }
        }
    }

}

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
package org.thingsboard.server.common.data.cf.configuration.aggregation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentsBasedCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.HasRelationPathLevel;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.common.data.cf.configuration.ScheduledUpdateSupportedCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.relation.RelationPathLevel;

import java.util.Map;

@Data
public class RelatedEntitiesAggregationCalculatedFieldConfiguration implements ArgumentsBasedCalculatedFieldConfiguration, ScheduledUpdateSupportedCalculatedFieldConfiguration, HasRelationPathLevel {

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

    private int scheduledUpdateInterval;

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

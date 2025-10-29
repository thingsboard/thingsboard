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

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentsBasedCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggMetric;
import org.thingsboard.server.common.data.cf.configuration.aggregation.single.interval.AggInterval;
import org.thingsboard.server.common.data.cf.configuration.aggregation.single.interval.Watermark;

import java.util.Map;

@Data
public class EntityAggregationCalculatedFieldConfiguration implements ArgumentsBasedCalculatedFieldConfiguration {

    private Map<String, Argument> arguments;
    @Valid
    @NotEmpty
    private Map<String, AggMetric> metrics;

    private AggInterval interval;
    private Watermark watermark;

    private Output output;

    @Override
    public CalculatedFieldType getType() {
        return CalculatedFieldType.ENTITY_AGGREGATION;
    }

    @Override
    public void validate() {
        if (arguments.containsKey("ctx")) {
            throw new IllegalArgumentException("Argument name 'ctx' is reserved and cannot be used.");
        }
        if (arguments.values().stream().anyMatch(Argument::hasTsRollingArgument)) {
            throw new IllegalArgumentException("Calculated field with type: '" + getType() + "' doesn't support TS_ROLLING arguments.");
        }
    }

}

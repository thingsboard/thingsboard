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
package org.thingsboard.server.common.data.cf.configuration.aggregation;

import lombok.Data;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.CalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;

import java.util.List;
import java.util.Map;

@Data
public class LatestValuesAggregationCalculatedFieldConfiguration implements CalculatedFieldConfiguration {

    private AggSource source;
    private Map<String, ReferencedEntityKey> inputs;
    private long deduplicationIntervalMillis;
    private Map<String, AggMetric> metrics;
    private Output output;

    @Override
    public CalculatedFieldType getType() {
        return CalculatedFieldType.LATEST_VALUES_AGGREGATION;
    }

    @Override
    public void validate() {
    }

    public CfAggTrigger buildTrigger() {
        return CfAggTrigger.builder()
                .inputs(List.copyOf(inputs.values()))
                .entityProfiles(List.copyOf(source.getEntityProfiles()))
                .build();
    }

}

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

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationPathLevel;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RelatedEntitiesAggregationCalculatedFieldConfigurationTest {

    @Test
    void typeShouldBeEntityAggregation() {
        var cfg = new RelatedEntitiesAggregationCalculatedFieldConfiguration();
        assertThat(cfg.getType()).isEqualTo(CalculatedFieldType.RELATED_ENTITIES_AGGREGATION);
    }

    @Test
    void validateShouldThrowWhenRelationIsNotSet() {
        var cfg = new RelatedEntitiesAggregationCalculatedFieldConfiguration();

        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Relation must be specified!");
    }

    @Test
    void validateShouldThrowWhenRelationIsNotValid() {
        var cfg = new RelatedEntitiesAggregationCalculatedFieldConfiguration();

        cfg.setRelation(new RelationPathLevel(null, null));

        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Direction must be specified!");
    }

    @Test
    void validateShouldThrowWhenArgumentsMapIsEmpty() {
        var cfg = new RelatedEntitiesAggregationCalculatedFieldConfiguration();

        cfg.setRelation(new RelationPathLevel(EntitySearchDirection.FROM, EntityRelation.CONTAINS_TYPE));

        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Arguments map cannot be empty.");
    }

    @Test
    void validateShouldThrowWhenTsRollingArgumentUsed() {
        var cfg = new RelatedEntitiesAggregationCalculatedFieldConfiguration();

        cfg.setRelation(new RelationPathLevel(EntitySearchDirection.FROM, EntityRelation.CONTAINS_TYPE));
        Argument argument = new Argument();
        argument.setRefEntityKey(new ReferencedEntityKey("key", ArgumentType.TS_ROLLING, null));
        cfg.setArguments(Map.of("k", argument));

        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Calculated field with type: '" + CalculatedFieldType.RELATED_ENTITIES_AGGREGATION + "' doesn't support TS_ROLLING arguments.");
    }

    @Test
    void validateShouldThrowWhenMetricMapIsEmpty() {
        var cfg = new RelatedEntitiesAggregationCalculatedFieldConfiguration();

        cfg.setRelation(new RelationPathLevel(EntitySearchDirection.FROM, EntityRelation.CONTAINS_TYPE));
        cfg.setArguments(Map.of("k", validArgument(ArgumentType.TS_LATEST)));
        cfg.setMetrics(Map.of());

        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Metrics map cannot be empty.");
    }

    @Test
    void validateShouldThrowWhenMetricReferencesUnknownArgument() {
        var cfg = new RelatedEntitiesAggregationCalculatedFieldConfiguration();

        cfg.setRelation(new RelationPathLevel(EntitySearchDirection.FROM, EntityRelation.CONTAINS_TYPE));
        cfg.setArguments(Map.of("k", validArgument(ArgumentType.TS_LATEST)));

        AggMetric metric = new AggMetric();
        metric.setInput(new AggKeyInput("unknown"));
        cfg.setMetrics(Map.of("m", metric));

        cfg.setOutput(new Output());

        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Metric references unknown argument: 'unknown'.");
    }

    private Argument validArgument(ArgumentType type) {
        Argument a = new Argument();
        a.setRefEntityKey(new ReferencedEntityKey("key", type, null));
        return a;
    }

}

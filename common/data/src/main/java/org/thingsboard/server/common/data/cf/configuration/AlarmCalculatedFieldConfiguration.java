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
package org.thingsboard.server.common.data.cf.configuration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.rule.AlarmRule;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.util.CollectionsUtil;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

import static java.util.Map.Entry.comparingByKey;

@Data
public class AlarmCalculatedFieldConfiguration implements ArgumentsBasedCalculatedFieldConfiguration {

    private Map<String, Argument> arguments;

    @Valid
    @NotEmpty
    private Map<AlarmSeverity, AlarmRule> createRules;
    @Valid
    private AlarmRule clearRule;

    private boolean propagate;
    private boolean propagateToOwner;
    private boolean propagateToTenant;
    private List<String> propagateRelationTypes;

    @Override
    public CalculatedFieldType getType() {
        return CalculatedFieldType.ALARM;
    }

    @Override
    public Output getOutput() {
        return null;
    }

    @JsonIgnore
    @Override
    public boolean requiresScheduledReevaluation() {
        return getAllRules().anyMatch(entry -> entry.getValue().requiresScheduledReevaluation());
    }

    @JsonIgnore
    public Stream<Pair<AlarmSeverity, AlarmRule>> getAllRules() {
        Stream<Pair<AlarmSeverity, AlarmRule>> rules = createRules.entrySet().stream()
                .map(entry -> Pair.of(entry.getKey(), entry.getValue()));
        if (clearRule != null) {
            rules = Stream.concat(rules, Stream.of(Pair.of(null, clearRule)));
        }
        return rules.sorted(comparingByKey(Comparator.nullsLast(Comparator.naturalOrder())));
    }

    public boolean rulesEqual(AlarmCalculatedFieldConfiguration other, BiPredicate<AlarmRule, AlarmRule> equalityCheck) {
        List<Pair<AlarmSeverity, AlarmRule>> thisRules = this.getAllRules().toList();
        List<Pair<AlarmSeverity, AlarmRule>> otherRules = other.getAllRules().toList();
        return CollectionsUtil.elementsEqual(thisRules, otherRules, (thisRule, otherRule) -> {
            if (!Objects.equals(thisRule.getKey(), otherRule.getKey())) {
                return false;
            }
            return equalityCheck.test(thisRule.getValue(), otherRule.getValue());
        });
    }

    public boolean propagationSettingsEqual(AlarmCalculatedFieldConfiguration other) {
        return this.propagate == other.propagate &&
               this.propagateToOwner == other.propagateToOwner &&
               this.propagateToTenant == other.propagateToTenant &&
               Objects.equals(this.propagateRelationTypes, other.propagateRelationTypes);
    }

}

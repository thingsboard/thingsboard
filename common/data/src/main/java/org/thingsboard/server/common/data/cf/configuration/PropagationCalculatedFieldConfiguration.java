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

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.relation.RelationPathLevel;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class PropagationCalculatedFieldConfiguration extends BaseCalculatedFieldConfiguration {

    public static final String PROPAGATION_CONFIG_ARGUMENT = "propagationCtx";

    @Valid
    @NotNull
    private RelationPathLevel relation;

    private boolean applyExpressionToResolvedArguments;

    @Override
    public CalculatedFieldType getType() {
        return CalculatedFieldType.PROPAGATION;
    }

    @Override
    public void validate() {
        baseCalculatedFieldRestriction();
        propagationRestriction();
        if (!applyExpressionToResolvedArguments) {
            arguments.forEach((name, argument) -> {
                if (!currentEntitySource(argument)) {
                    throw new IllegalArgumentException("Arguments in 'Arguments only' propagation mode support only the 'Current entity' source entity type!");
                }
                if (argument.getRefEntityKey() == null) {
                    throw new IllegalArgumentException("Argument: '" + name + "' doesn't have reference entity key configured!");
                }
                if (argument.getRefEntityKey().getType() == ArgumentType.TS_ROLLING) {
                    throw new IllegalArgumentException("Argument type: 'Time series rolling' detected for argument: '" + name + "'. " +
                                                       "Only 'Attribute' or 'Latest telemetry' arguments are allowed for 'Arguments only' propagation mode!");
                }
            });
        } else {
            boolean noneMatchCurrentEntitySource = arguments.entrySet()
                    .stream()
                    .noneMatch(entry -> currentEntitySource(entry.getValue()));
            if (noneMatchCurrentEntitySource) {
                throw new IllegalArgumentException("At least one argument must be configured with the 'Current entity' " +
                                                   "source entity type for 'Expression result' propagation mode!");
            }
            if (StringUtils.isBlank(expression)) {
                throw new IllegalArgumentException("Expression must be specified for 'Expression result' propagation mode!");
            }
        }
    }

    public Argument toPropagationArgument() {
        var refDynamicSourceConfiguration = new RelationPathQueryDynamicSourceConfiguration();
        refDynamicSourceConfiguration.setLevels(List.of(relation));
        var propagationArgument = new Argument();
        propagationArgument.setRefDynamicSourceConfiguration(refDynamicSourceConfiguration);
        return propagationArgument;
    }

    private void propagationRestriction() {
        if (arguments.entrySet().stream().anyMatch(entry -> entry.getKey().equals(PROPAGATION_CONFIG_ARGUMENT))) {
            throw new IllegalArgumentException("Argument name '" + PROPAGATION_CONFIG_ARGUMENT + "' is reserved and cannot be used.");
        }
    }

    private boolean currentEntitySource(Argument argument) {
        return argument.getRefEntityId() == null && argument.getRefDynamicSourceConfiguration() == null;
    }

}

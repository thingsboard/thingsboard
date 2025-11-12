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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationPathLevel;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.thingsboard.server.common.data.cf.configuration.PropagationCalculatedFieldConfiguration.PROPAGATION_CONFIG_ARGUMENT;

@ExtendWith(MockitoExtension.class)
public class PropagationCalculatedFieldConfigurationTest {

    @Test
    void typeShouldBePropagation() {
        var cfg = new PropagationCalculatedFieldConfiguration();
        assertThat(cfg.getType()).isEqualTo(CalculatedFieldType.PROPAGATION);
    }

    @Test
    void validateShouldThrowWhenConfigurationDisallowArgumentsWithReferencedEntity() {
        var cfg = new PropagationCalculatedFieldConfiguration();
        Argument argumentWithRefEntityIdSet = new Argument();
        argumentWithRefEntityIdSet.setRefEntityId(new DeviceId(UUID.fromString("bda14084-f40e-4acc-9b85-9d1dd209bb64")));
        cfg.setArguments(Map.of("argumentWithRefEntityIdSet", argumentWithRefEntityIdSet));
        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Arguments in 'Arguments only' propagation mode support only the 'Current entity' source entity type!");
    }

    @Test
    void validateShouldThrowWhenConfigurationDisallowArgumentsWithDynamicReferenceConfiguration() {
        var cfg = new PropagationCalculatedFieldConfiguration();
        Argument argumentWithDynamicRefEntitySource = new Argument();
        argumentWithDynamicRefEntitySource.setRefDynamicSourceConfiguration(new CurrentOwnerDynamicSourceConfiguration());
        cfg.setArguments(Map.of("argumentWithDynamicRefEntitySource", argumentWithDynamicRefEntitySource));
        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Arguments in 'Arguments only' propagation mode support only the 'Current entity' source entity type!");
    }

    @Test
    void validateShouldThrowWhenConfigurationHasNoArgumentsWithCurrentEntitySource() {
        var cfg = new PropagationCalculatedFieldConfiguration();
        Argument argumentWithRefEntityIdSet = new Argument();
        argumentWithRefEntityIdSet.setRefEntityId(new DeviceId(UUID.fromString("3703e895-3f9b-4b75-a715-b68f1ad51944")));
        cfg.setArguments(Map.of("argumentWithRefEntityIdSet", argumentWithRefEntityIdSet));
        cfg.setApplyExpressionToResolvedArguments(true);
        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("At least one argument must be configured with the 'Current entity' " +
                        "source entity type for 'Expression result' propagation mode!");
    }

    @Test
    void validateShouldThrowWhenUsedReservedPropagationArgumentName() {
        var cfg = new PropagationCalculatedFieldConfiguration();
        cfg.setArguments(Map.of(PROPAGATION_CONFIG_ARGUMENT, new Argument()));
        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Argument name '" + PROPAGATION_CONFIG_ARGUMENT + "' is reserved and cannot be used.");
    }

    @Test
    void validateShouldThrowWhenUsedReservedCtxArgumentName() {
        var cfg = new PropagationCalculatedFieldConfiguration();
        cfg.setArguments(Map.of("ctx", new Argument()));
        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Argument name 'ctx' is reserved and cannot be used.");
    }

    @Test
    void validateShouldThrowWhenReferencedEntityKeyIsNotSet() {
        var cfg = new PropagationCalculatedFieldConfiguration();
        cfg.setRelation(new RelationPathLevel(EntitySearchDirection.TO, EntityRelation.CONTAINS_TYPE));
        Argument argument = new Argument();
        cfg.setArguments(Map.of("someArgumentName", argument));
        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Argument: 'someArgumentName' doesn't have reference entity key configured!");
    }

    @Test
    void validateShouldThrowWhenReferencedEntityKeyTypeIsTsRolling() {
        var cfg = new PropagationCalculatedFieldConfiguration();
        ReferencedEntityKey referencedEntityKey = new ReferencedEntityKey("someKey", ArgumentType.TS_ROLLING, null);
        Argument argument = new Argument();
        argument.setRefEntityKey(referencedEntityKey);
        cfg.setArguments(Map.of("someArgumentName", argument));
        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Argument type: 'Time series rolling' detected for argument: 'someArgumentName'. " +
                            "Only 'Attribute' or 'Latest telemetry' arguments are allowed for 'Arguments only' propagation mode!");
    }

    @Test
    void validateShouldThrowWhenExpressionIsNotSet() {
        var cfg = new PropagationCalculatedFieldConfiguration();
        cfg.setArguments(Map.of("someArgumentName", new Argument()));
        cfg.setApplyExpressionToResolvedArguments(true);
        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Expression must be specified for 'Expression result' propagation mode!");
    }

    @Test
    void validateToPropagationArgumentMethodCallReturnCorrectArgument() {
        var cfg = new PropagationCalculatedFieldConfiguration();
        cfg.setRelation(new RelationPathLevel(EntitySearchDirection.TO, EntityRelation.CONTAINS_TYPE));

        Argument propagationArgument = cfg.toPropagationArgument();
        assertThat(propagationArgument).isNotNull();
        assertThat(propagationArgument.getRefEntityId()).isNull();
        assertThat(propagationArgument.getRefEntityKey()).isNull();
        assertThat(propagationArgument.getDefaultValue()).isNull();
        assertThat(propagationArgument.getTimeWindow()).isNull();
        assertThat(propagationArgument.getLimit()).isNull();

        assertThat(propagationArgument.getRefDynamicSourceConfiguration())
                .isNotNull()
                .isInstanceOf(RelationPathQueryDynamicSourceConfiguration.class);
        var refDynamicSourceConfiguration = (RelationPathQueryDynamicSourceConfiguration) propagationArgument.getRefDynamicSourceConfiguration();
        assertThat(refDynamicSourceConfiguration.getLevels()).isNotEmpty().hasSize(1);

        var relationPathLevel = refDynamicSourceConfiguration.getLevels().get(0);
        assertThat(relationPathLevel.direction()).isEqualTo(EntitySearchDirection.TO);
        assertThat(relationPathLevel.relationType()).isEqualTo(EntityRelation.CONTAINS_TYPE);
    }

}

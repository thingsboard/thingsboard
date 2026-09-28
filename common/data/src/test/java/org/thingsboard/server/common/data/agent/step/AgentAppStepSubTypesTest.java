// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent.step;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.agent.step.state.AgentAppStepState;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Adding a step type takes three hand-synced edits (the {@link AgentAppStepType} constant, the {@link AgentAppStep}
 * subtype registration and, for stateful steps, the {@link AgentAppStepState} one). A missing registration only
 * surfaces at template load time as an {@code InvalidTypeIdException} that the materializer swallows per version,
 * so the symptom is a version quietly missing from the registry rather than a build failure. These tests turn that
 * into a compile-time-adjacent failure instead.
 */
class AgentAppStepSubTypesTest {

    @Test
    void everyStepTypeIsRegisteredAsAStepSubType() {
        assertThat(subTypeNames(AgentAppStep.class))
                .as("@JsonSubTypes on AgentAppStep must cover every AgentAppStepType")
                .containsAll(allStepTypeNames());
    }

    @Test
    void everyStepSubTypeNameMatchesItsOwnStepType() throws Exception {
        for (JsonSubTypes.Type type : AgentAppStep.class.getAnnotation(JsonSubTypes.class).value()) {
            Class<?> clazz = type.value();
            if (Modifier.isAbstract(clazz.getModifiers())) {
                continue;
            }
            AgentAppStep step = (AgentAppStep) clazz.getDeclaredConstructor().newInstance();
            assertThat(step.getType().name())
                    .as("%s is registered under name '%s'", clazz.getSimpleName(), type.name())
                    .isEqualTo(type.name());
        }
    }

    @Test
    void everyStatefulStepTypeIsRegisteredAsAStateSubType() {
        Set<String> statefulStepTypes = Arrays.stream(AgentAppStep.class.getAnnotation(JsonSubTypes.class).value())
                .filter(type -> StatefulStep.class.isAssignableFrom(type.value()))
                .map(JsonSubTypes.Type::name)
                .collect(Collectors.toSet());

        assertThat(subTypeNames(AgentAppStepState.class))
                .as("@JsonSubTypes on AgentAppStepState must cover every stateful step type")
                .containsAll(statefulStepTypes);
    }

    @Test
    void everyStateSubTypeNameMatchesItsOwnStepType() throws Exception {
        for (JsonSubTypes.Type type : AgentAppStepState.class.getAnnotation(JsonSubTypes.class).value()) {
            Class<?> clazz = type.value();
            if (Modifier.isAbstract(clazz.getModifiers())) {
                continue;
            }
            AgentAppStepState state = (AgentAppStepState) clazz.getDeclaredConstructor().newInstance();
            assertThat(state.getType().name())
                    .as("%s is registered under name '%s'", clazz.getSimpleName(), type.name())
                    .isEqualTo(type.name());
        }
    }

    private static Set<String> subTypeNames(Class<?> baseClass) {
        JsonSubTypes subTypes = baseClass.getAnnotation(JsonSubTypes.class);
        assertThat(subTypes).as("%s must have @JsonSubTypes", baseClass.getSimpleName()).isNotNull();
        return Arrays.stream(subTypes.value()).map(JsonSubTypes.Type::name).collect(Collectors.toSet());
    }

    private static Set<String> allStepTypeNames() {
        return EnumSet.allOf(AgentAppStepType.class).stream().map(Enum::name).collect(Collectors.toSet());
    }
}

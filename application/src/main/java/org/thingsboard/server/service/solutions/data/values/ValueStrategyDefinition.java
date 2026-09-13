// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.solutions.data.values;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CounterValueStrategyDefinition.class, name = "counter"),
        @JsonSubTypes.Type(value = NaturalValueStrategyDefinition.class, name = "natural"),
        @JsonSubTypes.Type(value = EventValueStrategyDefinition.class, name = "event"),
        @JsonSubTypes.Type(value = SequenceValueStrategyDefinition.class, name = "sequence"),
        @JsonSubTypes.Type(value = ConstantValueStrategyDefinition.class, name = "constant"),
        @JsonSubTypes.Type(value = CompositeValueStrategyDefinition.class, name = "composite"),
        @JsonSubTypes.Type(value = ScheduleValueStrategyDefinition.class, name = "schedule"),
        @JsonSubTypes.Type(value = IncrementValueStrategyDefinition.class, name = "inc"),
        @JsonSubTypes.Type(value = DecrementValueStrategyDefinition.class, name = "dec")})
public interface ValueStrategyDefinition {

    ValueStrategyDefinitionType getStrategyType();

}

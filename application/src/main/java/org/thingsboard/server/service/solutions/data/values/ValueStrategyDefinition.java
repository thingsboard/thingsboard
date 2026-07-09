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

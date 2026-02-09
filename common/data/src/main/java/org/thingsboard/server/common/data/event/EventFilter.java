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
package org.thingsboard.server.common.data.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "eventType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = RuleNodeDebugEventFilter.class, name = "DEBUG_RULE_NODE"),
        @JsonSubTypes.Type(value = RuleChainDebugEventFilter.class, name = "DEBUG_RULE_CHAIN"),
        @JsonSubTypes.Type(value = ErrorEventFilter.class, name = "ERROR"),
        @JsonSubTypes.Type(value = LifeCycleEventFilter.class, name = "LC_EVENT"),
        @JsonSubTypes.Type(value = StatisticsEventFilter.class, name = "STATS"),
        @JsonSubTypes.Type(value = CalculatedFieldDebugEventFilter.class, name = "DEBUG_CALCULATED_FIELD")
})
public interface EventFilter {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "String value representing the event type", example = "STATS")
    EventType getEventType();

    boolean isNotEmpty();

}

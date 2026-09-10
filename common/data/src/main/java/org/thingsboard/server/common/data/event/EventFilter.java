// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        description = "Filter for various event types",
        discriminatorProperty = "eventType",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "DEBUG_RULE_NODE", schema = RuleNodeDebugEventFilter.class),
                @DiscriminatorMapping(value = "DEBUG_RULE_CHAIN", schema = RuleChainDebugEventFilter.class),
                @DiscriminatorMapping(value = "ERROR", schema = ErrorEventFilter.class),
                @DiscriminatorMapping(value = "LC_EVENT", schema = LifeCycleEventFilter.class),
                @DiscriminatorMapping(value = "STATS", schema = StatisticsEventFilter.class),
                @DiscriminatorMapping(value = "DEBUG_CALCULATED_FIELD", schema = CalculatedFieldDebugEventFilter.class)
        }
)
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

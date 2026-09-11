// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.telemetry.settings;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.thingsboard.rule.engine.telemetry.strategy.ProcessingStrategy;

import java.util.Objects;

import static org.thingsboard.rule.engine.telemetry.settings.AttributesProcessingSettings.Advanced;
import static org.thingsboard.rule.engine.telemetry.settings.AttributesProcessingSettings.Deduplicate;
import static org.thingsboard.rule.engine.telemetry.settings.AttributesProcessingSettings.OnEveryMessage;
import static org.thingsboard.rule.engine.telemetry.settings.AttributesProcessingSettings.WebSocketsOnly;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = OnEveryMessage.class, name = "ON_EVERY_MESSAGE"),
        @JsonSubTypes.Type(value = WebSocketsOnly.class, name = "WEBSOCKETS_ONLY"),
        @JsonSubTypes.Type(value = Deduplicate.class, name = "DEDUPLICATE"),
        @JsonSubTypes.Type(value = Advanced.class, name = "ADVANCED")
})
public sealed interface AttributesProcessingSettings extends ProcessingSettings permits OnEveryMessage, Deduplicate, WebSocketsOnly, Advanced {

    record Advanced(ProcessingStrategy attributes, ProcessingStrategy webSockets, ProcessingStrategy calculatedFields) implements AttributesProcessingSettings {

        public Advanced {
            Objects.requireNonNull(attributes);
            Objects.requireNonNull(webSockets);
            Objects.requireNonNull(calculatedFields);
        }

    }

}

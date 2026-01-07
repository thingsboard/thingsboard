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
package org.thingsboard.rule.engine.telemetry.settings;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.thingsboard.rule.engine.telemetry.strategy.ProcessingStrategy;

import java.util.Objects;

import static org.thingsboard.rule.engine.telemetry.settings.TimeseriesProcessingSettings.Advanced;
import static org.thingsboard.rule.engine.telemetry.settings.TimeseriesProcessingSettings.Deduplicate;
import static org.thingsboard.rule.engine.telemetry.settings.TimeseriesProcessingSettings.OnEveryMessage;
import static org.thingsboard.rule.engine.telemetry.settings.TimeseriesProcessingSettings.WebSocketsOnly;

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
public sealed interface TimeseriesProcessingSettings extends ProcessingSettings permits OnEveryMessage, Deduplicate, WebSocketsOnly, Advanced {

    record Advanced(ProcessingStrategy timeseries, ProcessingStrategy latest, ProcessingStrategy webSockets, ProcessingStrategy calculatedFields) implements TimeseriesProcessingSettings {

        public Advanced {
            Objects.requireNonNull(timeseries);
            Objects.requireNonNull(latest);
            Objects.requireNonNull(webSockets);
            Objects.requireNonNull(calculatedFields);
        }

    }

}

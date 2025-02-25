/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.rule.engine.telemetry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.rule.engine.telemetry.strategy.ProcessingStrategy;

import java.util.Objects;

import static org.thingsboard.rule.engine.telemetry.TbMsgTimeseriesNodeConfiguration.ProcessingSettings.Advanced;
import static org.thingsboard.rule.engine.telemetry.TbMsgTimeseriesNodeConfiguration.ProcessingSettings.Deduplicate;
import static org.thingsboard.rule.engine.telemetry.TbMsgTimeseriesNodeConfiguration.ProcessingSettings.OnEveryMessage;
import static org.thingsboard.rule.engine.telemetry.TbMsgTimeseriesNodeConfiguration.ProcessingSettings.WebSocketsOnly;

@Data
public class TbMsgTimeseriesNodeConfiguration implements NodeConfiguration<TbMsgTimeseriesNodeConfiguration> {

    private long defaultTTL;
    private boolean useServerTs;
    @NotNull
    private TbMsgTimeseriesNodeConfiguration.ProcessingSettings processingSettings;

    @Override
    public TbMsgTimeseriesNodeConfiguration defaultConfiguration() {
        TbMsgTimeseriesNodeConfiguration configuration = new TbMsgTimeseriesNodeConfiguration();
        configuration.setDefaultTTL(0L);
        configuration.setUseServerTs(false);
        configuration.setProcessingSettings(new OnEveryMessage());
        return configuration;
    }

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
    sealed interface ProcessingSettings permits OnEveryMessage, Deduplicate, WebSocketsOnly, Advanced {

        record OnEveryMessage() implements ProcessingSettings {}

        record WebSocketsOnly() implements ProcessingSettings {}

        @Getter
        final class Deduplicate implements ProcessingSettings {

            private final int deduplicationIntervalSecs;

            @JsonIgnore
            private final ProcessingStrategy processingStrategy;

            @JsonCreator
            Deduplicate(@JsonProperty("deduplicationIntervalSecs") int deduplicationIntervalSecs) {
                this.deduplicationIntervalSecs = deduplicationIntervalSecs;
                processingStrategy = ProcessingStrategy.deduplicate(deduplicationIntervalSecs);
            }

        }

        record Advanced(ProcessingStrategy timeseries, ProcessingStrategy latest, ProcessingStrategy webSockets) implements ProcessingSettings {

            public Advanced {
                Objects.requireNonNull(timeseries);
                Objects.requireNonNull(latest);
                Objects.requireNonNull(webSockets);
            }

        }

    }

}

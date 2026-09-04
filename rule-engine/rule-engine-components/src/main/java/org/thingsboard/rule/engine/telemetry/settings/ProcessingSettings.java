// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.telemetry.settings;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.thingsboard.rule.engine.telemetry.strategy.ProcessingStrategy;

sealed interface ProcessingSettings permits TimeseriesProcessingSettings, AttributesProcessingSettings {

    record OnEveryMessage() implements TimeseriesProcessingSettings, AttributesProcessingSettings {}

    record WebSocketsOnly() implements TimeseriesProcessingSettings, AttributesProcessingSettings {}

    @Getter
    final class Deduplicate implements TimeseriesProcessingSettings, AttributesProcessingSettings {

        private final int deduplicationIntervalSecs;

        @JsonIgnore
        private final ProcessingStrategy processingStrategy;

        @JsonCreator
        public Deduplicate(@JsonProperty("deduplicationIntervalSecs") int deduplicationIntervalSecs) {
            this.deduplicationIntervalSecs = deduplicationIntervalSecs;
            this.processingStrategy = ProcessingStrategy.deduplicate(deduplicationIntervalSecs);
        }

    }

}

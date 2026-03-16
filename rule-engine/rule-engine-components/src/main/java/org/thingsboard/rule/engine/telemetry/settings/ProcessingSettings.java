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

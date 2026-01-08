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
package org.thingsboard.rule.engine.telemetry.strategy;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.UUID;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = OnEveryMessageProcessingStrategy.class, name = "ON_EVERY_MESSAGE"),
        @JsonSubTypes.Type(value = DeduplicateProcessingStrategy.class, name = "DEDUPLICATE"),
        @JsonSubTypes.Type(value = SkipProcessingStrategy.class, name = "SKIP")
})
public sealed interface ProcessingStrategy permits OnEveryMessageProcessingStrategy, DeduplicateProcessingStrategy, SkipProcessingStrategy {

    static ProcessingStrategy onEveryMessage() {
        return OnEveryMessageProcessingStrategy.getInstance();
    }

    static ProcessingStrategy deduplicate(int deduplicationIntervalSecs) {
        return new DeduplicateProcessingStrategy(deduplicationIntervalSecs);
    }

    static ProcessingStrategy skip() {
        return SkipProcessingStrategy.getInstance();
    }

    boolean shouldProcess(long ts, UUID originatorUuid);

}

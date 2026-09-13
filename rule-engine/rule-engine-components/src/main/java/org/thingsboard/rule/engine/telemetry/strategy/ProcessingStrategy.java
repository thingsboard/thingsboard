// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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

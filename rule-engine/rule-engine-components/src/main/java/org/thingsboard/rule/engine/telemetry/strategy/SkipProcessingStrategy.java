// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.telemetry.strategy;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.UUID;

final class SkipProcessingStrategy implements ProcessingStrategy {

    private static final SkipProcessingStrategy INSTANCE = new SkipProcessingStrategy();

    private SkipProcessingStrategy() {}

    @JsonCreator
    public static SkipProcessingStrategy getInstance() {
        return INSTANCE;
    }

    @Override
    public boolean shouldProcess(long ts, UUID originatorUuid) {
        return false;
    }

}

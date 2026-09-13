// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.telemetry.strategy;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.UUID;

final class OnEveryMessageProcessingStrategy implements ProcessingStrategy {

    private static final OnEveryMessageProcessingStrategy INSTANCE = new OnEveryMessageProcessingStrategy();

    private OnEveryMessageProcessingStrategy() {}

    @JsonCreator
    public static OnEveryMessageProcessingStrategy getInstance() {
        return INSTANCE;
    }

    @Override
    public boolean shouldProcess(long ts, UUID originatorUuid) {
        return true;
    }

}

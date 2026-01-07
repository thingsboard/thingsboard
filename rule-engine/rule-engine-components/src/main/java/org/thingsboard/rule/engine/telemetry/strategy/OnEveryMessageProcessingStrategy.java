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

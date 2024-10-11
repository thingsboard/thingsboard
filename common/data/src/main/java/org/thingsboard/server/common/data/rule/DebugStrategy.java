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
package org.thingsboard.server.common.data.rule;

import lombok.Getter;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Getter
public enum DebugStrategy {
    DISABLED(0), ALL_EVENTS(1), ONLY_FAILURE_EVENTS(2);

    private final int protoNumber;

    DebugStrategy(int protoNumber) {
        this.protoNumber = protoNumber;
    }

    public boolean shouldPersistDebugInput(long lastUpdateTs, long msgTs, int debugModeDurationMinutes) {
        return isAllEventsStrategyAndMsgTsWithinDebugDuration(lastUpdateTs, msgTs, debugModeDurationMinutes);
    }

    public boolean shouldPersistDebugOutputForAllEvents(long lastUpdateTs, long msgTs, int debugModeDurationMinutes) {
        return isAllEventsStrategyAndMsgTsWithinDebugDuration(lastUpdateTs, msgTs, debugModeDurationMinutes);
    }

    public boolean shouldPersistDebugForFailureEventOnly(long lastUpdateTs, long msgTs, int debugModeDurationMinutes) {
        return shouldPersistDebugOutputForAllEvents(lastUpdateTs, msgTs, debugModeDurationMinutes) || isFailureOnlyStrategy();
    }

    public boolean shouldPersistDebugForFailureEventOnly(Set<String> nodeConnections) {
        return isFailureOnlyStrategy() && nodeConnections.contains(TbNodeConnectionType.FAILURE);
    }

    public boolean shouldPersistDebugForFailureEventOnly(String nodeConnection) {
        return isFailureOnlyStrategy() && TbNodeConnectionType.FAILURE.equals(nodeConnection);
    }

    private boolean isFailureOnlyStrategy() {
        return DebugStrategy.ONLY_FAILURE_EVENTS.equals(this);
    }

    private boolean isAllEventsStrategyAndMsgTsWithinDebugDuration(long lastUpdateTs, long msgTs, int debugModeDurationMinutes) {
        if (!DebugStrategy.ALL_EVENTS.equals(this)) {
            return false;
        }
        return isMsgTsWithinDebugDuration(lastUpdateTs, msgTs, debugModeDurationMinutes);
    }

    private boolean isMsgTsWithinDebugDuration(long lastUpdateTs, long msgCreationTs, int debugModeDurationMinutes) {
        if (debugModeDurationMinutes <= 0) {
            return true;
        }
        return msgCreationTs < lastUpdateTs + TimeUnit.MINUTES.toMillis(debugModeDurationMinutes);
    }

}

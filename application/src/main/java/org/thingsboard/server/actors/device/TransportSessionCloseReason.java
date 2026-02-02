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
package org.thingsboard.server.actors.device;

import lombok.Getter;

@Getter
public enum TransportSessionCloseReason {

    UNKNOWN_REASON(0, "Unknown Reason.", "Session closed with unknown reason."),
    CREDENTIALS_UPDATED(1, "device credentials updated!", "Close session due to device credentials update."),
    MAX_CONCURRENT_SESSIONS_LIMIT_REACHED(2, "max concurrent sessions limit reached per device!", "Remove eldest session (max concurrent sessions limit reached per device)."),
    SESSION_TIMEOUT(3, "session timeout!", "Close session due to session timeout."),
    RPC_DELIVERY_TIMEOUT(4, "RPC delivery failed!", "Close session due to RPC delivery failure.");

    private final int protoNumber;
    private final String notificationMessage;
    private final String logMessage;

    TransportSessionCloseReason(int protoNumber, String notificationMessage, String logMessage) {
        this.protoNumber = protoNumber;
        this.notificationMessage = notificationMessage;
        this.logMessage = logMessage;
    }

}

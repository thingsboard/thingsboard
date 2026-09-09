// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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

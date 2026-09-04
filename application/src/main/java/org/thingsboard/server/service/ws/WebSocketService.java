// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.ws;

import org.springframework.web.socket.CloseStatus;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.service.subscription.SubscriptionErrorCode;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.CmdUpdate;
import org.thingsboard.server.service.ws.telemetry.sub.TelemetrySubscriptionUpdate;

/**
 * Created by ashvayka on 27.03.18.
 */
public interface WebSocketService {

    void handleSessionEvent(WebSocketSessionRef sessionRef, SessionEvent sessionEvent);

    void handleCommands(WebSocketSessionRef sessionRef, WsCommandsWrapper commandsWrapper);

    void sendUpdate(String sessionId, int cmdId, TelemetrySubscriptionUpdate update);

    void sendUpdate(String sessionId, CmdUpdate update);

    void sendError(WebSocketSessionRef sessionRef, int subId, SubscriptionErrorCode errorCode, String errorMsg);

    void close(String sessionId, CloseStatus status);

    void cleanupIfStale(TenantId tenantId, String sessionId);

}

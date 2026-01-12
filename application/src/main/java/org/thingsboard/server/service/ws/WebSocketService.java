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

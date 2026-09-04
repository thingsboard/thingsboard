// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.ws;

import org.springframework.web.socket.CloseStatus;

import java.io.IOException;

/**
 * Created by ashvayka on 27.03.18.
 */
public interface WebSocketMsgEndpoint {

    void send(WebSocketSessionRef sessionRef, int subscriptionId, String msg) throws IOException;

    void sendPing(WebSocketSessionRef sessionRef, long currentTime) throws IOException;

    void close(WebSocketSessionRef sessionRef, CloseStatus withReason) throws IOException;

    boolean isOpen(String sessionId);
}

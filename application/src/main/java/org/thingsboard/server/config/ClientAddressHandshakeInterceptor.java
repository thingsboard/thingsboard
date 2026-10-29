// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.InetAddress;
import java.util.Map;

/**
 * Copies the client addresses captured by {@link ClientAddressCaptureFilter} on the WebSocket
 * handshake request into the WebSocket session attributes, so the session's authentication checks
 * can use the spoof-resistant resolved address instead of the forwarded-header-derived one.
 */
public class ClientAddressHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            copyAttribute(servletRequest, attributes, ClientAddressCaptureFilter.RESOLVED_CLIENT_ADDRESS_ATTR);
            copyAttribute(servletRequest, attributes, ClientAddressCaptureFilter.RAW_SOCKET_ADDRESS_ATTR);
            if (servletRequest.getServletRequest().getAttribute(ClientAddressCaptureFilter.CAPTURE_DONE_ATTR) != null) {
                attributes.put(ClientAddressCaptureFilter.CAPTURE_DONE_ATTR, Boolean.TRUE);
            }
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }

    private static void copyAttribute(ServletServerHttpRequest request, Map<String, Object> attributes, String name) {
        if (request.getServletRequest().getAttribute(name) instanceof InetAddress address) {
            attributes.put(name, address);
        }
    }

}

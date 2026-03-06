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
package org.thingsboard.server.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.auth.pat.ApiKeyAuthenticationProvider;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.Map;

@Slf4j
@Component
@TbCoreComponent
@RequiredArgsConstructor
public class ApiKeyHandshakeInterceptor implements HandshakeInterceptor {

    public static final String API_KEY_HEADER = "X-API-Key";
    public static final String API_KEY_SECURITY_CTX_ATTR = "apiKeySecurityCtx";

    private final ApiKeyAuthenticationProvider apiKeyAuthenticationProvider;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String apiKey = request.getHeaders().getFirst(API_KEY_HEADER);
        if (apiKey != null) {
            if (apiKey.isEmpty()) {
                log.debug("Empty API key provided during WS handshake");
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }
            try {
                SecurityUser securityUser = apiKeyAuthenticationProvider.authenticate(apiKey);
                attributes.put(API_KEY_SECURITY_CTX_ATTR, securityUser);
            } catch (Exception e) {
                log.debug("API key authentication failed during WS handshake: {}", e.getMessage());
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }

}

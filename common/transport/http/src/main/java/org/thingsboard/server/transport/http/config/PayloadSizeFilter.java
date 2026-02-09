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
package org.thingsboard.server.transport.http.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.msg.tools.MaxPayloadSizeExceededException;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class PayloadSizeFilter extends OncePerRequestFilter {

    private final Map<String, Long> limits = new LinkedHashMap<>();
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public PayloadSizeFilter(String limitsConfiguration) {
        for (String limit : limitsConfiguration.split(";")) {
            try {
                String urlPathPattern = limit.split("=")[0];
                long maxPayloadSize = Long.parseLong(limit.split("=")[1]);
                limits.put(urlPathPattern, maxPayloadSize);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse size limits configuration: " + limitsConfiguration);
            }
        }
        log.info("Initialized payload size filter with configuration: {}" , limitsConfiguration);
    }

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        for (String url : limits.keySet()) {
            if (pathMatcher.match(url, request.getRequestURI())) {
                if (checkMaxPayloadSizeExceeded(request, response, limits.get(url))) {
                    return;
                }
                break;
            }
        }
        chain.doFilter(request, response);
    }

    private boolean checkMaxPayloadSizeExceeded(HttpServletRequest request, HttpServletResponse response, long maxPayloadSize) throws IOException {
        if (request.getContentLength() > maxPayloadSize) {
            log.info("[{}] [{}] Payload size {} exceeds the limit of {} bytes", request.getRemoteAddr(), request.getRequestURL(), request.getContentLength(), maxPayloadSize);
            handleMaxPayloadSizeExceededException(response, new MaxPayloadSizeExceededException(maxPayloadSize));
            return true;
        }
        return false;
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }

    private void handleMaxPayloadSizeExceededException(HttpServletResponse response, MaxPayloadSizeExceededException exception) throws IOException {
        response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
        JacksonUtil.writeValue(response.getWriter(), exception.getMessage());
    }
}

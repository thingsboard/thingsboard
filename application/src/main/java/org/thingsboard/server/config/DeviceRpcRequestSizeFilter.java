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
package org.thingsboard.server.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.thingsboard.server.common.msg.tools.MaxPayloadSizeExceededException;
import org.thingsboard.server.exception.ThingsboardErrorResponseHandler;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceRpcRequestSizeFilter extends OncePerRequestFilter {

    private final List<String> urls = List.of("/api/plugins/rpc/**", "/api/rpc/**");
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final ThingsboardErrorResponseHandler errorResponseHandler;
    
    @Value("${transport.http.rpc_max_payload_size:65536}")
    private int rpcMaxPayloadSize;

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request.getContentLength() > rpcMaxPayloadSize) {
            if (log.isDebugEnabled()) {
                log.debug("Too large payload size. Url: {}, client ip: {}, content length: {}", request.getRequestURL(),
                        request.getRemoteAddr(), request.getContentLength());
            }
            errorResponseHandler.handle(new MaxPayloadSizeExceededException(), response);
            return;
        }
        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        for (String url : urls) {
            if (pathMatcher.match(url, request.getRequestURI())) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }
}

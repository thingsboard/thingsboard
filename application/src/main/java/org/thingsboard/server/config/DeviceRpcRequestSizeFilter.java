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
import org.thingsboard.server.common.msg.tools.TbMaxRequestSizeExceededException;
import org.thingsboard.server.exception.ThingsboardErrorResponseHandler;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceRpcRequestSizeFilter extends OncePerRequestFilter {

    private final Set<String> urls = new HashSet<>(Arrays.asList("/api/v1/*/rpc/*", "/api/v1/*/rpc"));
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final ThingsboardErrorResponseHandler errorResponseHandler;
    @Value("${transport.http.rpc_max_request_size:65536}")
    private int maxRequestSize;

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request.getContentLength() > maxRequestSize) {
            errorResponseHandler.handle(new TbMaxRequestSizeExceededException(), response);
            return;
        }
        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return urls.stream().noneMatch(url -> pathMatcher.match(url, request.getRequestURI()));
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

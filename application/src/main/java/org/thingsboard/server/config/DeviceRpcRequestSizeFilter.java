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

import com.amazonaws.HttpMethod;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.thingsboard.server.common.msg.tools.TbMaxPayloadSizeExceededException;
import org.thingsboard.server.exception.ThingsboardErrorResponseHandler;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceRpcRequestSizeFilter extends OncePerRequestFilter {

    private final RequestMatcher uriMatcher = new AntPathRequestMatcher("/api/v1/*/rpc/**", HttpMethod.POST.name());
    private final RequestMatcher matcher = new NegatedRequestMatcher(uriMatcher);
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
            errorResponseHandler.handle(new TbMaxPayloadSizeExceededException(), response);
            return;
        }
        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return matcher.matches(request);
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

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
package org.thingsboard.server.transport.http.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.msg.tools.MaxPayloadSizeExceededException;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class RequestSizeFilter extends OncePerRequestFilter {

    private final int maxPayloadSize;

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request.getContentLength() > maxPayloadSize) {
            if (log.isDebugEnabled()) {
                log.debug("Too large payload size. Url: {}, client ip: {}, content length: {}", request.getRequestURL(), request.getRemoteAddr(), request.getContentLength());
            }
            handleMaxPayloadSizeExceededException(response, new MaxPayloadSizeExceededException(maxPayloadSize));
            return;
        }
        chain.doFilter(request, response);
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

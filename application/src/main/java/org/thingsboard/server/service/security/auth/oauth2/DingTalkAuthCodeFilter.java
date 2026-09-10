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
package org.thingsboard.server.service.security.auth.oauth2;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationCodeGrantFilter;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Component
@TbCoreComponent
public class DingTalkAuthCodeFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authCode = request.getParameter("authCode");
        if (authCode != null && !authCode.isEmpty() && request.getParameter(OAuth2ParameterNames.CODE) == null) {
            HttpServletRequest wrappedRequest = new DingTalkAuthCodeRequestWrapper(request, authCode);
            filterChain.doFilter(wrappedRequest, response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static class DingTalkAuthCodeRequestWrapper extends HttpServletRequestWrapper {

        private final Map<String, String[]> modifiedParams;

        DingTalkAuthCodeRequestWrapper(HttpServletRequest request, String authCode) {
            super(request);
            this.modifiedParams = new HashMap<>(request.getParameterMap());
            this.modifiedParams.put(OAuth2ParameterNames.CODE, new String[]{authCode});
        }

        @Override
        public String getParameter(String name) {
            if (OAuth2ParameterNames.CODE.equals(name)) {
                return modifiedParams.get(OAuth2ParameterNames.CODE)[0];
            }
            return super.getParameter(name);
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            return Collections.unmodifiableMap(modifiedParams);
        }

        @Override
        public Enumeration<String> getParameterNames() {
            return Collections.enumeration(modifiedParams.keySet());
        }

        @Override
        public String[] getParameterValues(String name) {
            return modifiedParams.get(name);
        }
    }
}

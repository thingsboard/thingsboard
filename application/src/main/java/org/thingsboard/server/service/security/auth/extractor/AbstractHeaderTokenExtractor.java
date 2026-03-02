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
package org.thingsboard.server.service.security.auth.extractor;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.config.ThingsboardSecurityConfiguration;

public abstract class AbstractHeaderTokenExtractor implements TokenExtractor {

    private final String headerPrefix;

    protected AbstractHeaderTokenExtractor(String headerPrefix) {
        this.headerPrefix = headerPrefix;
    }

    @Override
    public String extract(HttpServletRequest request) {
        String header = request.getHeader(ThingsboardSecurityConfiguration.AUTHORIZATION_HEADER);
        if (StringUtils.isBlank(header)) {
            header = request.getHeader(ThingsboardSecurityConfiguration.AUTHORIZATION_HEADER_V2);
            if (StringUtils.isBlank(header)) {
                throw new AuthenticationServiceException("Authorization header cannot be blank!");
            }
        }

        if (header.length() < headerPrefix.length()) {
            throw new AuthenticationServiceException("Invalid authorization header size.");
        }

        return header.substring(headerPrefix.length());
    }

}

// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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

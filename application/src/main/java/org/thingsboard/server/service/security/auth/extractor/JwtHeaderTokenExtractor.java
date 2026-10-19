// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.security.auth.extractor;

import org.springframework.stereotype.Component;

import static org.thingsboard.server.config.ThingsboardSecurityConfiguration.BEARER_HEADER_PREFIX;

@Component(value = "jwtHeaderTokenExtractor")
public class JwtHeaderTokenExtractor extends AbstractHeaderTokenExtractor {

    public JwtHeaderTokenExtractor() {
        super(BEARER_HEADER_PREFIX);
    }

}

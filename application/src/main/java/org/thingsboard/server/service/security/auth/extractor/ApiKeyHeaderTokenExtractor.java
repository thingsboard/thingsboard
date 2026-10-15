// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.security.auth.extractor;

import org.springframework.stereotype.Component;

import static org.thingsboard.server.config.ThingsboardSecurityConfiguration.API_KEY_HEADER_PREFIX;

@Component(value = "apiKeyHeaderTokenExtractor")
public class ApiKeyHeaderTokenExtractor extends AbstractHeaderTokenExtractor {

    public ApiKeyHeaderTokenExtractor() {
        super(API_KEY_HEADER_PREFIX);
    }

}

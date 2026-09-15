// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.security.auth;

import org.thingsboard.server.service.security.model.SecurityUser;

public class MfaConfigurationToken extends AbstractJwtAuthenticationToken {
    public MfaConfigurationToken(SecurityUser securityUser) {
        super(securityUser);
    }
}

// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.security.auth;

import org.thingsboard.server.service.security.model.SecurityUser;

public class MfaAuthenticationToken extends AbstractJwtAuthenticationToken {
    public MfaAuthenticationToken(SecurityUser securityUser) {
        super(securityUser);
    }
}

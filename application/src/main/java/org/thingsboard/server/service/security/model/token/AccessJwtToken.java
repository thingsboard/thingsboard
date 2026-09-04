// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.security.model.token;

import org.thingsboard.server.common.data.security.model.JwtToken;

public final class AccessJwtToken implements JwtToken {
    private final String rawToken;

    public AccessJwtToken(String rawToken) {
        this.rawToken = rawToken;
    }

    public String getToken() {
        return this.rawToken;
    }

}

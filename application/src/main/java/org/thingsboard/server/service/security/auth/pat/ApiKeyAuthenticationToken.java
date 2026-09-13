// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.security.auth.pat;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.token.ApiKeyAuthRequest;

import java.io.Serial;

public class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {

    @Serial
    private static final long serialVersionUID = 2978710889397403536L;

    private ApiKeyAuthRequest apiKeyAuthRequest;
    private SecurityUser securityUser;

    public ApiKeyAuthenticationToken(ApiKeyAuthRequest apiKeyAuthRequest) {
        super(null);
        this.apiKeyAuthRequest = apiKeyAuthRequest;
        setAuthenticated(false);
    }

    public ApiKeyAuthenticationToken(SecurityUser securityUser) {
        super(securityUser.getAuthorities());
        this.eraseCredentials();
        this.securityUser = securityUser;
        super.setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return apiKeyAuthRequest;
    }

    @Override
    public Object getPrincipal() {
        return this.securityUser;
    }

    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
        this.apiKeyAuthRequest = null;
    }

}

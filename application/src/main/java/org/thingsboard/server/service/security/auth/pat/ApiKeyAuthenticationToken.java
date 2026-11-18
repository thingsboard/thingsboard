// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.security.auth.pat;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.token.ApiKeyAuthRequest;

import java.io.Serial;
import java.util.Collection;

public class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {

    @Serial
    private static final long serialVersionUID = 2978710889397403536L;

    private ApiKeyAuthRequest apiKeyAuthRequest;
    private SecurityUser securityUser;

    public ApiKeyAuthenticationToken(ApiKeyAuthRequest apiKeyAuthRequest) {
        super((Collection<? extends GrantedAuthority>) null);
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

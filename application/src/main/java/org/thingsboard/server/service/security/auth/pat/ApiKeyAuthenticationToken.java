// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.security.auth.pat;

import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.thingsboard.server.common.data.id.ApiKeyId;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.token.ApiKeyAuthRequest;

import java.io.Serial;

public class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {

    @Serial
    private static final long serialVersionUID = 2978710889397403536L;

    private ApiKeyAuthRequest apiKeyAuthRequest;
    private SecurityUser securityUser;
    // The key the request authenticated with; the principal alone cannot tell one key of a user from another.
    @Getter
    private ApiKeyId apiKeyId;

    public ApiKeyAuthenticationToken(ApiKeyAuthRequest apiKeyAuthRequest) {
        super(null);
        this.apiKeyAuthRequest = apiKeyAuthRequest;
        setAuthenticated(false);
    }

    public ApiKeyAuthenticationToken(SecurityUser securityUser, ApiKeyId apiKeyId) {
        super(securityUser.getAuthorities());
        this.eraseCredentials();
        this.securityUser = securityUser;
        this.apiKeyId = apiKeyId;
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

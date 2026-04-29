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

package org.thingsboard.server.service.security.auth;

import org.thingsboard.server.service.security.model.SecurityUser;

public class MfaSaveSettingsAuthenticationToken extends AbstractJwtAuthenticationToken {
    public MfaSaveSettingsAuthenticationToken(SecurityUser securityUser) {
        super(securityUser);
    }
}

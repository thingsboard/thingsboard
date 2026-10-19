// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.mobile.secret;

import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.security.model.JwtPair;
import org.thingsboard.server.service.security.model.SecurityUser;

public interface MobileAppSecretService {

    String generateMobileAppSecret(SecurityUser securityUser);

    JwtPair getJwtPair(String secret) throws ThingsboardException;

}

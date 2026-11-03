// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.security.auth.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationDetailsSource;

public class RestAuthenticationDetailsSource implements
        AuthenticationDetailsSource<HttpServletRequest, RestAuthenticationDetails> {

    public RestAuthenticationDetails buildDetails(HttpServletRequest context) {
        return new RestAuthenticationDetails(context);
    }
}

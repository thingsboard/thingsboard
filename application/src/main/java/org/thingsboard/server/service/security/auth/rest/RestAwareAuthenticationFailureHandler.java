// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.security.auth.rest;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.thingsboard.server.exception.ThingsboardErrorResponseHandler;

import java.io.IOException;

@Component(value = "defaultAuthenticationFailureHandler")
public class RestAwareAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final ThingsboardErrorResponseHandler errorResponseHandler;

    @Autowired
    public RestAwareAuthenticationFailureHandler(ThingsboardErrorResponseHandler errorResponseHandler) {
        this.errorResponseHandler = errorResponseHandler;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException e) throws IOException, ServletException {
        errorResponseHandler.handle(e, response);
    }
}

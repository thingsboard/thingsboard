// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.security.exception;

import org.springframework.security.authentication.CredentialsExpiredException;

public class UserPasswordExpiredException extends CredentialsExpiredException {

    private final String resetToken;

    public UserPasswordExpiredException(String msg, String resetToken) {
        super(msg);
        this.resetToken = resetToken;
    }

    public String getResetToken() {
        return resetToken;
    }

}

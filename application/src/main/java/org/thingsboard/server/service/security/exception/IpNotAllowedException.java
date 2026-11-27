// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.security.exception;

import org.springframework.security.core.AuthenticationException;

/**
 * Thrown when a System Administrator authentication attempt or request comes from an address
 * outside the configured sysadmin IP allowlist.
 */
public class IpNotAllowedException extends AuthenticationException {

    public static final String MESSAGE = "Access from your network location is not permitted";

    public IpNotAllowedException() {
        super(MESSAGE);
    }

}

// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.exception;

public class IntegrationConnectionException extends RuntimeException {

    private static final long serialVersionUID = -4372729481230555723L;

    public IntegrationConnectionException(String message) {
        super(message);
    }

    public IntegrationConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}

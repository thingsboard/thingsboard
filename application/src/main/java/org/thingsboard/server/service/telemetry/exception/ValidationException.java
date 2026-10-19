// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.telemetry.exception;

import org.thingsboard.server.service.security.ValidationResultCode;

public abstract class ValidationException extends Exception {

    public ValidationException(String message) {
        super(message);
    }

    public abstract ValidationResultCode getValidationResultCode();

}

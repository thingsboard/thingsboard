// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.exception;

import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;

public class ThingsboardRuntimeException extends RuntimeException {

    private ThingsboardErrorCode errorCode;

    public ThingsboardRuntimeException() {
        super();
    }

    public ThingsboardRuntimeException(ThingsboardErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    public ThingsboardRuntimeException(String message, ThingsboardErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public ThingsboardRuntimeException(String message, Throwable cause, ThingsboardErrorCode errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ThingsboardRuntimeException(Throwable cause, ThingsboardErrorCode errorCode) {
        super(cause);
        this.errorCode = errorCode;
    }

    public ThingsboardErrorCode getErrorCode() {
        return errorCode;
    }

}

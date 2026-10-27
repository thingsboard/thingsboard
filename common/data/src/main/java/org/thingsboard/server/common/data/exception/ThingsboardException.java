// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.exception;

public class ThingsboardException extends Exception {

    private static final long serialVersionUID = 1L;

    private ThingsboardErrorCode errorCode;

    public ThingsboardException() {
        super();
    }

    public ThingsboardException(ThingsboardErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    public ThingsboardException(String message, ThingsboardErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public ThingsboardException(String message, Throwable cause, ThingsboardErrorCode errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ThingsboardException(Throwable cause, ThingsboardErrorCode errorCode) {
        super(cause);
        this.errorCode = errorCode;
    }

    public ThingsboardErrorCode getErrorCode() {
        return errorCode;
    }

}

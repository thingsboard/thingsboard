// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.api.sms.exception;

public abstract class SmsException extends RuntimeException {

    public SmsException(String msg) {
        super(msg);
    }

    public SmsException(String msg, Throwable cause) {
        super(msg, cause);
    }

}

// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.api.sms.exception;

public class SmsSendException extends SmsException {

    public SmsSendException(String msg) {
        super(msg);
    }

    public SmsSendException(String msg, Throwable cause) {
        super(msg, cause);
    }
}

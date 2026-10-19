// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.api.sms.exception;

public class SmsParseException extends SmsException {

    public SmsParseException(String msg) {
        super(msg);
    }

    public SmsParseException(String msg, Throwable cause) {
        super(msg, cause);
    }

}

// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.api.sms;

import org.thingsboard.rule.engine.api.sms.exception.SmsException;

public interface SmsSender {

    int sendSms(String numberTo, String message) throws SmsException;

    void destroy();

}

// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.api.sms;

import org.thingsboard.server.common.data.sms.config.SmsProviderConfiguration;

public interface SmsSenderFactory {

    SmsSender createSmsSender(SmsProviderConfiguration config);

}

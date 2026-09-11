// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sms;

import org.springframework.stereotype.Component;
import org.thingsboard.rule.engine.api.sms.SmsSender;
import org.thingsboard.rule.engine.api.sms.SmsSenderFactory;
import org.thingsboard.server.common.data.sms.config.AwsSnsSmsProviderConfiguration;
import org.thingsboard.server.common.data.sms.config.SmppSmsProviderConfiguration;
import org.thingsboard.server.common.data.sms.config.SmsProviderConfiguration;
import org.thingsboard.server.common.data.sms.config.TwilioSmsProviderConfiguration;
import org.thingsboard.server.service.sms.aws.AwsSmsSender;
import org.thingsboard.server.service.sms.smpp.SmppSmsSender;
import org.thingsboard.server.service.sms.twilio.TwilioSmsSender;

@Component
public class DefaultSmsSenderFactory implements SmsSenderFactory {

    @Override
    public SmsSender createSmsSender(SmsProviderConfiguration config) {
        return switch (config.getType()) {
            case AWS_SNS -> new AwsSmsSender((AwsSnsSmsProviderConfiguration) config);
            case TWILIO -> new TwilioSmsSender((TwilioSmsProviderConfiguration) config);
            case SMPP -> new SmppSmsSender((SmppSmsProviderConfiguration) config);
            default -> throw new RuntimeException("Unknown SMS provider type " + config.getType());
        };
    }

}

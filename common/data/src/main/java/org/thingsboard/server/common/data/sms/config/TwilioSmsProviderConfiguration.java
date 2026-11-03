// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.sms.config;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema
@Data
public class TwilioSmsProviderConfiguration implements SmsProviderConfiguration {

    @Schema(description = "Twilio account Sid.")
    private String accountSid;
    @Schema(description = "Twilio account Token.")
    private String accountToken;
    @Schema(description = "The number/id of a sender.")
    private String numberFrom;

    @Override
    public SmsProviderType getType() {
        return SmsProviderType.TWILIO;
    }

}

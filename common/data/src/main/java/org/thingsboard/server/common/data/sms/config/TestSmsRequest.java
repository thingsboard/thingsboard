// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.sms.config;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema
@Data
public class TestSmsRequest {

    @Schema(description = "The SMS provider configuration")
    private SmsProviderConfiguration providerConfiguration;
    @Schema(description = "The phone number or other identifier to specify as a recipient of the SMS.")
    private String numberTo;
    @Schema(description = "The test message")
    private String message;

}

// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.sms.config;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema
@Data
public class AwsSnsSmsProviderConfiguration implements SmsProviderConfiguration {

    @Schema(description = "The AWS SNS Access Key ID.")
    private String accessKeyId;
    @Schema(description = "The AWS SNS Access Key.")
    private String secretAccessKey;
    @Schema(description = "The AWS region.")
    private String region;

    @Override
    public SmsProviderType getType() {
        return SmsProviderType.AWS_SNS;
    }

}

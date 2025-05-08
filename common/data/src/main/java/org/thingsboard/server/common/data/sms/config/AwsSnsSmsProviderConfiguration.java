/**
 * Copyright © 2016-2025 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

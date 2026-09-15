// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.ai.provider;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema
public record AmazonBedrockProviderConfig(
        @NotNull String region,
        @NotNull String accessKeyId,
        @NotNull String secretAccessKey
) implements AiProviderConfig {}

// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.ai.provider;

import jakarta.validation.constraints.NotNull;

public record AmazonBedrockProviderConfig(
        @NotNull String region,
        @NotNull String accessKeyId,
        @NotNull String secretAccessKey
) implements AiProviderConfig {}

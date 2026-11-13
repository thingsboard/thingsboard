// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.ai.provider;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
@Schema
public record GoogleVertexAiGeminiProviderConfig(
        String fileName, // not used on BE, but needed for UI; has to be nullable in PE since can be null if secrets are used
        @NotNull String projectId,
        @NotNull String location,
        @NotNull String serviceAccountKey
) implements AiProviderConfig {}

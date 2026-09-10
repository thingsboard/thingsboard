// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.ai.provider;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema
public record GoogleVertexAiGeminiProviderConfig(
        @NotBlank String fileName, // not used on BE, but needed for UI
        @NotNull String projectId,
        @NotNull String location,
        @NotNull String serviceAccountKey
) implements AiProviderConfig {}

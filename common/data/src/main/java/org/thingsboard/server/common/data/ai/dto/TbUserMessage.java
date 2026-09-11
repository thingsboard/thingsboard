// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record TbUserMessage(
        @NotEmpty
        @Valid
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                description = "A list of content parts that make up the complete user prompt"
        )
        List<TbContent> contents
) {}

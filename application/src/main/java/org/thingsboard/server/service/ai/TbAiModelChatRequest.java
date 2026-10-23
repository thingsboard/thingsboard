// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.thingsboard.rule.engine.ai.TbResponseFormat;
import org.thingsboard.server.common.data.ai.dto.TbUserMessage;

public record TbAiModelChatRequest(
        @Schema(
                requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                description = "A system-level instruction that frames the user's input, setting the persona, tone, and constraints for the generated response",
                example = "You are a helpful assistant. Only output valid JSON."
        )
        String systemMessage,

        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                description = "The actual user prompt that will be answered by the AI model"
        )
        @NotNull @Valid
        TbUserMessage userMessage,

        @Schema(
                requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                description = "Format of the generated response; plain text when omitted"
        )
        @Valid
        TbResponseFormat responseFormat
) {}

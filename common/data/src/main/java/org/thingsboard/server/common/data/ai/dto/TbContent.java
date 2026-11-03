// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.ai.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.TextContent;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import static org.thingsboard.server.common.data.ai.dto.TbContent.TbTextContent;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "contentType",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TbTextContent.class, name = "TEXT")
})
public sealed interface TbContent permits TbTextContent {

    TbContentType contentType();

    Content toLangChainContent();

    enum TbContentType {

        TEXT

    }

    @Schema(
            description = "Text-based content part of a user's prompt"
    )
    record TbTextContent(
            @NotBlank
            @Schema(
                    requiredMode = Schema.RequiredMode.REQUIRED,
                    description = "The text content",
                    example = "What is the weather like in Kyiv today?"
            )
            String text
    ) implements TbContent {

        @Override
        public TbContentType contentType() {
            return TbContentType.TEXT;
        }

        @Override
        public Content toLangChainContent() {
            return TextContent.from(text);
        }

    }

}

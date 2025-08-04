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
package org.thingsboard.server.common.data.ai.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.TextContent;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import static org.thingsboard.server.common.data.ai.dto.TbContent.TbTextContent;
import static org.thingsboard.server.common.data.ai.dto.TbContent.TbScriptContent;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "contentType",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TbTextContent.class, name = "TEXT"),
        @JsonSubTypes.Type(value = TbScriptContent.class, name = "SCRIPT")
})
public sealed interface TbContent permits TbTextContent, TbScriptContent {

    TbContentType contentType();

    Content toLangChainContent();

    static TbTextContent ofText(String text) {
        return new TbTextContent(text);
    }

    static TbScriptContent ofScript(String text) {
        return new TbScriptContent(text);
    }

    enum TbContentType {
        TEXT,
        SCRIPT
    }

    @Schema(description = "Text-based content part of a user's prompt")
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

    @Schema(description = "TBEL script content part of a user's prompt")
    record TbScriptContent(
            @NotBlank
            @Schema(
                    requiredMode = Schema.RequiredMode.REQUIRED,
                    description = "The TBEL script content",
                    example = "msg.rt = 68; return {msg: msg, metadata: metadata, msgType: msgType};"
            )
            String script
    ) implements TbContent {

        @Override
        public TbContentType contentType() {
            return TbContentType.SCRIPT;
        }

        @Override
        public Content toLangChainContent() {
            return TextContent.from(script);
        }
    }
}

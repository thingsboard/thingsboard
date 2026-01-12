/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.rule.engine.ai;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import jakarta.validation.constraints.NotNull;
import org.thingsboard.server.common.data.validation.ValidJsonSchema;

import static org.thingsboard.rule.engine.ai.TbResponseFormat.TbJsonResponseFormat;
import static org.thingsboard.rule.engine.ai.TbResponseFormat.TbJsonSchemaResponseFormat;
import static org.thingsboard.rule.engine.ai.TbResponseFormat.TbTextResponseFormat;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TbTextResponseFormat.class, name = "TEXT"),
        @JsonSubTypes.Type(value = TbJsonResponseFormat.class, name = "JSON"),
        @JsonSubTypes.Type(value = TbJsonSchemaResponseFormat.class, name = "JSON_SCHEMA")
})
public sealed interface TbResponseFormat permits TbTextResponseFormat, TbJsonResponseFormat, TbJsonSchemaResponseFormat {

    TbResponseFormatType type();

    ResponseFormat toLangChainResponseFormat();

    enum TbResponseFormatType {

        TEXT,
        JSON,
        JSON_SCHEMA

    }

    record TbTextResponseFormat() implements TbResponseFormat {

        @Override
        public TbResponseFormatType type() {
            return TbResponseFormatType.TEXT;
        }

        @Override
        public ResponseFormat toLangChainResponseFormat() {
            return ResponseFormat.TEXT;
        }

    }

    record TbJsonResponseFormat() implements TbResponseFormat {

        @Override
        public TbResponseFormatType type() {
            return TbResponseFormatType.JSON;
        }

        @Override
        public ResponseFormat toLangChainResponseFormat() {
            return ResponseFormat.JSON;
        }

    }

    record TbJsonSchemaResponseFormat(@NotNull @ValidJsonSchema ObjectNode schema) implements TbResponseFormat {

        @Override
        public TbResponseFormatType type() {
            return TbResponseFormatType.JSON_SCHEMA;
        }

        @Override
        public ResponseFormat toLangChainResponseFormat() {
            return ResponseFormat.builder()
                    .type(ResponseFormatType.JSON)
                    .jsonSchema(Langchain4jJsonSchemaAdapter.fromObjectNode(schema))
                    .build();
        }

    }

}

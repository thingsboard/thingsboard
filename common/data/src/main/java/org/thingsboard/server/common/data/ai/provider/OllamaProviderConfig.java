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
package org.thingsboard.server.common.data.ai.provider;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record OllamaProviderConfig(
        @NotNull String baseUrl,
        @NotNull @Valid OllamaAuth auth
) implements AiProviderConfig {

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.PROPERTY,
            property = "type"
    )
    @JsonSubTypes({
            @JsonSubTypes.Type(value = OllamaAuth.None.class, name = "NONE"),
            @JsonSubTypes.Type(value = OllamaAuth.Basic.class, name = "BASIC"),
            @JsonSubTypes.Type(value = OllamaAuth.Token.class, name = "TOKEN")
    })
    public sealed interface OllamaAuth {

        record None() implements OllamaAuth {}

        record Basic(@NotNull String username, @NotNull String password) implements OllamaAuth {}

        record Token(@NotNull String token) implements OllamaAuth {}

    }

}

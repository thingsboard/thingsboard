// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.ai.provider;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Schema
public record OllamaProviderConfig(
        @NotNull String baseUrl,
        @NotNull @Valid OllamaAuth auth
) implements AiProviderConfig {

    @Schema(
            description = "Ollama authentication schemes",
            discriminatorProperty = "type",
            discriminatorMapping = {
                    @DiscriminatorMapping(value = "NONE", schema = OllamaAuth.None.class),
                    @DiscriminatorMapping(value = "BASIC", schema = OllamaAuth.Basic.class),
                    @DiscriminatorMapping(value = "TOKEN", schema = OllamaAuth.Token.class)
            }
    )
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

// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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

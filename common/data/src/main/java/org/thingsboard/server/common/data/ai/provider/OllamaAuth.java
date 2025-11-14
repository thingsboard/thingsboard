package org.thingsboard.server.common.data.ai.provider;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotNull;

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

// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.codec.ServerSentEvent;
import org.thingsboard.ai.common.client.TbAiClient;
import org.thingsboard.ai.common.data.usage.ApiUsageInfo;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.ai.transport.TbAiTurnLink;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface TbAiService {

    default JsonNode process(TbAiCall<TbAiClient.TbAiResponse> action, SecurityUser user) {
        return process(action, user, true);
    }

    JsonNode process(TbAiCall<TbAiClient.TbAiResponse> action, SecurityUser user, boolean checkCredits);

    default Flux<ServerSentEvent<String>> processStream(UUID chatId, TbAiCall<Flux<ServerSentEvent<String>>> action, SecurityUser user) {
        return processStream(chatId, action, user, null, true);
    }

    /**
     * Streams a chat turn to the browser. {@code link}, when given, is told when the browser stops listening, which
     * ThingsBoard itself never does before the turn ends. {@code checkCredits} is false for following a turn that has
     * already been paid for.
     */
    Flux<ServerSentEvent<String>> processStream(UUID chatId, TbAiCall<Flux<ServerSentEvent<String>>> action, SecurityUser user,
                                                TbAiTurnLink link, boolean checkCredits);

    boolean isEnabled();

    ApiUsageInfo getApiUsageInfo(SecurityUser user);

    @FunctionalInterface
    interface TbAiCall<T> {

        T apply(TbAiClient.TokenProvider tokenProvider);

    }

}

// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.codec.ServerSentEvent;
import org.thingsboard.ai.common.client.TbAiClient;
import org.thingsboard.ai.common.data.usage.ApiUsageInfo;
import org.thingsboard.server.service.security.model.SecurityUser;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface TbAiService {

    default JsonNode process(TbAiCall<TbAiClient.TbAiResponse> action, SecurityUser user) {
        return process(action, user, true);
    }

    JsonNode process(TbAiCall<TbAiClient.TbAiResponse> action, SecurityUser user, boolean checkCredits);

    Flux<ServerSentEvent<String>> processStream(UUID chatId, TbAiCall<Flux<ServerSentEvent<String>>> action, SecurityUser user);

    boolean isEnabled();

    ApiUsageInfo getApiUsageInfo(SecurityUser user);

    @FunctionalInterface
    interface TbAiCall<T> {

        T apply(TbAiClient client, TbAiClient.TokenProvider tokenProvider);

    }

}

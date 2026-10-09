// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.transport;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.ai.common.client.TbAiClient;
import org.thingsboard.ai.common.client.TbAiClient.TbAiResponse;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;

@Slf4j
@Component
@TbCoreComponent
@RequiredArgsConstructor
public class TbAiOperations {

    private final ChannelTbAiOperations channelOperations;
    private final TbAiClientRequestFactory clientRequestFactory;

    /**
     * For a call made on the user's behalf with no browser request at hand and no callback into ThingsBoard.
     */
    public TbAiResponse execute(TbAiOperation operation, SecurityUser user, TbAiClient.TokenProvider tokenProvider) {
        return execute(operation, new TbAiTurnContext(user, null, null, tokenProvider, clientRequestFactory.forUser(user)));
    }

    public TbAiResponse execute(TbAiOperation operation, TbAiTurnContext context) {
        try {
            return channelOperations.execute(operation, context);
        } catch (TbAiChannelUnavailableException e) {
            log.warn("[{}][{}] AI channel unavailable for '{}': {}", context.tenantId(), context.userId(), operation.type(), e.getMessage());
            return ChannelTbAiOperations.failure("Service unavailable");
        } catch (TbAiOperationsUnsupportedException e) {
            log.warn("[{}][{}] {}", context.tenantId(), context.userId(), e.getMessage());
            return ChannelTbAiOperations.failure("The AI service does not support this ThingsBoard version");
        } catch (TbAiChannelRejectedException e) {
            return ChannelTbAiOperations.failure(e.getMessage());
        }
    }

}

// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.transport;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.ai.common.client.TbAiClient.TbAiResponse;
import org.thingsboard.server.queue.util.TbCoreComponent;

@Slf4j
@Component
@TbCoreComponent
@RequiredArgsConstructor
public class TbAiOperations {

    private final ChannelTbAiOperations channelOperations;
    private final TbAiChannelAvailability availability;
    private final TbAiCallbackOriginValidator callbackOriginValidator;

    public TbAiResponse execute(TbAiOperation operation, TbAiTurnContext context) {
        if (!availability.isUsable()) {
            return executeOverHttp(operation, context);
        }
        try {
            return channelOperations.execute(operation, context);
        } catch (TbAiChannelUnavailableException e) {
            log.warn("AI channel unavailable, using HTTP for '{}' and backing off for {}: {}",
                    operation.type(), availability.fallbackBackoff(), e.getMessage());
            availability.markUnavailable();
            return executeOverHttp(operation, context);
        } catch (TbAiOperationsUnsupportedException e) {
            log.debug("TB AI has no channel operations, using HTTP for '{}'", operation.type());
            return executeOverHttp(operation, context);
        }
    }

    private TbAiResponse executeOverHttp(TbAiOperation operation, TbAiTurnContext context) {
        callbackOriginValidator.validate(context.user());
        return operation.httpCall().get();
    }

}

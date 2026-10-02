// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.tool;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.ai.common.channel.ChannelFrame;
import org.thingsboard.ai.common.channel.ChannelProtocol;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.ai.TbAiService;
import org.thingsboard.server.service.ai.transport.TbAiChannelRegistry;
import org.thingsboard.server.service.ai.transport.TbAiOperation;
import org.thingsboard.server.service.ai.transport.TbAiOperations;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@TbCoreComponent
@RequiredArgsConstructor
class DefaultAiToolService implements AiToolService {

    private static final Duration CHANNEL_APPROVAL_TIMEOUT = Duration.ofSeconds(10);

    private final TbAiService tbAiService;
    private final TbAiChannelRegistry channelRegistry;
    private final TbAiOperations operations;

    @Override
    public JsonNode resolveToolApproval(JsonNode decision, SecurityUser user) {
        Optional<TbAiChannelRegistry.ApprovalRoute> route = executionId(decision).flatMap(channelRegistry::findApprovalRoute);
        if (route.isPresent()) {
            JsonNode status = resolveOverChannel(route.get(), decision);
            if (status != null) {
                return status;
            }
        }
        // The turn runs on another tb-core node: TB AI resolves the approval by its execution id.
        return tbAiService.process(tokenProvider -> operations.execute(
                new TbAiOperation(ChannelProtocol.TOOL_APPROVAL_RESOLVE, decision), user, tokenProvider), user, false);
    }

    private JsonNode resolveOverChannel(TbAiChannelRegistry.ApprovalRoute route, JsonNode decision) {
        try {
            ChannelFrame request = ChannelFrame.request(ChannelProtocol.TOOL_APPROVAL, JacksonUtil.toString(decision)).withOp(route.op());
            ChannelFrame reply = route.session().request(request, CHANNEL_APPROVAL_TIMEOUT)
                    .get(CHANNEL_APPROVAL_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            return JacksonUtil.toJsonNode(reply.payload());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            log.warn("Failed to resolve tool approval on the turn's channel, resolving it by execution id: {}", e.getMessage());
            return null;
        }
    }

    private static Optional<UUID> executionId(JsonNode decision) {
        JsonNode executionId = decision != null ? decision.get("executionId") : null;
        if (executionId == null || !executionId.isTextual()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(executionId.asText()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

}

// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.tool;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.ai.common.channel.ChannelProtocol;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.ai.TbAiService;
import org.thingsboard.server.service.ai.transport.TbAiOperation;
import org.thingsboard.server.service.ai.transport.TbAiOperations;
import org.thingsboard.server.service.security.model.SecurityUser;

@Service
@TbCoreComponent
@RequiredArgsConstructor
class DefaultAiToolService implements AiToolService {

    private final TbAiService tbAiService;
    private final TbAiOperations operations;

    @Override
    public JsonNode resolveToolApproval(JsonNode decision, SecurityUser user) {
        return tbAiService.process(tokenProvider -> operations.execute(
                new TbAiOperation(ChannelProtocol.TOOL_APPROVAL_RESOLVE, decision), user, tokenProvider), user, false);
    }

}

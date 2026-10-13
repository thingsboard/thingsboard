// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.tool;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.ai.TbAiService;
import org.thingsboard.server.service.security.model.SecurityUser;

@Service
@TbCoreComponent
@RequiredArgsConstructor
class DefaultAiToolService implements AiToolService {

    private final TbAiService tbAiService;

    @Override
    public JsonNode resolveToolApproval(JsonNode decision, SecurityUser user) {
        return tbAiService.process((client, tokenProvider) -> {
            return client.resolveToolApproval(decision, tokenProvider);
        }, user, false);
    }

}

// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.ai.tool.AiToolService;
import org.thingsboard.server.service.security.model.SecurityUser;

@RequiredArgsConstructor
@RestController
@TbCoreComponent
@RequestMapping("/api/ai/tools")
class AiToolController extends BaseController {

    private final AiToolService aiToolService;

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/resolve-approval")
    public JsonNode resolveToolApproval(@RequestBody JsonNode decision) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.AI, Operation.WRITE);
        return aiToolService.resolveToolApproval(decision, user);
    }

}

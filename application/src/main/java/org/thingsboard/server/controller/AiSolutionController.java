// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.ai.common.data.solution.SolutionStep;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.ai.solution.AiSolutionService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.UUID;

import static org.thingsboard.server.config.ThingsboardSecurityConfiguration.AUTHORIZATION_HEADER;

@RequiredArgsConstructor
@RestController
@TbCoreComponent
@RequestMapping("/api/ai/solution")
public class AiSolutionController extends BaseController {

    private final AiSolutionService aiSolutionService;

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/start")
    public JsonNode startNew() throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.AI, Operation.WRITE);
        return aiSolutionService.startNew(user);
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/{solutionId}")
    public JsonNode getSolution(@PathVariable UUID solutionId) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.AI, Operation.READ);
        return aiSolutionService.getSolution(solutionId, user);
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/infos")
    public JsonNode getSolutions() throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.AI, Operation.READ);
        return aiSolutionService.getSolutions(user);
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/{solutionId}/{step}/chat")
    public JsonNode chat(@PathVariable UUID solutionId,
                         @PathVariable SolutionStep step,
                         @RequestBody String message) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.AI, Operation.WRITE);
        return aiSolutionService.chat(solutionId, step, message, user);
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/{solutionId}/create")
    public JsonNode createSolution(@PathVariable UUID solutionId) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.AI, Operation.WRITE);
        return aiSolutionService.createSolution(solutionId, user);
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PutMapping("/{solutionId}/{dataKey}")
    public JsonNode updateData(@PathVariable UUID solutionId,
                               @PathVariable String dataKey,
                               @RequestBody JsonNode value) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.AI, Operation.WRITE);
        return aiSolutionService.updateData(solutionId, dataKey, value, user);
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping("/{solutionId}/{step}/clear")
    public void clearStep(@PathVariable UUID solutionId, @PathVariable SolutionStep step) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.AI, Operation.WRITE);
        aiSolutionService.clearStep(solutionId, step, user);
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/{solutionId}/install")
    public JsonNode installSolution(@PathVariable UUID solutionId,
                                    @RequestHeader(AUTHORIZATION_HEADER) String tbAccessToken) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.AI, Operation.WRITE);
        return aiSolutionService.installSolution(solutionId, tbAccessToken, user);
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping("/{solutionId}/uninstall")
    public JsonNode uninstallSolution(@PathVariable UUID solutionId,
                                      @RequestHeader(AUTHORIZATION_HEADER) String tbAccessToken) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.AI, Operation.WRITE);
        return aiSolutionService.uninstallSolution(solutionId, tbAccessToken, user);
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping("/{solutionId}")
    public void deleteSolution(@PathVariable UUID solutionId) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.AI, Operation.WRITE);
        aiSolutionService.deleteSolution(solutionId, user);
    }

}

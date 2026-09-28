// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.sync.solution.SolutionData;
import org.thingsboard.server.common.data.sync.solution.SolutionExportRequest;
import org.thingsboard.server.common.data.sync.solution.SolutionExportResponse;
import org.thingsboard.server.common.data.sync.solution.SolutionImportResult;
import org.thingsboard.server.common.data.sync.solution.SolutionValidationResult;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.sync.solution.SolutionExportImportService;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class SolutionExportImportController extends BaseController {

    private final SolutionExportImportService solutionService;

    @ApiOperation(value = "Export Solution (exportSolution)",
            notes = "Exports a set of entities as a portable solution package. " +
                    "The request specifies entities to include via 'internalIds' (server-internal UUIDs) and/or " +
                    "'externalIds' (looked up by the entity's stored externalId within the current tenant); " +
                    "at least one of the two collections must be non-empty, and entities reached via both sides " +
                    "are deduplicated. Optional export settings control inclusion of relations, attributes, and credentials. " +
                    "All resolved entities must belong to the current tenant. " +
                    "The response contains the solution data (entities grouped by type) and any dependency warnings " +
                    "(e.g. when an exported device profile references a rule chain that was not included in the export). " +
                    "The solution data can later be imported into the same or a different tenant via the import endpoint.\n\n" +
                    "Available for users with 'TENANT_ADMIN' authority. " +
                    "Requires VERSION_CONTROL WRITE permission.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/solution/export")
    public SolutionExportResponse exportSolution(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Export request with internal and/or external entity IDs and optional settings.")
                                                 @Valid @RequestBody SolutionExportRequest request) throws Exception {
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.VERSION_CONTROL, Operation.WRITE);
        return solutionService.exportSolution(user, request);
    }

    @ApiOperation(value = "Import Solution (importSolution)",
            notes = "Imports a solution package into the current tenant. " +
                    "Before importing, the endpoint checks for name conflicts " +
                    "with existing entities in the tenant. If name conflicts are detected, the import is rejected with HTTP 409 (Conflict). " +
                    "The import is transactional — if any entity fails to import, all changes are rolled back (all-or-nothing). " +
                    "Entities are imported in dependency order with a two-pass resolution for circular references " +
                    "(e.g. rule chains referencing each other).\n\n" +
                    "Available for users with 'TENANT_ADMIN' authority. " +
                    "Requires VERSION_CONTROL WRITE permission.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/solution/import")
    public SolutionImportResult importSolution(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Solution data exported via the export endpoint.")
                                               @Valid @RequestBody SolutionData solutionData) throws Exception {
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.VERSION_CONTROL, Operation.WRITE);
        return solutionService.importSolution(user, solutionData);
    }

    @ApiOperation(value = "Validate Solution (validateSolution)",
            notes = "Performs a dry-run validation of a solution without modifying any data. " +
                    "Detects duplicate entities within the solution, " +
                    "identifies name conflicts with existing entities in the current tenant, " +
                    "and reports missing dependency references (e.g. a device profile referencing an absent rule chain). " +
                    "The result indicates whether the solution is safe to import (valid=true) and lists any conflicts or warnings.\n\n" +
                    "Available for users with 'TENANT_ADMIN' authority. " +
                    "Requires VERSION_CONTROL WRITE permission.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/solution/validate")
    public SolutionValidationResult validateSolution(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Solution data to validate.")
                                                     @Valid @RequestBody SolutionData solutionData) throws Exception {
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.VERSION_CONTROL, Operation.WRITE);
        return solutionService.validateSolution(user, solutionData);
    }

}

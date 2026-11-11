// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventInfo;
import org.thingsboard.server.common.data.agent.AgentBulkAction;
import org.thingsboard.server.common.data.agent.AgentBulkActionEventStats;
import org.thingsboard.server.common.data.agent.AgentProcessingStatus;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AgentBulkActionId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;

import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class AgentBulkActionController extends BaseController {

    private static final String BULK_ACTION_ID = "bulkActionId";


    @ApiOperation(value = "Get Agent Bulk Action (getAgentBulkAction)",
            notes = "Fetch the Agent Bulk Action object based on the provided Bulk Action Id." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/agent/bulk/{bulkActionId}")
    @ResponseBody
    public AgentBulkAction getAgentBulkAction(
            @Parameter(description = "Bulk Action Id") @PathVariable(BULK_ACTION_ID) String strBulkActionId) throws ThingsboardException {
        checkParameter(BULK_ACTION_ID, strBulkActionId);
        AgentBulkActionId bulkActionId = new AgentBulkActionId(toUUID(strBulkActionId));
        return checkAgentBulkActionId(bulkActionId, Operation.READ);
    }

    @ApiOperation(value = "Get Agent Bulk Action Events (getAgentBulkActionEvents)",
            notes = "Returns a page of agent application events produced by the given bulk action, optionally filtered by action type and status. "
                    + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/agent/bulk/{bulkActionId}/events", params = {"pageSize", "page"})
    @ResponseBody
    public PageData<AgentAppEventInfo> getAgentBulkActionEvents(
            @Parameter(description = "Bulk Action Id") @PathVariable(BULK_ACTION_ID) String strBulkActionId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true) @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true) @RequestParam int page,
            @Parameter(description = "Case-insensitive 'substring' filter based on the event fields") @RequestParam(required = false) String textSearch,
            @Parameter(description = "A string value representing the action type to filter by, e.g. 'UPDATE'") @RequestParam(required = false) AgentAppEventActionType actionType,
            @Parameter(description = "A string value representing the event status to filter by, e.g. 'FINISHED'") @RequestParam(required = false) AgentProcessingStatus processingStatus,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION) @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION) @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter(BULK_ACTION_ID, strBulkActionId);
        AgentBulkActionId bulkActionId = new AgentBulkActionId(toUUID(strBulkActionId));
        checkAgentBulkActionId(bulkActionId, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return agentAppEventService.findInfosByBulkActionId(bulkActionId, actionType, processingStatus, pageLink);
    }

    @ApiOperation(value = "Get Agent Bulk Action Event Stats (getAgentBulkActionEventStats)",
            notes = "Returns the number of agent application events produced by the given bulk action per processing status, together with their total."
                    + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/agent/bulk/{bulkActionId}/eventStats")
    @ResponseBody
    public AgentBulkActionEventStats getAgentBulkActionEventStats(
            @Parameter(description = "Bulk Action Id") @PathVariable(BULK_ACTION_ID) String strBulkActionId) throws ThingsboardException {
        checkParameter(BULK_ACTION_ID, strBulkActionId);
        AgentBulkActionId bulkActionId = new AgentBulkActionId(toUUID(strBulkActionId));
        checkAgentBulkActionId(bulkActionId, Operation.READ);
        return agentAppEventService.findEventStatsByBulkActionId(bulkActionId);
    }
}

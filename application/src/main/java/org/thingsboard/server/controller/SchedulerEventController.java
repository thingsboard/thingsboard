// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.report.ReportConfig;
import org.thingsboard.server.common.data.report.ScheduledReportQuery;
import org.thingsboard.server.common.data.scheduler.ScheduledReportInfo;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.scheduler.SchedulerEventFilter;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.common.data.scheduler.SchedulerEventTimeFilter;
import org.thingsboard.server.common.data.scheduler.SchedulerEventWithCustomerInfo;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.subscription.PlatformFeature;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.scheduler.TbSchedulerService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.thingsboard.server.common.data.DataConstants.GENERATE_REPORT;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_ASSIGN_RECEIVE_STEP_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_UNASSIGN_RECEIVE_STEP_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.INCLUDE_CUSTOMERS_OR_SUB_CUSTOMERS;
import static org.thingsboard.server.controller.ControllerConstants.NEW_LINE;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_DELETE_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_READ_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_WRITE_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.REPORT_TEMPLATE_ID_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.REPORT_USER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SCHEDULER_EVENT_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;
import static org.thingsboard.server.controller.EdgeController.EDGE_ID;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class SchedulerEventController extends BaseController {

    private static final String SCHEDULER_EVENT_INFO_DESCRIPTION = "Scheduler Events allows you to schedule various types of events with flexible schedule configuration. " +
            "Scheduler fires configured scheduler events according to their schedule. See the 'Model' tab of the Response Class for more details. ";
    private static final String SCHEDULER_EVENT_WITH_CUSTOMER_INFO_DESCRIPTION = "Scheduler Event With Customer Info extends Scheduler Event Info object and adds " +
            "'customerTitle' - a String value representing the title of the customer which user created a Scheduler Event and " +
            "'customerIsPublic' - a boolean parameter that specifies if customer is public. See the 'Model' tab of the Response Class for more details. ";
    private static final String SCHEDULER_EVENT_DESCRIPTION = "Scheduler Event extends Scheduler Event Info object and adds " +
            "'configuration' - a JSON structure of scheduler event configuration. See the 'Model' tab of the Response Class for more details. ";
    private static final String INVALID_SCHEDULER_EVENT_ID = "Referencing non-existing Scheduler Event Id will cause 'Not Found' error.";
    private static final int DEFAULT_SCHEDULER_EVENT_LIMIT = 100;

    public static final String SCHEDULER_EVENT_ID = "schedulerEventId";

    private final TbSchedulerService tbSchedulerService;

    @ApiOperation(value = "Get Scheduler Event With Customer Info (getSchedulerEventInfoById)",
            notes = "Fetch the SchedulerEventWithCustomerInfo object based on the provided scheduler event Id. " +
                    SCHEDULER_EVENT_WITH_CUSTOMER_INFO_DESCRIPTION + INVALID_SCHEDULER_EVENT_ID +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + "\n\n" + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/schedulerEvent/info/{schedulerEventId}")
    public SchedulerEventWithCustomerInfo getSchedulerEventInfoById(
            @Parameter(description = SCHEDULER_EVENT_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(SCHEDULER_EVENT_ID) String strSchedulerEventId) throws ThingsboardException {
        checkParameter(SCHEDULER_EVENT_ID, strSchedulerEventId);
        SchedulerEventId schedulerEventId = new SchedulerEventId(toUUID(strSchedulerEventId));
        return checkSchedulerEventInfoId(schedulerEventId, Operation.READ);
    }

    @ApiOperation(value = "Get Scheduler Event (getSchedulerEventById)",
            notes = "Fetch the SchedulerEvent object based on the provided scheduler event Id. " +
                    SCHEDULER_EVENT_DESCRIPTION + INVALID_SCHEDULER_EVENT_ID +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + "\n\n" + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/schedulerEvent/{schedulerEventId}")
    public SchedulerEvent getSchedulerEventById(
            @Parameter(description = SCHEDULER_EVENT_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(SCHEDULER_EVENT_ID) String strSchedulerEventId) throws ThingsboardException {
        checkParameter(SCHEDULER_EVENT_ID, strSchedulerEventId);
        SchedulerEventId schedulerEventId = new SchedulerEventId(toUUID(strSchedulerEventId));
        return checkSchedulerEventId(schedulerEventId, Operation.READ);
    }

    @ApiOperation(value = "Save Scheduler Event (saveSchedulerEvent)",
            notes = "Creates or Updates scheduler event. " + SCHEDULER_EVENT_DESCRIPTION +
                    "When creating scheduler event, platform generates scheduler event Id as " + UUID_WIKI_LINK +
                    "The newly created scheduler event id will be present in the response. Specify existing scheduler event id to update the scheduler event. " +
                    "Referencing non-existing scheduler event Id will cause 'Not Found' error. " +
                    "Remove 'id', 'tenantId' and optionally 'customerId' from the request body example (below) to create new Scheduler Event entity. " +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping(value = "/schedulerEvent")
    public SchedulerEvent saveSchedulerEvent(
            @Parameter(description = "A JSON value representing the Scheduler Event.")
            @RequestBody SchedulerEvent schedulerEvent) throws ThingsboardException {
        checkFeatureAllowed(PlatformFeature.SCHEDULER);
        checkReportingAllowed(schedulerEvent);
        SecurityUser currentUser = getCurrentUser();
        schedulerEvent.setTenantId(currentUser.getTenantId());
        if (Authority.CUSTOMER_USER.equals(currentUser.getAuthority())) {
            schedulerEvent.setCustomerId(currentUser.getCustomerId());
        }
        checkEntity(schedulerEvent.getId(), schedulerEvent, Resource.SCHEDULER_EVENT);
        checkEventConfigPermissions(schedulerEvent);
        return tbSchedulerService.save(schedulerEvent, currentUser);
    }

    /**
     * A scheduler event of a report-producing type is a reporting write as much as a scheduler one: once
     * persisted it generates reports on its own schedule, so without this the scheduler is a way around a
     * withheld reporting feature. Only the write paths carry the check; reading and listing stay open.
     */
    private void checkReportingAllowed(SchedulerEvent schedulerEvent) throws ThingsboardException {
        if (schedulerEvent.isReportProducing()) {
            checkFeatureAllowed(PlatformFeature.REPORTING);
        }
    }

    private void checkEventConfigPermissions(SchedulerEvent schedulerEvent) throws ThingsboardException {
        if (GENERATE_REPORT.equals(schedulerEvent.getType())) {
            SecurityUser currentUser = getCurrentUser();
            ReportConfig reportConfig = checkNotNull(JacksonUtil.treeToValue(schedulerEvent.getConfiguration(), ReportConfig.class),
                    "Report scheduler event configuration should be specified!");
            checkReportTemplateId(reportConfig.getReportTemplateId(), Operation.READ);
            UserId reportUserId = checkNotNull(reportConfig.getUserId(), "Report user must be specified!");
            checkUserOwnerPermission(currentUser, reportUserId);
        }
    }

    @ApiOperation(value = "Enable or disable Scheduler Event (enableSchedulerEvent)",
            notes = "Updates scheduler event with enabled = true/false. " + SCHEDULER_EVENT_DESCRIPTION +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @PutMapping(value = "/schedulerEvent/{schedulerEventId}/enabled/{enabledValue}")
    public SchedulerEvent enableSchedulerEvent(
            @Parameter(description = SCHEDULER_EVENT_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(SCHEDULER_EVENT_ID) String strSchedulerEventId,
            @Parameter(description = "Enabled or disabled scheduler", required = true)
            @PathVariable(value = "enabledValue") Boolean enabledValue) throws ThingsboardException {
        // Guarded in the enabling direction only: pausing reduces use of a withheld feature, and refusing that
        // would leave a tenant with events that keep firing and no way to stop them.
        boolean enabling = Boolean.TRUE.equals(enabledValue);
        if (enabling) {
            checkFeatureAllowed(PlatformFeature.SCHEDULER);
        }
        checkParameter(SCHEDULER_EVENT_ID, strSchedulerEventId);
        SchedulerEventId schedulerEventId = new SchedulerEventId(toUUID(strSchedulerEventId));

        SchedulerEvent schedulerEvent = checkSchedulerEventId(schedulerEventId, Operation.WRITE);
        if (enabling) {
            checkReportingAllowed(schedulerEvent);
        }
        schedulerEvent.setEnabled(enabledValue);

        return tbSchedulerService.save(schedulerEvent, getCurrentUser());
    }

    @ApiOperation(value = "Delete Scheduler Event (deleteSchedulerEvent)",
            notes = "Deletes the scheduler event. " + INVALID_SCHEDULER_EVENT_ID + "\n\n" + RBAC_DELETE_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @DeleteMapping(value = "/schedulerEvent/{schedulerEventId}")
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteSchedulerEvent(
            @Parameter(description = SCHEDULER_EVENT_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(SCHEDULER_EVENT_ID) String strSchedulerEventId) throws ThingsboardException {
        // Deliberately unguarded: removal is the one write direction that reduces use of a withheld feature.
        // Persisted scheduler events keep firing while it is withheld, so refusing this would leave no way out.
        checkParameter(SCHEDULER_EVENT_ID, strSchedulerEventId);
        SchedulerEventId schedulerEventId = new SchedulerEventId(toUUID(strSchedulerEventId));
        SchedulerEvent schedulerEvent = checkSchedulerEventId(schedulerEventId, Operation.DELETE);

        tbSchedulerService.delete(schedulerEvent, getCurrentUser());
    }


    @Hidden
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/schedulerEvents")
    public List<SchedulerEventWithCustomerInfo> getAllSchedulerEventsV1(
            @RequestParam(required = false) String type) throws ThingsboardException {
        SecurityUser currentUser = getCurrentUser();
        accessControlService.checkPermission(currentUser, Resource.SCHEDULER_EVENT, Operation.READ);

        SchedulerEventFilter filter = SchedulerEventFilter.builder()
                .customerId(currentUser.getCustomerId())
                .type(type)
                .build();
        return schedulerEventService.findSchedulerEventsByTenantIdAndFilter(currentUser.getTenantId(), filter, new PageLink(1_000)).getData();
    }

    @ApiOperation(value = "Get all scheduler events (getAllSchedulerEvents)",
            notes = "Requested scheduler events must be owned by tenant or assigned to customer which user is performing the request. "
                    + SCHEDULER_EVENT_WITH_CUSTOMER_INFO_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + "\n\n" + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/schedulerEvents/all")
    public List<SchedulerEventWithCustomerInfo> getAllSchedulerEvents(
            @Parameter(description = "A string value representing the scheduler type. For example, 'generateReport'")
            @RequestParam(required = false) String type) throws ThingsboardException {
        return getAllSchedulerEventsV1(type);
    }

    @ApiOperation(value = "Get scheduler events (getSchedulerEvents)",
            notes = "Requested scheduler events must be owned by tenant or assigned to customer which user is performing the request. " +
                    SCHEDULER_EVENT_WITH_CUSTOMER_INFO_DESCRIPTION + NEW_LINE + PAGE_DATA_PARAMETERS + NEW_LINE +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + NEW_LINE + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/schedulerEvents", params = {"pageSize", "page"})
    public PageData<SchedulerEventWithCustomerInfo> getSchedulerEvents(@Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
                                                                       @RequestParam int pageSize,
                                                                       @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
                                                                       @RequestParam int page,
                                                                       @Parameter(description = "Case-insensitive 'substring' filter based on event's name, type, or customer's name")
                                                                       @RequestParam(required = false) String textSearch,
                                                                       @Parameter(description = SORT_PROPERTY_DESCRIPTION)
                                                                       @RequestParam(required = false) String sortProperty,
                                                                       @Parameter(description = SORT_ORDER_DESCRIPTION)
                                                                       @RequestParam(required = false) String sortOrder,
                                                                       @Parameter(description = "A string value representing the scheduler type. For example, 'generateReport'")
                                                                       @RequestParam(required = false) String type,
                                                                       @Parameter(description = EDGE_ID_PARAM_DESCRIPTION)
                                                                       @RequestParam(required = false) UUID edgeId) throws ThingsboardException {
        SecurityUser currentUser = getCurrentUser();
        accessControlService.checkPermission(currentUser, Resource.SCHEDULER_EVENT, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);

        SchedulerEventFilter filter = SchedulerEventFilter.builder()
                .customerId(currentUser.getCustomerId())
                .type(type)
                .edgeId(edgeId != null ? new EdgeId(edgeId) : null)
                .build();
        return schedulerEventService.findSchedulerEventsByTenantIdAndFilter(currentUser.getTenantId(), filter, pageLink);
    }

    @Hidden
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/schedulerEvents", params = {"startTime", "endTime"})
    public List<SchedulerEventWithCustomerInfo> getSchedulerEvents(@RequestParam(required = false) String type,
                                                                   @RequestParam long startTime,
                                                                   @RequestParam long endTime,
                                                                   @RequestParam(required = false) UUID edgeId,
                                                                   @RequestParam(required = false) String textSearch) throws ThingsboardException {
        SecurityUser currentUser = getCurrentUser();
        accessControlService.checkPermission(currentUser, Resource.SCHEDULER_EVENT, Operation.READ);

        SchedulerEventTimeFilter filter = SchedulerEventTimeFilter.builder()
                .customerId(currentUser.getCustomerId())
                .type(type)
                .startTime(startTime)
                .endTime(endTime)
                .edgeId(edgeId != null ? new EdgeId(edgeId) : null)
                .build();
        return schedulerEventService.findAllSchedulerEventsByTenantIdAndEventTimeFilter(currentUser.getTenantId(), filter, textSearch);
    }

    @ApiOperation(value = "Get scheduler events (getSchedulerEventsByRange)",
            notes = "Retrieves scheduler events filtering by event run time. " +
                    "Requested scheduler events must be owned by tenant or assigned to customer which user is performing the request. " +
                    SCHEDULER_EVENT_WITH_CUSTOMER_INFO_DESCRIPTION + NEW_LINE + PAGE_DATA_PARAMETERS + NEW_LINE +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + NEW_LINE + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/schedulerEvents/startTime/{startTime}/endTime/{endTime}")
    public List<SchedulerEventWithCustomerInfo> getSchedulerEventsByRange(@Parameter(description = "Start time filter in milliseconds for scheduler event run time")
                                                                          @PathVariable("startTime") long startTime,
                                                                          @Parameter(description = "End time filter in milliseconds for scheduler event run time")
                                                                          @PathVariable("endTime") long endTime,
                                                                          @Parameter(description = "A string value representing the scheduler type. For example, 'generateReport'")
                                                                          @RequestParam(required = false) String type,
                                                                          @Parameter(description = EDGE_ID_PARAM_DESCRIPTION)
                                                                          @RequestParam(required = false) UUID edgeId,
                                                                          @Parameter(description = "Case-insensitive 'substring' filter based on event's name, type, or customer's name")
                                                                          @RequestParam(required = false) String textSearch) throws ThingsboardException {
        return getSchedulerEvents(type, startTime, endTime, edgeId, textSearch);
    }

    @ApiOperation(value = "Get Scheduled Report Events (getScheduledReportEvents)",
            notes = TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + "\n\n" + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/scheduledReports")
    public PageData<ScheduledReportInfo> getScheduledReportEvents(
            @Parameter(description = REPORT_TEMPLATE_ID_DESCRIPTION)
            @RequestParam(required = false) UUID reportTemplateId,
            @Parameter(description = REPORT_USER_DESCRIPTION)
            @RequestParam(required = false) UUID userId,
            @Parameter(description = INCLUDE_CUSTOMERS_OR_SUB_CUSTOMERS)
            @RequestParam(required = false) Boolean includeCustomers,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true, schema = @Schema(minimum = "1"))
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true, schema = @Schema(minimum = "0"))
            @RequestParam int page,
            @Parameter(description = "The case insensitive 'substring' filter based on the scheduler event name or customer title.")
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION)
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.SCHEDULER_EVENT, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        ScheduledReportQuery query = ScheduledReportQuery.builder()
                .reportTemplateId(reportTemplateId)
                .userId(userId)
                .includeCustomers(includeCustomers != null && includeCustomers)
                .pageLink(pageLink)
                .build();
        if (Authority.TENANT_ADMIN.equals(getCurrentUser().getAuthority())) {
            return checkNotNull(schedulerEventService.findScheduledReportEvents(tenantId, query));
        } else {
            CustomerId customerId = getCurrentUser().getCustomerId();
            return checkNotNull(schedulerEventService.findScheduledReportEvents(tenantId, customerId, query));
        }
    }

    @Hidden
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/schedulerEvents", params = {"schedulerEventIds"})
    public List<SchedulerEventInfo> getSchedulerEventsByIdsV1(
            @RequestParam("schedulerEventIds") String[] strSchedulerEventIds) throws ThingsboardException, ExecutionException, InterruptedException {
        checkArrayParameter("schedulerEventIds", strSchedulerEventIds);
        if (!accessControlService.hasPermission(getCurrentUser(), Resource.SCHEDULER_EVENT, Operation.READ)) {
            return Collections.emptyList();
        }
        SecurityUser user = getCurrentUser();
        TenantId tenantId = user.getTenantId();
        List<SchedulerEventId> schedulerEventIds = new ArrayList<>();
        for (String strSchedulerEventId : strSchedulerEventIds) {
            schedulerEventIds.add(new SchedulerEventId(toUUID(strSchedulerEventId)));
        }
        List<SchedulerEventInfo> schedulerEvents = checkNotNull(schedulerEventService.findSchedulerEventInfoByIdsAsync(tenantId, schedulerEventIds).get());
        return filterSchedulerEventsByReadPermission(schedulerEvents);
    }

    @ApiOperation(value = "Get Scheduler Events By Ids (getSchedulerEventsByIds)",
            notes = "Requested scheduler events must be owned by tenant or assigned to customer which user is performing the request. "
                    + SCHEDULER_EVENT_INFO_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + "\n\n" + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/schedulerEvents/list")
    public List<SchedulerEventInfo> getSchedulerEventsByIds(
            @Parameter(description = "A list of scheduler event ids, separated by comma ','", array = @ArraySchema(schema = @Schema(type = "string")), required = true)
            @RequestParam("schedulerEventIds") String[] strSchedulerEventIds) throws ThingsboardException, ExecutionException, InterruptedException {
        return getSchedulerEventsByIdsV1(strSchedulerEventIds);
    }

    private List<SchedulerEventInfo> filterSchedulerEventsByReadPermission(List<SchedulerEventInfo> schedulerEvents) {
        return schedulerEvents.stream().filter(schedulerEvent -> {
            try {
                return accessControlService.hasPermission(getCurrentUser(), Resource.SCHEDULER_EVENT, Operation.READ, schedulerEvent.getId(), schedulerEvent);
            } catch (ThingsboardException e) {
                return false;
            }
        }).toList();
    }

    @ApiOperation(value = "Assign scheduler event to edge (assignSchedulerEventToEdge)",
            notes = "Creates assignment of an existing scheduler event to an instance of The Edge. " +
                    "Assignment works in async way - first, notification event pushed to edge service queue on platform. " +
                    "Second, remote edge service will receive a copy of assignment scheduler event " +
                    EDGE_ASSIGN_RECEIVE_STEP_DESCRIPTION +
                    "Third, once scheduler event will be delivered to edge service, it is going to be available for usage on remote edge instance. " +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_WRITE_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping(value = "/edge/{edgeId}/schedulerEvent/{schedulerEventId}")
    public SchedulerEventInfo assignSchedulerEventToEdge(@Parameter(description = EDGE_ID_PARAM_DESCRIPTION)
                                                         @PathVariable(EDGE_ID) String strEdgeId,
                                                         @Parameter(description = SCHEDULER_EVENT_ID_PARAM_DESCRIPTION)
                                                         @PathVariable(SCHEDULER_EVENT_ID) String strSchedulerEventId) throws ThingsboardException {
        checkFeatureAllowed(PlatformFeature.SCHEDULER);
        checkParameter("edgeId", strEdgeId);
        checkParameter(SCHEDULER_EVENT_ID, strSchedulerEventId);

        // Assignment hands the event to an edge that runs a scheduler of its own, so a report-producing event
        // is a reporting write here too. Loaded before the edge, so the refusal does not depend on the edge.
        SchedulerEventId schedulerEventId = new SchedulerEventId(toUUID(strSchedulerEventId));
        checkReportingAllowed(checkSchedulerEventId(schedulerEventId, Operation.READ));

        EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        Edge edge = checkEdgeId(edgeId, Operation.WRITE);

        return tbSchedulerService.assignToEdge(schedulerEventId, edge, getCurrentUser());
    }

    @ApiOperation(value = "Unassign scheduler event from edge (unassignSchedulerEventFromEdge)",
            notes = "Clears assignment of the scheduler event to the edge. " +
                    "Unassignment works in async way - first, 'unassign' notification event pushed to edge queue on platform. " +
                    "Second, remote edge service will receive an 'unassign' command to remove entity group " +
                    EDGE_UNASSIGN_RECEIVE_STEP_DESCRIPTION +
                    "Third, once 'unassign' command will be delivered to edge service, it's going to remove entity group and entities inside this group locally." +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_WRITE_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @DeleteMapping(value = "/edge/{edgeId}/schedulerEvent/{schedulerEventId}")
    public SchedulerEventInfo unassignSchedulerEventFromEdge(@Parameter(description = EDGE_ID_PARAM_DESCRIPTION)
                                                             @PathVariable(EDGE_ID) String strEdgeId,
                                                             @Parameter(description = SCHEDULER_EVENT_ID_PARAM_DESCRIPTION)
                                                             @PathVariable(SCHEDULER_EVENT_ID) String strSchedulerEventId) throws ThingsboardException {
        // The scheduler gate stands; only the reporting check the assignment above makes is deliberately
        // absent, since taking the event back off the edge hands no report-producing event to anything.
        checkFeatureAllowed(PlatformFeature.SCHEDULER);
        checkParameter("edgeId", strEdgeId);
        checkParameter(SCHEDULER_EVENT_ID, strSchedulerEventId);
        EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        Edge edge = checkEdgeId(edgeId, Operation.WRITE);
        SchedulerEventId schedulerEventId = new SchedulerEventId(toUUID(strSchedulerEventId));
        checkSchedulerEventId(schedulerEventId, Operation.READ);

        return tbSchedulerService.unassignFromEdge(schedulerEventId, edge, getCurrentUser());
    }

    @ApiOperation(value = "Get Edge Scheduler Events (getEdgeSchedulerEvents)",
            notes = "Returns a page of  Scheduler Events Info objects based on the provided Edge entity. " +
                    SCHEDULER_EVENT_DESCRIPTION + SCHEDULER_EVENT_INFO_DESCRIPTION +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/edge/{edgeId}/schedulerEvents", params = {"pageSize", "page"})
    public PageData<SchedulerEventInfo> getEdgeSchedulerEvents(
            @Parameter(description = EDGE_ID_PARAM_DESCRIPTION)
            @PathVariable(EDGE_ID) String strEdgeId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true, schema = @Schema(minimum = "1"))
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true, schema = @Schema(minimum = "0"))
            @RequestParam int page,
            @Parameter(description = "The case insensitive 'startsWith' filter based on the scheduler event name.")
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION)
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter("edgeId", strEdgeId);
        TenantId tenantId = getCurrentUser().getTenantId();
        EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        checkEdgeId(edgeId, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(schedulerEventService.findSchedulerEventInfosByTenantIdAndEdgeId(tenantId, edgeId, pageLink));
    }

    @ApiOperation(value = "Get All Edge Scheduler Events (getAllEdgeSchedulerEvents)",
            notes = "Fetch the list of Scheduler Event Info objects based on the provided Edge entity. "
                    + SCHEDULER_EVENT_DESCRIPTION + SCHEDULER_EVENT_INFO_DESCRIPTION +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/edge/{edgeId}/allSchedulerEvents")
    public List<SchedulerEventInfo> getAllEdgeSchedulerEvents(@Parameter(description = EDGE_ID_PARAM_DESCRIPTION)
                                                              @PathVariable(EDGE_ID) String strEdgeId) throws ThingsboardException {
        checkParameter("edgeId", strEdgeId);
        TenantId tenantId = getCurrentUser().getTenantId();
        EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        checkEdgeId(edgeId, Operation.READ);
        List<SchedulerEventInfo> result = new ArrayList<>();
        PageLink pageLink = new PageLink(DEFAULT_SCHEDULER_EVENT_LIMIT);
        PageData<SchedulerEventInfo> pageData;
        do {
            pageData = schedulerEventService.findSchedulerEventInfosByTenantIdAndEdgeId(tenantId, edgeId, pageLink);
            if (!pageData.getData().isEmpty()) {
                result.addAll(pageData.getData());
                if (pageData.hasNext()) {
                    pageLink = pageLink.nextPageLink();
                }
            }
        } while (pageData.hasNext());
        return checkNotNull(result);
    }

}

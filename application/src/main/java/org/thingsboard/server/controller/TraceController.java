/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2026 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.trace.TraceCoverageSetting;
import org.thingsboard.server.common.data.trace.TraceListFilters;
import org.thingsboard.server.common.data.trace.TracePathDTO;
import org.thingsboard.server.common.data.trace.TracePathDetailsDTO;
import org.thingsboard.server.common.data.trace.TracePathFilters;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.trace.SortOrder;
import org.thingsboard.server.service.trace.TracePathListQuery;
import org.thingsboard.server.service.trace.TracePathSortField;
import org.thingsboard.server.service.trace.Trace;
import org.thingsboard.server.service.trace.TraceCoverageSettingService;
import org.thingsboard.server.service.trace.SpanAttributeDTO;
import org.thingsboard.server.service.trace.SpanDTO;
import org.thingsboard.server.service.trace.SpanEventDTO;
import org.thingsboard.server.service.trace.SpanLinkDTO;
import org.thingsboard.server.service.trace.SpanSubResourceQuery;
import org.thingsboard.server.service.trace.SpanTreeNode;
import org.thingsboard.server.service.trace.TraceDTO;
import org.thingsboard.server.service.trace.TraceSearchQuery;
import org.thingsboard.server.service.trace.TraceService;
import org.thingsboard.server.service.trace.TraceSpanListQuery;
import org.thingsboard.server.service.trace.TraceSortField;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@TbCoreComponent
@RequestMapping("/api/traces")
@PreAuthorize("hasAuthority('TENANT_ADMIN')")
@RequiredArgsConstructor
public class TraceController extends BaseController {
    private final TraceService traceService;
    private final TraceCoverageSettingService traceCoverageSettingService;

    @Operation(summary = "Get trace coverage setting",
            description = "Get the trace coverage setting for the current tenant. Returns the disabled default when none is stored. " +
                    "Trace coverage drives pack-level trace-id injection: at most 'tracesPerInterval' trace groups " +
                    "(queue + tenant + message type) are admitted per 'intervalSeconds' window, and one message within each " +
                    "admitted group receives a trace id.")
    @GetMapping("/coverage")
    @ResponseStatus(value = HttpStatus.OK)
    public TraceCoverageSetting getTraceCoverageSetting() throws ThingsboardException {
        return traceCoverageSettingService.findTraceCoverageSettingByTenantId(getTenantId());
    }

    @Operation(summary = "Save trace coverage setting",
            description = "Create or update (upsert) the trace coverage setting for the current tenant. " +
                    "Validates that tracesPerInterval, switchPeriodSeconds and relatedTraceSampleIntervalSeconds are " +
                    "positive integers, tracesPerPack is zero (unlimited) or positive, and intervalSeconds is a positive number of seconds.")
    @PostMapping("/coverage")
    @ResponseStatus(value = HttpStatus.OK)
    public TraceCoverageSetting saveTraceCoverageSetting(
            @Parameter(description = "Trace coverage setting", required = true)
            @RequestBody TraceCoverageSetting setting) throws ThingsboardException {
        return traceCoverageSettingService.saveTraceCoverageSetting(getTenantId(), checkNotNull(setting));
    }

    @Operation(summary = "Get trace by ID",
            description = "Get trace details including first span name, in-queue time, and total span time. " +
                    "In-queue time is calculated accounting for parallel/overlapping spans using interval merging. " +
                    "Total span time is the sum of all individual span durations.")
    @GetMapping("/{traceId}")
    public TraceDTO getTrace(
            @Parameter(description = "Trace ID (string, OpenTelemetry format)", required = true)
            @PathVariable String traceId) throws ThingsboardException {
        return TraceDTO.fromTrace(traceService.getTrace(getTenantId(), traceId));
    }

    @Operation(summary = "Get spans by trace ID",
            description = "Get all spans for a specific trace (flat list, ordered by start time)")
    @GetMapping("/{traceId}/spans")
    public List<SpanDTO> getSpansByTraceId(
            @Parameter(description = "Trace ID (string, OpenTelemetry format)", required = true)
            @PathVariable String traceId) throws ThingsboardException {
        TraceSpanListQuery query = new TraceSpanListQuery(traceId);
        return traceService.getSpans(getTenantId(), query).stream()
                .map(SpanDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Operation(summary = "Get spans tree by trace ID",
            description = "Get all spans for a specific trace organized in a hierarchical tree structure. " +
                    "Root spans (with no parent) are at the top level, and children are nested recursively. " +
                    "All spans are sorted by start time within their level.")
    @GetMapping("/{traceId}/spans/tree")
    @ResponseStatus(value = HttpStatus.OK)
    public List<SpanTreeNode> getSpansTreeByTraceId(
            @Parameter(description = "Trace ID (string, OpenTelemetry format)", required = true)
            @PathVariable String traceId) throws ThingsboardException {
        return traceService.getSpansTree(getTenantId(), new TraceSpanListQuery(traceId));
    }

    @Operation(summary = "Get span attributes",
            description = "Get all attributes for a specific span")
    @GetMapping("/{traceId}/spans/{spanId}/attributes")
    @ResponseStatus(value = HttpStatus.OK)
    public List<SpanAttributeDTO> getSpanAttributes(
            @Parameter(description = "Trace ID (string, OpenTelemetry format)", required = true)
            @PathVariable String traceId,
            @Parameter(description = "Span ID (string, OpenTelemetry format)", required = true)
            @PathVariable String spanId) throws ThingsboardException {
        SpanSubResourceQuery query = new SpanSubResourceQuery(traceId, spanId);
        return traceService.getSpanAttributes(getTenantId(), query).stream()
                .map(SpanAttributeDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Operation(summary = "Get span events",
            description = "Get all events for a specific span")
    @GetMapping("/{traceId}/spans/{spanId}/events")
    @ResponseStatus(value = HttpStatus.OK)
    public List<SpanEventDTO> getSpanEvents(
            @Parameter(description = "Trace ID (string, OpenTelemetry format)", required = true)
            @PathVariable String traceId,
            @Parameter(description = "Span ID (string, OpenTelemetry format)", required = true)
            @PathVariable String spanId) throws ThingsboardException {
        SpanSubResourceQuery query = new SpanSubResourceQuery(traceId, spanId);
        return traceService.getSpanEvents(getTenantId(), query).stream()
                .map(SpanEventDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Operation(summary = "Get span links",
            description = "Get all links for a specific span")
    @GetMapping("/{traceId}/spans/{spanId}/links")
    @ResponseStatus(value = HttpStatus.OK)
    public List<SpanLinkDTO> getSpanLinks(
            @Parameter(description = "Trace ID (string, OpenTelemetry format)", required = true)
            @PathVariable String traceId,
            @Parameter(description = "Span ID (string, OpenTelemetry format)", required = true)
            @PathVariable String spanId) throws ThingsboardException {
        SpanSubResourceQuery query = new SpanSubResourceQuery(trimToNull(traceId), trimToNull(spanId));
        return traceService.getSpanLinks(getTenantId(), query).stream()
                .map(SpanLinkDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Operation(summary = "Get trace filter options",
            description = "Returns the consolidated filter dropdown options for the Traces list: the tenant's current " +
                    "queues, rule chains, rule nodes and rule-engine services, plus the known message types. Names " +
                    "are resolved server-side.")
    @GetMapping("/filters")
    @ResponseStatus(value = HttpStatus.OK)
    public TraceListFilters getTraceFilters() throws ThingsboardException {
        return traceService.getTraceFilters(getTenantId());
    }

    @Operation(summary = "Search traces with pagination and sorting",
            description = "Search traces by service name, time range, and optional filters. " +
                    "Supports pagination and sorting. Returns traces with the first span name included. " +
                    "When traceId is specified, returns only that trace (other filters ignored).")
    @GetMapping
    public PageData<TraceDTO> searchTraces(
            @Parameter(description = "Service name filter (optional)")
            @RequestParam(required = false) String serviceName,
            @Parameter(description = "Start time in milliseconds (epoch)", required = true)
            @RequestParam long startTime,
            @Parameter(description = "End time in milliseconds (epoch)", required = true)
            @RequestParam long endTime,
            @Parameter(description = "Span name filter - exact match on ANY span in the trace (optional). " +
                    "Example: '[Transform] Parse JSON' returns traces containing at least one span with this exact name.")
            @RequestParam(required = false) String spanName,
            @Parameter(description = "Queue name filter - exact match on queue.name attribute (optional). " +
                    "Returns traces containing at least one span with this queue name.")
            @RequestParam(required = false) String queueName,
            @Parameter(description = "Originator ID filter - exact match on originator.id attribute (optional). " +
                    "Returns traces containing at least one span with this originator ID (UUID).")
            @RequestParam(required = false) String originatorId,
            @Parameter(description = "Message type filter - exact match on message.type attribute (optional). " +
                    "Returns traces containing at least one span with this message type.")
            @RequestParam(required = false) String messageType,
            @Parameter(description = "Message ID filter - exact match on message.id attribute (optional). " +
                    "Returns traces containing at least one span with this message ID.")
            @RequestParam(required = false) String messageId,
            @Parameter(description = "Message data filter - substring match on input.msg.data attribute (optional). " +
                    "Returns traces containing at least one span with input.msg.data containing this string (case-sensitive).")
            @RequestParam(required = false) String messageData,
            @Parameter(description = "Message metadata filter - substring match on input.msg.metadata attribute (optional). " +
                    "Returns traces containing at least one span with input.msg.metadata containing this string (case-sensitive).")
            @RequestParam(required = false) String messageMetadata,
            @Parameter(description = "Trace ID for exact match (optional). " +
                    "When specified, searches only by this ID and ignores other filters.")
            @RequestParam(required = false) String traceId,
            @Parameter(description = "When true, return only traces with errors (error_count > 0). Default: false.")
            @RequestParam(required = false, defaultValue = "false") boolean errorsOnly,
            @Parameter(description = "Rule chain ID filter - when combined with errorsOnly=true, returns traces with an ERROR span on this rule chain (optional).")
            @RequestParam(required = false) String ruleChainId,
            @Parameter(description = "Rule node ID filter - returns traces that contain a span with this rule node. When combined with errorsOnly=true, narrows to traces with an ERROR span on this node. Can be used independently without ruleChainId (optional).")
            @RequestParam(required = false) String ruleNodeId,
            @Parameter(description = "Execution path id filter (optional). When set, returns only the path's saved (related) traces: " +
                    "the time range is day-floored to locate the path's trace_group buckets, their related trace ids are collected, " +
                    "and the exact start_time range plus any other filters are still applied on top.")
            @RequestParam(required = false) String pathId,
            @RequestParam(required = false) boolean hasTimeOutsByQueue,
            @Parameter(description = "Page number (0-based, default: 0)")
            @RequestParam(required = false, defaultValue = "0") int page,
            @Parameter(description = "Page size (default: 20, max: 100)")
            @RequestParam(required = false, defaultValue = "20") int pageSize,
            @Parameter(description = "Sort field: START_TIME, END_TIME, FIRST_SPAN_NAME, DURATION, SPAN_COUNT, ERROR_COUNT, CREATED_TIME, TOTAL_SPAN_TIME, or IN_QUEUE_TIME (default: START_TIME)")
            @RequestParam(required = false, defaultValue = "START_TIME") TraceSortField sortBy,
            @Parameter(description = "Sort order: ASC or DESC (default: DESC)")
            @RequestParam(required = false, defaultValue = "DESC") SortOrder sortOrder) throws ThingsboardException {
        TraceSearchQuery query = buildTraceSearchQuery(serviceName, startTime, endTime, spanName, queueName, originatorId,
                messageType, messageId, messageData, messageMetadata, traceId, errorsOnly, ruleChainId, ruleNodeId, pathId, hasTimeOutsByQueue, page, pageSize, sortBy, sortOrder);
        if (!query.hasTraceId()) {
            validateTimeRange(startTime, endTime);
        }
        PageData<Trace> tracesPage = traceService.searchTracesPaginated(getTenantId(), query);
        List<TraceDTO> traces = tracesPage.getData().stream()
                .map(TraceDTO::fromTrace)
                .collect(Collectors.toList());
        return new PageData<>(traces, tracesPage.getTotalPages(), tracesPage.getTotalElements(), tracesPage.hasNext());
    }

    @Operation(summary = "List execution paths",
            description = "Returns a paginated, aggregated list of execution paths from trace_group for the given time " +
                    "range. Rows are summed across day buckets per path. Supports entry-point filters (root queue / " +
                    "message type / rule chain), a visited rule node filter, errorsOnly, and server-side sorting.")
    @GetMapping("/paths")
    @ResponseStatus(value = HttpStatus.OK)
    public PageData<TracePathDTO> getPaths(
            @Parameter(description = "Start time in milliseconds (epoch)", required = true)
            @RequestParam long startTime,
            @Parameter(description = "End time in milliseconds (epoch)", required = true)
            @RequestParam long endTime,
            @Parameter(description = "Root queue ID filter (optional)")
            @RequestParam(required = false) UUID rootQueueId,
            @Parameter(description = "Root message type filter (optional)")
            @RequestParam(required = false) String rootMessageType,
            @Parameter(description = "Root rule chain ID filter (optional)")
            @RequestParam(required = false) UUID rootRuleChainId,
            @Parameter(description = "Rule node ID filter - only paths that visit this rule node (optional)")
            @RequestParam(required = false) UUID ruleNodeId,
            @Parameter(description = "When true, return only paths with at least one rule-node error. Default: false.")
            @RequestParam(required = false, defaultValue = "false") boolean errorsOnly,
            @Parameter(description = "Page number (0-based, default: 0)")
            @RequestParam(required = false, defaultValue = "0") int page,
            @Parameter(description = "Page size (default: 20, max: 100)")
            @RequestParam(required = false, defaultValue = "20") int pageSize,
            @Parameter(description = "Sort field (default: TRACES_COUNT)")
            @RequestParam(required = false, defaultValue = "TRACES_COUNT") TracePathSortField sortBy,
            @Parameter(description = "Sort order: ASC or DESC (default: DESC)")
            @RequestParam(required = false, defaultValue = "DESC") SortOrder sortOrder) throws ThingsboardException {
        validateTimeRange(startTime, endTime);
        int effectivePageSize = Math.min(Math.max(pageSize, 1), 100);
        return traceService.getPathsPaginated(getTenantId(), TracePathListQuery.builder()
                .startTime(startTime).endTime(endTime)
                .rootQueueId(rootQueueId != null ? rootQueueId.toString() : null)
                .rootMessageType(trimToNull(rootMessageType))
                .rootRuleChainId(rootRuleChainId != null ? rootRuleChainId.toString() : null)
                .ruleNodeId(ruleNodeId != null ? ruleNodeId.toString() : null)
                .errorsOnly(errorsOnly)
                .page(Math.max(page, 0)).pageSize(effectivePageSize)
                .sortField(sortBy).sortOrder(sortOrder)
                .build());
    }

    @Operation(summary = "Get execution path filter options",
            description = "Returns the consolidated filter dropdown options for the Execution Paths view: the tenant's " +
                    "current queues, rule chains and rule nodes (same source as the rule engine monitoring filters) " +
                    "plus the known message types. Names are resolved server-side.")
    @GetMapping("/paths/filters")
    @ResponseStatus(value = HttpStatus.OK)
    public TracePathFilters getPathFilters() throws ThingsboardException {
        return traceService.getPathFilters(getTenantId());
    }

    @Operation(summary = "Get execution path details",
            description = "Returns the aggregated summary of a single execution path over the given time range plus its " +
                    "rule node tree. The tree is reconstructed from the visited rule nodes' connections (captured from " +
                    "span parent/child relations); per-node metrics are aggregated across the bucket range and queue / " +
                    "rule chain / rule node names are resolved server-side.")
    @GetMapping("/paths/{pathId}")
    @ResponseStatus(value = HttpStatus.OK)
    public TracePathDetailsDTO getPathDetails(
            @Parameter(description = "Execution path id", required = true)
            @PathVariable String pathId,
            @Parameter(description = "Start time in milliseconds (epoch)", required = true)
            @RequestParam long startTime,
            @Parameter(description = "End time in milliseconds (epoch)", required = true)
            @RequestParam long endTime) throws ThingsboardException {
        validateTimeRange(startTime, endTime);
        return traceService.getPathDetails(getTenantId(), pathId, startTime, endTime);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String trimToNull(String value) {
        String trimmed = trim(value);
        return StringUtils.isEmpty(trimmed) ? null : trimmed;
    }

    private TraceSearchQuery buildTraceSearchQuery(String serviceName, long startTime, long endTime,
                                                   String spanName, String queueName, String originatorId, String messageType,
                                                   String messageId, String messageData, String messageMetadata, String traceId,
                                                   boolean errorsOnly, String ruleChainId, String ruleNodeId,
                                                   String pathId, boolean hasTimeOutsByQueue, int page, int pageSize,
                                                   TraceSortField sortBy, SortOrder sortOrder) {
        return TraceSearchQuery.builder()
                .serviceName(trimToNull(serviceName))
                .startTime(startTime)
                .endTime(endTime)
                .spanName(trimToNull(spanName))
                .queueName(trimToNull(queueName))
                .originatorId(trimToNull(originatorId))
                .messageType(trimToNull(messageType))
                .messageId(trimToNull(messageId))
                .messageData(trimToNull(messageData))
                .messageMetadata(trimToNull(messageMetadata))
                .traceId(trimToNull(traceId))
                .errorsOnly(errorsOnly)
                .hasTimeOutsByQueue(hasTimeOutsByQueue)
                .ruleChainId(trimToNull(ruleChainId))
                .ruleNodeId(trimToNull(ruleNodeId))
                .pathId(trimToNull(pathId))
                .page(page)
                .pageSize(pageSize)
                .sortField(sortBy)
                .sortOrder(sortOrder)
                .build();
    }
}

// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.ruleenginemonitoring.MergedStatsDelta;
import org.thingsboard.server.common.data.ruleenginemonitoring.MergedStatsTableRow;
import org.thingsboard.server.common.data.ruleenginemonitoring.QueueLagTimeseriesEntry;
import org.thingsboard.server.common.data.ruleenginemonitoring.QueueTableEntry;
import org.thingsboard.server.common.data.ruleenginemonitoring.QueueTimeseriesEntry;
import org.thingsboard.server.common.data.ruleenginemonitoring.RuleNodeTableRow;
import org.thingsboard.server.common.data.ruleenginemonitoring.RuleNodeTsKvEntry;
import org.thingsboard.server.common.data.ruleenginemonitoring.RuleEngineMonitoringFilters;
import org.thingsboard.server.dao.sql.ruleenginemonitoring.RuleNodeDimension;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.ruleenginemonitoring.RuleEngineMonitoringQueryService;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@TbCoreComponent
@RequestMapping("/api/ruleEngineMonitoring")
@PreAuthorize("hasAuthority('TENANT_ADMIN')")
@RequiredArgsConstructor
public class RuleEngineMonitoringController extends BaseController {
    private final RuleEngineMonitoringQueryService queryService;

    @Value("${database.ts_max_intervals:700}")
    private long maxTsIntervals;

    @GetMapping("/filters")
    public RuleEngineMonitoringFilters getFilterOptions() throws ThingsboardException {
        checkRuleChainPermission(Operation.READ);
        return queryService.getFilters(getTenantId().getId());
    }

    @GetMapping("/nodeStats/timeseries")
    public List<RuleNodeTsKvEntry> getRuleNodeTimeSeries(
            @RequestParam long startTs,
            @RequestParam long endTs,
            @RequestParam(required = false) String groupBy,
            @RequestParam(required = false) List<UUID> queueIds,
            @RequestParam(required = false) List<UUID> ruleChainIds,
            @RequestParam(required = false) List<UUID> ruleNodeIds,
            @RequestParam(required = false) List<String> serviceIds,
            @RequestParam(required = false) Long intervalMs
    ) throws ThingsboardException {
        checkRuleChainPermission(Operation.READ);
        validateTimeseriesQuery(startTs, endTs, intervalMs);
        UUID tenantId = getTenantId().getId();
        return queryService.getNodeStatsTimeseries(
                tenantId, startTs, endTs, parseGroupBy(groupBy),
                queueIds, ruleChainIds, ruleNodeIds, serviceIds, intervalMs);
    }

    @GetMapping("/nodeStats/table")
    public List<RuleNodeTableRow> getRuleNodeTable(
            @RequestParam long startTs,
            @RequestParam long endTs,
            @RequestParam(required = false) String groupBy,
            @RequestParam(required = false) List<UUID> queueIds,
            @RequestParam(required = false) List<UUID> ruleChainIds,
            @RequestParam(required = false) List<UUID> ruleNodeIds,
            @RequestParam(required = false) List<String> serviceIds
    ) throws ThingsboardException {
        checkRuleChainPermission(Operation.READ);
        UUID tenantId = getTenantId().getId();
        return queryService.getNodeStatsTable(
                tenantId, startTs, endTs, parseGroupBy(groupBy),
                queueIds, ruleChainIds, ruleNodeIds, serviceIds);
    }

    @GetMapping("/queueStats/timeseries")
    public List<QueueTimeseriesEntry> getQueueTimeSeries(
            @RequestParam long startTs,
            @RequestParam long endTs,
            @RequestParam(required = false) String groupBy,
            @RequestParam(required = false) List<UUID> queueIds,
            @RequestParam(required = false) List<UUID> ruleChainIds,
            @RequestParam(required = false) List<UUID> ruleNodeIds,
            @RequestParam(required = false) List<String> serviceIds,
            @RequestParam(required = false) Long intervalMs
    ) throws ThingsboardException {
        checkRuleChainPermission(Operation.READ);
        validateTimeseriesQuery(startTs, endTs, intervalMs);
        UUID tenantId = getTenantId().getId();
        return queryService.getQueueStatsTimeseries(
                tenantId, startTs, endTs, parseQueueGroupBy(groupBy),
                queueIds, ruleChainIds, ruleNodeIds, serviceIds, intervalMs);
    }

    @GetMapping("/queueStats/table")
    public List<QueueTableEntry> getQueueTable(
            @RequestParam long startTs,
            @RequestParam long endTs,
            @RequestParam(required = false) String groupBy,
            @RequestParam(required = false) List<UUID> queueIds,
            @RequestParam(required = false) List<UUID> ruleChainIds,
            @RequestParam(required = false) List<UUID> ruleNodeIds,
            @RequestParam(required = false) List<String> serviceIds
    ) throws ThingsboardException {
        checkRuleChainPermission(Operation.READ);
        UUID tenantId = getTenantId().getId();
        return queryService.getQueueStatsTable(
                tenantId, startTs, endTs, parseQueueGroupBy(groupBy),
                queueIds, ruleChainIds, ruleNodeIds, serviceIds);
    }

    @GetMapping("/queueLagStats/timeseries")
    public List<QueueLagTimeseriesEntry> getQueueLagTimeSeries(
            @RequestParam long startTs,
            @RequestParam long endTs,
            @RequestParam(required = false) String groupBy,
            @RequestParam(required = false) List<UUID> queueIds,
            @RequestParam(required = false) Long intervalMs
    ) throws ThingsboardException {
        checkRuleChainPermission(Operation.READ);
        validateTimeseriesQuery(startTs, endTs, intervalMs);
        UUID tenantId = getTenantId().getId();
        return queryService.getQueueLagTimeseries(
                tenantId, startTs, endTs, parseLagGroupBy(groupBy), queueIds, intervalMs);
    }

    @GetMapping("/queueLagStats/last")
    public Long getCurrentQueueLag(
            @RequestParam(required = false) List<UUID> queueIds
    ) throws ThingsboardException {
        checkRuleChainPermission(Operation.READ);
        UUID tenantId = getTenantId().getId();
        return queryService.getCurrentQueueLag(tenantId, queueIds);
    }

    @GetMapping("/stats/table")
    public List<MergedStatsTableRow> getMergedStatsTable(
            @RequestParam long startTs,
            @RequestParam long endTs,
            @RequestParam(required = false) String groupBy,
            @RequestParam(required = false) List<UUID> queueIds,
            @RequestParam(required = false) List<UUID> ruleChainIds,
            @RequestParam(required = false) List<UUID> ruleNodeIds,
            @RequestParam(required = false) List<String> serviceIds
    ) throws ThingsboardException {
        checkRuleChainPermission(Operation.READ);
        return queryService.getMergedStatsTable(
                getTenantId().getId(),
                startTs, endTs,
                parseGroupBy(groupBy), parseQueueGroupBy(groupBy),
                queueIds, ruleChainIds, ruleNodeIds, serviceIds
        );
    }

    @GetMapping("/stats/table/compare")
    public List<MergedStatsDelta> getMergedStatsTableCompare(
            @RequestParam long startTs,
            @RequestParam long endTs,
            @RequestParam long compareStartTs,
            @RequestParam long compareEndTs,
            @RequestParam(required = false) String groupBy,
            @RequestParam(required = false) List<UUID> queueIds,
            @RequestParam(required = false) List<UUID> ruleChainIds,
            @RequestParam(required = false) List<UUID> ruleNodeIds,
            @RequestParam(required = false) List<String> serviceIds
    ) throws ThingsboardException {
        checkRuleChainPermission(Operation.READ);
        return queryService.getMergedStatsTableCompare(
                getTenantId().getId(),
                startTs, endTs,
                compareStartTs, compareEndTs,
                parseGroupBy(groupBy), parseQueueGroupBy(groupBy),
                queueIds, ruleChainIds, ruleNodeIds, serviceIds
        );
    }

    private Set<RuleNodeDimension> parseGroupBy(String groupBy) {
        Set<RuleNodeDimension> dims = new LinkedHashSet<>();
        if (groupBy == null || groupBy.isBlank()) return dims;
        for (String token : groupBy.split(",")) {
            switch (token.trim()) {
                case "tenantId"     -> dims.add(RuleNodeDimension.TENANT_ID);
                case "serviceId"    -> dims.add(RuleNodeDimension.SERVICE_ID);
                case "queueId"      -> dims.add(RuleNodeDimension.QUEUE_ID);
                case "ruleChainId"  -> dims.add(RuleNodeDimension.RULE_CHAIN_ID);
                case "ruleNodeId"   -> dims.add(RuleNodeDimension.RULE_NODE_ID);
            }
        }
        return dims;
    }

    private Set<RuleNodeDimension> parseLagGroupBy(String groupBy) {
        Set<RuleNodeDimension> dims = new LinkedHashSet<>();
        if (groupBy == null || groupBy.isBlank()) return dims;
        for (String token : groupBy.split(",")) {
            switch (token.trim()) {
                case "queueTenantId" -> dims.add(RuleNodeDimension.QUEUE_TENANT_ID);
                case "queueId"       -> dims.add(RuleNodeDimension.QUEUE_ID);
            }
        }
        return dims;
    }

    private Set<RuleNodeDimension> parseQueueGroupBy(String groupBy) {
        Set<RuleNodeDimension> dims = new LinkedHashSet<>();
        if (groupBy == null || groupBy.isBlank()) return dims;
        for (String token : groupBy.split(",")) {
            switch (token.trim()) {
                case "queueTenantId" -> dims.add(RuleNodeDimension.QUEUE_TENANT_ID);
                case "tenantId"      -> dims.add(RuleNodeDimension.TENANT_ID);
                case "serviceId"     -> dims.add(RuleNodeDimension.SERVICE_ID);
                case "queueId"       -> dims.add(RuleNodeDimension.QUEUE_ID);
                case "ruleChainId"   -> dims.add(RuleNodeDimension.RULE_CHAIN_ID);
                case "ruleNodeId"    -> dims.add(RuleNodeDimension.LAST_VISITED_RULE_NODE_ID);
            }
        }
        return dims;
    }

    // Same limits as telemetry queries (BaseTimeseriesService): no intervalMs means the stored buckets are returned as is.
    private void validateTimeseriesQuery(long startTs, long endTs, Long intervalMs) throws ThingsboardException {
        if (endTs <= startTs) {
            throw new ThingsboardException("'endTs' must be greater than 'startTs'", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        if (intervalMs == null) {
            return;
        }
        if (intervalMs < 1) {
            throw new ThingsboardException("'intervalMs' must be greater than 0, but got " + intervalMs, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        long intervals = (endTs - startTs) / Math.max(intervalMs, 1000);
        if (intervals > maxTsIntervals) {
            throw new ThingsboardException("Number of intervals is too high - " + intervals +
                    ". Increase 'intervalMs' or reduce the time range", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }

    private void checkRuleChainPermission(Operation operation) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.RULE_CHAIN, operation);
    }
}

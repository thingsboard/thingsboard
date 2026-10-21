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
package org.thingsboard.server.service.trace;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.QueueId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.trace.RuleNodeTreeNodeDTO;
import org.thingsboard.server.common.data.trace.TraceListFilters;
import org.thingsboard.server.common.data.trace.TracePathDTO;
import org.thingsboard.server.common.data.trace.TracePathDetailsDTO;
import org.thingsboard.server.common.data.trace.TracePathFilters;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.TraceGroup;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.sql.ruleenginemonitoring.RuleEngineMonitoringDao;
import org.thingsboard.server.dao.model.sql.trace.SpanAttributeEntity;
import org.thingsboard.server.dao.model.sql.trace.SpanEntity;
import org.thingsboard.server.dao.model.sql.trace.SpanEventEntity;
import org.thingsboard.server.dao.model.sql.trace.SpanLinkEntity;
import org.thingsboard.server.dao.model.sql.trace.TraceProjection;
import org.thingsboard.server.dao.sql.trace.TraceDao;
import org.thingsboard.server.service.component.ComponentDiscoveryService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TraceService {
    private static final String DELETED_NAME = "Deleted";

    private final TraceDao traceDao;
    private final RuleChainService ruleChainService;
    private final QueueService queueService;
    private final RuleEngineMonitoringDao ruleEngineMonitoringDao;
    private final PartitionService partitionService;
    private final ComponentDiscoveryService componentDiscoveryService;

    public Trace getTrace(TenantId tenantId, String traceId) throws ThingsboardException {
        TraceProjection entity = traceDao.findTraceById(tenantId.getId(), traceId);
        if (entity == null) {
            throw new ThingsboardException("Trace not found: " + traceId, ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
        return Trace.fromEntity(entity, resolveRootQueueName(tenantId, entity.getRootQueueId(), new HashMap<>()));
    }

    public List<SpanEntity> getSpans(TenantId tenantId, TraceSpanListQuery query) {
        return traceDao.findSpansByTraceId(tenantId.getId(), query.getTraceId());
    }

    public List<SpanAttributeEntity> getSpanAttributes(TenantId tenantId, SpanSubResourceQuery query) {
        return traceDao.findSpanAttributes(tenantId.getId(), query.getSpanId(), query.getTraceId());
    }

    public List<SpanEventEntity> getSpanEvents(TenantId tenantId, SpanSubResourceQuery query) {
        return traceDao.findSpanEvents(tenantId.getId(), query.getSpanId(), query.getTraceId());
    }

    public List<SpanLinkEntity> getSpanLinks(TenantId tenantId, SpanSubResourceQuery query) {
        return traceDao.findSpanLinks(tenantId.getId(), query.getSpanId(), query.getTraceId());
    }

    public List<SpanTreeNode> getSpansTree(TenantId tenantId, TraceSpanListQuery query) {
        List<SpanEntity> spans = traceDao.findSpansByTraceId(tenantId.getId(), query.getTraceId());

        if (spans.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, List<SpanAttributeEntity>> attributesBySpanId = new HashMap<>();
        List<SpanAttributeEntity> attributes = traceDao.findSpanAttributesByTraceId(tenantId.getId(), query.getTraceId());
        for (SpanAttributeEntity attribute : attributes) {
            attributesBySpanId.computeIfAbsent(attribute.getSpanId(), ignored -> new ArrayList<>()).add(attribute);
        }

        Map<String, SpanTreeNode> nodeMap = new HashMap<>();

        for (SpanEntity span : spans) {
            List<SpanAttributeEntity> spanAttributes = attributesBySpanId.getOrDefault(span.getSpanId(), new ArrayList<>());
            nodeMap.put(span.getSpanId(), SpanTreeNode.fromEntity(span, spanAttributes));
        }

        List<SpanTreeNode> rootNodes = new ArrayList<>();

        for (SpanEntity span : spans) {
            SpanTreeNode node = nodeMap.get(span.getSpanId());

            if (span.getParentSpanId() == null) {
                rootNodes.add(node);
            } else {
                SpanTreeNode parent = nodeMap.get(span.getParentSpanId());
                if (parent != null) {
                    parent.getChildren().add(node);
                } else {
                    rootNodes.add(node);
                }
            }
        }

        rootNodes.sort(Comparator.comparingLong(SpanTreeNode::getStartTime));

        sortChildrenRecursively(nodeMap.values());

        return rootNodes;
    }

    public PageData<Trace> searchTracesPaginated(TenantId tenantId, TraceSearchQuery query) {
        String sortFieldStr = query.getSortField() != null ? query.getSortField().name() : TraceSortField.START_TIME.name();
        String sortOrderStr = query.getSortOrder() != null ? query.getSortOrder().name() : SortOrder.DESC.name();

        PageData<TraceProjection> entities = traceDao.findTracesPaginated(
                tenantId.getId(), query.getServiceName(), query.getStartTime(), query.getEndTime(), query.getSpanName(), query.getQueueName(),
                query.getOriginatorId(), query.getMessageType(), query.getMessageId(), query.getMessageData(),
                query.getMessageMetadata(), query.getTraceId(), query.isErrorsOnly(),
                query.getRuleChainId(), query.getRuleNodeId(), query.getPathId(), query.isHasTimeOutsByQueue(),
                query.getPage(), query.getPageSize(), sortFieldStr, sortOrderStr);

        Map<String, String> queueNames = new HashMap<>();
        List<Trace> traces = entities.getData().stream()
                .map(entity -> Trace.fromEntity(entity, resolveRootQueueName(tenantId, entity.getRootQueueId(), queueNames)))
                .collect(Collectors.toList());

        return new PageData<>(traces, entities.getTotalPages(), entities.getTotalElements(), entities.hasNext());
    }

    public PageData<TracePathDTO> getPathsPaginated(TenantId tenantId, TracePathListQuery query) {
        if (query.getEndTime() <= query.getStartTime()) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        String sortField = query.getSortField() != null ? query.getSortField().name() : TracePathSortField.TRACES_COUNT.name();
        String sortOrder = query.getSortOrder() != null ? query.getSortOrder().name() : SortOrder.DESC.name();
        PageData<TracePathDTO> pageData = traceDao.findPathsPaginated(
                tenantId.getId(), query.getStartTime(), query.getEndTime(),
                query.getRootQueueId(), query.getRootMessageType(), query.getRootRuleChainId(), query.getRuleNodeId(),
                query.isErrorsOnly(), query.getPage(), query.getPageSize(), sortField, sortOrder);
        enrichPathNames(tenantId, pageData.getData());
        return pageData;
    }

    private void enrichPathNames(TenantId tenantId, List<TracePathDTO> paths) {
        Map<String, String> ruleChainNames = new HashMap<>();
        Map<String, String> queueNames = new HashMap<>();
        for (TracePathDTO path : paths) {
            if (path.getRootRuleChainId() != null && !path.getRootRuleChainId().isEmpty()) {
                path.setRootRuleChainName(ruleChainNames.computeIfAbsent(path.getRootRuleChainId(),
                        id -> resolveRuleChainName(tenantId, id)));
            }
            if (path.getRootQueueId() != null && !path.getRootQueueId().isEmpty()) {
                path.setRootQueueName(queueNames.computeIfAbsent(path.getRootQueueId(),
                        id -> resolveQueueName(tenantId, id)));
            }
        }
    }

    private String resolveRuleChainName(TenantId tenantId, String ruleChainId) {
        RuleChainId id = toRuleChainId(ruleChainId);
        if (id == null) {
            return DELETED_NAME;
        }
        RuleChain ruleChain = ruleChainService.findRuleChainById(tenantId, id);
        return ruleChain != null ? ruleChain.getName() : DELETED_NAME;
    }

    private String resolveQueueName(TenantId tenantId, String queueId) {
        try {
            Queue queue = queueService.findQueueById(tenantId, new QueueId(UUID.fromString(queueId)));
            return queue != null ? queue.getName() : DELETED_NAME;
        } catch (IllegalArgumentException e) {
            log.debug("Ignoring invalid queue id '{}' from trace group", queueId, e);
            return DELETED_NAME;
        }
    }

    private String resolveRootQueueName(TenantId tenantId, String rootQueueId, Map<String, String> cache) {
        if (rootQueueId == null || rootQueueId.isEmpty()) {
            return null;
        }
        return cache.computeIfAbsent(rootQueueId, id -> resolveQueueName(tenantId, id));
    }

    public TracePathFilters getPathFilters(TenantId tenantId) {
        UUID tid = tenantId.getId();
        List<TracePathFilters.Option> queues = ruleEngineMonitoringDao.findFilterQueues(tid).stream()
                .map(q -> new TracePathFilters.Option(q.id() != null ? q.id().toString() : null, q.name()))
                .collect(Collectors.toList());
        List<TracePathFilters.Option> ruleChains = ruleEngineMonitoringDao.findFilterRuleChains(tid).stream()
                .map(c -> new TracePathFilters.Option(c.id() != null ? c.id().toString() : null, c.name()))
                .collect(Collectors.toList());
        List<TracePathFilters.RuleNodeOption> ruleNodes = ruleEngineMonitoringDao.findFilterRuleNodes(tid).stream()
                .map(n -> new TracePathFilters.RuleNodeOption(
                        n.id() != null ? n.id().toString() : null, n.name(),
                        n.ruleChainId() != null ? n.ruleChainId().toString() : null))
                .collect(Collectors.toList());
        List<String> messageTypes = Arrays.stream(TbMsgType.values())
                .map(Enum::name)
                .collect(Collectors.toList());
        return TracePathFilters.builder()
                .queues(queues)
                .ruleChains(ruleChains)
                .ruleNodes(ruleNodes)
                .messageTypes(messageTypes)
                .build();
    }

    public TraceListFilters getTraceFilters(TenantId tenantId) {
        UUID tid = tenantId.getId();
        List<TraceListFilters.Option> queues = ruleEngineMonitoringDao.findFilterQueues(tid).stream()
                .map(q -> new TraceListFilters.Option(q.id() != null ? q.id().toString() : null, q.name()))
                .collect(Collectors.toList());
        List<TraceListFilters.Option> ruleChains = ruleEngineMonitoringDao.findFilterRuleChains(tid).stream()
                .map(c -> new TraceListFilters.Option(c.id() != null ? c.id().toString() : null, c.name()))
                .collect(Collectors.toList());
        List<TraceListFilters.RuleNodeOption> ruleNodes = ruleEngineMonitoringDao.findFilterRuleNodes(tid).stream()
                .map(n -> new TraceListFilters.RuleNodeOption(
                        n.id() != null ? n.id().toString() : null, n.name(),
                        n.ruleChainId() != null ? n.ruleChainId().toString() : null))
                .collect(Collectors.toList());
        List<String> messageTypes = Arrays.stream(TbMsgType.values()).map(Enum::name).collect(Collectors.toList());
        List<String> ruleEngines = partitionService.getAllServiceIds(ServiceType.TB_RULE_ENGINE).stream().toList();
        return TraceListFilters.builder()
                .ruleEngines(ruleEngines)
                .queues(queues)
                .ruleChains(ruleChains)
                .ruleNodes(ruleNodes)
                .messageTypes(messageTypes)
                .build();
    }

    public TracePathDetailsDTO getPathDetails(TenantId tenantId, String pathId, long startTime, long endTime) throws ThingsboardException {
        if (endTime <= startTime) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        List<TraceGroup> rows = traceDao.findTraceGroupsByPathId(tenantId.getId(), pathId, startTime, endTime);
        if (rows.isEmpty()) {
            throw new ThingsboardException("Execution path not found: " + pathId, ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
        TraceGroup first = rows.get(0);

        long processedTraces = 0;
        long processedTracesWithTimeout = 0;
        long processedTracesWithError = 0;
        long storedTraces = 0;
        long totalTime = 0;
        long maxTime = 0;
        long totalInQueueTime = 0;
        long totalRuleNodeProcessingTime = 0;
        long lastObserved = 0;
        for (TraceGroup row : rows) {
            processedTraces += row.getProcessedTraces();
            processedTracesWithTimeout += row.getProcessedTracesWithTimeout();
            processedTracesWithError += row.getProcessedTracesWithError();
            storedTraces += row.getStoredTraces();
            totalTime += row.getTotalTime();
            maxTime = Math.max(maxTime, row.getMaxTime());
            totalInQueueTime += row.getTotalInQueueTime();
            totalRuleNodeProcessingTime += row.getTotalRuleNodeProcessingTime();
            lastObserved = Math.max(lastObserved, row.getLastObserved());
        }

        Map<String, TraceGroup.RuleNodePathMetric> mergedMetrics = new HashMap<>();
        for (TraceGroup row : rows) {
            TraceGroup.mergeRuleNodePathMetrics(mergedMetrics, row.getRuleNodePathMetrics());
        }
        long errorCount = mergedMetrics.values().stream().mapToLong(TraceGroup.RuleNodePathMetric::getErrors).sum();

        Map<UUID, MergedVisitedNode> nodes = new LinkedHashMap<>();
        for (TraceGroup row : rows) {
            if (row.getVisitedRuleNodes() == null) {
                continue;
            }
            for (TraceGroup.VisitedRuleNode visited : row.getVisitedRuleNodes()) {
                if (visited.getRuleNodeId() == null) {
                    continue;
                }
                MergedVisitedNode merged = nodes.computeIfAbsent(visited.getRuleNodeId(),
                        k -> new MergedVisitedNode(visited.getQueueId(), visited.getRuleChainId(), visited.getRuleNodeId()));
                if (visited.getFromNodeConnections() != null) {
                    merged.getIncoming().addAll(visited.getFromNodeConnections());
                }
                if (visited.getToNodeConnections() != null) {
                    merged.getOutgoing().addAll(visited.getToNodeConnections());
                }
            }
        }

        List<RuleNodeTreeNodeDTO> tree = buildRuleNodeTree(tenantId, nodes, mergedMetrics);

        return TracePathDetailsDTO.builder()
                .pathId(pathId)
                .rootQueueId(uuidStr(first.getRootQueueId()))
                .rootQueueName(first.getRootQueueId() != null ? resolveQueueName(tenantId, first.getRootQueueId().toString()) : null)
                .rootMessageType(first.getRootMessageType())
                .rootRuleChainId(uuidStr(first.getRootRuleChainId()))
                .rootRuleChainName(first.getRootRuleChainId() != null ? resolveRuleChainName(tenantId, first.getRootRuleChainId().toString()) : null)
                .processedTraces(processedTraces)
                .processedTracesWithTimeout(processedTracesWithTimeout)
                .processedTracesWithError(processedTracesWithError)
                .storedTraces(storedTraces)
                .totalTime(totalTime)
                .maxTime(maxTime)
                .totalInQueueTime(totalInQueueTime)
                .totalRuleNodeProcessingTime(totalRuleNodeProcessingTime)
                .errorCount(errorCount)
                .lastObserved(lastObserved)
                .ruleNodeTree(tree)
                .build();
    }

    private List<RuleNodeTreeNodeDTO> buildRuleNodeTree(TenantId tenantId, Map<UUID, MergedVisitedNode> nodes,
                                                        Map<String, TraceGroup.RuleNodePathMetric> metrics) {
        Map<UUID, String> ruleChainNames = new HashMap<>();
        Map<UUID, String[]> ruleNodeInfo = new HashMap<>();
        Map<UUID, String> queueNames = new HashMap<>();
        Set<UUID> placed = new HashSet<>();
        List<RuleNodeTreeNodeDTO> roots = new ArrayList<>();

        for (MergedVisitedNode node : nodes.values()) {
            if (node.getIncoming().isEmpty()) {
                roots.add(toTreeNode(tenantId, node, nodes, metrics, placed, ruleChainNames, ruleNodeInfo, queueNames));
            }
        }

        for (MergedVisitedNode node : nodes.values()) {
            if (!placed.contains(node.getRuleNodeId())) {
                roots.add(toTreeNode(tenantId, node, nodes, metrics, placed, ruleChainNames, ruleNodeInfo, queueNames));
            }
        }
        return roots;
    }

    private RuleNodeTreeNodeDTO toTreeNode(TenantId tenantId, MergedVisitedNode node, Map<UUID, MergedVisitedNode> nodes,
                                           Map<String, TraceGroup.RuleNodePathMetric> metrics, Set<UUID> placed,
                                           Map<UUID, String> ruleChainNames, Map<UUID, String[]> ruleNodeInfo,
                                           Map<UUID, String> queueNames) {
        placed.add(node.getRuleNodeId());
        List<RuleNodeTreeNodeDTO> children = new ArrayList<>();
        for (UUID childId : node.getOutgoing()) {
            MergedVisitedNode child = nodes.get(childId);
            if (child == null || placed.contains(childId)) {
                continue;
            }
            children.add(toTreeNode(tenantId, child, nodes, metrics, placed, ruleChainNames, ruleNodeInfo, queueNames));
        }
        TraceGroup.RuleNodePathMetric metric = metrics.get(node.getRuleNodeId().toString());
        String[] info = ruleNodeInfo.computeIfAbsent(node.getRuleNodeId(), id -> resolveRuleNodeInfo(tenantId, id));
        return RuleNodeTreeNodeDTO.builder()
                .ruleNodeId(node.getRuleNodeId().toString())
                .ruleNodeName(info[0])
                .ruleNodeType(info[1])
                .ruleChainId(uuidStr(node.getRuleChainId()))
                .ruleChainName(node.getRuleChainId() != null
                        ? ruleChainNames.computeIfAbsent(node.getRuleChainId(), id -> resolveRuleChainName(tenantId, id.toString()))
                        : null)
                .queueId(uuidStr(node.getQueueId()))
                .queueName(node.getQueueId() != null
                        ? queueNames.computeIfAbsent(node.getQueueId(), id -> resolveQueueName(tenantId, id.toString()))
                        : null)
                .relation(null)
                .totalTime(metric != null ? metric.getTotalTime() : 0L)
                .totalCount(metric != null ? metric.getTotalCount() : 0L)
                .errors(metric != null ? metric.getErrors() : 0L)
                .children(children)
                .build();
    }

    private String[] resolveRuleNodeInfo(TenantId tenantId, UUID ruleNodeId) {
        RuleNode ruleNode = ruleChainService.findRuleNodeById(tenantId, new RuleNodeId(ruleNodeId));
        return ruleNode != null ? new String[]{ruleNode.getName(), resolveRuleNodeTypeName(ruleNode.getType())} : new String[]{DELETED_NAME, null};
    }

    private String resolveRuleNodeTypeName(String type) {
        if (type == null || type.isEmpty()) {
            return null;
        }
        return componentDiscoveryService.getRuleNodeInfo(type)
                .map(info -> info.getAnnotation().name())
                .orElseGet(() -> shortRuleNodeType(type));
    }

    private static String shortRuleNodeType(String type) {
        if (type == null || type.isEmpty()) {
            return null;
        }
        int idx = type.lastIndexOf('.');
        return idx >= 0 ? type.substring(idx + 1) : type;
    }

    private static String uuidStr(UUID uuid) {
        return uuid != null ? uuid.toString() : null;
    }

    private RuleChainId toRuleChainId(String ruleChainId) {
        try {
            return new RuleChainId(UUID.fromString(ruleChainId));
        } catch (IllegalArgumentException e) {
            log.debug("Ignoring invalid rule chain id '{}' from span attributes", ruleChainId, e);
            return null;
        }
    }

    private void sortChildrenRecursively(Collection<SpanTreeNode> nodes) {
        for (SpanTreeNode node : nodes) {
            if (!node.getChildren().isEmpty()) {
                node.getChildren().sort(Comparator.comparingLong(SpanTreeNode::getStartTime));
                sortChildrenRecursively(node.getChildren());
            }
        }
    }
}

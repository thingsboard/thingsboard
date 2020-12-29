/**
 * Copyright © 2016-2020 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.rule;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.NodeConnectionInfo;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainConnectionInfo;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.tenant.TenantDao;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static org.thingsboard.server.dao.service.Validator.validateId;

/**
 * Created by igor on 3/12/18.
 */
@Service
@Slf4j
public class BaseRuleChainService extends AbstractEntityService implements RuleChainService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";

    @Autowired
    private RuleChainDao ruleChainDao;

    @Autowired
    private RuleNodeDao ruleNodeDao;

    @Autowired
    private TenantDao tenantDao;

    @Override
    public RuleChain saveRuleChain(RuleChain ruleChain) {
        ruleChainValidator.validate(ruleChain, RuleChain::getTenantId);
        RuleChain savedRuleChain = ruleChainDao.save(ruleChain.getTenantId(), ruleChain);
        if (ruleChain.isRoot() && ruleChain.getId() == null) {
            try {
                createRelation(ruleChain.getTenantId(), new EntityRelation(savedRuleChain.getTenantId(), savedRuleChain.getId(),
                        EntityRelation.CONTAINS_TYPE, RelationTypeGroup.RULE_CHAIN));
            } catch (Exception e) {
                log.warn("[{}] Failed to create tenant to root rule chain relation. from: [{}], to: [{}]",
                        savedRuleChain.getTenantId(), savedRuleChain.getTenantId(), savedRuleChain.getId(), e);
                throw new RuntimeException(e);
            }
        }
        return savedRuleChain;
    }

    @Override
    public boolean setRootRuleChain(TenantId tenantId, RuleChainId ruleChainId) {
        RuleChain ruleChain = ruleChainDao.findById(tenantId, ruleChainId.getId());
        if (!ruleChain.isRoot()) {
            RuleChain previousRootRuleChain = getRootTenantRuleChain(ruleChain.getTenantId());
            try {
                if (previousRootRuleChain == null) {
                    setRootAndSave(tenantId, ruleChain);
                    return true;
                } else if (!previousRootRuleChain.getId().equals(ruleChain.getId())) {
                    deleteRelation(tenantId, new EntityRelation(previousRootRuleChain.getTenantId(), previousRootRuleChain.getId(),
                            EntityRelation.CONTAINS_TYPE, RelationTypeGroup.RULE_CHAIN));
                    previousRootRuleChain.setRoot(false);
                    ruleChainDao.save(tenantId, previousRootRuleChain);
                    setRootAndSave(tenantId, ruleChain);
                    return true;
                }
            } catch (ExecutionException | InterruptedException e) {
                log.warn("[{}] Failed to set root rule chain, ruleChainId: [{}]", ruleChainId);
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    private void setRootAndSave(TenantId tenantId, RuleChain ruleChain) throws ExecutionException, InterruptedException {
        createRelation(tenantId, new EntityRelation(ruleChain.getTenantId(), ruleChain.getId(),
                EntityRelation.CONTAINS_TYPE, RelationTypeGroup.RULE_CHAIN));
        ruleChain.setRoot(true);
        ruleChainDao.save(tenantId, ruleChain);
    }

    @Override
    public RuleChainMetaData saveRuleChainMetaData(TenantId tenantId, RuleChainMetaData ruleChainMetaData) {
        Validator.validateId(ruleChainMetaData.getRuleChainId(), "Incorrect rule chain id.");
        RuleChain ruleChain = findRuleChainById(tenantId, ruleChainMetaData.getRuleChainId());
        if (ruleChain == null) {
            return null;
        }

        if (CollectionUtils.isNotEmpty(ruleChainMetaData.getConnections())) {
            validateCircles(ruleChainMetaData.getConnections());
        }

        List<RuleNode> nodes = ruleChainMetaData.getNodes();
        List<RuleNode> toAddOrUpdate = new ArrayList<>();
        List<RuleNode> toDelete = new ArrayList<>();

        Map<RuleNodeId, Integer> ruleNodeIndexMap = new HashMap<>();
        if (nodes != null) {
            for (RuleNode node : nodes) {
                if (node.getId() != null) {
                    ruleNodeIndexMap.put(node.getId(), nodes.indexOf(node));
                } else {
                    toAddOrUpdate.add(node);
                }
            }
        }

        List<RuleNode> existingRuleNodes = getRuleChainNodes(tenantId, ruleChainMetaData.getRuleChainId());
        for (RuleNode existingNode : existingRuleNodes) {
            deleteEntityRelations(tenantId, existingNode.getId());
            Integer index = ruleNodeIndexMap.get(existingNode.getId());
            if (index != null) {
                toAddOrUpdate.add(ruleChainMetaData.getNodes().get(index));
            } else {
                toDelete.add(existingNode);
            }
        }
        for (RuleNode node : toAddOrUpdate) {
            node.setRuleChainId(ruleChain.getId());
            RuleNode savedNode = ruleNodeDao.save(tenantId, node);
            try {
                createRelation(tenantId, new EntityRelation(ruleChainMetaData.getRuleChainId(), savedNode.getId(),
                        EntityRelation.CONTAINS_TYPE, RelationTypeGroup.RULE_CHAIN));
            } catch (Exception e) {
                log.warn("[{}] Failed to create rule chain to rule node relation. from: [{}], to: [{}]",
                        ruleChainMetaData.getRuleChainId(), savedNode.getId());
                throw new RuntimeException(e);
            }
            int index = nodes.indexOf(node);
            nodes.set(index, savedNode);
            ruleNodeIndexMap.put(savedNode.getId(), index);
        }
        for (RuleNode node : toDelete) {
            deleteRuleNode(tenantId, node.getId());
        }
        RuleNodeId firstRuleNodeId = null;
        if (ruleChainMetaData.getFirstNodeIndex() != null) {
            firstRuleNodeId = nodes.get(ruleChainMetaData.getFirstNodeIndex()).getId();
        }
        if ((ruleChain.getFirstRuleNodeId() != null && !ruleChain.getFirstRuleNodeId().equals(firstRuleNodeId))
                || (ruleChain.getFirstRuleNodeId() == null && firstRuleNodeId != null)) {
            ruleChain.setFirstRuleNodeId(firstRuleNodeId);
            ruleChainDao.save(tenantId, ruleChain);
        }
        if (ruleChainMetaData.getConnections() != null) {
            for (NodeConnectionInfo nodeConnection : ruleChainMetaData.getConnections()) {
                EntityId from = nodes.get(nodeConnection.getFromIndex()).getId();
                EntityId to = nodes.get(nodeConnection.getToIndex()).getId();
                String type = nodeConnection.getType();
                try {
                    createRelation(tenantId, new EntityRelation(from, to, type, RelationTypeGroup.RULE_NODE));
                } catch (Exception e) {
                    log.warn("[{}] Failed to create rule node relation. from: [{}], to: [{}]", from, to);
                    throw new RuntimeException(e);
                }
            }
        }
        if (ruleChainMetaData.getRuleChainConnections() != null) {
            for (RuleChainConnectionInfo nodeToRuleChainConnection : ruleChainMetaData.getRuleChainConnections()) {
                EntityId from = nodes.get(nodeToRuleChainConnection.getFromIndex()).getId();
                EntityId to = nodeToRuleChainConnection.getTargetRuleChainId();
                String type = nodeToRuleChainConnection.getType();
                try {
                    createRelation(tenantId, new EntityRelation(from, to, type, RelationTypeGroup.RULE_NODE, nodeToRuleChainConnection.getAdditionalInfo()));
                } catch (Exception e) {
                    log.warn("[{}] Failed to create rule node to rule chain relation. from: [{}], to: [{}]", from, to);
                    throw new RuntimeException(e);
                }
            }
        }

        return loadRuleChainMetaData(tenantId, ruleChainMetaData.getRuleChainId());
    }

    private void validateCircles(List<NodeConnectionInfo> connectionInfos) {
        Map<Integer, Set<Integer>> connectionsMap = new HashMap<>();
        for (NodeConnectionInfo nodeConnection : connectionInfos) {
            if (nodeConnection.getFromIndex() == nodeConnection.getToIndex()) {
                throw new DataValidationException("Can't create the relation to yourself.");
            }
            connectionsMap
                    .computeIfAbsent(nodeConnection.getFromIndex(), from -> new HashSet<>())
                    .add(nodeConnection.getToIndex());
        }
        connectionsMap.keySet().forEach(key -> validateCircles(key, connectionsMap.get(key), connectionsMap));
    }

    private void validateCircles(int from, Set<Integer> toList, Map<Integer, Set<Integer>> connectionsMap) {
        if (toList == null) {
            return;
        }
        for (Integer to : toList) {
            if (from == to) {
                throw new DataValidationException("Can't create circling relations in rule chain.");
            }
            validateCircles(from, connectionsMap.get(to), connectionsMap);
        }
    }

    @Override
    public RuleChainMetaData loadRuleChainMetaData(TenantId tenantId, RuleChainId ruleChainId) {
        Validator.validateId(ruleChainId, "Incorrect rule chain id.");
        RuleChain ruleChain = findRuleChainById(tenantId, ruleChainId);
        if (ruleChain == null) {
            return null;
        }
        RuleChainMetaData ruleChainMetaData = new RuleChainMetaData();
        ruleChainMetaData.setRuleChainId(ruleChainId);
        List<RuleNode> ruleNodes = getRuleChainNodes(tenantId, ruleChainId);
        Map<RuleNodeId, Integer> ruleNodeIndexMap = new HashMap<>();
        for (RuleNode node : ruleNodes) {
            ruleNodeIndexMap.put(node.getId(), ruleNodes.indexOf(node));
        }
        ruleChainMetaData.setNodes(ruleNodes);
        if (ruleChain.getFirstRuleNodeId() != null) {
            ruleChainMetaData.setFirstNodeIndex(ruleNodeIndexMap.get(ruleChain.getFirstRuleNodeId()));
        }
        for (RuleNode node : ruleNodes) {
            int fromIndex = ruleNodeIndexMap.get(node.getId());
            List<EntityRelation> nodeRelations = getRuleNodeRelations(tenantId, node.getId());
            for (EntityRelation nodeRelation : nodeRelations) {
                String type = nodeRelation.getType();
                if (nodeRelation.getTo().getEntityType() == EntityType.RULE_NODE) {
                    RuleNodeId toNodeId = new RuleNodeId(nodeRelation.getTo().getId());
                    int toIndex = ruleNodeIndexMap.get(toNodeId);
                    ruleChainMetaData.addConnectionInfo(fromIndex, toIndex, type);
                } else if (nodeRelation.getTo().getEntityType() == EntityType.RULE_CHAIN) {
                    RuleChainId targetRuleChainId = new RuleChainId(nodeRelation.getTo().getId());
                    ruleChainMetaData.addRuleChainConnectionInfo(fromIndex, targetRuleChainId, type, nodeRelation.getAdditionalInfo());
                }
            }
        }
        return ruleChainMetaData;
    }

    @Override
    public RuleChain findRuleChainById(TenantId tenantId, RuleChainId ruleChainId) {
        Validator.validateId(ruleChainId, "Incorrect rule chain id for search request.");
        return ruleChainDao.findById(tenantId, ruleChainId.getId());
    }

    @Override
    public RuleNode findRuleNodeById(TenantId tenantId, RuleNodeId ruleNodeId) {
        Validator.validateId(ruleNodeId, "Incorrect rule node id for search request.");
        return ruleNodeDao.findById(tenantId, ruleNodeId.getId());
    }

    @Override
    public ListenableFuture<RuleChain> findRuleChainByIdAsync(TenantId tenantId, RuleChainId ruleChainId) {
        Validator.validateId(ruleChainId, "Incorrect rule chain id for search request.");
        return ruleChainDao.findByIdAsync(tenantId, ruleChainId.getId());
    }

    @Override
    public ListenableFuture<RuleNode> findRuleNodeByIdAsync(TenantId tenantId, RuleNodeId ruleNodeId) {
        Validator.validateId(ruleNodeId, "Incorrect rule node id for search request.");
        return ruleNodeDao.findByIdAsync(tenantId, ruleNodeId.getId());
    }

    @Override
    public RuleChain getRootTenantRuleChain(TenantId tenantId) {
        return getRootRuleChainByType(tenantId, RuleChainType.CORE);
    }

    private RuleChain getRootRuleChainByType(TenantId tenantId, RuleChainType type) {
        Validator.validateId(tenantId, "Incorrect tenant id for search request.");
        List<EntityRelation> relations = relationService.findByFrom(tenantId, tenantId, RelationTypeGroup.RULE_CHAIN);
        if (relations != null && !relations.isEmpty()) {
            for (EntityRelation relation : relations) {
                RuleChainId ruleChainId = new RuleChainId(relation.getTo().getId());
                RuleChain ruleChainById = findRuleChainById(tenantId, ruleChainId);
                if (type.equals(ruleChainById.getType())) {
                    return ruleChainById;
                }
            }
            return null;
        } else {
            return null;
        }
    }

    @Override
    public List<RuleNode> getRuleChainNodes(TenantId tenantId, RuleChainId ruleChainId) {
        Validator.validateId(ruleChainId, "Incorrect rule chain id for search request.");
        List<EntityRelation> relations = getRuleChainToNodeRelations(tenantId, ruleChainId);
        List<RuleNode> ruleNodes = new ArrayList<>();
        for (EntityRelation relation : relations) {
            RuleNode ruleNode = ruleNodeDao.findById(tenantId, relation.getTo().getId());
            if (ruleNode != null) {
                ruleNodes.add(ruleNode);
            } else {
                relationService.deleteRelation(tenantId, relation);
            }
        }
        return ruleNodes;
    }

    @Override
    public List<RuleNode> getReferencingRuleChainNodes(TenantId tenantId, RuleChainId ruleChainId) {
        Validator.validateId(ruleChainId, "Incorrect rule chain id for search request.");
        List<EntityRelation> relations = getNodeToRuleChainRelations(tenantId, ruleChainId);
        List<RuleNode> ruleNodes = new ArrayList<>();
        for (EntityRelation relation : relations) {
            RuleNode ruleNode = ruleNodeDao.findById(tenantId, relation.getFrom().getId());
            if (ruleNode != null) {
                ruleNodes.add(ruleNode);
            }
        }
        return ruleNodes;
    }

    @Override
    public List<EntityRelation> getRuleNodeRelations(TenantId tenantId, RuleNodeId ruleNodeId) {
        Validator.validateId(ruleNodeId, "Incorrect rule node id for search request.");
        List<EntityRelation> relations = relationService.findByFrom(tenantId, ruleNodeId, RelationTypeGroup.RULE_NODE);
        List<EntityRelation> validRelations = new ArrayList<>();
        for (EntityRelation relation : relations) {
            boolean valid = true;
            EntityType toType = relation.getTo().getEntityType();
            if (toType == EntityType.RULE_NODE || toType == EntityType.RULE_CHAIN) {
                BaseData entity;
                if (relation.getTo().getEntityType() == EntityType.RULE_NODE) {
                    entity = ruleNodeDao.findById(tenantId, relation.getTo().getId());
                } else {
                    entity = ruleChainDao.findById(tenantId, relation.getTo().getId());
                }
                if (entity == null) {
                    relationService.deleteRelation(tenantId, relation);
                    valid = false;
                }
            }
            if (valid) {
                validRelations.add(relation);
            }
        }
        return validRelations;
    }

    @Override
    public TextPageData<RuleChain> findTenantRuleChainsByType(TenantId tenantId, RuleChainType type, TextPageLink pageLink) {
        Validator.validateId(tenantId, "Incorrect tenant id for search rule chain request.");
        Validator.validatePageLink(pageLink, "Incorrect PageLink object for search rule chain request.");
        List<RuleChain> ruleChains = ruleChainDao.findRuleChainsByTenantIdAndType(tenantId.getId(), type, pageLink);
        return new TextPageData<>(ruleChains, pageLink);
    }

    @Override
    public void deleteRuleChainById(TenantId tenantId, RuleChainId ruleChainId) {
        Validator.validateId(ruleChainId, "Incorrect rule chain id for delete request.");
        RuleChain ruleChain = ruleChainDao.findById(tenantId, ruleChainId.getId());
        if (ruleChain != null) {
            if (ruleChain.isRoot()) {
                throw new DataValidationException("Deletion of Root Tenant Rule Chain is prohibited!");
            }
            if (RuleChainType.EDGE.equals(ruleChain.getType())) {
                try {
                    List<Edge> edges = edgeService.findEdgesByTenantIdAndRuleChainId(tenantId, ruleChainId).get();
                    if (edges != null && !edges.isEmpty()) {
                        for (Edge edge : edges) {
                            if (edge.getRootRuleChainId() != null && edge.getRootRuleChainId().equals(ruleChainId)) {
                                throw new DataValidationException("Can't delete rule chain that is root for edge [" + edge.getName() + "]. Please assign another root rule chain first to the edge!");
                            }
                        }
                    }
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Can't get edges by tenant id [{}] and rule chain id [{}]", tenantId.getId(), ruleChainId.getId(), e);
                }
            }
        }
        checkRuleNodesAndDelete(tenantId, ruleChainId);
    }

    @Override
    public void deleteRuleChainsByTenantId(TenantId tenantId) {
        Validator.validateId(tenantId, "Incorrect tenant id for delete rule chains request.");
        tenantRuleChainsRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    public RuleChain assignRuleChainToEdge(TenantId tenantId, RuleChainId ruleChainId, EdgeId edgeId) {
        RuleChain ruleChain = findRuleChainById(tenantId, ruleChainId);
        Edge edge = edgeService.findEdgeById(tenantId, edgeId);
        if (edge == null) {
            throw new DataValidationException("Can't assign ruleChain to non-existent edge!");
        }
        if (!edge.getTenantId().equals(ruleChain.getTenantId())) {
            throw new DataValidationException("Can't assign ruleChain to edge from different tenant!");
        }
        try {
            createRelation(tenantId, new EntityRelation(edgeId, ruleChainId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE));
        } catch (Exception e) {
            log.warn("[{}] Failed to create ruleChain relation. Edge Id: [{}]", ruleChainId, edgeId);
            throw new RuntimeException(e);
        }
        return ruleChain;
    }

    @Override
    public RuleChain unassignRuleChainFromEdge(TenantId tenantId, RuleChainId ruleChainId, EdgeId edgeId, boolean remove) {
        RuleChain ruleChain = findRuleChainById(tenantId, ruleChainId);
        Edge edge = edgeService.findEdgeById(tenantId, edgeId);
        if (edge == null) {
            throw new DataValidationException("Can't unassign rule chain from non-existent edge!");
        }
        if (!remove && edge.getRootRuleChainId() != null && edge.getRootRuleChainId().equals(ruleChainId)) {
            throw new DataValidationException("Can't unassign root rule chain from edge [" + edge.getName() + "]. Please assign another root rule chain first!");
        }
        try {
            deleteRelation(tenantId, new EntityRelation(edgeId, ruleChainId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE));
        } catch (Exception e) {
            log.warn("[{}] Failed to delete rule chain relation. Edge Id: [{}]", ruleChainId, edgeId);
            throw new RuntimeException(e);
        }
        return ruleChain;
    }

    @Override
    public ListenableFuture<TimePageData<RuleChain>> findRuleChainsByTenantIdAndEdgeId(TenantId tenantId, EdgeId edgeId, TimePageLink pageLink) {
        log.trace("Executing findRuleChainsByTenantIdAndEdgeId, tenantId [{}], edgeId [{}], pageLink [{}]", tenantId, edgeId, pageLink);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        Validator.validateId(edgeId, "Incorrect edgeId " + edgeId);
        Validator.validatePageLink(pageLink, "Incorrect page link " + pageLink);
        ListenableFuture<List<RuleChain>> ruleChains = ruleChainDao.findRuleChainsByTenantIdAndEdgeId(tenantId.getId(), edgeId.getId(), pageLink);

        return Futures.transform(ruleChains, new Function<List<RuleChain>, TimePageData<RuleChain>>() {
            @Nullable
            @Override
            public TimePageData<RuleChain> apply(@Nullable List<RuleChain> ruleChains) {
                return new TimePageData<>(ruleChains, pageLink);
            }
        }, MoreExecutors.directExecutor());
    }

    @Override
    public RuleChain getEdgeTemplateRootRuleChain(TenantId tenantId) {
        return getRootRuleChainByType(tenantId, RuleChainType.EDGE);
    }

    @Override
    public boolean setEdgeTemplateRootRuleChain(TenantId tenantId, RuleChainId ruleChainId) {
        RuleChain ruleChain = ruleChainDao.findById(tenantId, ruleChainId.getId());
        RuleChain previousEdgeTemplateRootRuleChain = getEdgeTemplateRootRuleChain(ruleChain.getTenantId());
        if (previousEdgeTemplateRootRuleChain == null || !previousEdgeTemplateRootRuleChain.getId().equals(ruleChain.getId())) {
            try {
                if (previousEdgeTemplateRootRuleChain != null) {
                    deleteRelation(tenantId, new EntityRelation(previousEdgeTemplateRootRuleChain.getTenantId(), previousEdgeTemplateRootRuleChain.getId(),
                            EntityRelation.CONTAINS_TYPE, RelationTypeGroup.RULE_CHAIN));
                    previousEdgeTemplateRootRuleChain.setRoot(false);
                    ruleChainDao.save(tenantId, previousEdgeTemplateRootRuleChain);
                }
                createRelation(tenantId, new EntityRelation(ruleChain.getTenantId(), ruleChain.getId(),
                        EntityRelation.CONTAINS_TYPE, RelationTypeGroup.RULE_CHAIN));
                ruleChain.setRoot(true);
                ruleChainDao.save(tenantId, ruleChain);
                return true;
            } catch (Exception e) {
                log.warn("Failed to set edge template root rule chain, ruleChainId: [{}]", ruleChainId, e);
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    @Override
    public boolean setAutoAssignToEdgeRuleChain(TenantId tenantId, RuleChainId ruleChainId) {
        try {
            createRelation(tenantId, new EntityRelation(tenantId, ruleChainId,
                    EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE_AUTO_ASSIGN_RULE_CHAIN));
            return true;
        } catch (Exception e) {
            log.warn("Failed to set auto assign to edge rule chain, ruleChainId: [{}]", ruleChainId, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean unsetAutoAssignToEdgeRuleChain(TenantId tenantId, RuleChainId ruleChainId) {
        try {
            deleteRelation(tenantId, new EntityRelation(tenantId, ruleChainId,
                    EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE_AUTO_ASSIGN_RULE_CHAIN));
            return true;
        } catch (Exception e) {
            log.warn("Failed to unset auto assign to edge rule chain, ruleChainId: [{}]", ruleChainId, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public ListenableFuture<List<RuleChain>> findAutoAssignToEdgeRuleChainsByTenantId(TenantId tenantId) {
        log.trace("Executing findAutoAssignToEdgeRuleChainsByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return ruleChainDao.findAutoAssignToEdgeRuleChainsByTenantId(tenantId.getId());
    }


    private void checkRuleNodesAndDelete(TenantId tenantId, RuleChainId ruleChainId) {
        List<EntityRelation> nodeRelations = getRuleChainToNodeRelations(tenantId, ruleChainId);
        for (EntityRelation relation : nodeRelations) {
            deleteRuleNode(tenantId, relation.getTo());
        }
        deleteEntityRelations(tenantId, ruleChainId);
        ruleChainDao.removeById(tenantId, ruleChainId.getId());
    }

    private List<EntityRelation> getRuleChainToNodeRelations(TenantId tenantId, RuleChainId ruleChainId) {
        return relationService.findByFrom(tenantId, ruleChainId, RelationTypeGroup.RULE_CHAIN);
    }

    private List<EntityRelation> getNodeToRuleChainRelations(TenantId tenantId, RuleChainId ruleChainId) {
        return relationService.findByTo(tenantId, ruleChainId, RelationTypeGroup.RULE_NODE);
    }

    private void deleteRuleNode(TenantId tenantId, EntityId entityId) {
        deleteEntityRelations(tenantId, entityId);
        ruleNodeDao.removeById(tenantId, entityId.getId());
    }


    private DataValidator<RuleChain> ruleChainValidator =
            new DataValidator<RuleChain>() {
                @Override
                protected void validateDataImpl(TenantId tenantId, RuleChain ruleChain) {
                    if (StringUtils.isEmpty(ruleChain.getName())) {
                        throw new DataValidationException("Rule chain name should be specified!");
                    }
                    if (ruleChain.getType() == null) {
                        ruleChain.setType(RuleChainType.CORE);
                    }
                    if (ruleChain.getTenantId() == null || ruleChain.getTenantId().isNullUid()) {
                        throw new DataValidationException("Rule chain should be assigned to tenant!");
                    }
                    Tenant tenant = tenantDao.findById(tenantId, ruleChain.getTenantId().getId());
                    if (tenant == null) {
                        throw new DataValidationException("Rule chain is referencing to non-existent tenant!");
                    }
                    if (ruleChain.isRoot() && RuleChainType.CORE.equals(ruleChain.getType())) {
                        RuleChain rootRuleChain = getRootTenantRuleChain(ruleChain.getTenantId());
                        if (rootRuleChain != null && !rootRuleChain.getId().equals(ruleChain.getId())) {
                            throw new DataValidationException("Another root rule chain is present in scope of current tenant!");
                        }
                    }
                    if (ruleChain.isRoot() && RuleChainType.EDGE.equals(ruleChain.getType())) {
                        RuleChain edgeTemplateRootRuleChain = getEdgeTemplateRootRuleChain(ruleChain.getTenantId());
                        if (edgeTemplateRootRuleChain != null && !edgeTemplateRootRuleChain.getId().equals(ruleChain.getId())) {
                            throw new DataValidationException("Another edge template root rule chain is present in scope of current tenant!");
                        }
                    }
                }
            };

    private PaginatedRemover<TenantId, RuleChain> tenantRuleChainsRemover =
            new PaginatedRemover<TenantId, RuleChain>() {

                @Override
                protected List<RuleChain> findEntities(TenantId tenantId, TenantId id, TextPageLink pageLink) {
                    return ruleChainDao.findRuleChainsByTenantId(id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, RuleChain entity) {
                    checkRuleNodesAndDelete(tenantId, entity.getId());
                }
            };
}

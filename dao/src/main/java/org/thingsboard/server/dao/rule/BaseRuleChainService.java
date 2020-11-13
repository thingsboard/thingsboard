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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.NodeConnectionInfo;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainConnectionInfo;
import org.thingsboard.server.common.data.rule.RuleChainData;
import org.thingsboard.server.common.data.rule.RuleChainImportResult;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantDao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.DataConstants.TENANT;

/**
 * Created by igor on 3/12/18.
 */
@Service
@Slf4j
public class BaseRuleChainService extends AbstractEntityService implements RuleChainService {

    private static final int DEFAULT_PAGE_SIZE = 1000;
    @Autowired
    private RuleChainDao ruleChainDao;

    @Autowired
    private RuleNodeDao ruleNodeDao;

    @Autowired
    private TenantDao tenantDao;

    @Autowired
    @Lazy
    private TbTenantProfileCache tenantProfileCache;

    @Override
    public RuleChain saveRuleChain(RuleChain ruleChain) {
        ruleChainValidator.validate(ruleChain, RuleChain::getTenantId);
        RuleChain savedRuleChain = ruleChainDao.save(ruleChain.getTenantId(), ruleChain);
        if (ruleChain.isRoot() && ruleChain.getId() == null) {
            try {
                createRelation(ruleChain.getTenantId(), new EntityRelation(savedRuleChain.getTenantId(), savedRuleChain.getId(),
                        EntityRelation.CONTAINS_TYPE, RelationTypeGroup.RULE_CHAIN));
            } catch (ExecutionException | InterruptedException e) {
                log.warn("[{}] Failed to create tenant to root rule chain relation. from: [{}], to: [{}]",
                        savedRuleChain.getTenantId(), savedRuleChain.getId());
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
            } catch (ExecutionException | InterruptedException e) {
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
                } catch (ExecutionException | InterruptedException e) {
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
                } catch (ExecutionException | InterruptedException e) {
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
        Validator.validateId(tenantId, "Incorrect tenant id for search request.");
        List<EntityRelation> relations = relationService.findByFrom(tenantId, tenantId, RelationTypeGroup.RULE_CHAIN);
        if (relations != null && !relations.isEmpty()) {
            EntityRelation relation = relations.get(0);
            RuleChainId ruleChainId = new RuleChainId(relation.getTo().getId());
            return findRuleChainById(tenantId, ruleChainId);
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
    public PageData<RuleChain> findTenantRuleChains(TenantId tenantId, PageLink pageLink) {
        Validator.validateId(tenantId, "Incorrect tenant id for search rule chain request.");
        Validator.validatePageLink(pageLink);
        return ruleChainDao.findRuleChainsByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public void deleteRuleChainById(TenantId tenantId, RuleChainId ruleChainId) {
        Validator.validateId(ruleChainId, "Incorrect rule chain id for delete request.");
        RuleChain ruleChain = ruleChainDao.findById(tenantId, ruleChainId.getId());
        if (ruleChain != null && ruleChain.isRoot()) {
            throw new DataValidationException("Deletion of Root Tenant Rule Chain is prohibited!");
        }
        checkRuleNodesAndDelete(tenantId, ruleChainId);
    }

    @Override
    public void deleteRuleChainsByTenantId(TenantId tenantId) {
        Validator.validateId(tenantId, "Incorrect tenant id for delete rule chains request.");
        tenantRuleChainsRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    public RuleChainData exportTenantRuleChains(TenantId tenantId, PageLink pageLink) {
        Validator.validateId(tenantId, "Incorrect tenant id for search rule chain request.");
        Validator.validatePageLink(pageLink);
        PageData<RuleChain> ruleChainData = ruleChainDao.findRuleChainsByTenantId(tenantId.getId(), pageLink);
        List<RuleChain> ruleChains = ruleChainData.getData();
        List<RuleChainMetaData> metadata = ruleChains.stream().map(rc -> loadRuleChainMetaData(tenantId, rc.getId())).collect(Collectors.toList());
        RuleChainData rcData = new RuleChainData();
        rcData.setRuleChains(ruleChains);
        rcData.setMetadata(metadata);
        setRandomRuleChainIds(rcData);
        resetRuleNodeIds(metadata);
        return rcData;
    }

    @Override
    public List<RuleChainImportResult> importTenantRuleChains(TenantId tenantId, RuleChainData ruleChainData, boolean overwrite) {
        List<RuleChainImportResult> importResults = new ArrayList<>();
        setRandomRuleChainIds(ruleChainData);
        resetRuleNodeIds(ruleChainData.getMetadata());
        resetRuleChainMetadataTenantIds(tenantId, ruleChainData.getMetadata());
        if (overwrite) {
            List<RuleChain> persistentRuleChains = findAllTenantRuleChains(tenantId);
            for (RuleChain ruleChain : ruleChainData.getRuleChains()) {
                ComponentLifecycleEvent lifecycleEvent;
                Optional<RuleChain> persistentRuleChainOpt = persistentRuleChains.stream().filter(rc -> rc.getName().equals(ruleChain.getName())).findFirst();
                if (persistentRuleChainOpt.isPresent()) {
                    setNewRuleChainId(ruleChain, ruleChainData.getMetadata(), ruleChain.getId(), persistentRuleChainOpt.get().getId());
                    ruleChain.setRoot(persistentRuleChainOpt.get().isRoot());
                    lifecycleEvent = ComponentLifecycleEvent.UPDATED;
                } else {
                    ruleChain.setRoot(false);
                    lifecycleEvent = ComponentLifecycleEvent.CREATED;
                }
                ruleChain.setTenantId(tenantId);
                ruleChainDao.save(tenantId, ruleChain);
                importResults.add(new RuleChainImportResult(tenantId, ruleChain.getId(), lifecycleEvent));
            }
        } else {
            if (!CollectionUtils.isEmpty(ruleChainData.getRuleChains())) {
                ruleChainData.getRuleChains().forEach(rc -> {
                    rc.setTenantId(tenantId);
                    rc.setRoot(false);
                    RuleChain savedRc = ruleChainDao.save(tenantId, rc);
                    importResults.add(new RuleChainImportResult(tenantId, savedRc.getId(), ComponentLifecycleEvent.CREATED));
                });
            }
        }
        if (!CollectionUtils.isEmpty(ruleChainData.getMetadata())) {
            ruleChainData.getMetadata().forEach(md -> saveRuleChainMetaData(tenantId, md));
        }
        return importResults;
    }

    private void resetRuleChainMetadataTenantIds(TenantId tenantId, List<RuleChainMetaData> metaData) {
        for (RuleChainMetaData md : metaData) {
            for (RuleNode node : md.getNodes()) {
                JsonNode nodeConfiguration = node.getConfiguration();
                searchTenantIdRecursive(tenantId, nodeConfiguration);
            }
        }
    }

    private void searchTenantIdRecursive(TenantId tenantId, JsonNode node) {
        Iterator<String> iter = node.fieldNames();
        boolean isTenantId = false;
        while (iter.hasNext()) {
            String field = iter.next();
            if ("entityType".equals(field) && TENANT.equals(node.get(field).asText())) {
                isTenantId = true;
                break;
            }
        }
        if (isTenantId) {
            ObjectNode objNode = (ObjectNode) node;
            objNode.put("id", tenantId.getId().toString());
        } else {
            Iterator<JsonNode> childIter = node.iterator();
            while (childIter.hasNext()) {
                searchTenantIdRecursive(tenantId, childIter.next());
            }
        }
    }

    private void setRandomRuleChainIds(RuleChainData ruleChainData) {
        for (RuleChain ruleChain : ruleChainData.getRuleChains()) {
            RuleChainId oldRuleChainId = ruleChain.getId();
            RuleChainId newRuleChainId = new RuleChainId(Uuids.timeBased());
            setNewRuleChainId(ruleChain, ruleChainData.getMetadata(), oldRuleChainId, newRuleChainId);
            ruleChain.setTenantId(null);
        }
    }

    private void resetRuleNodeIds(List<RuleChainMetaData> metaData) {
        for (RuleChainMetaData md : metaData) {
            for (RuleNode node : md.getNodes()) {
                node.setId(null);
                node.setRuleChainId(null);
            }
        }
    }

    private List<RuleChain> findAllTenantRuleChains(TenantId tenantId) {
        PageLink pageLink = new PageLink(DEFAULT_PAGE_SIZE);
        return findAllTenantRuleChainsRecursive(tenantId, new ArrayList<>(), pageLink);
    }

    private List<RuleChain> findAllTenantRuleChainsRecursive(TenantId tenantId, List<RuleChain> accumulator, PageLink pageLink) {
        PageData<RuleChain> persistentRuleChainData = findTenantRuleChains(tenantId, pageLink);
        List<RuleChain> ruleChains = persistentRuleChainData.getData();
        if (!CollectionUtils.isEmpty(ruleChains)) {
            accumulator.addAll(ruleChains);
        }
        if (persistentRuleChainData.hasNext()) {
            return findAllTenantRuleChainsRecursive(tenantId, accumulator, pageLink.nextPageLink());
        }
        return accumulator;
    }

    private void setNewRuleChainId(RuleChain ruleChain, List<RuleChainMetaData> metadata, RuleChainId oldRuleChainId, RuleChainId newRuleChainId) {
        ruleChain.setId(newRuleChainId);
        for (RuleChainMetaData metaData : metadata) {
            if (metaData.getRuleChainId().equals(oldRuleChainId)) {
                metaData.setRuleChainId(newRuleChainId);
            }
            if (!CollectionUtils.isEmpty(metaData.getRuleChainConnections())) {
                for (RuleChainConnectionInfo rcConnInfo : metaData.getRuleChainConnections()) {
                    if (rcConnInfo.getTargetRuleChainId().equals(oldRuleChainId)) {
                        rcConnInfo.setTargetRuleChainId(newRuleChainId);
                    }
                }
            }
        }
    }

    private void checkRuleNodesAndDelete(TenantId tenantId, RuleChainId ruleChainId) {
        try{
            ruleChainDao.removeById(tenantId, ruleChainId.getId());
        } catch (Exception t) {
            ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("fk_default_rule_chain_device_profile")) {
                throw new DataValidationException("The rule chain referenced by the device profiles cannot be deleted!");
            } else {
                throw t;
            }
        }
        List<EntityRelation> nodeRelations = getRuleChainToNodeRelations(tenantId, ruleChainId);
        for (EntityRelation relation : nodeRelations) {
            deleteRuleNode(tenantId, relation.getTo());
        }
        deleteEntityRelations(tenantId, ruleChainId);
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

    private void createRelation(TenantId tenantId, EntityRelation relation) throws ExecutionException, InterruptedException {
        log.debug("Creating relation: {}", relation);
        relationService.saveRelation(tenantId, relation);
    }

    private void deleteRelation(TenantId tenantId, EntityRelation relation) throws ExecutionException, InterruptedException {
        log.debug("Deleting relation: {}", relation);
        relationService.deleteRelation(tenantId, relation);
    }

    private DataValidator<RuleChain> ruleChainValidator =
            new DataValidator<RuleChain>() {
                @Override
                protected void validateCreate(TenantId tenantId, RuleChain data) {
                    DefaultTenantProfileConfiguration profileConfiguration =
                            (DefaultTenantProfileConfiguration)tenantProfileCache.get(tenantId).getProfileData().getConfiguration();
                    long maxRuleChains = profileConfiguration.getMaxRuleChains();
                    if (maxRuleChains > 0) {
                        long currentRuleChainsCount = ruleChainDao.countRuleChainsByTenantId(tenantId);
                        if (currentRuleChainsCount >= maxRuleChains) {
                            throw new DataValidationException("Can't create rule chains more then " + maxRuleChains);
                        }
                    }
                }

                @Override
                protected void validateDataImpl(TenantId tenantId, RuleChain ruleChain) {
                    if (StringUtils.isEmpty(ruleChain.getName())) {
                        throw new DataValidationException("Rule chain name should be specified!.");
                    }
                    if (ruleChain.getTenantId() == null || ruleChain.getTenantId().isNullUid()) {
                        throw new DataValidationException("Rule chain should be assigned to tenant!");
                    }
                    Tenant tenant = tenantDao.findById(tenantId, ruleChain.getTenantId().getId());
                    if (tenant == null) {
                        throw new DataValidationException("Rule chain is referencing to non-existent tenant!");
                    }
                    if (ruleChain.isRoot()) {
                        RuleChain rootRuleChain = getRootTenantRuleChain(ruleChain.getTenantId());
                        if (rootRuleChain != null && !rootRuleChain.getId().equals(ruleChain.getId())) {
                            throw new DataValidationException("Another root rule chain is present in scope of current tenant!");
                        }
                    }
                }
            };

    private PaginatedRemover<TenantId, RuleChain> tenantRuleChainsRemover =
            new PaginatedRemover<TenantId, RuleChain>() {

                @Override
                protected PageData<RuleChain> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
                    return ruleChainDao.findRuleChainsByTenantId(id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, RuleChain entity) {
                    checkRuleNodesAndDelete(tenantId, entity.getId());
                }
            };
}

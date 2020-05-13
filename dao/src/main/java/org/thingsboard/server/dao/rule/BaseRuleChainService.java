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

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.NodeConnectionInfo;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainConnectionInfo;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.tenant.TenantDao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Created by igor on 3/12/18.
 */
@Service
@Slf4j
public class BaseRuleChainService extends AbstractEntityService implements RuleChainService {

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

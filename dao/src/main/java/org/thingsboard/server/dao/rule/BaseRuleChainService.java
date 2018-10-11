/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.*;
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
        ruleChainValidator.validate(ruleChain);
        RuleChain savedRuleChain = ruleChainDao.save(ruleChain);
        if (ruleChain.isRoot() && ruleChain.getId() == null) {
            try {
                createRelation(new EntityRelation(savedRuleChain.getTenantId(), savedRuleChain.getId(),
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
    public boolean setRootRuleChain(RuleChainId ruleChainId) {
        RuleChain ruleChain = ruleChainDao.findById(ruleChainId.getId());
        if (!ruleChain.isRoot()) {
            RuleChain previousRootRuleChain = getRootTenantRuleChain(ruleChain.getTenantId());
            if (!previousRootRuleChain.getId().equals(ruleChain.getId())) {
                try {
                    deleteRelation(new EntityRelation(previousRootRuleChain.getTenantId(), previousRootRuleChain.getId(),
                            EntityRelation.CONTAINS_TYPE, RelationTypeGroup.RULE_CHAIN));
                    previousRootRuleChain.setRoot(false);
                    ruleChainDao.save(previousRootRuleChain);
                    createRelation(new EntityRelation(ruleChain.getTenantId(), ruleChain.getId(),
                            EntityRelation.CONTAINS_TYPE, RelationTypeGroup.RULE_CHAIN));
                    ruleChain.setRoot(true);
                    ruleChainDao.save(ruleChain);
                    return true;
                } catch (ExecutionException | InterruptedException e) {
                    log.warn("[{}] Failed to set root rule chain, ruleChainId: [{}]", ruleChainId);
                    throw new RuntimeException(e);
                }
            }
        }
        return false;
    }

    @Override
    public RuleChainMetaData saveRuleChainMetaData(RuleChainMetaData ruleChainMetaData) {
        Validator.validateId(ruleChainMetaData.getRuleChainId(), "Incorrect rule chain id.");
        RuleChain ruleChain = findRuleChainById(ruleChainMetaData.getRuleChainId());
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

        List<RuleNode> existingRuleNodes = getRuleChainNodes(ruleChainMetaData.getRuleChainId());
        for (RuleNode existingNode : existingRuleNodes) {
            deleteEntityRelations(existingNode.getId());
            Integer index = ruleNodeIndexMap.get(existingNode.getId());
            if (index != null) {
                toAddOrUpdate.add(ruleChainMetaData.getNodes().get(index));
            } else {
                toDelete.add(existingNode);
            }
        }
        for (RuleNode node : toAddOrUpdate) {
            node.setRuleChainId(ruleChain.getId());
            RuleNode savedNode = ruleNodeDao.save(node);
            try {
                createRelation(new EntityRelation(ruleChainMetaData.getRuleChainId(), savedNode.getId(),
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
            deleteRuleNode(node.getId());
        }
        RuleNodeId firstRuleNodeId = null;
        if (ruleChainMetaData.getFirstNodeIndex() != null) {
            firstRuleNodeId = nodes.get(ruleChainMetaData.getFirstNodeIndex()).getId();
        }
        if ((ruleChain.getFirstRuleNodeId() != null && !ruleChain.getFirstRuleNodeId().equals(firstRuleNodeId))
                || (ruleChain.getFirstRuleNodeId() == null && firstRuleNodeId != null)) {
            ruleChain.setFirstRuleNodeId(firstRuleNodeId);
            ruleChainDao.save(ruleChain);
        }
        if (ruleChainMetaData.getConnections() != null) {
            for (NodeConnectionInfo nodeConnection : ruleChainMetaData.getConnections()) {
                EntityId from = nodes.get(nodeConnection.getFromIndex()).getId();
                EntityId to = nodes.get(nodeConnection.getToIndex()).getId();
                String type = nodeConnection.getType();
                try {
                    createRelation(new EntityRelation(from, to, type, RelationTypeGroup.RULE_NODE));
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
                    createRelation(new EntityRelation(from, to, type, RelationTypeGroup.RULE_NODE, nodeToRuleChainConnection.getAdditionalInfo()));
                } catch (ExecutionException | InterruptedException e) {
                    log.warn("[{}] Failed to create rule node to rule chain relation. from: [{}], to: [{}]", from, to);
                    throw new RuntimeException(e);
                }
            }
        }

        return loadRuleChainMetaData(ruleChainMetaData.getRuleChainId());
    }

    @Override
    public RuleChainMetaData loadRuleChainMetaData(RuleChainId ruleChainId) {
        Validator.validateId(ruleChainId, "Incorrect rule chain id.");
        RuleChain ruleChain = findRuleChainById(ruleChainId);
        if (ruleChain == null) {
            return null;
        }
        RuleChainMetaData ruleChainMetaData = new RuleChainMetaData();
        ruleChainMetaData.setRuleChainId(ruleChainId);
        List<RuleNode> ruleNodes = getRuleChainNodes(ruleChainId);
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
            List<EntityRelation> nodeRelations = getRuleNodeRelations(node.getId());
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
    public RuleChain findRuleChainById(RuleChainId ruleChainId) {
        Validator.validateId(ruleChainId, "Incorrect rule chain id for search request.");
        return ruleChainDao.findById(ruleChainId.getId());
    }

    @Override
    public RuleNode findRuleNodeById(RuleNodeId ruleNodeId) {
        Validator.validateId(ruleNodeId, "Incorrect rule node id for search request.");
        return ruleNodeDao.findById(ruleNodeId.getId());
    }

    @Override
    public ListenableFuture<RuleChain> findRuleChainByIdAsync(RuleChainId ruleChainId) {
        Validator.validateId(ruleChainId, "Incorrect rule chain id for search request.");
        return ruleChainDao.findByIdAsync(ruleChainId.getId());
    }

    @Override
    public ListenableFuture<RuleNode> findRuleNodeByIdAsync(RuleNodeId ruleNodeId) {
        Validator.validateId(ruleNodeId, "Incorrect rule node id for search request.");
        return ruleNodeDao.findByIdAsync(ruleNodeId.getId());
    }

    @Override
    public RuleChain getRootTenantRuleChain(TenantId tenantId) {
        Validator.validateId(tenantId, "Incorrect tenant id for search request.");
        List<EntityRelation> relations = relationService.findByFrom(tenantId, RelationTypeGroup.RULE_CHAIN);
        if (relations != null && !relations.isEmpty()) {
            EntityRelation relation = relations.get(0);
            RuleChainId ruleChainId = new RuleChainId(relation.getTo().getId());
            return findRuleChainById(ruleChainId);
        } else {
            return null;
        }
    }

    @Override
    public List<RuleNode> getRuleChainNodes(RuleChainId ruleChainId) {
        Validator.validateId(ruleChainId, "Incorrect rule chain id for search request.");
        List<EntityRelation> relations = getRuleChainToNodeRelations(ruleChainId);
        List<RuleNode> ruleNodes = relations.stream().map(relation -> ruleNodeDao.findById(relation.getTo().getId())).collect(Collectors.toList());
        return ruleNodes;
    }

    @Override
    public List<EntityRelation> getRuleNodeRelations(RuleNodeId ruleNodeId) {
        Validator.validateId(ruleNodeId, "Incorrect rule node id for search request.");
        return relationService.findByFrom(ruleNodeId, RelationTypeGroup.RULE_NODE);
    }

    @Override
    public TextPageData<RuleChain> findTenantRuleChains(TenantId tenantId, TextPageLink pageLink) {
        Validator.validateId(tenantId, "Incorrect tenant id for search rule chain request.");
        Validator.validatePageLink(pageLink, "Incorrect PageLink object for search rule chain request.");
        List<RuleChain> ruleChains = ruleChainDao.findRuleChainsByTenantId(tenantId.getId(), pageLink);
        return new TextPageData<>(ruleChains, pageLink);
    }

    @Override
    public void deleteRuleChainById(RuleChainId ruleChainId) {
        Validator.validateId(ruleChainId, "Incorrect rule chain id for delete request.");
        RuleChain ruleChain = ruleChainDao.findById(ruleChainId.getId());
        if (ruleChain != null && ruleChain.isRoot()) {
            throw new DataValidationException("Deletion of Root Tenant Rule Chain is prohibited!");
        }
        checkRuleNodesAndDelete(ruleChainId);
    }

    @Override
    public void deleteRuleChainsByTenantId(TenantId tenantId) {
        Validator.validateId(tenantId, "Incorrect tenant id for delete rule chains request.");
        tenantRuleChainsRemover.removeEntities(tenantId);
    }

    private void checkRuleNodesAndDelete(RuleChainId ruleChainId) {
        List<EntityRelation> nodeRelations = getRuleChainToNodeRelations(ruleChainId);
        for (EntityRelation relation : nodeRelations) {
            deleteRuleNode(relation.getTo());
        }
        deleteEntityRelations(ruleChainId);
        ruleChainDao.removeById(ruleChainId.getId());
    }

    private List<EntityRelation> getRuleChainToNodeRelations(RuleChainId ruleChainId) {
        return relationService.findByFrom(ruleChainId, RelationTypeGroup.RULE_CHAIN);
    }

    private void deleteRuleNode(EntityId entityId) {
        deleteEntityRelations(entityId);
        ruleNodeDao.removeById(entityId.getId());
    }

    private DataValidator<RuleChain> ruleChainValidator =
            new DataValidator<RuleChain>() {
                @Override
                protected void validateDataImpl(RuleChain ruleChain) {
                    if (StringUtils.isEmpty(ruleChain.getName())) {
                        throw new DataValidationException("Rule chain name should be specified!.");
                    }
                    if (ruleChain.getTenantId() == null || ruleChain.getTenantId().isNullUid()) {
                        throw new DataValidationException("Rule chain should be assigned to tenant!");
                    }
                    Tenant tenant = tenantDao.findById(ruleChain.getTenantId().getId());
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
                protected List<RuleChain> findEntities(TenantId id, TextPageLink pageLink) {
                    return ruleChainDao.findRuleChainsByTenantId(id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(RuleChain entity) {
                    checkRuleNodesAndDelete(entity.getId());
                }
            };
}

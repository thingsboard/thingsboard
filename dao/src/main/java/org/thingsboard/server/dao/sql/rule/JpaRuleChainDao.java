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
package org.thingsboard.server.dao.sql.rule;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.RuleChainEntity;
import org.thingsboard.server.dao.relation.RelationDao;
import org.thingsboard.server.dao.rule.RuleChainDao;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
@SqlDao
public class JpaRuleChainDao extends JpaAbstractSearchTextDao<RuleChainEntity, RuleChain> implements RuleChainDao {

    @Autowired
    private RuleChainRepository ruleChainRepository;

    @Autowired
    private RelationDao relationDao;

    @Override
    protected Class getEntityClass() {
        return RuleChainEntity.class;
    }

    @Override
    protected CrudRepository getCrudRepository() {
        return ruleChainRepository;
    }

    @Override
    public PageData<RuleChain> findRuleChainsByTenantId(UUID tenantId, PageLink pageLink) {
        log.debug("Try to find rule chains by tenantId [{}] and pageLink [{}]", tenantId, pageLink);
        return DaoUtil.toPageData(ruleChainRepository
                .findByTenantId(
                        UUIDConverter.fromTimeUUID(tenantId),
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<RuleChain> findRuleChainsByTenantIdAndType(UUID tenantId, RuleChainType type, PageLink pageLink) {
        log.debug("Try to find rule chains by tenantId [{}], type [{}] and pageLink [{}]", tenantId, type, pageLink);
        return DaoUtil.toPageData(ruleChainRepository
                .findByTenantIdAndType(
                        UUIDConverter.fromTimeUUID(tenantId),
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public ListenableFuture<PageData<RuleChain>> findRuleChainsByTenantIdAndEdgeId(UUID tenantId, UUID edgeId, TimePageLink pageLink) {
        log.debug("Try to find rule chains by tenantId [{}], edgeId [{}] and pageLink [{}]", tenantId, edgeId, pageLink);
        ListenableFuture<PageData<EntityRelation>> relations =
                relationDao.findRelations(new TenantId(tenantId), new EdgeId(edgeId), EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE, EntityType.RULE_CHAIN, pageLink);
        return Futures.transformAsync(relations, relationsData -> {
            if (relationsData != null && relationsData.getData() != null && !relationsData.getData().isEmpty()) {
                List<ListenableFuture<RuleChain>> ruleChainFutures = new ArrayList<>(relationsData.getData().size());
                for (EntityRelation relation : relationsData.getData()) {
                    ruleChainFutures.add(findByIdAsync(new TenantId(tenantId), relation.getTo().getId()));
                }
                return Futures.transform(Futures.successfulAsList(ruleChainFutures),
                        ruleChains -> new PageData<>(ruleChains, relationsData.getTotalPages(), relationsData.getTotalElements(),
                                relationsData.hasNext()), MoreExecutors.directExecutor());
            } else {
                return Futures.immediateFuture(new PageData<>());
            }
        }, MoreExecutors.directExecutor());
    }

    @Override
    public ListenableFuture<List<RuleChain>> findDefaultEdgeRuleChainsByTenantId(UUID tenantId) {
        log.debug("Try to find default edge rule chains by tenantId [{}]", tenantId);
        ListenableFuture<List<EntityRelation>> relations =
                relationDao.findAllByFromAndType(new TenantId(tenantId), new TenantId(tenantId), EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE_DEFAULT_RULE_CHAIN);
        return Futures.transformAsync(relations, input -> {
            if (input != null && !input.isEmpty()) {
                List<ListenableFuture<RuleChain>> ruleChainsFutures = new ArrayList<>(input.size());
                for (EntityRelation relation : input) {
                    ruleChainsFutures.add(findByIdAsync(new TenantId(tenantId), relation.getTo().getId()));
                }
                return Futures.successfulAsList(ruleChainsFutures);
            } else {
                return Futures.immediateFuture(Collections.emptyList());
            }
        }, MoreExecutors.directExecutor());
    }
}

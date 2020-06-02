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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.nosql.RuleChainEntity;
import org.thingsboard.server.dao.nosql.CassandraAbstractSearchTextDao;
import org.thingsboard.server.dao.relation.RelationDao;
import org.thingsboard.server.dao.util.NoSqlDao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static org.thingsboard.server.dao.model.ModelConstants.DEVICE_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.DEVICE_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.RULE_CHAIN_BY_TENANT_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.RULE_CHAIN_BY_TENANT_BY_TYPE_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.RULE_CHAIN_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.RULE_CHAIN_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.RULE_CHAIN_TYPE_PROPERTY;

@Component
@Slf4j
@NoSqlDao
public class CassandraRuleChainDao extends CassandraAbstractSearchTextDao<RuleChainEntity, RuleChain> implements RuleChainDao {

    @Autowired
    private RelationDao relationDao;

    @Override
    protected Class<RuleChainEntity> getColumnFamilyClass() {
        return RuleChainEntity.class;
    }

    @Override
    protected String getColumnFamilyName() {
        return RULE_CHAIN_COLUMN_FAMILY_NAME;
    }

    @Override
    public List<RuleChain> findRuleChainsByTenantId(UUID tenantId, TextPageLink pageLink) {
        log.debug("Try to find rule chains by tenantId [{}] and pageLink [{}]", tenantId, pageLink);
        List<RuleChainEntity> ruleChainEntities = findPageWithTextSearch(new TenantId(tenantId), RULE_CHAIN_BY_TENANT_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Collections.singletonList(eq(RULE_CHAIN_TENANT_ID_PROPERTY, tenantId)),
                pageLink);

        log.trace("Found rule chains [{}] by tenantId [{}] and pageLink [{}]", ruleChainEntities, tenantId, pageLink);
        return DaoUtil.convertDataList(ruleChainEntities);
    }

    @Override
    public List<RuleChain> findRuleChainsByTenantIdAndType(UUID tenantId, RuleChainType type, TextPageLink pageLink) {
        log.debug("Try to find rule chains by tenantId [{}], type [{}] and pageLink [{}]", tenantId, type, pageLink);
        List<RuleChainEntity> ruleChainEntities = findPageWithTextSearch(new TenantId(tenantId), RULE_CHAIN_BY_TENANT_BY_TYPE_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Arrays.asList(eq(RULE_CHAIN_TYPE_PROPERTY, type),
                        eq(RULE_CHAIN_TENANT_ID_PROPERTY, tenantId)),
                pageLink);
        log.trace("Found rule chains [{}] by tenantId [{}] and pageLink [{}]", ruleChainEntities, tenantId, pageLink);
        return DaoUtil.convertDataList(ruleChainEntities);
    }

    @Override
    public ListenableFuture<List<RuleChain>> findRuleChainsByTenantIdAndEdgeId(UUID tenantId, UUID edgeId, TimePageLink pageLink) {
        log.debug("Try to find rule chains by tenantId [{}], edgeId [{}] and pageLink [{}]", tenantId, edgeId, pageLink);
        ListenableFuture<List<EntityRelation>> relations = relationDao.findRelations(new TenantId(tenantId), new EdgeId(edgeId), EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE, EntityType.RULE_CHAIN, pageLink);
        return Futures.transformAsync(relations, input -> {
            List<ListenableFuture<RuleChain>> ruleChainFutures = new ArrayList<>(input.size());
            for (EntityRelation relation : input) {
                ruleChainFutures.add(findByIdAsync(new TenantId(tenantId), relation.getTo().getId()));
            }
            return Futures.successfulAsList(ruleChainFutures);
        }, MoreExecutors.directExecutor());
    }

    @Override
    public ListenableFuture<List<RuleChain>> findDefaultEdgeRuleChainsByTenantId(UUID tenantId) {
        log.debug("Try to find default edge rule chains by tenantId [{}]", tenantId);
        ListenableFuture<List<EntityRelation>> relations = relationDao.findAllByFromAndType(new TenantId(tenantId), new TenantId(tenantId), EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE_DEFAULT_RULE_CHAIN);
        return Futures.transformAsync(relations, input -> {
            List<ListenableFuture<RuleChain>> ruleChainFutures = new ArrayList<>(input.size());
            for (EntityRelation relation : input) {
                ruleChainFutures.add(findByIdAsync(new TenantId(tenantId), relation.getTo().getId()));
            }
            return Futures.successfulAsList(ruleChainFutures);
        }, MoreExecutors.directExecutor());
    }

}

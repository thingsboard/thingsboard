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

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.nosql.RuleChainEntity;
import org.thingsboard.server.dao.nosql.CassandraAbstractSearchTextDao;
import org.thingsboard.server.dao.util.NoSqlDao;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static org.thingsboard.server.dao.model.ModelConstants.RULE_CHAIN_BY_TENANT_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.RULE_CHAIN_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.RULE_CHAIN_TENANT_ID_PROPERTY;

@Component
@Slf4j
@NoSqlDao
public class CassandraRuleChainDao extends CassandraAbstractSearchTextDao<RuleChainEntity, RuleChain> implements RuleChainDao {

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
        List<RuleChainEntity> ruleChainEntities = findPageWithTextSearch(RULE_CHAIN_BY_TENANT_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Collections.singletonList(eq(RULE_CHAIN_TENANT_ID_PROPERTY, tenantId)),
                pageLink);

        log.trace("Found rule chains [{}] by tenantId [{}] and pageLink [{}]", ruleChainEntities, tenantId, pageLink);
        return DaoUtil.convertDataList(ruleChainEntities);
    }

}

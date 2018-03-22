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

import com.datastax.driver.core.querybuilder.Select;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.RuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.rule.RuleMetaData;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.nosql.RuleMetaDataEntity;
import org.thingsboard.server.dao.nosql.CassandraAbstractSearchTextDao;
import org.thingsboard.server.dao.util.NoSqlDao;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.*;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

@Component
@Slf4j
@NoSqlDao
public class CassandraBaseRuleDao extends CassandraAbstractSearchTextDao<RuleMetaDataEntity, RuleMetaData> implements RuleDao {

    public static final String SEARCH_RESULT = "Search result: [{}]";

    @Override
    protected Class<RuleMetaDataEntity> getColumnFamilyClass() {
        return RuleMetaDataEntity.class;
    }

    @Override
    protected String getColumnFamilyName() {
        return ModelConstants.RULE_COLUMN_FAMILY_NAME;
    }

    @Override
    public RuleMetaData findById(RuleId ruleId) {
        return findById(ruleId.getId());
    }

    @Override
    public List<RuleMetaData> findByTenantIdAndPageLink(TenantId tenantId, TextPageLink pageLink) {
        log.debug("Try to find rules by tenantId [{}] and pageLink [{}]", tenantId, pageLink);
        List<RuleMetaDataEntity> entities = findPageWithTextSearch(ModelConstants.RULE_BY_TENANT_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Arrays.asList(eq(ModelConstants.RULE_TENANT_ID_PROPERTY, tenantId.getId())), pageLink);
        if (log.isTraceEnabled()) {
            log.trace(SEARCH_RESULT, Arrays.toString(entities.toArray()));
        } else {
            log.debug(SEARCH_RESULT, entities.size());
        }
        return DaoUtil.convertDataList(entities);
    }

    @Override
    public List<RuleMetaData> findAllTenantRulesByTenantId(UUID tenantId, TextPageLink pageLink) {
        log.debug("Try to find all tenant rules by tenantId [{}] and pageLink [{}]", tenantId, pageLink);
        List<RuleMetaDataEntity> entities = findPageWithTextSearch(ModelConstants.RULE_BY_TENANT_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Arrays.asList(in(ModelConstants.RULE_TENANT_ID_PROPERTY, Arrays.asList(NULL_UUID, tenantId))),
                pageLink);
        if (log.isTraceEnabled()) {
            log.trace(SEARCH_RESULT, Arrays.toString(entities.toArray()));
        } else {
            log.debug(SEARCH_RESULT, entities.size());
        }
        return DaoUtil.convertDataList(entities);
    }

    @Override
    public List<RuleMetaData> findRulesByPlugin(String pluginToken) {
        log.debug("Search rules by api token [{}]", pluginToken);
        Select select = select().from(ModelConstants.RULE_BY_PLUGIN_TOKEN);
        Select.Where query = select.where();
        query.and(eq(ModelConstants.RULE_PLUGIN_TOKEN_PROPERTY, pluginToken));
        return DaoUtil.convertDataList(findListByStatement(query));
    }

    @Override
    public void deleteById(UUID id) {
        log.debug("Delete rule meta-data entity by id [{}]", id);
        boolean result = removeById(id);
        log.debug("Delete result: [{}]", result);
    }

    @Override
    public void deleteById(RuleId ruleId) {
        deleteById(ruleId.getId());
    }
}

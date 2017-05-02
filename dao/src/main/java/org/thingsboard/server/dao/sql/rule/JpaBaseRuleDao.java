/**
 * Copyright © 2016-2017 The Thingsboard Authors
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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.RuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.rule.RuleMetaData;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.sql.RuleMetaDataEntity;
import org.thingsboard.server.dao.rule.RuleDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 4/30/2017.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "sql", value = "enabled", havingValue = "true", matchIfMissing = false)
public class JpaBaseRuleDao extends JpaAbstractDao<RuleMetaDataEntity, RuleMetaData> implements RuleDao {

    @Autowired
    private RuleMetaDataRepository ruleMetaDataRepository;

    @Override
    protected Class<RuleMetaDataEntity> getEntityClass() {
        return RuleMetaDataEntity.class;
    }
    @Override
    protected String getColumnFamilyName() {
        return ModelConstants.RULE_COLUMN_FAMILY_NAME;
    }

    @Override
    protected CrudRepository<RuleMetaDataEntity, UUID> getCrudRepository() {
        return ruleMetaDataRepository;
    }

    @Override
    protected boolean isSearchTextDao() {
        return true;
    }

    @Override
    public RuleMetaData findById(RuleId ruleId) {
        return findById(ruleId.getId());
    }

    @Override
    public List<RuleMetaData> findRulesByPlugin(String pluginToken) {
        log.debug("Search rules by api token [{}]", pluginToken);
        return DaoUtil.convertDataList(ruleMetaDataRepository.findByPluginToken(pluginToken));
    }

    @Override
    public List<RuleMetaData> findByTenantIdAndPageLink(TenantId tenantId, TextPageLink pageLink) {
        log.debug("Try to find rules by tenantId [{}] and pageLink [{}]", tenantId, pageLink);
        List<RuleMetaDataEntity> entities;
        if (pageLink.getIdOffset() == null) {
            entities = ruleMetaDataRepository
                    .findByTenantIdAndPageLinkFirstPage(pageLink.getLimit(), tenantId.getId(), pageLink.getTextSearch());
        } else {
            entities = ruleMetaDataRepository
                    .findByTenantIdAndPageLinkNextPage(pageLink.getLimit(), tenantId.getId(), pageLink.getTextSearch(), pageLink.getIdOffset());
        }
        if (log.isTraceEnabled()) {
            log.trace("Search result: [{}]", Arrays.toString(entities.toArray()));
        } else {
            log.debug("Search result: [{}]", entities.size());
        }
        return DaoUtil.convertDataList(entities);
    }

    @Override
    public List<RuleMetaData> findAllTenantRulesByTenantId(UUID tenantId, TextPageLink pageLink) {
        log.debug("Try to find all tenant rules by tenantId [{}] and pageLink [{}]", tenantId, pageLink);
        List<RuleMetaDataEntity> entities;
        if (pageLink.getIdOffset() == null) {
            entities = ruleMetaDataRepository
                    .findAllTenantRulesByTenantIdFirstPage(pageLink.getLimit(), tenantId, pageLink.getTextSearch());
        } else {
            entities = ruleMetaDataRepository
                    .findAllTenantRulesByTenantIdNextPage(pageLink.getLimit(), tenantId, pageLink.getTextSearch(), pageLink.getIdOffset());
        }
        if (log.isTraceEnabled()) {
            log.trace("Search result: [{}]", Arrays.toString(entities.toArray()));
        } else {
            log.debug("Search result: [{}]", entities.size());
        }
        return DaoUtil.convertDataList(entities);
    }

    @Override
    public void deleteById(UUID id) {
        log.debug("Delete rule meta-data entity by id [{}]", id);
        ruleMetaDataRepository.delete(id);
    }

    @Override
    public void deleteById(RuleId ruleId) {
        deleteById(ruleId.getId());
    }
}

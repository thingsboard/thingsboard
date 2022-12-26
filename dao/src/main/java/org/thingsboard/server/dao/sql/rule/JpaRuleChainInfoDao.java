/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rule.RuleChainInfo;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.RuleChainInfoEntity;
import org.thingsboard.server.dao.rule.RuleChainInfoDao;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
@SqlDao
public class JpaRuleChainInfoDao extends JpaAbstractSearchTextDao<RuleChainInfoEntity, RuleChainInfo> implements RuleChainInfoDao {

    @Autowired
    private RuleChainInfoRepository ruleChainInfoRepository;

    @Override
    protected Class<RuleChainInfoEntity> getEntityClass() {
        return RuleChainInfoEntity.class;
    }

    @Override
    protected JpaRepository<RuleChainInfoEntity, UUID> getRepository() {
        return ruleChainInfoRepository;
    }

    public PageData<RuleChainInfo> findErrorStatisticsByTenantIdAndRuleChain(UUID tenantId, RuleChainType type, PageLink pageLink) {
        log.debug("Try to find root rule chain by tenantId [{}] and type [{}]", tenantId, type);
        return DaoUtil.toPageData(ruleChainInfoRepository
                .findErrorStatisticsByTenantIdAndRuleChain(
                        tenantId,
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

}

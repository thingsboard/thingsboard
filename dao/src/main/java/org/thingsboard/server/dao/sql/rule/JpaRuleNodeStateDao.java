// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql.rule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rule.RuleNodeState;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.RuleNodeStateEntity;
import org.thingsboard.server.dao.rule.RuleNodeStateDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.UUID;

@Slf4j
@Component
@SqlDao
public class JpaRuleNodeStateDao extends JpaAbstractDao<RuleNodeStateEntity, RuleNodeState> implements RuleNodeStateDao {

    @Autowired
    private RuleNodeStateRepository ruleNodeStateRepository;

    @Override
    protected Class<RuleNodeStateEntity> getEntityClass() {
        return RuleNodeStateEntity.class;
    }

    @Override
    protected JpaRepository<RuleNodeStateEntity, UUID> getRepository() {
        return ruleNodeStateRepository;
    }

    @Override
    public PageData<RuleNodeState> findByRuleNodeId(UUID ruleNodeId, PageLink pageLink) {
        return DaoUtil.toPageData(ruleNodeStateRepository.findByRuleNodeId(ruleNodeId, DaoUtil.toPageable(pageLink)));
    }

    @Override
    public RuleNodeState findByRuleNodeIdAndEntityId(UUID ruleNodeId, UUID entityId) {
        return DaoUtil.getData(ruleNodeStateRepository.findByRuleNodeIdAndEntityId(ruleNodeId, entityId));
    }

    @Transactional
    @Override
    public void removeByRuleNodeId(UUID ruleNodeId) {
        ruleNodeStateRepository.removeByRuleNodeId(ruleNodeId);
    }

    @Transactional
    @Override
    public void removeByRuleNodeIdAndEntityId(UUID ruleNodeId, UUID entityId) {
        ruleNodeStateRepository.removeByRuleNodeIdAndEntityId(ruleNodeId, entityId);
    }
}

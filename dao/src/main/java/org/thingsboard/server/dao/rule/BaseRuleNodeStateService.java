// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.rule;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rule.RuleNodeState;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;

@Service
@Slf4j
public class BaseRuleNodeStateService extends AbstractEntityService implements RuleNodeStateService {

    @Autowired
    private RuleNodeStateDao ruleNodeStateDao;

    @Override
    public PageData<RuleNodeState> findByRuleNodeId(TenantId tenantId, RuleNodeId ruleNodeId, PageLink pageLink) {
        if (tenantId == null) {
            throw new DataValidationException("Tenant id should be specified!.");
        }
        if (ruleNodeId == null) {
            throw new DataValidationException("RuleNode id should be specified!.");
        }
        return ruleNodeStateDao.findByRuleNodeId(ruleNodeId.getId(), pageLink);
    }

    @Override
    public RuleNodeState findByRuleNodeIdAndEntityId(TenantId tenantId, RuleNodeId ruleNodeId, EntityId entityId) {
        if (tenantId == null) {
            throw new DataValidationException("Tenant id should be specified!.");
        }
        if (ruleNodeId == null) {
            throw new DataValidationException("RuleNode id should be specified!.");
        }
        if (entityId == null) {
            throw new DataValidationException("Entity id should be specified!.");
        }
        return ruleNodeStateDao.findByRuleNodeIdAndEntityId(ruleNodeId.getId(), entityId.getId());
    }

    @Override
    public RuleNodeState save(TenantId tenantId, RuleNodeState ruleNodeState) {
        if (tenantId == null) {
            throw new DataValidationException("Tenant id should be specified!.");
        }
        return saveOrUpdate(tenantId, ruleNodeState, false);
    }

    @Override
    public void removeByRuleNodeId(TenantId tenantId, RuleNodeId ruleNodeId) {
        if (tenantId == null) {
            throw new DataValidationException("Tenant id should be specified!.");
        }
        if (ruleNodeId == null) {
            throw new DataValidationException("Rule node id should be specified!.");
        }
        ruleNodeStateDao.removeByRuleNodeId(ruleNodeId.getId());
    }

    @Override
    public void removeByRuleNodeIdAndEntityId(TenantId tenantId, RuleNodeId ruleNodeId, EntityId entityId) {
        if (tenantId == null) {
            throw new DataValidationException("Tenant id should be specified!.");
        }
        if (ruleNodeId == null) {
            throw new DataValidationException("Rule node id should be specified!.");
        }
        if (entityId == null) {
            throw new DataValidationException("Entity id should be specified!.");
        }
        ruleNodeStateDao.removeByRuleNodeIdAndEntityId(ruleNodeId.getId(), entityId.getId());
    }

    public RuleNodeState saveOrUpdate(TenantId tenantId, RuleNodeState ruleNodeState, boolean update) {
        try {
            if (update) {
                RuleNodeState old = ruleNodeStateDao.findByRuleNodeIdAndEntityId(ruleNodeState.getRuleNodeId().getId(), ruleNodeState.getEntityId().getId());
                if (old != null && !old.getId().equals(ruleNodeState.getId())) {
                    ruleNodeState.setId(old.getId());
                    ruleNodeState.setCreatedTime(old.getCreatedTime());
                }
            }
            return ruleNodeStateDao.save(tenantId, ruleNodeState);
        } catch (Exception t) {
            ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("rule_node_state_unq_key")) {
                if (!update) {
                    return saveOrUpdate(tenantId, ruleNodeState, true);
                } else {
                    throw new DataValidationException("Rule node state for such rule node id and entity id already exists!");
                }
            } else {
                throw t;
            }
        }
    }
}

// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql.rule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleChainDetails;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.RuleChainDetailsEntity;
import org.thingsboard.server.dao.rule.RuleChainDetailsDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;
import org.thingsboard.server.exception.DataValidationException;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@SqlDao
@RequiredArgsConstructor
public class JpaRuleChainDetailsDao extends JpaAbstractDao<RuleChainDetailsEntity, RuleChainDetails> implements RuleChainDetailsDao {

    private final RuleChainDetailsRepository ruleChainDetailsRepository;

    @Override
    public RuleChainDetails findById(TenantId tenantId, UUID key) {
        Optional<RuleChainDetailsEntity> entity = getRepository().findById(key);
        // Detaching to avoid stale version conflict with RuleChainEntity which maps to the same table.
        // Without detach, a loaded RuleChainDetailsEntity stays in the persistence context and becomes stale
        // when the same row is updated via RuleChainEntity (e.g. during rule chain import with circular references).
        entity.ifPresent(e -> getEntityManager().detach(e));
        return DaoUtil.getData(entity);
    }

    @Override
    protected RuleChainDetailsEntity doSave(RuleChainDetailsEntity entity, boolean isNew, boolean flush) {
        try {
            return super.doSave(entity, isNew, flush);
        } catch (Exception e) {
            String rootMsg = ExceptionUtils.getRootCauseMessage(e);
            if (StringUtils.contains(rootMsg, "value too long")) {
                throw new DataValidationException("Rule chain notes data is too large. Please reduce the number or size of notes.");
            }
            throw e;
        }
    }

    @Override
    protected Class<RuleChainDetailsEntity> getEntityClass() {
        return RuleChainDetailsEntity.class;
    }

    @Override
    protected JpaRepository<RuleChainDetailsEntity, UUID> getRepository() {
        return ruleChainDetailsRepository;
    }

}

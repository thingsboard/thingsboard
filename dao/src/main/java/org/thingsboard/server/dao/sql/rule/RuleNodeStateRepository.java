// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql.rule;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.RuleNodeStateEntity;

import java.util.UUID;

public interface RuleNodeStateRepository extends JpaRepository<RuleNodeStateEntity, UUID> {

    @Query("SELECT e FROM RuleNodeStateEntity e WHERE e.ruleNodeId = :ruleNodeId")
    Page<RuleNodeStateEntity> findByRuleNodeId(@Param("ruleNodeId") UUID ruleNodeId, Pageable pageable);

    @Query("SELECT e FROM RuleNodeStateEntity e WHERE e.ruleNodeId = :ruleNodeId and e.entityId = :entityId")
    RuleNodeStateEntity findByRuleNodeIdAndEntityId(@Param("ruleNodeId") UUID ruleNodeId, @Param("entityId") UUID entityId);

    void removeByRuleNodeId(@Param("ruleNodeId") UUID ruleNodeId);

    void removeByRuleNodeIdAndEntityId(@Param("ruleNodeId") UUID ruleNodeId, @Param("entityId") UUID entityId);
}

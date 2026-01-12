/**
 * Copyright © 2016-2026 The Thingsboard Authors
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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.model.sql.RuleNodeEntity;

import java.util.List;
import java.util.UUID;

public interface RuleNodeRepository extends JpaRepository<RuleNodeEntity, UUID> {

    @Query(nativeQuery = true, value = "SELECT * FROM rule_node r WHERE r.rule_chain_id in " +
            "(select id from rule_chain rc WHERE rc.tenant_id = :tenantId) AND r.type = :ruleType " +
            " AND (:searchText IS NULL OR r.configuration ILIKE CONCAT('%', :searchText, '%'))")
    List<RuleNodeEntity> findRuleNodesByTenantIdAndType(@Param("tenantId") UUID tenantId,
                                                        @Param("ruleType") String ruleType,
                                                        @Param("searchText") String searchText);

    @Query(nativeQuery = true, value = "SELECT * FROM rule_node r WHERE r.type = :ruleType " +
            " AND (:searchText IS NULL OR r.configuration ILIKE CONCAT('%', :searchText, '%'))")
    Page<RuleNodeEntity> findAllRuleNodesByType(@Param("ruleType") String ruleType,
                                                @Param("searchText") String searchText,
                                                Pageable pageable);

    @Query(nativeQuery = true, value = "SELECT * FROM rule_node r WHERE r.type = :ruleType " +
            " AND r.configuration_version < :version " +
            " AND (:searchText IS NULL OR r.configuration ILIKE CONCAT('%', :searchText, '%'))")
    Page<RuleNodeEntity> findAllRuleNodesByTypeAndVersionLessThan(@Param("ruleType") String ruleType,
                                                                  @Param("version") int version,
                                                                  @Param("searchText") String searchText,
                                                                  Pageable pageable);

    @Query("SELECT r.id FROM RuleNodeEntity r WHERE r.type = :ruleType AND r.configurationVersion < :version")
    Slice<UUID> findAllRuleNodeIdsByTypeAndVersionLessThan(@Param("ruleType") String ruleType,
                                                           @Param("version") int version,
                                                           Pageable pageable);

    List<RuleNodeEntity> findRuleNodesByRuleChainIdAndExternalIdIn(UUID ruleChainId, List<UUID> externalIds);

    @Transactional
    @Modifying
    @Query("DELETE FROM RuleNodeEntity e where e.id in :ids")
    void deleteByIdIn(@Param("ids") List<UUID> ids);

}

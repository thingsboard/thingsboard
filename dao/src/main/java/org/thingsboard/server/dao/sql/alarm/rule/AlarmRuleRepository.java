/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.alarm.rule;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.ExportableEntityRepository;
import org.thingsboard.server.dao.model.sql.AlarmRuleEntity;
import org.thingsboard.server.dao.model.sql.AlarmRuleInfoEntity;

import java.util.List;
import java.util.UUID;

public interface AlarmRuleRepository extends JpaRepository<AlarmRuleEntity, UUID>, ExportableEntityRepository<AlarmRuleEntity> {

    @Query("SELECT ai FROM AlarmRuleInfoEntity ai WHERE ai.tenantId = :tenantId " +
            "AND (:searchText IS NULL OR (ilike(ai.name, CONCAT('%', :searchText, '%')) = true))")
    Page<AlarmRuleInfoEntity> findInfosByTenantId(@Param("tenantId") UUID tenantId,
                                                  @Param("searchText") String searchText,
                                                  Pageable pageable);

    @Query("SELECT ai FROM AlarmRuleEntity ai WHERE ai.tenantId = :tenantId " +
            "AND (:searchText IS NULL OR (ilike(ai.name, CONCAT('%', :searchText, '%')) = true))")
    Page<AlarmRuleEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                         @Param("searchText") String searchText,
                                         Pageable pageable);

    @Query("SELECT ai FROM AlarmRuleEntity ai WHERE ai.tenantId = :tenantId " +
            "AND ai.enabled = true " +
            "AND (:searchText IS NULL OR (ilike(ai.name, CONCAT('%', :searchText, '%')) = true))")
    Page<AlarmRuleEntity> findByTenantIdAndEnabled(@Param("tenantId") UUID tenantId,
                                                   @Param("searchText") String searchText,
                                                   Pageable pageable);

    @Query(value = "SELECT cast(to_json(r) as varchar) FROM (" +
            "SELECT rns.entity_id, rns.rule_node_id, rn.debug_mode, rns.state_data FROM rule_node_state rns " +
            "INNER JOIN rule_node rn ON rns.rule_node_id = rn.id " +
            "WHERE rn.type = :type " +
            "AND rn.rule_chain_id = :ruleChainId " +
            "AND (SELECT d.device_profile_id FROM device d WHERE d.id = rns.entity_id) = :deviceProfileId) r;",
            nativeQuery = true)
    List<String> findRuleNodeStatesByRuleChainIdAndRuleNodeType(@Param("deviceProfileId") UUID deviceProfileId,
                                                                @Param("ruleChainId") UUID ruleChainId,
                                                                @Param("type") String type);

    @Query("SELECT externalId FROM AlarmRuleEntity WHERE id = :id")
    UUID getExternalIdById(@Param("id") UUID id);

    AlarmRuleEntity findByTenantIdAndName(UUID tenantId, String name);
}

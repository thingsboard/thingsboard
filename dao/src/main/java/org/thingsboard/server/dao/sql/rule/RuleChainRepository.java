/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.dao.model.sql.DashboardInfoEntity;
import org.thingsboard.server.dao.model.sql.RuleChainEntity;

import java.util.UUID;

public interface RuleChainRepository extends PagingAndSortingRepository<RuleChainEntity, UUID> {

    @Query("SELECT rc FROM RuleChainEntity rc WHERE rc.tenantId = :tenantId " +
            "AND LOWER(rc.searchText) LIKE LOWER(CONCAT(:searchText, '%'))")
    Page<RuleChainEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                         @Param("searchText") String searchText,
                                         Pageable pageable);

    @Query("SELECT rc FROM RuleChainEntity rc WHERE rc.tenantId = :tenantId " +
            "AND rc.type = :type " +
            "AND LOWER(rc.searchText) LIKE LOWER(CONCAT(:searchText, '%'))")
    Page<RuleChainEntity> findByTenantIdAndType(@Param("tenantId") UUID tenantId,
                                                @Param("type") RuleChainType type,
                                                @Param("searchText") String searchText,
                                                Pageable pageable);

    @Query("SELECT rc FROM RuleChainEntity rc, RelationEntity re WHERE rc.tenantId = :tenantId " +
            "AND rc.id = re.toId AND re.toType = 'RULE_CHAIN' AND re.relationTypeGroup = 'EDGE' " +
            "AND re.relationType = 'Contains' AND re.fromId = :edgeId AND re.fromType = 'EDGE' " +
            "AND LOWER(rc.searchText) LIKE LOWER(CONCAT(:searchText, '%'))")
    Page<RuleChainEntity> findByTenantIdAndEdgeId(@Param("tenantId") UUID tenantId,
                                                  @Param("edgeId") UUID edgeId,
                                                  @Param("searchText") String searchText,
                                                  Pageable pageable);
    Long countByTenantId(UUID tenantId);
}

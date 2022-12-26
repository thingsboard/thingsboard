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


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.dao.ExportableEntityRepository;
import org.thingsboard.server.dao.model.sql.RuleChainInfoEntity;

import java.util.UUID;

public interface RuleChainInfoRepository extends JpaRepository<RuleChainInfoEntity, UUID>, ExportableEntityRepository<RuleChainInfoEntity> {

        @Query(value = "SELECT new org.thingsboard.server.dao.model.sql.RuleChainInfoEntity(rc, CASE WHEN rc.id IN (SELECT rc_inner.id FROM RuleChainEntity AS rc_inner " +
                "INNER JOIN RuleNodeEntity AS rn ON rc_inner.id = rn.ruleChainId INNER JOIN AttributeKvEntity AS akv ON rn.id = akv.id.entityId WHERE akv.id.attributeKey = 'ruleNodeStats') THEN true ELSE false END) " +
                "FROM RuleChainEntity AS rc WHERE rc.tenantId = :tenantId AND rc.type = :type AND LOWER(rc.searchText) LIKE LOWER(CONCAT('%', :searchText, '%')) ",
                countQuery = "SELECT count(rc) FROM RuleChainEntity rc WHERE rc.tenantId = :tenantId AND rc.type = :type AND LOWER(rc.searchText) LIKE LOWER(CONCAT('%', :searchText, '%'))")
        Page<RuleChainInfoEntity> findErrorStatisticsByTenantIdAndRuleChain(@Param("tenantId") UUID tenantId,
                                                                            @Param("type") RuleChainType type,
                                                                            @Param("searchText") String searchText,
                                                                            Pageable pageable);
}

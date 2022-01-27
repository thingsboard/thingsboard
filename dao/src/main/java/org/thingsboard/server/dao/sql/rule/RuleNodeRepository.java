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

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.RuleNodeEntity;

import java.util.List;
import java.util.UUID;

public interface RuleNodeRepository extends CrudRepository<RuleNodeEntity, UUID> {

    @Query("SELECT r FROM RuleNodeEntity r WHERE r.ruleChainId in " +
            "(select id from RuleChainEntity rc WHERE rc.tenantId = :tenantId) " +
            "AND r.type = :ruleType AND LOWER(r.configuration) LIKE LOWER(CONCAT('%', :searchText, '%')) ")
    List<RuleNodeEntity> findRuleNodesByTenantIdAndType(@Param("tenantId") UUID tenantId,
                                                    @Param("ruleType") String ruleType,
                                                    @Param("searchText") String searchText);

}

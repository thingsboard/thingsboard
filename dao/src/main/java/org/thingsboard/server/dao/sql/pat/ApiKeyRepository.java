/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.pat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.model.sql.ApiKeyEntity;

import java.util.Set;
import java.util.UUID;

public interface ApiKeyRepository extends JpaRepository<ApiKeyEntity, UUID> {

    ApiKeyEntity findByValue(String value);

    @Transactional
    @Modifying
    @Query(value = """
                DELETE FROM api_key
                WHERE tenant_id = :tenantId
                RETURNING value
            """, nativeQuery = true
    )
    Set<String> deleteByTenantId(@Param("tenantId") UUID tenantId);

    @Transactional
    @Modifying
    @Query(value = """
                DELETE FROM api_key
                WHERE tenant_id = :tenantId AND user_id = :userId
                RETURNING value
            """, nativeQuery = true
    )
    Set<String> deleteByUserId(@Param("tenantId") UUID tenantId,
                               @Param("userId") UUID userId);

    @Transactional
    @Modifying
    @Query("DELETE FROM ApiKeyEntity ak WHERE ak.expirationTime > 0 AND ak.expirationTime < :ts")
    int deleteAllByExpirationTimeBefore(@Param("ts") long ts);

}

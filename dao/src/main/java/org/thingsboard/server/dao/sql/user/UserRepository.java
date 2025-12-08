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
package org.thingsboard.server.dao.sql.user;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.edqs.fields.UserFields;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.dao.model.sql.UserEntity;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    UserEntity findByEmail(String email);

    UserEntity findByTenantIdAndEmail(UUID tenantId, String email);

    @Query("SELECT u FROM UserEntity u WHERE u.tenantId = :tenantId " +
            "AND u.customerId = :customerId AND u.authority = :authority " +
            "AND (:searchText IS NULL OR ilike(u.email, CONCAT('%', :searchText, '%')) = true)")
    Page<UserEntity> findUsersByAuthority(@Param("tenantId") UUID tenantId,
                                          @Param("customerId") UUID customerId,
                                          @Param("searchText") String searchText,
                                          @Param("authority") Authority authority,
                                          Pageable pageable);

    @Query("SELECT u FROM UserEntity u WHERE u.tenantId = :tenantId " +
            "AND u.customerId IN (:customerIds) " +
            "AND (:searchText IS NULL OR ilike(u.email, CONCAT('%', :searchText, '%')) = true)")
    Page<UserEntity> findTenantAndCustomerUsers(@Param("tenantId") UUID tenantId,
                                                @Param("customerIds") Collection<UUID> customerIds,
                                                @Param("searchText") String searchText,
                                                Pageable pageable);

    @Query("SELECT u FROM UserEntity u WHERE u.tenantId = :tenantId " +
            "AND (:searchText IS NULL OR ilike(u.email, CONCAT('%', :searchText, '%')) = true)")
    Page<UserEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                    @Param("searchText") String searchText,
                                    Pageable pageable);

    Page<UserEntity> findAllByAuthority(Authority authority, Pageable pageable);

    Page<UserEntity> findByAuthorityAndTenantIdIn(Authority authority, Collection<UUID> tenantsIds, Pageable pageable);

    @Query("SELECT u FROM UserEntity u INNER JOIN TenantEntity t ON u.tenantId = t.id AND u.authority = :authority " +
            "INNER JOIN TenantProfileEntity p ON t.tenantProfileId = p.id " +
            "WHERE p.id IN :profiles")
    Page<UserEntity> findByAuthorityAndTenantProfilesIds(@Param("authority") Authority authority,
                                                         @Param("profiles") Collection<UUID> tenantProfilesIds,
                                                         Pageable pageable);

    Long countByTenantId(UUID tenantId);

    @Query("SELECT new org.thingsboard.server.common.data.edqs.fields.UserFields(u.id, u.createdTime, u.tenantId," +
            "u.customerId, u.version, u.firstName, u.lastName, u.email, u.phone, u.additionalInfo) " +
            "FROM UserEntity u WHERE u.id > :id ORDER BY u.id")
    List<UserFields> findNextBatch(@Param("id") UUID id, Limit limit);

    int countByTenantIdAndAuthority(UUID tenantId, Authority authority);

    @Query("SELECT new org.thingsboard.server.common.data.util.TbPair(u, uc.enabled) " +
            "FROM UserEntity u JOIN UserCredentialsEntity uc ON u.id = uc.userId WHERE u.id = :userId ")
    TbPair<UserEntity, Boolean> findUserAuthDetailsByUserId(@Param("userId") UUID userId);

    List<UserEntity> findUsersByTenantIdAndIdIn(UUID tenantId, List<UUID> userIds);

}

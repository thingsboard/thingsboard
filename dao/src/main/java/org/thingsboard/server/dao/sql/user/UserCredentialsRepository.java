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
package org.thingsboard.server.dao.sql.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.model.sql.UserCredentialsEntity;

import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 4/22/2017.
 */
public interface UserCredentialsRepository extends JpaRepository<UserCredentialsEntity, UUID> {

    UserCredentialsEntity findByUserId(UUID userId);

    UserCredentialsEntity findByActivateToken(String activateToken);

    UserCredentialsEntity findByResetToken(String resetToken);

    @Transactional
    void removeByUserId(UUID userId);

    @Transactional
    @Modifying
    @Query("UPDATE UserCredentialsEntity SET lastLoginTs = :lastLoginTs WHERE userId = :userId")
    void updateLastLoginTsByUserId(UUID userId, long lastLoginTs);

    @Transactional
    @Query(value = "UPDATE user_credentials SET failed_login_attempts = coalesce(failed_login_attempts, 0) + 1 " +
            "WHERE user_id = :userId RETURNING failed_login_attempts", nativeQuery = true)
    int incrementFailedLoginAttemptsByUserId(UUID userId);

    @Transactional
    @Modifying
    @Query("UPDATE UserCredentialsEntity SET failedLoginAttempts = :failedLoginAttempts WHERE userId = :userId")
    void updateFailedLoginAttemptsByUserId(UUID userId, int failedLoginAttempts);

    @Query("SELECT c FROM UserCredentialsEntity c WHERE c.userId IN (SELECT u.id FROM UserEntity u WHERE u.tenantId = :tenantId)")
    Page<UserCredentialsEntity> findByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

}

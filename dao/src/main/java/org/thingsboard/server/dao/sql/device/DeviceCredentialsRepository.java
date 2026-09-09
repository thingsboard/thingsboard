// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql.device;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.model.sql.DeviceCredentialsEntity;

import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
public interface DeviceCredentialsRepository extends JpaRepository<DeviceCredentialsEntity, UUID> {

    DeviceCredentialsEntity findByDeviceId(UUID deviceId);

    DeviceCredentialsEntity findByCredentialsId(String credentialsId);

    @Transactional
    @Query(value = "DELETE FROM device_credentials WHERE device_id = :deviceId RETURNING *", nativeQuery = true)
    DeviceCredentialsEntity deleteByDeviceId(@Param("deviceId") UUID deviceId);


    @Query("SELECT c FROM DeviceCredentialsEntity c WHERE c.deviceId IN (SELECT d.id FROM DeviceEntity d WHERE d.tenantId = :tenantId)")
    Page<DeviceCredentialsEntity> findByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

}

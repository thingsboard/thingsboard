// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.sql.device;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.DeviceProfileCacheInfo;
import org.thingsboard.server.common.data.DeviceProfileInfo;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.edqs.fields.DeviceProfileFields;
import org.thingsboard.server.dao.ExportableEntityRepository;
import org.thingsboard.server.dao.model.sql.DeviceProfileEntity;

import java.util.List;
import java.util.UUID;

public interface DeviceProfileRepository extends JpaRepository<DeviceProfileEntity, UUID>, ExportableEntityRepository<DeviceProfileEntity> {

    @Query("SELECT new org.thingsboard.server.common.data.DeviceProfileInfo(d.id, d.tenantId, d.name, d.image, d.defaultDashboardId, d.type, d.transportType) " +
            "FROM DeviceProfileEntity d " +
            "WHERE d.id = :deviceProfileId")
    DeviceProfileInfo findDeviceProfileInfoById(@Param("deviceProfileId") UUID deviceProfileId);

    @Query("SELECT d FROM DeviceProfileEntity d WHERE d.tenantId = :tenantId " +
            "AND (:textSearch IS NULL OR ilike(d.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<DeviceProfileEntity> findDeviceProfiles(@Param("tenantId") UUID tenantId,
                                                 @Param("textSearch") String textSearch,
                                                 Pageable pageable);

    @Query("SELECT new org.thingsboard.server.common.data.DeviceProfileInfo(d.id, d.tenantId, d.name, d.image, d.defaultDashboardId, d.type, d.transportType) " +
            "FROM DeviceProfileEntity d WHERE d.tenantId = :tenantId " +
            "AND (:textSearch IS NULL OR ilike(d.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<DeviceProfileInfo> findDeviceProfileInfos(@Param("tenantId") UUID tenantId,
                                                   @Param("textSearch") String textSearch,
                                                   Pageable pageable);

    @Query("SELECT new org.thingsboard.server.common.data.DeviceProfileInfo(d.id, d.tenantId, d.name, d.image, d.defaultDashboardId, d.type, d.transportType) " +
            "FROM DeviceProfileEntity d WHERE d.tenantId = :tenantId AND d.transportType = :transportType " +
            "AND (:textSearch IS NULL OR ilike(d.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<DeviceProfileInfo> findDeviceProfileInfos(@Param("tenantId") UUID tenantId,
                                                   @Param("textSearch") String textSearch,
                                                   @Param("transportType") DeviceTransportType transportType,
                                                   Pageable pageable);

    @Query("SELECT new org.thingsboard.server.common.data.DeviceProfileInfo(d.id, d.tenantId, d.name, d.image, d.defaultDashboardId, d.type, d.transportType) " +
            "FROM DeviceProfileEntity d WHERE " +
            "d.tenantId = :tenantId AND d.id IN :deviceProfileIds")
    List<DeviceProfileInfo> findDeviceProfileInfosByTenantIdAndIdIn(@Param("tenantId") UUID tenantId,
                                                                    @Param("deviceProfileIds") List<UUID> deviceProfileIds);

    @Query("SELECT d FROM DeviceProfileEntity d " +
            "WHERE d.tenantId = :tenantId AND d.isDefault = true")
    DeviceProfileEntity findByDefaultTrueAndTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT new org.thingsboard.server.common.data.DeviceProfileInfo(d.id, d.tenantId, d.name, d.image, d.defaultDashboardId, d.type, d.transportType) " +
            "FROM DeviceProfileEntity d " +
            "WHERE d.tenantId = :tenantId AND d.isDefault = true")
    DeviceProfileInfo findDefaultDeviceProfileInfo(@Param("tenantId") UUID tenantId);

    DeviceProfileEntity findByTenantIdAndName(UUID id, String profileName);

    DeviceProfileEntity findByProvisionDeviceKey(@Param("provisionDeviceKey") String provisionDeviceKey);

    @Query("SELECT new org.thingsboard.server.common.data.DeviceProfileInfo(d.id, d.tenantId, d.name, d.image, d.defaultDashboardId, d.type, d.transportType) " +
            "FROM DeviceProfileEntity d WHERE d.tenantId = :tenantId AND d.image = :imageLink")
    List<DeviceProfileInfo> findByTenantAndImageLink(@Param("tenantId") UUID tenantId, @Param("imageLink") String imageLink, Pageable page);

    @Query("SELECT new org.thingsboard.server.common.data.DeviceProfileInfo(d.id, d.tenantId, d.name, d.image, d.defaultDashboardId, d.type, d.transportType) " +
            "FROM DeviceProfileEntity d WHERE d.image = :imageLink")
    List<DeviceProfileInfo> findByImageLink(@Param("imageLink") String imageLink, Pageable page);

    @Query("SELECT externalId FROM DeviceProfileEntity WHERE id = :id")
    UUID getExternalIdById(@Param("id") UUID id);

    Page<DeviceProfileEntity> findAllByImageNotNull(Pageable pageable);

    // Non-correlated IN-subquery semi-join (an "active" profile = one referenced by at least one device of the
    // same tenant). This replaces the prior comma cross-join + DISTINCT, which materialized one row per
    // (profile, device) pair before dedup (pathological fan-out). The IN form is equivalent (same names, no
    // ordering on either) and is both Citus-legal and faster in plain mode, so it is kept unconditional.
    @Query("SELECT new org.thingsboard.server.common.data.EntityInfo(dp.id, 'DEVICE_PROFILE', dp.name) " +
            "FROM DeviceProfileEntity dp " +
            "WHERE dp.tenantId = :tenantId " +
            "AND dp.id IN (SELECT dv.deviceProfileId FROM DeviceEntity dv WHERE dv.tenantId = :tenantId)")
    List<EntityInfo> findActiveTenantDeviceProfileNames(@Param("tenantId") UUID tenantId);

    @Query("SELECT new org.thingsboard.server.common.data.EntityInfo(d.id, 'DEVICE_PROFILE', d.name) " +
            "FROM DeviceProfileEntity d WHERE d.tenantId = :tenantId")
    List<EntityInfo> findAllTenantDeviceProfileNames(@Param("tenantId") UUID tenantId);

    @Query("SELECT new org.thingsboard.server.common.data.edqs.fields.DeviceProfileFields(d.id, d.createdTime, d.tenantId," +
            "d.name, d.version, d.type, d.isDefault) FROM DeviceProfileEntity d WHERE d.id > :id ORDER BY d.id")
    List<DeviceProfileFields> findNextBatch(@Param("id") UUID id, Limit limit);

    @Query("SELECT new org.thingsboard.server.common.data.DeviceProfileCacheInfo(d.id, d.tenantId, d.name, d.defaultRuleChainId, d.defaultQueueName) " +
            "FROM DeviceProfileEntity d WHERE d.id > :id ORDER BY d.id")
    List<DeviceProfileCacheInfo> findDeviceProfileCacheInfos(@Param("id") UUID id, Limit limit);

}

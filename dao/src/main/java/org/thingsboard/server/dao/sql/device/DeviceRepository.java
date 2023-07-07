/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.device;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.dao.ExportableEntityRepository;
import org.thingsboard.server.dao.model.sql.DeviceEntity;
import org.thingsboard.server.dao.model.sql.DeviceInfoEntity;

import java.util.List;
import java.util.UUID;

public interface DeviceRepository extends JpaRepository<DeviceEntity, UUID>, ExportableEntityRepository<DeviceEntity> {

    @Query("SELECT d FROM DeviceInfoEntity d WHERE d.id = :deviceId")
    DeviceInfoEntity findDeviceInfoById(@Param("deviceId") UUID deviceId);

    @Query("SELECT d FROM DeviceEntity d WHERE d.tenantId = :tenantId " +
            "AND d.customerId = :customerId " +
            "AND (LOWER(d.name) LIKE LOWER(CONCAT('%', :textSearch, '%')) " +
            "OR LOWER(d.label) LIKE LOWER(CONCAT('%', :textSearch, '%')))")
    Page<DeviceEntity> findByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                                   @Param("customerId") UUID customerId,
                                                   @Param("textSearch") String textSearch,
                                                   Pageable pageable);

    @Query("SELECT d FROM DeviceEntity d WHERE d.tenantId = :tenantId " +
            "AND d.deviceProfileId = :profileId " +
            "AND (LOWER(d.name) LIKE LOWER(CONCAT('%', :textSearch, '%')) " +
            "OR LOWER(d.label) LIKE LOWER(CONCAT('%', :textSearch, '%')))")
    Page<DeviceEntity> findByTenantIdAndProfileId(@Param("tenantId") UUID tenantId,
                                                  @Param("profileId") UUID profileId,
                                                  @Param("textSearch") String textSearch,
                                                  Pageable pageable);

    @Query("SELECT d FROM DeviceInfoEntity d " +
            "WHERE d.tenantId = :tenantId " +
            "AND d.customerId = :customerId " +
            "AND (LOWER(d.name) LIKE LOWER(CONCAT('%', :textSearch, '%')) " +
            "OR LOWER(d.label) LIKE LOWER(CONCAT('%', :textSearch, '%')))")
    Page<DeviceInfoEntity> findDeviceInfosByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                                                  @Param("customerId") UUID customerId,
                                                                  @Param("textSearch") String textSearch,
                                                                  Pageable pageable);

    @Query("SELECT d FROM DeviceEntity d WHERE d.tenantId = :tenantId")
    Page<DeviceEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                      Pageable pageable);

    @Query("SELECT d FROM DeviceEntity d WHERE d.tenantId = :tenantId " +
            "AND (LOWER(d.name) LIKE LOWER(CONCAT('%', :textSearch, '%')) " +
            "OR LOWER(d.label) LIKE LOWER(CONCAT('%', :textSearch, '%')))")
    Page<DeviceEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                      @Param("textSearch") String textSearch,
                                      Pageable pageable);

    @Query("SELECT d FROM DeviceEntity d WHERE d.tenantId = :tenantId " +
            "AND d.type = :type " +
            "AND (LOWER(d.name) LIKE LOWER(CONCAT('%', :textSearch, '%')) " +
            "OR LOWER(d.label) LIKE LOWER(CONCAT('%', :textSearch, '%')))")
    Page<DeviceEntity> findByTenantIdAndType(@Param("tenantId") UUID tenantId,
                                             @Param("type") String type,
                                             @Param("textSearch") String textSearch,
                                             Pageable pageable);

    @Query("SELECT d FROM DeviceEntity d WHERE d.tenantId = :tenantId " +
            "AND d.deviceProfileId = :deviceProfileId " +
            "AND d.firmwareId = null " +
            "AND (LOWER(d.name) LIKE LOWER(CONCAT('%', :textSearch, '%')) " +
            "OR LOWER(d.label) LIKE LOWER(CONCAT('%', :textSearch, '%')))")
    Page<DeviceEntity> findByTenantIdAndTypeAndFirmwareIdIsNull(@Param("tenantId") UUID tenantId,
                                                                @Param("deviceProfileId") UUID deviceProfileId,
                                                                @Param("textSearch") String textSearch,
                                                                Pageable pageable);

    @Query("SELECT d FROM DeviceEntity d WHERE d.tenantId = :tenantId " +
            "AND d.deviceProfileId = :deviceProfileId " +
            "AND d.softwareId = null " +
            "AND (LOWER(d.name) LIKE LOWER(CONCAT('%', :textSearch, '%')) " +
            "OR LOWER(d.label) LIKE LOWER(CONCAT('%', :textSearch, '%')))")
    Page<DeviceEntity> findByTenantIdAndTypeAndSoftwareIdIsNull(@Param("tenantId") UUID tenantId,
                                                                @Param("deviceProfileId") UUID deviceProfileId,
                                                                @Param("textSearch") String textSearch,
                                                                Pageable pageable);

    @Query("SELECT count(*) FROM DeviceEntity d WHERE d.tenantId = :tenantId " +
            "AND d.deviceProfileId = :deviceProfileId " +
            "AND d.firmwareId = null")
    Long countByTenantIdAndDeviceProfileIdAndFirmwareIdIsNull(@Param("tenantId") UUID tenantId,
                                                              @Param("deviceProfileId") UUID deviceProfileId);

    @Query("SELECT count(*) FROM DeviceEntity d WHERE d.tenantId = :tenantId " +
            "AND d.deviceProfileId = :deviceProfileId " +
            "AND d.softwareId = null")
    Long countByTenantIdAndDeviceProfileIdAndSoftwareIdIsNull(@Param("tenantId") UUID tenantId,
                                                              @Param("deviceProfileId") UUID deviceProfileId);

    @Query("SELECT d FROM DeviceEntity d WHERE d.tenantId = :tenantId " +
            "AND d.customerId = :customerId " +
            "AND d.type = :type " +
            "AND (LOWER(d.name) LIKE LOWER(CONCAT('%', :textSearch, '%')) " +
            "OR LOWER(d.label) LIKE LOWER(CONCAT('%', :textSearch, '%')))")
    Page<DeviceEntity> findByTenantIdAndCustomerIdAndType(@Param("tenantId") UUID tenantId,
                                                          @Param("customerId") UUID customerId,
                                                          @Param("type") String type,
                                                          @Param("textSearch") String textSearch,
                                                          Pageable pageable);

    @Query("SELECT d FROM DeviceInfoEntity d " +
            "WHERE d.tenantId = :tenantId " +
            "AND (:customerId IS NULL OR d.customerId = uuid(:customerId)) " +
            "AND (:edgeId IS NULL OR d.id IN (SELECT re.toId FROM RelationEntity re WHERE re.toType = 'DEVICE' AND re.relationTypeGroup = 'EDGE' AND re.relationType = 'Contains' AND re.fromType = 'EDGE' AND re.fromId = uuid(:edgeId))) " +
            "AND ((:deviceType) IS NULL OR d.type = :deviceType) " +
            "AND (:deviceProfileId IS NULL OR d.deviceProfileId = uuid(:deviceProfileId)) " +
            "AND ((:filterByActive) = FALSE OR d.active = :deviceActive) " +
            "AND (LOWER(d.name) LIKE LOWER(CONCAT('%', :textSearch, '%')) " +
            "OR LOWER(d.label) LIKE LOWER(CONCAT('%', :textSearch, '%')) " +
            "OR LOWER(d.type) LIKE LOWER(CONCAT('%', :textSearch, '%')) " +
            "OR LOWER(d.customerTitle) LIKE LOWER(CONCAT('%', :textSearch, '%')))")
    Page<DeviceInfoEntity> findDeviceInfosByFilter(@Param("tenantId") UUID tenantId,
                                                   @Param("customerId") String customerId,
                                                   @Param("edgeId") String edgeId,
                                                   @Param("deviceType") String type,
                                                   @Param("deviceProfileId") String deviceProfileId,
                                                   @Param("filterByActive") boolean filterByActive,
                                                   @Param("deviceActive") boolean active,
                                                   @Param("textSearch") String textSearch,
                                                   Pageable pageable);

    @Query("SELECT DISTINCT d.type FROM DeviceEntity d WHERE d.tenantId = :tenantId")
    List<String> findTenantDeviceTypes(@Param("tenantId") UUID tenantId);

    DeviceEntity findByTenantIdAndName(UUID tenantId, String name);

    List<DeviceEntity> findDevicesByTenantIdAndCustomerIdAndIdIn(UUID tenantId, UUID customerId, List<UUID> deviceIds);

    List<DeviceEntity> findDevicesByTenantIdAndIdIn(UUID tenantId, List<UUID> deviceIds);

    List<DeviceEntity> findDevicesByIdIn(List<UUID> deviceIds);

    DeviceEntity findByTenantIdAndId(UUID tenantId, UUID id);

    Long countByDeviceProfileId(UUID deviceProfileId);

    @Query("SELECT d FROM DeviceEntity d, RelationEntity re WHERE d.tenantId = :tenantId " +
            "AND d.id = re.toId AND re.toType = 'DEVICE' AND re.relationTypeGroup = 'EDGE' " +
            "AND re.relationType = 'Contains' AND re.fromId = :edgeId AND re.fromType = 'EDGE' " +
            "AND (LOWER(d.name) LIKE LOWER(CONCAT('%', :textSearch, '%')) " +
            "OR LOWER(d.label) LIKE LOWER(CONCAT('%', :textSearch, '%')))")
    Page<DeviceEntity> findByTenantIdAndEdgeId(@Param("tenantId") UUID tenantId,
                                               @Param("edgeId") UUID edgeId,
                                               @Param("textSearch") String textSearch,
                                               Pageable pageable);

    @Query("SELECT d FROM DeviceEntity d, RelationEntity re WHERE d.tenantId = :tenantId " +
            "AND d.id = re.toId AND re.toType = 'DEVICE' AND re.relationTypeGroup = 'EDGE' " +
            "AND re.relationType = 'Contains' AND re.fromId = :edgeId AND re.fromType = 'EDGE' " +
            "AND d.type = :type " +
            "AND (LOWER(d.name) LIKE LOWER(CONCAT('%', :textSearch, '%')) " +
            "OR LOWER(d.label) LIKE LOWER(CONCAT('%', :textSearch, '%')))")
    Page<DeviceEntity> findByTenantIdAndEdgeIdAndType(@Param("tenantId") UUID tenantId,
                                                      @Param("edgeId") UUID edgeId,
                                                      @Param("type") String type,
                                                      @Param("textSearch") String textSearch,
                                                      Pageable pageable);

    /**
     * Count devices by tenantId.
     * Custom query applied because default QueryDSL produces slow count(id).
     *
     * There is two way to count devices.
     * OPTIMAL: count(*)
     * - returns _row_count_ and use index-only scan (super fast).
     * SLOW: count(id)
     * - returns _NON_NULL_id_count and performs table scan to verify isNull for each id in filtered rows.
     */
    @Query("SELECT count(*) FROM DeviceEntity d WHERE d.tenantId = :tenantId")
    Long countByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT d.id FROM DeviceEntity d " +
            "INNER JOIN DeviceProfileEntity p ON d.deviceProfileId = p.id " +
            "WHERE p.transportType = :transportType")
    Page<UUID> findIdsByDeviceProfileTransportType(@Param("transportType") DeviceTransportType transportType, Pageable pageable);

    @Query("SELECT externalId FROM DeviceEntity WHERE id = :id")
    UUID getExternalIdById(@Param("id") UUID id);

}

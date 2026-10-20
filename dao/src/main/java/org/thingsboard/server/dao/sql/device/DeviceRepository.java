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
import org.thingsboard.server.common.data.DeviceCacheInfo;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.edqs.fields.DeviceFields;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.dao.ExportableEntityRepository;
import org.thingsboard.server.dao.model.sql.DeviceEntity;

import java.util.List;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
public interface DeviceRepository extends JpaRepository<DeviceEntity, UUID>, ExportableEntityRepository<DeviceEntity> {

    String COUNT_QUERY_BY_DEVICE_PROFILE_AND_FIRMWARE_IS_NULL = "SELECT count(d.*) FROM device d " +
            "WHERE d.device_profile_id = :deviceProfileId " +
            "AND d.tenant_id = :tenantId " +
            "AND d.firmware_id is null " +
            "AND d.id NOT IN ( " +
            "SELECT r.to_id " +
            "FROM relation r " +
            "WHERE r.to_id IN (" +
            "SELECT d.id " +
            "FROM device d " +
            "WHERE d.device_profile_id = :deviceProfileId " +
            "AND d.tenant_id = :tenantId " +
            "AND d.firmware_id is null) " +
            "AND r.to_type = 'DEVICE' " +
            "AND r.relation_type_group = 'FROM_ENTITY_GROUP' " +
            "AND r.from_id IN (" +
            "SELECT dgop.group_id " +
            "FROM ota_package ota " +
            "INNER JOIN device_group_ota_package dgop ON dgop.ota_package_id = ota.id AND dgop.ota_package_type = 'FIRMWARE' " +
            "WHERE ota.device_profile_id = :deviceProfileId)" +
            ")";
    String COUNT_QUERY_BY_DEVICE_PROFILE_AND_SOFTWARE_IS_NULL = "SELECT count(d.*) FROM device d " +
            "WHERE d.device_profile_id = :deviceProfileId " +
            "AND d.tenant_id = :tenantId " +
            "AND d.software_id is null " +
            "AND d.id NOT IN ( " +
            "SELECT r.to_id " +
            "FROM relation r " +
            "WHERE r.to_id IN (" +
            "SELECT d.id " +
            "FROM device d " +
            "WHERE d.device_profile_id = :deviceProfileId " +
            "AND d.tenant_id = :tenantId " +
            "AND d.software_id is null) " +
            "AND r.to_type = 'DEVICE' " +
            "AND r.relation_type_group = 'FROM_ENTITY_GROUP' " +
            "AND r.from_id IN (" +
            "SELECT dgop.group_id " +
            "FROM ota_package ota " +
            "INNER JOIN device_group_ota_package dgop ON dgop.ota_package_id = ota.id AND dgop.ota_package_type = 'SOFTWARE' " +
            "WHERE ota.device_profile_id = :deviceProfileId)" +
            ")";

    @Query("SELECT d FROM DeviceEntity d WHERE d.tenantId = :tenantId " +
            "AND d.customerId = :customerId " +
            "AND (:textSearch IS NULL OR ilike(d.name, CONCAT('%', :textSearch, '%')) = true " +
            "OR ilike(d.label, CONCAT('%', :textSearch, '%')) = true)")
    Page<DeviceEntity> findByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                                   @Param("customerId") UUID customerId,
                                                   @Param("textSearch") String textSearch,
                                                   Pageable pageable);

    @Query("SELECT d FROM DeviceEntity d WHERE d.tenantId = :tenantId " +
            "AND d.deviceProfileId = :profileId " +
            "AND (:textSearch IS NULL OR ilike(d.name, CONCAT('%', :textSearch, '%')) = true " +
            "OR ilike(d.label, CONCAT('%', :textSearch, '%')) = true)")
    Page<DeviceEntity> findByTenantIdAndProfileId(@Param("tenantId") UUID tenantId,
                                                  @Param("profileId") UUID profileId,
                                                  @Param("textSearch") String textSearch,
                                                  Pageable pageable);

    @Query("SELECT d FROM DeviceEntity d WHERE d.tenantId = :tenantId")
    Page<DeviceEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                      Pageable pageable);

    @Query("SELECT d FROM DeviceEntity d WHERE d.tenantId = :tenantId " +
            "AND (:textSearch IS NULL OR ilike(d.name, CONCAT('%', :textSearch, '%')) = true " +
            "OR ilike(d.label, CONCAT('%', :textSearch, '%')) = true)")
    Page<DeviceEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                      @Param("textSearch") String textSearch,
                                      Pageable pageable);

    @Query("SELECT d FROM DeviceEntity d WHERE d.tenantId = :tenantId " +
            "AND d.type = :type " +
            "AND (:textSearch IS NULL OR ilike(d.name, CONCAT('%', :textSearch, '%')) = true " +
            "OR ilike(d.label, CONCAT('%', :textSearch, '%')) = true)")
    Page<DeviceEntity> findByTenantIdAndType(@Param("tenantId") UUID tenantId,
                                             @Param("type") String type,
                                             @Param("textSearch") String textSearch,
                                             Pageable pageable);

    @Query("SELECT d.id FROM DeviceEntity d WHERE d.tenantId = :tenantId " +
            "AND d.deviceProfileId = :deviceProfileId " +
            "AND (:textSearch IS NULL OR ilike(d.type, CONCAT('%', :textSearch, '%')) = true)")
    Page<UUID> findIdsByTenantIdAndDeviceProfileId(@Param("tenantId") UUID tenantId,
                                                   @Param("deviceProfileId") UUID deviceProfileId,
                                                   @Param("textSearch") String textSearch,
                                                   Pageable pageable);

    @Query("SELECT d FROM DeviceEntity d WHERE d.tenantId = :tenantId " +
            "AND d.customerId = :customerId " +
            "AND d.type = :type " +
            "AND (:textSearch IS NULL OR ilike(d.name, CONCAT('%', :textSearch, '%')) = true " +
            "OR ilike(d.label, CONCAT('%', :textSearch, '%')) = true)")
    Page<DeviceEntity> findByTenantIdAndCustomerIdAndType(@Param("tenantId") UUID tenantId,
                                                          @Param("customerId") UUID customerId,
                                                          @Param("type") String type,
                                                          @Param("textSearch") String textSearch,
                                                          Pageable pageable);

    @Query("SELECT d.id FROM DeviceEntity d WHERE d.tenantId = :tenantId AND (d.customerId is null OR d.customerId = org.thingsboard.server.common.data.id.EntityId.NULL_UUID)")
    Page<UUID> findIdsByTenantIdAndNullCustomerId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT d.id FROM DeviceEntity d WHERE d.tenantId = :tenantId AND d.customerId = :customerId")
    Page<UUID> findIdsByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                              @Param("customerId") UUID customerId,
                                              Pageable pageable);

    @Query("SELECT d FROM DeviceEntity d, " +
            "RelationEntity re " +
            "WHERE d.id = re.toId AND re.toType = 'DEVICE' " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains' " +
            "AND re.fromId = :groupId AND re.fromType = 'ENTITY_GROUP' " +
            "AND (:textSearch IS NULL OR ilike(d.name, CONCAT('%', :textSearch, '%')) = true " +
            "OR ilike(d.label, CONCAT('%', :textSearch, '%')) = true)")
    Page<DeviceEntity> findByEntityGroupId(@Param("groupId") UUID groupId,
                                           @Param("textSearch") String textSearch,
                                           Pageable pageable);

    @Query("SELECT d FROM DeviceEntity d, " +
            "RelationEntity re " +
            "WHERE d.id = re.toId AND re.toType = 'DEVICE' " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains' " +
            "AND re.fromId in :groupIds AND re.fromType = 'ENTITY_GROUP' " +
            "AND (:textSearch IS NULL OR ilike(d.name, CONCAT('%', :textSearch, '%')) = true " +
            "OR ilike(d.label, CONCAT('%', :textSearch, '%')) = true)")
    Page<DeviceEntity> findByEntityGroupIds(@Param("groupIds") List<UUID> groupIds,
                                            @Param("textSearch") String textSearch,
                                            Pageable pageable);

    @Query("SELECT d FROM DeviceEntity d, " +
            "RelationEntity re " +
            "WHERE d.id = re.toId AND re.toType = 'DEVICE' " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains' " +
            "AND re.fromId in :groupIds AND re.fromType = 'ENTITY_GROUP' " +
            "AND d.type = :type " +
            "AND (:textSearch IS NULL OR ilike(d.name, CONCAT('%', :textSearch, '%')) = true " +
            "OR ilike(d.label, CONCAT('%', :textSearch, '%')) = true)")
    Page<DeviceEntity> findByEntityGroupIdsAndType(@Param("groupIds") List<UUID> groupIds,
                                                   @Param("type") String type,
                                                   @Param("textSearch") String textSearch,
                                                   Pageable pageable);

    // First-match (not strict single-result): under Citus the (tenant_id, name) UNIQUE constraint is dropped,
    // so a select-then-insert race can momentarily leave duplicate rows. Returning the first match converts the
    // resulting IncorrectResultSizeDataAccessException (HTTP 500) into the intended DataValidationException (400);
    // it does not fix the inherent race (best-effort). Plain mode keeps the constraint, so at most one row matches.
    DeviceEntity findFirstByTenantIdAndName(UUID tenantId, String name);

    @Override
    @Query(value = "SELECT * FROM device WHERE tenant_id = :tenantId AND external_id = :externalId LIMIT 1", nativeQuery = true)
    DeviceEntity findByTenantIdAndExternalId(@Param("tenantId") UUID tenantId, @Param("externalId") UUID externalId);

    @Query("SELECT new org.thingsboard.server.common.data.EntityInfo(a.id, 'DEVICE', a.name) " +
            "FROM DeviceEntity a WHERE a.tenantId = :tenantId AND a.name LIKE CONCAT(:prefix, '%')")
    List<EntityInfo> findEntityInfosByNamePrefix(UUID tenantId, String prefix);

    List<DeviceEntity> findDevicesByTenantIdAndCustomerIdAndIdIn(UUID tenantId, UUID customerId, List<UUID> deviceIds);

    List<DeviceEntity> findDevicesByTenantIdAndIdIn(UUID tenantId, List<UUID> deviceIds);

    List<DeviceEntity> findDevicesByIdIn(List<UUID> deviceIds);

    DeviceEntity findByTenantIdAndId(UUID tenantId, UUID id);

    Long countByDeviceProfileId(UUID deviceProfileId);

    /**
     * Count devices by tenantId.
     * Custom query applied because default QueryDSL produces slow count(id).
     * <p>
     * There is two way to count devices.
     * OPTIMAL: count(*)
     *   - returns _row_count_ and use index-only scan (super fast).
     * SLOW: count(id)
     *   - returns _NON_NULL_id_count and performs table scan to verify isNull for each id in filtered rows.
     * */
    @Query("SELECT count(*) FROM DeviceEntity d WHERE d.tenantId = :tenantId")
    Long countByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT d.id FROM DeviceEntity d " +
            "INNER JOIN DeviceProfileEntity p ON d.deviceProfileId = p.id " +
            "WHERE p.transportType = :transportType")
    Page<UUID> findIdsByDeviceProfileTransportType(@Param("transportType") DeviceTransportType transportType, Pageable pageable);

    @Query("SELECT d FROM DeviceEntity d, " +
            "RelationEntity re " +
            "WHERE d.id = re.toId AND re.toType = 'DEVICE' " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains' " +
            "AND re.fromId = :groupId AND re.fromType = 'ENTITY_GROUP' " +
            "AND d.deviceProfileId = :deviceProfileId " +
            "AND d.firmwareId IS NULL")
    Page<DeviceEntity> findByEntityGroupIdAndDeviceProfileIdAndFirmwareIdIsNull(@Param("groupId") UUID groupId,
                                                                                @Param("deviceProfileId") UUID deviceProfileId,
                                                                                Pageable pageable);

    @Query("SELECT d FROM DeviceEntity d, " +
            "RelationEntity re " +
            "WHERE d.id = re.toId AND re.toType = 'DEVICE' " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains' " +
            "AND re.fromId = :groupId AND re.fromType = 'ENTITY_GROUP' " +
            "AND d.deviceProfileId = :deviceProfileId " +
            "AND d.softwareId IS NULL")
    Page<DeviceEntity> findByEntityGroupIdAndDeviceProfileIdAndSoftwareIdIsNull(@Param("groupId") UUID groupId,
                                                                                @Param("deviceProfileId") UUID deviceProfileId,
                                                                                Pageable pageable);

    @Query(value = "SELECT d.* FROM device d " +
            "WHERE d.device_profile_id = :deviceProfileId " +
            "AND d.tenant_id = :tenantId " +
            "AND d.firmware_id is null " +
            "AND d.id NOT IN ( " +
            "SELECT r.to_id " +
            "FROM relation r " +
            "WHERE r.to_id IN (" +
            "SELECT d.id " +
            "FROM device d " +
            "WHERE d.device_profile_id = :deviceProfileId " +
            "AND d.tenant_id = :tenantId " +
            "AND d.firmware_id is null) " +
            "AND r.to_type = 'DEVICE' " +
            "AND r.relation_type_group = 'FROM_ENTITY_GROUP' " +
            "AND r.from_id IN (" +
            "SELECT dgop.group_id " +
            "FROM ota_package ota " +
            "INNER JOIN device_group_ota_package dgop ON dgop.ota_package_id = ota.id AND dgop.ota_package_type = 'FIRMWARE' " +
            "WHERE ota.device_profile_id = :deviceProfileId)" +
            ")",

            countQuery = COUNT_QUERY_BY_DEVICE_PROFILE_AND_FIRMWARE_IS_NULL,
            nativeQuery = true)
    Page<DeviceEntity> findByDeviceProfileIdAndFirmwareIdIsNull(@Param("tenantId") UUID tenantId,
                                                                @Param("deviceProfileId") UUID deviceProfileId,
                                                                Pageable pageable);

    @Query(value = "SELECT d.* FROM device d " +
            "WHERE d.device_profile_id = :deviceProfileId " +
            "AND d.tenant_id = :tenantId " +
            "AND d.software_id is null " +
            "AND d.id NOT IN ( " +
            "SELECT r.to_id " +
            "FROM relation r " +
            "WHERE r.to_id IN (" +
            "SELECT d.id " +
            "FROM device d " +
            "WHERE d.device_profile_id = :deviceProfileId " +
            "AND d.tenant_id = :tenantId " +
            "AND d.software_id is null) " +
            "AND r.to_type = 'DEVICE' " +
            "AND r.relation_type_group = 'FROM_ENTITY_GROUP' " +
            "AND r.from_id IN (" +
            "SELECT dgop.group_id " +
            "FROM ota_package ota " +
            "INNER JOIN device_group_ota_package dgop ON dgop.ota_package_id = ota.id AND dgop.ota_package_type = 'SOFTWARE' " +
            "WHERE ota.device_profile_id = :deviceProfileId)" +
            ")",

            countQuery = COUNT_QUERY_BY_DEVICE_PROFILE_AND_SOFTWARE_IS_NULL,
            nativeQuery = true)
    Page<DeviceEntity> findByDeviceProfileIdAndSoftwareIdIsNull(@Param("tenantId") UUID tenantId,
                                                                @Param("deviceProfileId") UUID deviceProfileId,
                                                                Pageable pageable);

    @Query("SELECT count(*) FROM DeviceEntity d, " +
            "RelationEntity re " +
            "WHERE d.id = re.toId AND re.toType = 'DEVICE' " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains' " +
            "AND re.fromId = :groupId AND re.fromType = 'ENTITY_GROUP' " +
            "AND d.deviceProfileId = (SELECT op.deviceProfileId FROM OtaPackageInfoEntity op WHERE op.id = :otaPackageId) " +
            "AND d.firmwareId IS NULL")
    Long countByEntityGroupIdAndFirmwareIdIsNull(@Param("groupId") UUID groupId,
                                                 @Param("otaPackageId") UUID otaPackageId);

    @Query("SELECT count(*) FROM DeviceEntity d, " +
            "RelationEntity re " +
            "WHERE d.id = re.toId AND re.toType = 'DEVICE' " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains' " +
            "AND re.fromId = :groupId AND re.fromType = 'ENTITY_GROUP' " +
            "AND d.deviceProfileId = (SELECT op.deviceProfileId FROM OtaPackageInfoEntity op WHERE op.id = :otaPackageId) " +
            "AND d.softwareId IS NULL")
    Long countByEntityGroupIdAndSoftwareIdIsNull(@Param("groupId") UUID groupId,
                                                 @Param("otaPackageId") UUID otaPackageId);

    @Query(value = COUNT_QUERY_BY_DEVICE_PROFILE_AND_FIRMWARE_IS_NULL,
            nativeQuery = true)
    Long countByDeviceProfileIdAndFirmwareIdIsNull(@Param("tenantId") UUID tenantId,
                                                   @Param("deviceProfileId") UUID deviceProfileId);

    @Query(value = COUNT_QUERY_BY_DEVICE_PROFILE_AND_SOFTWARE_IS_NULL,
            nativeQuery = true)
    Long countByDeviceProfileIdAndSoftwareIdIsNull(@Param("tenantId") UUID tenantId,
                                                   @Param("deviceProfileId") UUID deviceProfileId);

    @Query("SELECT externalId FROM DeviceEntity WHERE id = :id")
    UUID getExternalIdById(@Param("id") UUID id);

    @Query("SELECT new org.thingsboard.server.common.data.edqs.fields.DeviceFields(d.id, d.createdTime, d.tenantId, d.customerId," +
            "d.name, d.version, d.type, d.label, d.deviceProfileId, d.additionalInfo) FROM DeviceEntity d WHERE d.id > :id ORDER BY d.id")
    List<DeviceFields> findNextBatch(@Param("id") UUID id, Limit limit);

    @Query("SELECT new org.thingsboard.server.common.data.EntityInfo(d.id, 'DEVICE', d.name) " +
            "FROM DeviceEntity d WHERE d.tenantId = :tenantId AND d.deviceProfileId = :profileId")
    Page<EntityInfo> findEntityInfosByTenantIdAndProfileId(UUID tenantId, UUID profileId, Pageable pageable);

    @Query("SELECT new org.thingsboard.server.common.data.EntityInfo(d.id, 'DEVICE', d.name) " +
            "FROM DeviceEntity d WHERE d.id = :id")
    EntityInfo findEntityInfoById(UUID id);

    @Query("SELECT DISTINCT new org.thingsboard.server.common.data.DeviceCacheInfo(d.id, d.tenantId, d.customerId, d.name, d.type, d.deviceProfileId) " +
            "FROM DeviceEntity d, RelationEntity re WHERE d.id = re.toId AND re.toType = 'DEVICE' " +
            "AND re.relationType = :relationType AND d.id > :id ORDER BY d.id")
    List<DeviceCacheInfo> findDeviceCacheInfosByRelationType(@Param("relationType") String relationType,
                                                             @Param("id") UUID id,
                                                             Limit limit);

    @Query("SELECT new org.thingsboard.server.common.data.util.TbPair(dpe.transportType, count(*)) FROM DeviceEntity de INNER JOIN DeviceProfileEntity dpe on de.deviceProfileId = dpe.id GROUP BY dpe.transportType")
    List<TbPair<DeviceTransportType, Long>> countDevicesPerTransportType();

}

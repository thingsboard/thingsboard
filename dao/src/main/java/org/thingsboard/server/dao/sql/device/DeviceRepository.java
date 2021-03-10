/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.dao.model.sql.DeviceEntity;
import org.thingsboard.server.dao.model.sql.DeviceInfoEntity;

import java.util.List;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
public interface DeviceRepository extends PagingAndSortingRepository<DeviceEntity, UUID> {

    @Query("SELECT new org.thingsboard.server.dao.model.sql.DeviceInfoEntity(d, c.title, c.additionalInfo, p.name) " +
            "FROM DeviceEntity d " +
            "LEFT JOIN CustomerEntity c on c.id = d.customerId " +
            "LEFT JOIN DeviceProfileEntity p on p.id = d.deviceProfileId " +
            "WHERE d.id = :deviceId")
    DeviceInfoEntity findDeviceInfoById(@Param("deviceId") UUID deviceId);

    @Query("SELECT d FROM DeviceEntity d WHERE d.tenantId = :tenantId " +
            "AND d.customerId = :customerId " +
            "AND LOWER(d.searchText) LIKE LOWER(CONCAT(:searchText, '%'))")
    Page<DeviceEntity> findByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                                   @Param("customerId") UUID customerId,
                                                   @Param("searchText") String searchText,
                                                   Pageable pageable);

    @Query("SELECT d FROM DeviceEntity d WHERE d.tenantId = :tenantId " +
            "AND d.deviceProfileId = :profileId " +
            "AND LOWER(d.searchText) LIKE LOWER(CONCAT(:searchText, '%'))")
    Page<DeviceEntity> findByTenantIdAndProfileId(@Param("tenantId") UUID tenantId,
                                                  @Param("profileId") UUID profileId,
                                                  @Param("searchText") String searchText,
                                                  Pageable pageable);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.DeviceInfoEntity(d, c.title, c.additionalInfo, p.name) " +
            "FROM DeviceEntity d " +
            "LEFT JOIN CustomerEntity c on c.id = d.customerId " +
            "LEFT JOIN DeviceProfileEntity p on p.id = d.deviceProfileId " +
            "WHERE d.tenantId = :tenantId " +
            "AND d.customerId = :customerId " +
            "AND LOWER(d.searchText) LIKE LOWER(CONCAT(:searchText, '%'))")
    Page<DeviceInfoEntity> findDeviceInfosByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                                                  @Param("customerId") UUID customerId,
                                                                  @Param("searchText") String searchText,
                                                                  Pageable pageable);

    @Query("SELECT d FROM DeviceEntity d WHERE d.tenantId = :tenantId")
    Page<DeviceEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                      Pageable pageable);

    @Query("SELECT d FROM DeviceEntity d WHERE d.tenantId = :tenantId " +
            "AND LOWER(d.searchText) LIKE LOWER(CONCAT(:textSearch, '%'))")
    Page<DeviceEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                      @Param("textSearch") String textSearch,
                                      Pageable pageable);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.DeviceInfoEntity(d, c.title, c.additionalInfo, p.name) " +
            "FROM DeviceEntity d " +
            "LEFT JOIN CustomerEntity c on c.id = d.customerId " +
            "LEFT JOIN DeviceProfileEntity p on p.id = d.deviceProfileId " +
            "WHERE d.tenantId = :tenantId " +
            "AND LOWER(d.searchText) LIKE LOWER(CONCAT(:textSearch, '%'))")
    Page<DeviceInfoEntity> findDeviceInfosByTenantId(@Param("tenantId") UUID tenantId,
                                                     @Param("textSearch") String textSearch,
                                                     Pageable pageable);

    @Query("SELECT d FROM DeviceEntity d WHERE d.tenantId = :tenantId " +
            "AND d.type = :type " +
            "AND LOWER(d.searchText) LIKE LOWER(CONCAT(:textSearch, '%'))")
    Page<DeviceEntity> findByTenantIdAndType(@Param("tenantId") UUID tenantId,
                                             @Param("type") String type,
                                             @Param("textSearch") String textSearch,
                                             Pageable pageable);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.DeviceInfoEntity(d, c.title, c.additionalInfo, p.name) " +
            "FROM DeviceEntity d " +
            "LEFT JOIN CustomerEntity c on c.id = d.customerId " +
            "LEFT JOIN DeviceProfileEntity p on p.id = d.deviceProfileId " +
            "WHERE d.tenantId = :tenantId " +
            "AND d.type = :type " +
            "AND LOWER(d.searchText) LIKE LOWER(CONCAT(:textSearch, '%'))")
    Page<DeviceInfoEntity> findDeviceInfosByTenantIdAndType(@Param("tenantId") UUID tenantId,
                                                            @Param("type") String type,
                                                            @Param("textSearch") String textSearch,
                                                            Pageable pageable);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.DeviceInfoEntity(d, c.title, c.additionalInfo, p.name) " +
            "FROM DeviceEntity d " +
            "LEFT JOIN CustomerEntity c on c.id = d.customerId " +
            "LEFT JOIN DeviceProfileEntity p on p.id = d.deviceProfileId " +
            "WHERE d.tenantId = :tenantId " +
            "AND d.deviceProfileId = :deviceProfileId " +
            "AND LOWER(d.searchText) LIKE LOWER(CONCAT(:textSearch, '%'))")
    Page<DeviceInfoEntity> findDeviceInfosByTenantIdAndDeviceProfileId(@Param("tenantId") UUID tenantId,
                                                                       @Param("deviceProfileId") UUID deviceProfileId,
                                                                       @Param("textSearch") String textSearch,
                                                                       Pageable pageable);

    @Query("SELECT d FROM DeviceEntity d WHERE d.tenantId = :tenantId " +
            "AND d.customerId = :customerId " +
            "AND d.type = :type " +
            "AND LOWER(d.searchText) LIKE LOWER(CONCAT(:textSearch, '%'))")
    Page<DeviceEntity> findByTenantIdAndCustomerIdAndType(@Param("tenantId") UUID tenantId,
                                                          @Param("customerId") UUID customerId,
                                                          @Param("type") String type,
                                                          @Param("textSearch") String textSearch,
                                                          Pageable pageable);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.DeviceInfoEntity(d, c.title, c.additionalInfo, p.name) " +
            "FROM DeviceEntity d " +
            "LEFT JOIN CustomerEntity c on c.id = d.customerId " +
            "LEFT JOIN DeviceProfileEntity p on p.id = d.deviceProfileId " +
            "WHERE d.tenantId = :tenantId " +
            "AND d.customerId = :customerId " +
            "AND d.type = :type " +
            "AND LOWER(d.searchText) LIKE LOWER(CONCAT(:textSearch, '%'))")
    Page<DeviceInfoEntity> findDeviceInfosByTenantIdAndCustomerIdAndType(@Param("tenantId") UUID tenantId,
                                                                         @Param("customerId") UUID customerId,
                                                                         @Param("type") String type,
                                                                         @Param("textSearch") String textSearch,
                                                                         Pageable pageable);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.DeviceInfoEntity(d, c.title, c.additionalInfo, p.name) " +
            "FROM DeviceEntity d " +
            "LEFT JOIN CustomerEntity c on c.id = d.customerId " +
            "LEFT JOIN DeviceProfileEntity p on p.id = d.deviceProfileId " +
            "WHERE d.tenantId = :tenantId " +
            "AND d.customerId = :customerId " +
            "AND d.deviceProfileId = :deviceProfileId " +
            "AND LOWER(d.searchText) LIKE LOWER(CONCAT(:textSearch, '%'))")
    Page<DeviceInfoEntity> findDeviceInfosByTenantIdAndCustomerIdAndDeviceProfileId(@Param("tenantId") UUID tenantId,
                                                                                    @Param("customerId") UUID customerId,
                                                                                    @Param("deviceProfileId") UUID deviceProfileId,
                                                                                    @Param("textSearch") String textSearch,
                                                                                    Pageable pageable);

    @Query("SELECT DISTINCT d.type FROM DeviceEntity d WHERE d.tenantId = :tenantId")
    List<String> findTenantDeviceTypes(@Param("tenantId") UUID tenantId);

    DeviceEntity findByTenantIdAndName(UUID tenantId, String name);

    List<DeviceEntity> findDevicesByTenantIdAndCustomerIdAndIdIn(UUID tenantId, UUID customerId, List<UUID> deviceIds);

    List<DeviceEntity> findDevicesByTenantIdAndIdIn(UUID tenantId, List<UUID> deviceIds);

    DeviceEntity findByTenantIdAndId(UUID tenantId, UUID id);

    Long countByDeviceProfileId(UUID deviceProfileId);

    Long countByTenantId(UUID tenantId);

    @Query("SELECT d FROM DeviceEntity d " +
            "INNER JOIN DeviceProfileEntity p ON d.deviceProfileId = p.id " +
            "WHERE p.transportType = :transportType")
    List<DeviceEntity> findByDeviceProfileTransportType(@Param("transportType") DeviceTransportType transportType);
}

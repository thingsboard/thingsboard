// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.device;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.DeviceInfoEntity;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.SUB_CUSTOMERS_QUERY;

public interface DeviceInfoRepository extends JpaRepository<DeviceInfoEntity, UUID> {

    // Shared derived table so the count and select queries filter over the IDENTICAL owner_name column. owner_name is
    // c.title only when the owner is a DIFFERENT customer (c.id != :customerId) and NULL for the requesting customer's
    // own devices; a count built straight off device_info_view.owner_name (which lacks that exclusion) would match
    // rows on a self-title text search that the select excludes, making count > returned rows.
    String OWNER_FROM = "FROM (select d.id, d.created_time, d.additional_info, d.customer_id, d.device_profile_id, " +
            "d.device_data, d.type, d.name, d.label, d.tenant_id, d.firmware_id, d.software_id, d.external_id, d.version, d.groups, " +
            "c.title as owner_name, d.active as active from device_info_view d " +
            "LEFT JOIN customer c on c.id = d.customer_id AND c.id != :customerId) e ";

    String OWNER_SELECT = "SELECT e.*, e.owner_name as ownername, e.created_time as createdtime " + OWNER_FROM + "WHERE";

    String OWNER_COUNT = "SELECT count(e.id) " + OWNER_FROM + "WHERE";

    // The pre-resolved sub-customer ids are bound as a single typed uuid[] parameter and unnested,
    // so the statement stays at one JDBC placeholder regardless of subtree size (a plain IN (:customerIds)
    // collection binding expands to one placeholder per id and hits the pgjdbc 32767 bind-parameter cap).
    String IN_CUSTOMER_IDS = " e.tenant_id = :tenantId AND e.customer_id IN (SELECT unnest(:customerIds)) ";

    @Query("SELECT d FROM DeviceInfoEntity d " +
            "WHERE d.tenantId = :tenantId " +
            "AND (" +
            "((:customerId IS NULL AND (:includeCustomers) = TRUE)) " +
            "OR ((:customerId IS NULL AND (:includeCustomers) = FALSE) AND (d.customerId IS NULL OR d.customerId = org.thingsboard.server.common.data.id.EntityId.NULL_UUID)) " +
            "OR (:customerId IS NOT NULL AND d.customerId = :customerId) " +
            ") " +
            "AND (:deviceProfileId IS NULL OR d.deviceProfileId = :deviceProfileId) " +
            "AND ((:filterByActive) = FALSE OR d.active = :deviceActive) " +
            "AND (:textSearch IS NULL OR ilike(d.name, CONCAT('%', :textSearch, '%'))  = true " +
            "OR ilike(d.label, CONCAT('%', :textSearch, '%')) = true " +
            "OR ilike(d.type, CONCAT('%', :textSearch, '%')) = true " +
            "OR ilike(d.ownerName, CONCAT('%', :textSearch, '%')) = true )")
    Page<DeviceInfoEntity> findDeviceInfosByFilter(@Param("tenantId") UUID tenantId,
                                                   @Param("includeCustomers") boolean includeCustomers,
                                                   @Param("customerId") UUID customerId,
                                                   @Param("deviceProfileId") UUID deviceProfileId,
                                                   @Param("filterByActive") boolean filterByActive,
                                                   @Param("deviceActive") boolean active,
                                                   @Param("textSearch") String textSearch,
                                                   Pageable pageable);

    @Query(value = OWNER_SELECT + SUB_CUSTOMERS_QUERY +
            "AND (:deviceProfileId IS NULL OR e.device_profile_id = :deviceProfileId) " +
            "AND ((:filterByActive) IS FALSE OR e.active = :deviceActive) " +
            "AND (:textSearch IS NULL OR e.name ILIKE CONCAT('%', :textSearch, '%') " +
            "OR e.label ILIKE CONCAT('%', :textSearch, '%') " +
            "OR e.type ILIKE CONCAT('%', :textSearch, '%') " +
            "OR e.owner_name ILIKE CONCAT('%', :textSearch, '%'))",
            countQuery = OWNER_COUNT + SUB_CUSTOMERS_QUERY +
                    "AND (:deviceProfileId IS NULL OR e.device_profile_id = :deviceProfileId) " +
                    "AND ((:filterByActive) IS FALSE OR e.active = :deviceActive) " +
                    "AND (:textSearch IS NULL OR e.name ILIKE CONCAT('%', :textSearch, '%') " +
                    "OR e.label ILIKE CONCAT('%', :textSearch, '%') " +
                    "OR e.type ILIKE CONCAT('%', :textSearch, '%') " +
                    "OR e.owner_name ILIKE CONCAT('%', :textSearch, '%'))",
            nativeQuery = true)
    Page<DeviceInfoEntity> findDeviceInfosByFilterIncludingSubCustomers(@Param("tenantId") UUID tenantId,
                                                        @Param("customerId") UUID customerId,
                                                        @Param("deviceProfileId") UUID deviceProfileId,
                                                        @Param("filterByActive") boolean filterByActive,
                                                        @Param("deviceActive") boolean active,
                                                        @Param("textSearch") String textSearch, Pageable pageable);

    @Query(value = OWNER_SELECT + IN_CUSTOMER_IDS +
            "AND (:deviceProfileId IS NULL OR e.device_profile_id = :deviceProfileId) " +
            "AND ((:filterByActive) IS FALSE OR e.active = :deviceActive) " +
            "AND (:textSearch IS NULL OR e.name ILIKE CONCAT('%', :textSearch, '%') " +
            "OR e.label ILIKE CONCAT('%', :textSearch, '%') " +
            "OR e.type ILIKE CONCAT('%', :textSearch, '%') " +
            "OR e.owner_name ILIKE CONCAT('%', :textSearch, '%'))",
            countQuery = OWNER_COUNT + IN_CUSTOMER_IDS +
                    "AND (:deviceProfileId IS NULL OR e.device_profile_id = :deviceProfileId) " +
                    "AND ((:filterByActive) IS FALSE OR e.active = :deviceActive) " +
                    "AND (:textSearch IS NULL OR e.name ILIKE CONCAT('%', :textSearch, '%') " +
                    "OR e.label ILIKE CONCAT('%', :textSearch, '%') " +
                    "OR e.type ILIKE CONCAT('%', :textSearch, '%') " +
                    "OR e.owner_name ILIKE CONCAT('%', :textSearch, '%'))",
            nativeQuery = true)
    Page<DeviceInfoEntity> findDeviceInfosByFilterInCustomerIds(@Param("tenantId") UUID tenantId,
                                                        @Param("customerId") UUID customerId,
                                                        @Param("customerIds") UUID[] customerIds,
                                                        @Param("deviceProfileId") UUID deviceProfileId,
                                                        @Param("filterByActive") boolean filterByActive,
                                                        @Param("deviceActive") boolean active,
                                                        @Param("textSearch") String textSearch, Pageable pageable);
}

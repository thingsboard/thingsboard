// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.entityview;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.EntityViewInfoEntity;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.SUB_CUSTOMERS_QUERY;

public interface EntityViewInfoRepository extends JpaRepository<EntityViewInfoEntity, UUID> {

    // The count's c.title filter must stay filter-equivalent to the select's owner_name: it is equivalent today
    // only because the count's customer join carries the identical c.id != :customerId exclusion as the select's
    // derived table. The count deliberately counts from the lean base table (entity_view + one customer join) instead
    // of the entity_view_info_view the select uses, because counting over the base table is cheaper; that is why the
    // shared-FROM shape used by DeviceInfoRepository.OWNER_FROM was not adopted here. See DeviceInfoRepository.OWNER_FROM
    // for the rationale (and for the shared-FROM shape to adopt if these two ever drift).
    String OWNER_SELECT = "SELECT e.*, e.owner_name as ownername, e.created_time as createdtime " +
            "FROM (select ev.id, ev.created_time, ev.entity_id, ev.entity_type, ev.tenant_id, ev.customer_id, " +
            "ev.type, ev.name, ev.keys, ev.start_ts, ev.end_ts, ev.additional_info, ev.external_id, ev.version, ev.groups, " +
            "c.title as owner_name from entity_view_info_view ev " +
            "LEFT JOIN customer c on c.id = ev.customer_id AND c.id != :customerId) e " +
            "WHERE";

    String OWNER_COUNT = "SELECT count(e.id) FROM entity_view e " +
            "LEFT JOIN customer c on c.id = e.customer_id AND c.id != :customerId " +
            "WHERE";

    // The pre-resolved sub-customer ids are bound as a single typed uuid[] parameter and unnested,
    // so the statement stays at one JDBC placeholder regardless of subtree size (a plain IN (:customerIds)
    // collection binding expands to one placeholder per id and hits the pgjdbc 32767 bind-parameter cap).
    String IN_CUSTOMER_IDS = " e.tenant_id = :tenantId AND e.customer_id IN (SELECT unnest(:customerIds)) ";

    @Query("SELECT evi FROM EntityViewInfoEntity evi " +
            "WHERE evi.tenantId = :tenantId " +
            "AND (:searchText IS NULL OR ilike(evi.name, CONCAT('%', :searchText, '%')) = true " +
            "OR ilike(evi.ownerName, CONCAT('%', :searchText, '%')) = true)")
    Page<EntityViewInfoEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                              @Param("searchText") String searchText,
                                              Pageable pageable);

    @Query("SELECT evi FROM EntityViewInfoEntity evi " +
            "WHERE evi.tenantId = :tenantId " +
            "AND evi.type = :type " +
            "AND (:searchText IS NULL OR ilike(evi.name, CONCAT('%', :searchText, '%')) = true " +
            "OR ilike(evi.ownerName, CONCAT('%', :searchText, '%')) = true)")
    Page<EntityViewInfoEntity> findByTenantIdAndType(@Param("tenantId") UUID tenantId,
                                                     @Param("type") String type,
                                                     @Param("searchText") String searchText,
                                                     Pageable pageable);

    @Query("SELECT evi FROM EntityViewInfoEntity evi " +
            "WHERE evi.tenantId = :tenantId AND (evi.customerId IS NULL OR evi.customerId = org.thingsboard.server.common.data.id.EntityId.NULL_UUID) " +
            "AND (:searchText IS NULL OR ilike(evi.name, CONCAT('%', :searchText, '%')) = true)")
    Page<EntityViewInfoEntity> findTenantEntityViewsByTenantId(@Param("tenantId") UUID tenantId,
                                                               @Param("searchText") String searchText,
                                                               Pageable pageable);

    @Query("SELECT evi FROM EntityViewInfoEntity evi " +
            "WHERE evi.tenantId = :tenantId AND (evi.customerId IS NULL OR evi.customerId = org.thingsboard.server.common.data.id.EntityId.NULL_UUID) " +
            "AND evi.type = :type " +
            "AND (:searchText IS NULL OR ilike(evi.name, CONCAT('%', :searchText, '%')) = true)")
    Page<EntityViewInfoEntity> findTenantEntityViewsByTenantIdAndType(@Param("tenantId") UUID tenantId,
                                                                      @Param("type") String type,
                                                                      @Param("searchText") String searchText,
                                                                      Pageable pageable);

    @Query("SELECT evi FROM EntityViewInfoEntity evi WHERE evi.tenantId = :tenantId AND evi.customerId = :customerId " +
            "AND (:searchText IS NULL OR ilike(evi.name, CONCAT('%', :searchText, '%')) = true)")
    Page<EntityViewInfoEntity> findByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                                           @Param("customerId") UUID customerId,
                                                           @Param("searchText") String searchText,
                                                           Pageable pageable);

    @Query("SELECT evi FROM EntityViewInfoEntity evi WHERE evi.tenantId = :tenantId AND evi.customerId = :customerId " +
            "AND evi.type = :type " +
            "AND (:searchText IS NULL OR ilike(evi.name, CONCAT('%', :searchText, '%')) = true)")
    Page<EntityViewInfoEntity> findByTenantIdAndCustomerIdAndType(@Param("tenantId") UUID tenantId,
                                                                  @Param("customerId") UUID customerId,
                                                                  @Param("type") String type,
                                                                  @Param("searchText") String searchText,
                                                                  Pageable pageable);

    @Query(value = OWNER_SELECT + SUB_CUSTOMERS_QUERY +
            "AND (:searchText IS NULL OR e.name ILIKE CONCAT('%', :searchText, '%') " +
            "OR e.owner_name ILIKE CONCAT('%', :searchText, '%'))",
            countQuery = OWNER_COUNT + SUB_CUSTOMERS_QUERY +
                    "AND (:searchText IS NULL OR e.name ILIKE CONCAT('%', :searchText, '%') " +
                    "OR c.title ILIKE CONCAT('%', :searchText, '%'))",
            nativeQuery = true)
    Page<EntityViewInfoEntity> findByTenantIdAndCustomerIdIncludingSubCustomers(@Param("tenantId") UUID tenantId,
                                                                                @Param("customerId") UUID customerId,
                                                                                @Param("searchText") String searchText,
                                                                                Pageable pageable);

    @Query(value = OWNER_SELECT + SUB_CUSTOMERS_QUERY +
            "AND e.type = :type " +
            "AND (:searchText IS NULL OR e.name ILIKE CONCAT('%', :searchText, '%') " +
            "OR e.owner_name ILIKE CONCAT('%', :searchText, '%'))",
            countQuery = OWNER_COUNT + SUB_CUSTOMERS_QUERY +
                    "AND e.type = :type " +
                    "AND (:searchText IS NULL OR e.name ILIKE CONCAT('%', :searchText, '%') " +
                    "OR c.title ILIKE CONCAT('%', :searchText, '%'))",
            nativeQuery = true)
    Page<EntityViewInfoEntity> findByTenantIdAndCustomerIdAndTypeIncludingSubCustomers(@Param("tenantId") UUID tenantId,
                                                                                       @Param("customerId") UUID customerId,
                                                                                       @Param("type") String type,
                                                                                       @Param("searchText") String searchText,
                                                                                       Pageable pageable);

    @Query(value = OWNER_SELECT + IN_CUSTOMER_IDS +
            "AND (:searchText IS NULL OR e.name ILIKE CONCAT('%', :searchText, '%') " +
            "OR e.owner_name ILIKE CONCAT('%', :searchText, '%'))",
            countQuery = OWNER_COUNT + IN_CUSTOMER_IDS +
                    "AND (:searchText IS NULL OR e.name ILIKE CONCAT('%', :searchText, '%') " +
                    "OR c.title ILIKE CONCAT('%', :searchText, '%'))",
            nativeQuery = true)
    Page<EntityViewInfoEntity> findByTenantIdAndCustomerIdInCustomerIds(@Param("tenantId") UUID tenantId,
                                                                        @Param("customerId") UUID customerId,
                                                                        @Param("customerIds") UUID[] customerIds,
                                                                        @Param("searchText") String searchText,
                                                                        Pageable pageable);

    @Query(value = OWNER_SELECT + IN_CUSTOMER_IDS +
            "AND e.type = :type " +
            "AND (:searchText IS NULL OR e.name ILIKE CONCAT('%', :searchText, '%') " +
            "OR e.owner_name ILIKE CONCAT('%', :searchText, '%'))",
            countQuery = OWNER_COUNT + IN_CUSTOMER_IDS +
                    "AND e.type = :type " +
                    "AND (:searchText IS NULL OR e.name ILIKE CONCAT('%', :searchText, '%') " +
                    "OR c.title ILIKE CONCAT('%', :searchText, '%'))",
            nativeQuery = true)
    Page<EntityViewInfoEntity> findByTenantIdAndCustomerIdAndTypeInCustomerIds(@Param("tenantId") UUID tenantId,
                                                                               @Param("customerId") UUID customerId,
                                                                               @Param("customerIds") UUID[] customerIds,
                                                                               @Param("type") String type,
                                                                               @Param("searchText") String searchText,
                                                                               Pageable pageable);
}

// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.asset;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.AssetInfoEntity;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.SUB_CUSTOMERS_QUERY;

public interface AssetInfoRepository extends JpaRepository<AssetInfoEntity, UUID> {

    // The count's c.title filter must stay filter-equivalent to the select's owner_name: it is equivalent today
    // only because the count's customer join carries the identical c.id != :customerId exclusion as the select's
    // derived table. The count deliberately counts from the lean base table (asset + one customer join) instead of
    // the asset_info_view the select uses, because counting over the base table is cheaper; that is why the
    // shared-FROM shape used by DeviceInfoRepository.OWNER_FROM was not adopted here. See DeviceInfoRepository.OWNER_FROM
    // for the rationale (and for the shared-FROM shape to adopt if these two ever drift).
    String OWNER_SELECT = "SELECT e.*, e.owner_name as ownername, e.created_time as createdtime " +
            "FROM (select a.id, a.created_time, a.additional_info, a.customer_id, a.\"name\", a.label, " +
            "a.tenant_id, a.type, a.external_id, a.version, a.asset_profile_id, a.groups, " +
            "c.title as owner_name from asset_info_view a " +
            "LEFT JOIN customer c on c.id = a.customer_id AND c.id != :customerId) e " +
            "WHERE";

    String OWNER_COUNT = "SELECT count(e.id) FROM asset e " +
            "LEFT JOIN customer c on c.id = e.customer_id AND c.id != :customerId " +
            "WHERE";

    // The pre-resolved sub-customer ids are bound as a single typed uuid[] parameter and unnested,
    // so the statement stays at one JDBC placeholder regardless of subtree size (a plain IN (:customerIds)
    // collection binding expands to one placeholder per id and hits the pgjdbc 32767 bind-parameter cap).
    String IN_CUSTOMER_IDS = " e.tenant_id = :tenantId AND e.customer_id IN (SELECT unnest(:customerIds)) ";

    @Query("SELECT ai FROM AssetInfoEntity ai " +
            "WHERE ai.tenantId = :tenantId " +
            "AND (:searchText IS NULL OR ilike(ai.name, CONCAT('%', :searchText, '%')) = true " +
            "  OR ilike(ai.ownerName, CONCAT('%', :searchText, '%')) = true " +
            "  OR ilike(ai.label, CONCAT('%', :searchText, '%')) = true " +
            "  OR ilike(ai.type, CONCAT('%', :searchText, '%')) = true) ")
    Page<AssetInfoEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                         @Param("searchText") String searchText,
                                         Pageable pageable);

    @Query("SELECT ai FROM AssetInfoEntity ai " +
            "WHERE ai.tenantId = :tenantId " +
            "AND ai.assetProfileId = :assetProfileId " +
            "AND (:searchText IS NULL OR ilike(ai.name, CONCAT('%', :searchText, '%')) = true " +
            "  OR ilike(ai.ownerName, CONCAT('%', :searchText, '%')) = true " +
            "  OR ilike(ai.label, CONCAT('%', :searchText, '%')) = true " +
            "  OR ilike(ai.type, CONCAT('%', :searchText, '%')) = true) ")
    Page<AssetInfoEntity> findByTenantIdAndAssetProfileId(@Param("tenantId") UUID tenantId,
                                                          @Param("assetProfileId") UUID assetProfileId,
                                                          @Param("searchText") String searchText,
                                                          Pageable pageable);

    @Query("SELECT ai FROM AssetInfoEntity ai " +
            "WHERE ai.tenantId = :tenantId AND (ai.customerId IS NULL OR ai.customerId = org.thingsboard.server.common.data.id.EntityId.NULL_UUID) " +
            "AND (:searchText IS NULL OR ilike(ai.name, CONCAT('%', :searchText, '%')) = true " +
            "  OR ilike(ai.label, CONCAT('%', :searchText, '%')) = true " +
            "  OR ilike(ai.type, CONCAT('%', :searchText, '%')) = true) ")
    Page<AssetInfoEntity> findTenantAssetsByTenantId(@Param("tenantId") UUID tenantId,
                                                     @Param("searchText") String searchText,
                                                     Pageable pageable);

    @Query("SELECT ai FROM AssetInfoEntity ai " +
            "WHERE ai.tenantId = :tenantId AND (ai.customerId IS NULL OR ai.customerId = org.thingsboard.server.common.data.id.EntityId.NULL_UUID) " +
            "AND ai.assetProfileId = :assetProfileId " +
            "AND (:searchText IS NULL OR ilike(ai.name, CONCAT('%', :searchText, '%')) = true " +
            "  OR ilike(ai.label, CONCAT('%', :searchText, '%')) = true " +
            "  OR ilike(ai.type, CONCAT('%', :searchText, '%')) = true) ")
    Page<AssetInfoEntity> findTenantAssetsByTenantIdAndAssetProfileId(@Param("tenantId") UUID tenantId,
                                                                      @Param("assetProfileId") UUID assetProfileId,
                                                                      @Param("searchText") String searchText,
                                                                      Pageable pageable);

    @Query("SELECT ai FROM AssetInfoEntity ai WHERE ai.tenantId = :tenantId AND ai.customerId = :customerId " +
            "AND (:searchText IS NULL OR ilike(ai.name, CONCAT('%', :searchText, '%')) = true " +
            "  OR ilike(ai.label, CONCAT('%', :searchText, '%')) = true " +
            "  OR ilike(ai.type, CONCAT('%', :searchText, '%')) = true) ")
    Page<AssetInfoEntity> findByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                                      @Param("customerId") UUID customerId,
                                                      @Param("searchText") String searchText,
                                                      Pageable pageable);

    @Query("SELECT ai FROM AssetInfoEntity ai WHERE ai.tenantId = :tenantId AND ai.customerId = :customerId " +
            "AND ai.assetProfileId = :assetProfileId " +
            "AND (:searchText IS NULL OR ilike(ai.name, CONCAT('%', :searchText, '%')) = true " +
            "  OR ilike(ai.label, CONCAT('%', :searchText, '%')) = true " +
            "  OR ilike(ai.type, CONCAT('%', :searchText, '%')) = true) ")
    Page<AssetInfoEntity> findByTenantIdAndCustomerIdAndAssetProfileId(@Param("tenantId") UUID tenantId,
                                                                       @Param("customerId") UUID customerId,
                                                                       @Param("assetProfileId") UUID assetProfileId,
                                                                       @Param("searchText") String searchText,
                                                                       Pageable pageable);

    @Query(value = OWNER_SELECT + SUB_CUSTOMERS_QUERY +
            "AND (:searchText IS NULL OR e.name ILIKE CONCAT('%', :searchText, '%') " +
            "  OR e.label ILIKE CONCAT('%', :searchText, '%') " +
            "  OR e.type ILIKE CONCAT('%', :searchText, '%') " +
            "  OR e.owner_name ILIKE CONCAT('%', :searchText, '%'))",
            countQuery = OWNER_COUNT + SUB_CUSTOMERS_QUERY +
                    "AND (:searchText IS NULL OR e.name ILIKE CONCAT('%', :searchText, '%') " +
                    "  OR e.label ILIKE CONCAT('%', :searchText, '%') " +
                    "  OR e.type ILIKE CONCAT('%', :searchText, '%') " +
                    "  OR c.title ILIKE CONCAT('%', :searchText, '%'))",
            nativeQuery = true)
    Page<AssetInfoEntity> findByTenantIdAndCustomerIdIncludingSubCustomers(@Param("tenantId") UUID tenantId,
                                                                           @Param("customerId") UUID customerId,
                                                                           @Param("searchText") String searchText,
                                                                           Pageable pageable);

    @Query(value = OWNER_SELECT + SUB_CUSTOMERS_QUERY +
            "AND e.asset_profile_id = :assetProfileId " +
            "AND (:searchText IS NULL OR e.name ILIKE CONCAT('%', :searchText, '%') " +
            "  OR e.label ILIKE CONCAT('%', :searchText, '%') " +
            "  OR e.type ILIKE CONCAT('%', :searchText, '%') " +
            "  OR e.owner_name ILIKE CONCAT('%', :searchText, '%'))",
            countQuery = OWNER_COUNT + SUB_CUSTOMERS_QUERY +
                    "AND e.asset_profile_id = :assetProfileId " +
                    "AND (:searchText IS NULL OR e.name ILIKE CONCAT('%', :searchText, '%') " +
                    "  OR e.label ILIKE CONCAT('%', :searchText, '%') " +
                    "  OR e.type ILIKE CONCAT('%', :searchText, '%') " +
                    "  OR c.title ILIKE CONCAT('%', :searchText, '%'))",
            nativeQuery = true)
    Page<AssetInfoEntity> findByTenantIdAndCustomerIdAndAssetProfileIdIncludingSubCustomers(@Param("tenantId") UUID tenantId,
                                                                                            @Param("customerId") UUID customerId,
                                                                                            @Param("assetProfileId") UUID assetProfileId,
                                                                                            @Param("searchText") String searchText,
                                                                                            Pageable pageable);

    @Query(value = OWNER_SELECT + IN_CUSTOMER_IDS +
            "AND (:searchText IS NULL OR e.name ILIKE CONCAT('%', :searchText, '%') " +
            "  OR e.label ILIKE CONCAT('%', :searchText, '%') " +
            "  OR e.type ILIKE CONCAT('%', :searchText, '%') " +
            "  OR e.owner_name ILIKE CONCAT('%', :searchText, '%'))",
            countQuery = OWNER_COUNT + IN_CUSTOMER_IDS +
                    "AND (:searchText IS NULL OR e.name ILIKE CONCAT('%', :searchText, '%') " +
                    "  OR e.label ILIKE CONCAT('%', :searchText, '%') " +
                    "  OR e.type ILIKE CONCAT('%', :searchText, '%') " +
                    "  OR c.title ILIKE CONCAT('%', :searchText, '%'))",
            nativeQuery = true)
    Page<AssetInfoEntity> findByTenantIdAndCustomerIdInCustomerIds(@Param("tenantId") UUID tenantId,
                                                                   @Param("customerId") UUID customerId,
                                                                   @Param("customerIds") UUID[] customerIds,
                                                                   @Param("searchText") String searchText,
                                                                   Pageable pageable);

    @Query(value = OWNER_SELECT + IN_CUSTOMER_IDS +
            "AND e.asset_profile_id = :assetProfileId " +
            "AND (:searchText IS NULL OR e.name ILIKE CONCAT('%', :searchText, '%') " +
            "  OR e.label ILIKE CONCAT('%', :searchText, '%') " +
            "  OR e.type ILIKE CONCAT('%', :searchText, '%') " +
            "  OR e.owner_name ILIKE CONCAT('%', :searchText, '%'))",
            countQuery = OWNER_COUNT + IN_CUSTOMER_IDS +
                    "AND e.asset_profile_id = :assetProfileId " +
                    "AND (:searchText IS NULL OR e.name ILIKE CONCAT('%', :searchText, '%') " +
                    "  OR e.label ILIKE CONCAT('%', :searchText, '%') " +
                    "  OR e.type ILIKE CONCAT('%', :searchText, '%') " +
                    "  OR c.title ILIKE CONCAT('%', :searchText, '%'))",
            nativeQuery = true)
    Page<AssetInfoEntity> findByTenantIdAndCustomerIdAndAssetProfileIdInCustomerIds(@Param("tenantId") UUID tenantId,
                                                                                    @Param("customerId") UUID customerId,
                                                                                    @Param("customerIds") UUID[] customerIds,
                                                                                    @Param("assetProfileId") UUID assetProfileId,
                                                                                    @Param("searchText") String searchText,
                                                                                    Pageable pageable);
}

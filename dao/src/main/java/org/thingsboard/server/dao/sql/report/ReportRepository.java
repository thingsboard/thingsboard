// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.report;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.edqs.fields.ReportFields;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.dao.model.sql.ReportDataEntity;
import org.thingsboard.server.dao.model.sql.ReportEntity;

import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.SUB_CUSTOMERS_QUERY;

@Repository
public interface ReportRepository extends JpaRepository<ReportEntity, UUID> {

    @Query("SELECT r FROM ReportEntity r WHERE r.tenantId = :tenantId " +
           "AND (:searchText IS NULL OR ilike(r.name, CONCAT('%', :searchText, '%')) = true)")
    Page<ReportEntity> findByTenantIdAndSearchText(@Param("tenantId") UUID tenantId,
                                                   @Param("searchText") String searchText,
                                                   Pageable pageable);

    @Query("SELECT r FROM ReportEntity r WHERE r.tenantId = :tenantId " +
           "AND (r.customerId IS NULL OR r.customerId = org.thingsboard.server.common.data.id.EntityId.NULL_UUID) " +
           "AND (:searchText IS NULL OR ilike(r.name, CONCAT('%', :searchText, '%')) = true)")
    Page<ReportEntity> findTenantReports(@Param("tenantId") UUID tenantId,
                                         @Param("searchText") String searchText,
                                         Pageable pageable);

    @Query("SELECT r FROM ReportEntity r WHERE r.tenantId = :tenantId AND r.customerId = :customerId " +
           "AND (:searchText IS NULL OR ilike(r.name, CONCAT('%', :searchText, '%')) = true)")
    Page<ReportEntity> findByTenantIdAndCustomerIdAndSearchText(@Param("tenantId") UUID tenantId,
                                                                @Param("customerId") UUID customerId,
                                                                @Param("searchText") String searchText,
                                                                Pageable pageable);

    @Query(value = "SELECT e.* FROM report e " +
                   "WHERE" + SUB_CUSTOMERS_QUERY +
                   "AND (:searchText IS NULL OR e.name ILIKE CONCAT('%', :searchText, '%')) ",
            countQuery = "SELECT count(e.id) FROM report e " +
                         "WHERE" + SUB_CUSTOMERS_QUERY +
                         "AND (:searchText IS NULL OR e.name ILIKE CONCAT('%', :searchText, '%')) ",
            nativeQuery = true)
    Page<ReportEntity> findCustomerReportsIncludingSubCustomers(@Param("tenantId") UUID tenantId,
                                                                @Param("customerId") UUID customerId,
                                                                @Param("searchText") String searchText,
                                                                Pageable pageable);

    @Modifying
    @Query(value = "UPDATE report SET data = :data WHERE id = :id", nativeQuery = true)
    void saveData(UUID id, byte[] data);

    @Query(value = "SELECT data, name, format FROM report WHERE id = :id", nativeQuery = true)
    ReportDataEntity getReportDataById(@Param("id") UUID id);

    @Query(value = "SELECT data, name, format FROM report WHERE public_key = :publicKey AND is_public = true", nativeQuery = true)
    ReportDataEntity getReportDataByPublicKey(@Param("publicKey") String publicKey);

    @Transactional
    @Modifying
    @Query("DELETE FROM ReportEntity r WHERE r.tenantId = :tenantId")
    void deleteByTenantId(@Param("tenantId") UUID tenantId);

    @Transactional
    @Modifying
    @Query("DELETE FROM ReportEntity r WHERE r.tenantId = :tenantId AND r.customerId = :customerId")
    void deleteByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId, @Param("customerId") UUID customerId);

    @Query("SELECT new org.thingsboard.server.common.data.util.TbPair(r.format, COUNT(*)) FROM ReportEntity r GROUP BY r.format")
    List<TbPair<TbReportFormat, Long>> countReportsByFormatType();

    @Query("SELECT new org.thingsboard.server.common.data.edqs.fields.ReportFields(r.id, r.createdTime, r.tenantId, " +
           "r.customerId, r.name, r.format) FROM ReportEntity r WHERE r.id > :id ORDER BY r.id")
    List<ReportFields> findNextBatch(@Param("id") UUID id, Limit limit);

}

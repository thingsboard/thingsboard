// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.report;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.report.ReportTemplateType;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.dao.model.sql.ReportTemplateInfoEntity;

import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.SUB_CUSTOMERS_QUERY;

public interface ReportTemplateInfoRepository extends JpaRepository<ReportTemplateInfoEntity, UUID> {

    @Query("SELECT ri FROM ReportTemplateInfoEntity ri " +
            "WHERE ri.tenantId = :tenantId AND (ri.customerId IS NULL OR ri.customerId = org.thingsboard.server.common.data.id.EntityId.NULL_UUID) " +
            "AND (:searchText IS NULL OR ilike(ri.name, CONCAT('%', :searchText, '%')) = true) " +
            "AND ((:#{#reportTemplateTypes == null} = true) OR ri.type IN (:reportTemplateTypes)) " +
            "AND ((:#{#reportTemplateFormats == null} = true) OR ri.format IN (:reportTemplateFormats))")
    Page<ReportTemplateInfoEntity> findTenantReportTemplates(@Param("tenantId") UUID tenantId,
                                                             @Param("searchText") String searchText,
                                                             @Param("reportTemplateTypes") List<ReportTemplateType> reportTemplateTypes,
                                                             @Param("reportTemplateFormats") List<TbReportFormat> reportTemplateFormats,
                                                             Pageable pageable);

    @Query("SELECT ri FROM ReportTemplateInfoEntity ri " +
            "WHERE ri.tenantId = :tenantId " +
            "AND (:searchText IS NULL OR ilike(ri.name, CONCAT('%', :searchText, '%')) = true " +
            "OR ilike(ri.ownerName, CONCAT('%', :searchText, '%')) = true) " +
            "AND ((:#{#reportTemplateTypes == null} = true) OR ri.type IN (:reportTemplateTypes)) " +
            "AND ((:#{#reportTemplateFormats == null} = true) OR ri.format IN (:reportTemplateFormats))")
    Page<ReportTemplateInfoEntity> findTenantReportTemplatesIncludingCustomers(@Param("tenantId") UUID tenantId,
                                                                               @Param("searchText") String searchText,
                                                                               @Param("reportTemplateTypes") List<ReportTemplateType> reportTemplateTypes,
                                                                               @Param("reportTemplateFormats") List<TbReportFormat> reportTemplateFormats,
                                                                               Pageable pageable);



    @Query("SELECT ri FROM ReportTemplateInfoEntity ri " +
            "WHERE ri.tenantId = :tenantId AND ri.customerId = :customerId " +
            "AND (:searchText IS NULL OR ilike(ri.name, CONCAT('%', :searchText, '%')) = true) " +
            "AND ((:#{#reportTemplateTypes == null} = true) OR ri.type IN (:reportTemplateTypes)) " +
            "AND ((:#{#reportTemplateFormats == null} = true) OR ri.format IN (:reportTemplateFormats))")
    Page<ReportTemplateInfoEntity> findCustomerReportTemplates(@Param("tenantId") UUID tenantId,
                                                               @Param("customerId") UUID customerId,
                                                               @Param("searchText") String searchText,
                                                               @Param("reportTemplateTypes") List<ReportTemplateType> reportTemplateTypes,
                                                               @Param("reportTemplateFormats") List<TbReportFormat> reportTemplateFormats,
                                                               Pageable pageable);

    @Query(value = "SELECT e.*, e.owner_name as ownername, e.created_time as createdtime " +
            "FROM (select r.id, r.created_time, r.customer_id, r.\"name\", r.format, r.type, r.description, " +
            "r.tenant_id, r.external_id, r.version, " +
            "c.title as owner_name from report_template_info_view r " +
            "LEFT JOIN customer c on c.id = r.customer_id AND c.id != :customerId) e " +
            "WHERE" + SUB_CUSTOMERS_QUERY +
            "AND (:searchText IS NULL OR e.name ILIKE CONCAT('%', :searchText, '%') " +
            "  OR e.owner_name ILIKE CONCAT('%', :searchText, '%')) " +
            "AND (COALESCE(:reportTemplateTypes) IS NULL OR e.type IN (:reportTemplateTypes)) " +
            "AND (COALESCE(:reportTemplateFormats) IS NULL OR e.format IN (:reportTemplateFormats))",
            countQuery = "SELECT count(e.id) FROM report_template e " +
                    "LEFT JOIN customer c on c.id = e.customer_id AND c.id != :customerId " +
                    "WHERE" + SUB_CUSTOMERS_QUERY +
                    "AND (:searchText IS NULL OR e.name ILIKE CONCAT('%', :searchText, '%') " +
                    "  OR c.title ILIKE CONCAT('%', :searchText, '%')) " +
                    "AND (COALESCE(:reportTemplateTypes) IS NULL OR e.type IN (:reportTemplateTypes)) " +
                    "AND (COALESCE(:reportTemplateFormats) IS NULL OR e.format IN (:reportTemplateFormats))",
            nativeQuery = true)
    Page<ReportTemplateInfoEntity> findCustomerReportTemplatesIncludingSubCustomers(@Param("tenantId") UUID tenantId,
                                                                                    @Param("customerId") UUID customerId,
                                                                                    @Param("searchText") String searchText,
                                                                                    @Param("reportTemplateTypes") List<String> reportTemplateTypes,
                                                                                    @Param("reportTemplateFormats") List<String> reportTemplateFormats,
                                                                                    Pageable pageable);

    List<ReportTemplateInfoEntity> findByIdIn(List<UUID> reportTemplateIds);
}

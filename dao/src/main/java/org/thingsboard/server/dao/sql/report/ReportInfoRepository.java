// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.report;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.thingsboard.server.dao.model.sql.ReportInfoEntity;

import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.SUB_CUSTOMERS_QUERY;

@Repository
public interface ReportInfoRepository extends JpaRepository<ReportInfoEntity, UUID> {

    @Query("SELECT ri FROM ReportInfoEntity ri WHERE ri.tenantId = :tenantId " +
            "AND (:reportTemplateId IS NULL OR (ri.templateId = :reportTemplateId)) " +
            "AND (:userId IS NULL OR (ri.userId = :userId)) " +
            "AND (:searchText IS NULL OR ilike(ri.name, CONCAT('%', :searchText, '%')) = true " +
            "OR ilike(ri.customerTitle, CONCAT('%', :searchText, '%')) = true)")
    Page<ReportInfoEntity> findTenantReportInfosIncludingCustomers(UUID tenantId, UUID reportTemplateId, UUID userId, String searchText, Pageable pageable);

    @Query("SELECT ri FROM ReportInfoEntity ri WHERE ri.tenantId = :tenantId " +
            "AND (ri.customerId IS NULL OR ri.customerId = org.thingsboard.server.common.data.id.EntityId.NULL_UUID) " +
            "AND (:reportTemplateId IS NULL OR (ri.templateId = :reportTemplateId)) " +
            "AND (:userId IS NULL OR (ri.userId = :userId)) " +
            "AND (:searchText IS NULL OR ilike(ri.name, CONCAT('%', :searchText, '%')) = true)")
    Page<ReportInfoEntity> findTenantReportInfos(UUID tenantId, UUID reportTemplateId, UUID userId, String searchText, Pageable pageable);

    @Query(value = "SELECT e.*, e.created_time as createdtime, e.report_template_name as reportTemplateName, e.customer_title as customertitle, e.user_name as username " +
            "FROM (select r.id, r.created_time, r.tenant_id, r.customer_id, c.title as customer_title, r.template_id, r.report_template_name, " +
            "r.format, r.name, r.user_id, r.data, r.user_name, r.public_key, r.is_public from report_info_view r  " +
            "LEFT JOIN customer c on c.id = r.customer_id AND c.id != :customerId) e  " +
            "WHERE" + SUB_CUSTOMERS_QUERY +
            "AND (:reportTemplateId IS NULL OR (e.template_id = :reportTemplateId)) " +
            "AND (:userId IS NULL OR (e.user_id = :userId)) " +
            "AND (:searchText IS NULL OR e.name ILIKE CONCAT('%', :searchText, '%') " +
            "OR e.customer_title ILIKE CONCAT('%', :searchText, '%')) ",
            countQuery = "SELECT count(e.id) FROM scheduled_reports_info_view e " +
                    "WHERE" + SUB_CUSTOMERS_QUERY +
                    "AND (:reportTemplateId IS NULL OR (e.report_template_id = :reportTemplateId))" +
                    "AND (:userId IS NULL OR (e.user_id = :userId))" +
                    "AND (:searchText IS NULL OR e.name ILIKE CONCAT('%', :searchText, '%')) ",
            nativeQuery = true)
    Page<ReportInfoEntity> findCustomerReportInfosIncludingSubCustomers(UUID tenantId, UUID customerId, UUID reportTemplateId, UUID userId, String searchText, Pageable pageable);

    @Query("SELECT ri FROM ReportInfoEntity ri WHERE ri.tenantId = :tenantId " +
            "AND (ri.customerId = :customerId) " +
            "AND (:reportTemplateId IS NULL OR (ri.templateId = :reportTemplateId)) " +
            "AND (:userId IS NULL OR (ri.userId = :userId)) " +
            "AND (:searchText IS NULL OR ilike(ri.name, CONCAT('%', :searchText, '%')) = true)")
    Page<ReportInfoEntity> findCustomerReportInfos(UUID tenantId, UUID customerId, UUID reportTemplateId, UUID userId, String searchText, Pageable pageable);

    List<ReportInfoEntity> findByIdIn(List<UUID> toUUIDs);

}

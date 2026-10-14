// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.report;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.edqs.fields.ReportTemplateFields;
import org.thingsboard.server.common.data.report.ReportTemplateType;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.common.data.util.TbTriple;
import org.thingsboard.server.dao.ExportableEntityRepository;
import org.thingsboard.server.dao.model.sql.ReportTemplateEntity;

import java.util.List;
import java.util.UUID;

public interface ReportTemplateRepository extends JpaRepository<ReportTemplateEntity, UUID>, ExportableEntityRepository<ReportTemplateEntity> {

    Page<ReportTemplateEntity> findByTenantId(UUID tenantId, Pageable pageable);

    @Query("SELECT r.id FROM ReportTemplateEntity r WHERE r.tenantId = :tenantId AND (r.customerId is null OR r.customerId = org.thingsboard.server.common.data.id.EntityId.NULL_UUID)")
    Page<UUID> findIdsByTenantIdAndNullCustomerId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT r.id FROM ReportTemplateEntity r WHERE r.tenantId = :tenantId AND r.customerId = :customerId")
    Page<UUID> findIdsByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                              @Param("customerId") UUID customerId,
                                              Pageable pageable);

    @Query("SELECT externalId FROM ReportTemplateEntity WHERE id = :id")
    UUID getExternalIdById(@Param("id") UUID id);

    @Query("SELECT se.id FROM ReportTemplateEntity se WHERE se.tenantId = :tenantId")
    Page<UUID> findIdsByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT NEW org.thingsboard.server.common.data.util.TbTriple(rt.format, rt.type, count(*)) FROM ReportTemplateEntity rt GROUP BY rt.format, rt.type")
    List<TbTriple<TbReportFormat, ReportTemplateType, Long>> countTemplatesByFormatAndType();

    @Query("SELECT new org.thingsboard.server.common.data.edqs.fields.ReportTemplateFields(r.id, r.createdTime, r.tenantId, " +
           "r.customerId, r.name, r.type, r.format, r.version) FROM ReportTemplateEntity r WHERE r.id > :id ORDER BY r.id")
    List<ReportTemplateFields> findNextBatch(@Param("id") UUID id, Limit limit);

}

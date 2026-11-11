// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.sync.ie.importing.impl;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.dao.report.ReportTemplateService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class ReportTemplateImportService extends BaseEntityImportService<ReportTemplateId, ReportTemplate, EntityExportData<ReportTemplate>> {

    private static final LinkedHashSet<EntityType> HINTS = new LinkedHashSet<>(Arrays.asList(EntityType.DASHBOARD, EntityType.DEVICE, EntityType.ASSET));
    private final ReportTemplateService reportTemplateService;

    @Override
    protected void setOwner(TenantId tenantId, ReportTemplate reportTemplate, IdProvider idProvider) {
        reportTemplate.setTenantId(tenantId);
        if (reportTemplate.getOwnerId() instanceof TenantId) {
            reportTemplate.setOwnerId(tenantId);
        } else {
            reportTemplate.setOwnerId(idProvider.getInternalId(reportTemplate.getOwnerId()));
        }
    }

    @Override
    protected ReportTemplate prepare(EntitiesImportCtx ctx, ReportTemplate reportTemplate, ReportTemplate oldReportTemplate, EntityExportData<ReportTemplate> exportData, IdProvider idProvider) {
        for (JsonNode entityAlias : reportTemplate.getEntityAliasesConfig()) {
            replaceIdsRecursively(ctx, idProvider, entityAlias, Set.of("id"), null, HINTS);
        }
        for (JsonNode dataSource : reportTemplate.getComponentDataSources()) {
            replaceIdsRecursively(ctx, idProvider, dataSource, Set.of("entityAlias"), null, HINTS);
        }
        return reportTemplate;
    }

    @Override
    protected ReportTemplate deepCopy(ReportTemplate reportTemplate) {
        return new ReportTemplate(reportTemplate);
    }

    @Override
    protected ReportTemplate saveOrUpdate(EntitiesImportCtx ctx, ReportTemplate reportTemplate, EntityExportData<ReportTemplate> exportData, IdProvider idProvider, CompareResult compareResult) throws Exception {
        return reportTemplateService.saveReportTemplate(reportTemplate);
    }

    @Override
    protected void cleanupForComparison(ReportTemplate reportTemplate) {
        super.cleanupForComparison(reportTemplate);
        if (reportTemplate.getCustomerId() != null && reportTemplate.getCustomerId().isNullUid()) {
            reportTemplate.setCustomerId(null);
        }
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.REPORT_TEMPLATE;
    }

}

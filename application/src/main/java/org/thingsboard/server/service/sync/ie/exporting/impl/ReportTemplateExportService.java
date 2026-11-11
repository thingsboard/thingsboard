// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.sync.ie.exporting.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesExportCtx;

import java.util.Set;

@Service
@TbCoreComponent
public class ReportTemplateExportService extends BaseEntityExportService<ReportTemplateId, ReportTemplate, EntityExportData<ReportTemplate>> {

    @Override
    protected void setRelatedEntities(EntitiesExportCtx<?> ctx, ReportTemplate reportTemplate, EntityExportData<ReportTemplate> exportData) {
        reportTemplate.setCustomerId(getExternalIdOrElseInternal(ctx, reportTemplate.getCustomerId()));
        for (JsonNode entityAlias : reportTemplate.getEntityAliasesConfig()) {
            replaceUuidsRecursively(ctx, entityAlias, Set.of("id"), null);
        }
        for (JsonNode dataSource : reportTemplate.getComponentDataSources()) {
            replaceUuidsRecursively(ctx, dataSource, Set.of("entityAliasId"), null);
        }
    }

    @Override
    public Set<EntityType> getSupportedEntityTypes() {
        return Set.of(EntityType.REPORT_TEMPLATE);
    }

}

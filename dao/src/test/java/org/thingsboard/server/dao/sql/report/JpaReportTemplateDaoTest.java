// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.report;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.configuration.PdfReportTemplateConfig;
import org.thingsboard.server.common.data.report.configuration.ReportTemplateConfig;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.report.ReportTemplateDao;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JpaReportTemplateDaoTest extends AbstractJpaDaoTest {

    @Autowired
    private ReportTemplateDao reportTemplateDao;


    @Test
    public void testSaveReportTemplateName0x00_thenSomeDatabaseException() {
        assertThatThrownBy(() ->
                saveReportTemplate(UUID.randomUUID(), Uuids.timeBased(), Uuids.timeBased(), "F0929906\000\000\000\000\000\000\000\000\000"));
    }

    private ReportTemplate saveReportTemplate(UUID id, UUID tenantId, UUID customerId, String name) {
        return saveReportTemplate(id, tenantId, customerId, name, new PdfReportTemplateConfig(), null);
    }

    private ReportTemplate saveReportTemplate(UUID id, UUID tenantId, UUID customerId, String name, ReportTemplateConfig configuration, String description) {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setId(new ReportTemplateId(id));
        reportTemplate.setTenantId(TenantId.fromUUID(tenantId));
        reportTemplate.setCustomerId(new CustomerId(customerId));
        reportTemplate.setName(name);
        reportTemplate.setFormat(configuration.getFormat());
        reportTemplate.setConfiguration(configuration);
        reportTemplate.setDescription(description);
        return reportTemplateDao.save(TenantId.fromUUID(tenantId), reportTemplate);
    }
}

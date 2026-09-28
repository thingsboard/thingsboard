// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.service;

import lombok.experimental.SuperBuilder;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.task.ReportTask;
import org.thingsboard.server.common.data.report.ReportData;
import org.thingsboard.server.common.data.report.configuration.CsvReportTemplateConfig;
import org.thingsboard.server.common.data.report.configuration.ReportTemplateConfig;
import org.thingsboard.server.report.context.TbReportCtx;
import org.thingsboard.server.report.util.CsvUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the CSV choke point for the non-production notice: {@link CsvReportService#generateReport} itself,
 * not {@link CsvUtils}, which is a plain format writer and knows nothing about licensing.
 */
public class CsvReportServiceTest {

    @Test
    void testGenerateReportIncludesNonProductionNoticeWhenNonProduction() throws Exception {
        String csv = generateCsvReport(true);

        assertThat(csv).startsWith(DataConstants.NON_PRODUCTION_NOTICE);
    }

    @Test
    void testGenerateReportOmitsNonProductionNoticeWhenNotNonProduction() throws Exception {
        String csv = generateCsvReport(false);

        assertThat(csv).doesNotContain(DataConstants.NON_PRODUCTION_NOTICE);
    }

    private String generateCsvReport(boolean nonProduction) throws Exception {
        CsvReportService service = new CsvReportService(List.of());

        CsvReportTemplateConfig configuration = CsvReportTemplateConfig.builder()
                .components(List.of())
                .build();

        TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
        ReportTask task = ReportTask.builder()
                .tenantId(tenantId)
                .reportTemplateConfig(configuration)
                .build();

        TestReportCtx ctx = TestReportCtx.builder()
                .tenantId(tenantId)
                .configuration(configuration)
                .nonProduction(nonProduction)
                .build();

        ReportData reportData = service.generateReport(task, ctx);
        return new String(reportData.getData(), StandardCharsets.UTF_8);
    }

    @SuperBuilder(toBuilder = true)
    private static class TestReportCtx extends TbReportCtx {

        @Override
        public TbReportCtx createSubReportCxt(ReportTemplateConfig reportTemplateConfig) {
            throw new UnsupportedOperationException();
        }

    }

}

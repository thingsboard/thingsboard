// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.service;

import lombok.experimental.SuperBuilder;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.task.ReportTask;
import org.thingsboard.server.common.data.report.ReportData;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.configuration.CsvReportTemplateConfig;
import org.thingsboard.server.common.data.report.configuration.ReportTemplateConfig;
import org.thingsboard.server.common.data.report.configuration.components.SubReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.TimeseriesTableComponent;
import org.thingsboard.server.common.data.report.configuration.timewindow.FixedTimeWindow;
import org.thingsboard.server.common.data.report.configuration.timewindow.History;
import org.thingsboard.server.common.data.report.configuration.timewindow.TimeWindowConfiguration;
import org.thingsboard.server.report.context.TbReportCtx;
import org.thingsboard.server.report.datasource.ReportDataService;
import org.thingsboard.server.report.util.CsvUtils;
import org.thingsboard.server.report.util.ReportTimeRangeResolver;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Covers the CSV choke point for the non-production notice: {@link CsvReportService#generateReport} itself,
 * not {@link CsvUtils}, which is a plain format writer and knows nothing about licensing.
 */
public class CsvReportServiceTest {

    private static final long START_TS = 1704164640000L;
    private static final long END_TS = 1704434820000L;

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

    @Test
    void testGenerateReportPassesRootReportTimeRangeToSubReport() throws Exception {
        CsvReportService service = new CsvReportService(List.of());
        service.dataService = mock(ReportDataService.class);
        service.timeRangeResolver = new ReportTimeRangeResolver(service.dataService);

        ReportTemplateId subReportTemplateId = new ReportTemplateId(UUID.randomUUID());
        ReportTemplate subReportTemplate = new ReportTemplate(subReportTemplateId);
        subReportTemplate.setConfiguration(CsvReportTemplateConfig.builder()
                .timeDataPattern("dd/MM/yyyy")
                .components(List.of(timeseriesTable(START_TS, END_TS)))
                .build());
        when(service.dataService.findReportTemplate(eq(subReportTemplateId), any())).thenReturn(subReportTemplate);

        SubReportComponent subReport = new SubReportComponent();
        subReport.setTemplateId(subReportTemplateId);
        CsvReportTemplateConfig configuration = CsvReportTemplateConfig.builder()
                .timeDataPattern("yyyy-MM-dd HH:mm")
                .components(List.of(subReport))
                .build();

        TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
        ReportTask task = ReportTask.builder()
                .tenantId(tenantId)
                .reportTemplateConfig(configuration)
                .build();
        TestReportCtx ctx = TestReportCtx.builder()
                .tenantId(tenantId)
                .configuration(configuration)
                .timeZone("UTC")
                .build();

        service.generateReport(task, ctx);

        assertThat(ctx.subReportCtxs).hasSize(1);
        assertThat(service.resolveAndCacheReportTimeRange(ctx.subReportCtxs.get(0)).timeRange())
                .isEqualTo("2024-01-02 03:04 – 2024-01-05 06:07");
    }

    private static TimeseriesTableComponent timeseriesTable(long startTs, long endTs) {
        FixedTimeWindow fixedTimeWindow = new FixedTimeWindow();
        fixedTimeWindow.setStartTimeMs(startTs);
        fixedTimeWindow.setEndTimeMs(endTs);
        History history = new History();
        history.setHistoryType(1);
        history.setFixedTimewindow(fixedTimeWindow);
        TimeWindowConfiguration timeWindow = new TimeWindowConfiguration();
        timeWindow.setHistory(history);
        TimeseriesTableComponent component = new TimeseriesTableComponent();
        component.setTimewindow(timeWindow);
        return component;
    }

    private String generateCsvReport(boolean nonProduction) throws Exception {
        CsvReportService service = new CsvReportService(List.of());
        service.timeRangeResolver = new ReportTimeRangeResolver(mock(ReportDataService.class));

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

        private final List<TbReportCtx> subReportCtxs = new ArrayList<>();

        @Override
        public TbReportCtx createSubReportCxt(ReportTemplateConfig reportTemplateConfig) {
            TestReportCtx copy = this.toBuilder().configuration(reportTemplateConfig).build();
            copy.getParams().putAll(this.getParams());
            subReportCtxs.add(copy);
            return copy;
        }

    }

}

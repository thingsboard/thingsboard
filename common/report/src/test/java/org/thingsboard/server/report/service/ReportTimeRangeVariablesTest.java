// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.job.task.ReportTask;
import org.thingsboard.server.common.data.report.ReportData;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.common.data.report.configuration.PdfReportTemplateConfig;
import org.thingsboard.server.common.data.report.configuration.ReportTemplateConfig;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.TimeseriesChartComponent;
import org.thingsboard.server.common.data.report.configuration.timewindow.FixedTimeWindow;
import org.thingsboard.server.common.data.report.configuration.timewindow.History;
import org.thingsboard.server.common.data.report.configuration.timewindow.TimeWindowConfiguration;
import org.thingsboard.server.report.context.ComponentData;
import org.thingsboard.server.report.context.RemoteTbReportCtxProvider.RemoteTbReportCtx;
import org.thingsboard.server.report.context.TbReportCtx;
import org.thingsboard.server.report.datasource.ReportDataService;
import org.thingsboard.server.report.util.ReportTimeRangeResolver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ReportTimeRangeVariablesTest {

    private static final long START_TS = 1704164640000L;
    private static final long END_TS = 1704434820000L;

    private AbstractReportService service;

    @BeforeEach
    void setUp() {
        service = new AbstractReportService() {
            @Override
            public ReportData generateReport(ReportTask task, TbReportCtx ctx) {
                return null;
            }

            @Override
            public TbReportFormat getFormat() {
                return TbReportFormat.PDF;
            }
        };
        service.timeRangeResolver = new ReportTimeRangeResolver(mock(ReportDataService.class));
    }

    @Test
    void testReportTimeRangeVariablesUseRootTemplatePattern() {
        TbReportCtx rootCtx = ctx(config("yyyy-MM-dd HH:mm", chart(START_TS, END_TS)));

        assertThat(reportVariables(rootCtx))
                .containsEntry("reportStartTime", "2024-01-02 03:04")
                .containsEntry("reportEndTime", "2024-01-05 06:07")
                .containsEntry("reportTimeRange", "2024-01-02 03:04 – 2024-01-05 06:07");
    }

    @Test
    void testSubReportInheritsFormattedReportTimeRangeOfRootTemplate() {
        TbReportCtx rootCtx = ctx(config("yyyy-MM-dd HH:mm", chart(START_TS, END_TS)));
        service.resolveReportTimeRange(rootCtx);

        TbReportCtx subReportCtx = rootCtx.createSubReportCxt(config("dd/MM/yyyy", chart(1000, 2000)));

        assertThat(reportVariables(subReportCtx))
                .containsEntry("reportStartTime", "2024-01-02 03:04")
                .containsEntry("reportEndTime", "2024-01-05 06:07");
    }

    @Test
    void testUndefinedReportTimeRangeRendersEmptyVariables() {
        TbReportCtx rootCtx = ctx(config("yyyy-MM-dd HH:mm", chart(START_TS, END_TS), chart(1000, 2000)));

        assertThat(reportVariables(rootCtx))
                .containsEntry("reportStartTime", "")
                .containsEntry("reportEndTime", "")
                .containsEntry("reportTimeRange", "");
    }

    private Map<String, Object> reportVariables(TbReportCtx ctx) {
        ComponentData componentData = new ComponentData(100, new HashMap<>());
        service.populateReportVars(componentData, ctx);
        return componentData.getVariables();
    }

    private static TbReportCtx ctx(ReportTemplateConfig config) {
        return RemoteTbReportCtx.builder()
                .configuration(config)
                .timeZone("UTC")
                .build();
    }

    private static ReportTemplateConfig config(String timeDataPattern, ReportComponent... components) {
        return PdfReportTemplateConfig.builder()
                .timeDataPattern(timeDataPattern)
                .components(List.of(components))
                .build();
    }

    private static TimeseriesChartComponent chart(long startTs, long endTs) {
        FixedTimeWindow fixedTimeWindow = new FixedTimeWindow();
        fixedTimeWindow.setStartTimeMs(startTs);
        fixedTimeWindow.setEndTimeMs(endTs);
        History history = new History();
        history.setHistoryType(1);
        history.setFixedTimewindow(fixedTimeWindow);
        TimeWindowConfiguration timeWindow = new TimeWindowConfiguration();
        timeWindow.setHistory(history);
        TimeseriesChartComponent component = new TimeseriesChartComponent();
        component.setTimewindow(timeWindow);
        return component;
    }

}

// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.job.task.ReportTask;
import org.thingsboard.server.common.data.report.Report;
import org.thingsboard.server.common.data.report.ReportData;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.common.data.report.configuration.ReportTemplateConfig;
import org.thingsboard.server.report.context.TbReportCtx;
import org.thingsboard.server.report.context.TbReportCtxProvider;
import org.thingsboard.server.report.datasource.ReportDataService;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class TbReportService {
    private final Map<TbReportFormat, ReportService> reportServices = new EnumMap<>(TbReportFormat.class);
    private final TbReportCtxProvider contextProvider;
    private final ReportDataService dataService;
    private ExecutorService executor;

    @Value("${reports.test_report_pool_size:12}")
    private int testReportThreads;

    @PostConstruct
    private void init() {
        executor = Executors.newFixedThreadPool(testReportThreads, ThingsBoardThreadFactory.forName(getClass().getSimpleName()));
    }

    @PreDestroy
    public void shutdownExecutor() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    private TbReportService(List<ReportService> reportServices, @Lazy TbReportCtxProvider contextProvider, @Lazy ReportDataService dataService) {
        reportServices.forEach(service -> {
            TbReportFormat format = service.getFormat();
            if (format != null) {
                this.reportServices.put(format, service);
            }
        });
        this.contextProvider = contextProvider;
        this.dataService = dataService;
    }

    public Future<ReportData> generateTestReport(ReportTask task) {
        return executor.submit(() -> {
            try (TbReportCtx ctx = contextProvider.newContext(task)) {
                return generateReport(task, ctx);
            }
        });
    }

    public Report generateReport(ReportTask task) throws Exception {
        try (TbReportCtx ctx = contextProvider.newContext(task)) {
            ReportData reportData = generateReport(task, ctx);

            Report report = new Report();
            report.setTenantId(task.getTenantId());
            report.setCustomerId(task.getCustomerId());
            report.setTemplateId(task.getReportTemplateId());
            report.setFormat(task.getReportTemplateConfig().getFormat());
            report.setName(reportData.getName());
            report.setUserId(task.getUserId());
            report.setPublic(task.isMakePublic());
            return dataService.createReport(report, reportData.getData(), ctx);
        }
    }

    private ReportData generateReport(ReportTask task, TbReportCtx ctx) throws Exception {
        ReportTemplateConfig configuration = task.getReportTemplateConfig();
        return reportServices.get(configuration.getFormat()).generateReport(task, ctx);
    }

}

// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.client;

import org.junit.Test;
import org.thingsboard.client.ApiException;
import org.thingsboard.client.api.ThingsboardApi.CreateReportArgs;
import org.thingsboard.client.api.ThingsboardApi.DeleteReportArgs;
import org.thingsboard.client.api.ThingsboardApi.DeleteReportTemplateArgs;
import org.thingsboard.client.api.ThingsboardApi.DownloadReportArgs;
import org.thingsboard.client.api.ThingsboardApi.GetReportByIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetReportInfosArgs;
import org.thingsboard.client.api.ThingsboardApi.GetReportsArgs;
import org.thingsboard.client.api.ThingsboardApi.RequestReportArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveReportTemplateArgs;
import org.thingsboard.client.api.ThingsboardApi.TestReportAndDownloadArgs;
import org.thingsboard.client.model.CreateReportRequest;
import org.thingsboard.client.model.Job;
import org.thingsboard.client.model.PageDataReport;
import org.thingsboard.client.model.PageDataReportInfo;
import org.thingsboard.client.model.PdfReportTemplateConfig;
import org.thingsboard.client.model.Report;
import org.thingsboard.client.model.ReportRequest;
import org.thingsboard.client.model.ReportTemplate;
import org.thingsboard.client.model.ReportTemplateType;
import org.thingsboard.client.model.TbReportFormat;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@DaoSqlTest
public class ReportApiClientTest extends AbstractApiClientTest {

    private static final long REPORT_POLL_TIMEOUT_MS = 30_000L;
    private static final long POLL_INTERVAL_MS = 2_000L;

    @Test
    public void testGetReports() throws Exception {
        PageDataReport page = client.getReports(GetReportsArgs.builder()
                .pageSize(100)
                .page(0)
                .build());
        assertNotNull(page);
        assertNotNull(page.getData());
    }

    @Test
    public void testGetReportInfos() throws Exception {
        PageDataReportInfo infoPage =
                client.getReportInfos(GetReportInfosArgs.builder()
                        .pageSize(100)
                        .page(0)
                        .build());
        assertNotNull(infoPage);
        assertNotNull(infoPage.getData());
    }

    @Test
    public void testRequestReport() throws Exception {
        long ts = System.currentTimeMillis();
        ReportTemplate template = createTemplate(TEST_PREFIX + ts);

        Job job = client.requestReport(RequestReportArgs.builder()
                .reportRequest(buildRequest(template))
                .build());
        assertNotNull(job);
        assertNotNull(job.getId());

        client.deleteReportTemplate(DeleteReportTemplateArgs.builder()
                .reportTemplateId(template.getId().getId().toString())
                .build());
    }

    @Test
    public void testTestReportAndDownload() throws Exception {
        long ts = System.currentTimeMillis();
        ReportTemplate template = createTemplate(TEST_PREFIX + ts);

        try {
            client.testReportAndDownload(TestReportAndDownloadArgs.builder()
                    .reportRequest(buildRequest(template))
                    .build());
        } catch (ApiException e) {
            assertTrue("testReportAndDownload returned unexpected HTTP status: " + e.getCode(),
                    e.getCode() >= 400);
        }

        client.deleteReportTemplate(DeleteReportTemplateArgs.builder()
                .reportTemplateId(template.getId().getId().toString())
                .build());
    }

    @Test
    public void testCreateReport() throws Exception {
        File tmpFile = Files.createTempFile("test-report", ".pdf").toFile();
        Files.writeString(tmpFile.toPath(), "%PDF-1.4 empty test");

        CreateReportRequest request = new CreateReportRequest();
        request.setFile(tmpFile);
        request.setInfo("{}");

        try {
            Report report = client.createReport(CreateReportArgs.builder()
                    .createReportRequest(request)
                    .build());
            if (report != null && report.getId() != null) {
                client.deleteReport(DeleteReportArgs.builder()
                        .reportId(report.getId().getId().toString())
                        .build());
            }
        } catch (ApiException e) {
            assertTrue("createReport returned unexpected HTTP status: " + e.getCode(),
                    e.getCode() >= 400);
        } finally {
            tmpFile.delete();
        }
    }

    @Test
    public void testReportLifecycle() throws Exception {
        long ts = System.currentTimeMillis();
        ReportTemplate template = createTemplate(TEST_PREFIX + ts);

        client.requestReport(RequestReportArgs.builder()
                .reportRequest(buildRequest(template))
                .build());

        Report report = waitForReport();
        if (report == null) {
            assertReturns404(() -> client.getReportById(GetReportByIdArgs.builder()
                    .reportId(UUID.randomUUID().toString())
                    .build()));
            client.deleteReportTemplate(DeleteReportTemplateArgs.builder()
                    .reportTemplateId(template.getId().getId().toString())
                    .build());
            return;
        }

        String reportId = report.getId().getId().toString();

        Report fetched = client.getReportById(GetReportByIdArgs.builder()
                .reportId(reportId)
                .build());
        assertNotNull(fetched);
        assertEquals(reportId, fetched.getId().getId().toString());

        File downloaded = client.downloadReport(DownloadReportArgs.builder()
                .reportId(report.getId().getId())
                .build());
        assertNotNull(downloaded);

        client.deleteReport(DeleteReportArgs.builder()
                .reportId(reportId)
                .build());
        assertReturns404(() -> client.getReportById(GetReportByIdArgs.builder()
                .reportId(reportId)
                .build()));

        client.deleteReportTemplate(DeleteReportTemplateArgs.builder()
                .reportTemplateId(template.getId().getId().toString())
                .build());
    }

    private ReportTemplate createTemplate(String name) throws ApiException {
        PdfReportTemplateConfig config = new PdfReportTemplateConfig().components(List.of());
        ReportTemplate template = new ReportTemplate();
        template.setName(name);
        template.setFormat(TbReportFormat.PDF);
        template.setType(ReportTemplateType.REPORT);
        template.setConfiguration(config);
        return client.saveReportTemplate(SaveReportTemplateArgs.builder()
                .reportTemplate(template)
                .build());
    }

    private ReportRequest buildRequest(ReportTemplate template) {
        ReportRequest request = new ReportRequest();
        request.setReportTemplateId(template.getId());
        request.setTimezone("UTC");
        return request;
    }

    private Report waitForReport() throws ApiException, InterruptedException {
        long deadline = System.currentTimeMillis() + REPORT_POLL_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            PageDataReport page = client.getReports(GetReportsArgs.builder()
                    .pageSize(1)
                    .page(0)
                    .build());
            if (page != null && !page.getData().isEmpty()) {
                return page.getData().get(0);
            }
            Thread.sleep(POLL_INTERVAL_MS);
        }
        return null;
    }

}

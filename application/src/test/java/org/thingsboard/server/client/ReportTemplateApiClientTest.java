// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.client;

import org.junit.Test;
import org.thingsboard.client.ApiException;
import org.thingsboard.client.api.ThingsboardApi.DeleteReportTemplateArgs;
import org.thingsboard.client.api.ThingsboardApi.GetAllReportTemplateInfosArgs;
import org.thingsboard.client.api.ThingsboardApi.GetReportTemplateByIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetReportTemplateInfoByIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetReportTemplatesByIdsArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveReportTemplateArgs;
import org.thingsboard.client.model.PageDataReportTemplateInfo;
import org.thingsboard.client.model.PdfReportTemplateConfig;
import org.thingsboard.client.model.ReportComponentSubType;
import org.thingsboard.client.model.ReportTemplate;
import org.thingsboard.client.model.ReportTemplateInfo;
import org.thingsboard.client.model.ReportTemplateType;
import org.thingsboard.client.model.ReportTimeSeriesChartSettings;
import org.thingsboard.client.model.TbReportFormat;
import org.thingsboard.client.model.TimeseriesChartComponent;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@DaoSqlTest
public class ReportTemplateApiClientTest extends AbstractApiClientTest {

    @Test
    public void testReportTemplateLifecycle() throws Exception {
        long ts = System.currentTimeMillis();
        String name = TEST_PREFIX + ts;

        ReportTemplate saved = createTemplate(name);
        assertNotNull(saved);
        assertNotNull(saved.getId());
        assertEquals(name, saved.getName());
        assertEquals(TbReportFormat.PDF, saved.getFormat());
        assertEquals(ReportTemplateType.REPORT, saved.getType());
        String templateId = saved.getId().getId().toString();

        ReportTemplate fetched = client.getReportTemplateById(GetReportTemplateByIdArgs.builder()
                .reportTemplateId(templateId)
                .build());
        assertNotNull(fetched);
        assertEquals(templateId, fetched.getId().getId().toString());
        assertEquals(name, fetched.getName());

        ReportTemplateInfo info = client.getReportTemplateInfoById(GetReportTemplateInfoByIdArgs.builder()
                .reportTemplateId(templateId)
                .build());
        assertNotNull(info);
        assertEquals(templateId, info.getId().getId().toString());
        assertEquals(name, info.getName());
        assertEquals(TbReportFormat.PDF, info.getFormat());

        fetched.setName(name + "_updated");
        ReportTemplate updated = client.saveReportTemplate(SaveReportTemplateArgs.builder()
                .reportTemplate(fetched)
                .build());
        assertEquals(name + "_updated", updated.getName());

        client.deleteReportTemplate(DeleteReportTemplateArgs.builder()
                .reportTemplateId(templateId)
                .build());
        assertReturns404(() -> client.getReportTemplateById(GetReportTemplateByIdArgs.builder()
                .reportTemplateId(templateId)
                .build()));
    }

    @Test
    public void testGetAllReportTemplateInfos() throws Exception {
        long ts = System.currentTimeMillis();

        ReportTemplate t1 = createTemplate(TEST_PREFIX + ts + "_1");
        ReportTemplate t2 = createTemplate(TEST_PREFIX + ts + "_2");
        ReportTemplate t3 = createTemplate(TEST_PREFIX + ts + "_3");
        String id1 = t1.getId().getId().toString();
        String id2 = t2.getId().getId().toString();
        String id3 = t3.getId().getId().toString();

        PageDataReportTemplateInfo page = client.getAllReportTemplateInfos(GetAllReportTemplateInfosArgs.builder()
                .pageSize(100)
                .page(0)
                .textSearch(TEST_PREFIX + ts)
                .build());
        assertNotNull(page);
        assertTrue(page.getTotalElements() >= 3);
        assertTrue(page.getData().stream().anyMatch(t -> t.getId().getId().toString().equals(id1)));
        assertTrue(page.getData().stream().anyMatch(t -> t.getId().getId().toString().equals(id2)));
        assertTrue(page.getData().stream().anyMatch(t -> t.getId().getId().toString().equals(id3)));

        client.deleteReportTemplate(DeleteReportTemplateArgs.builder()
                .reportTemplateId(id1)
                .build());
        client.deleteReportTemplate(DeleteReportTemplateArgs.builder()
                .reportTemplateId(id2)
                .build());
        client.deleteReportTemplate(DeleteReportTemplateArgs.builder()
                .reportTemplateId(id3)
                .build());
    }

    @Test
    public void testGetReportTemplatesByIds() throws Exception {
        long ts = System.currentTimeMillis();

        ReportTemplate t1 = createTemplate(TEST_PREFIX + ts + "_a");
        ReportTemplate t2 = createTemplate(TEST_PREFIX + ts + "_b");
        String id1 = t1.getId().getId().toString();
        String id2 = t2.getId().getId().toString();

        List<ReportTemplateInfo> result = client.getReportTemplatesByIds(GetReportTemplatesByIdsArgs.builder()
                .reportTemplateIds(List.of(id1, id2))
                .build());
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(t -> t.getId().getId().toString().equals(id1)));
        assertTrue(result.stream().anyMatch(t -> t.getId().getId().toString().equals(id2)));

        client.deleteReportTemplate(DeleteReportTemplateArgs.builder()
                .reportTemplateId(id1)
                .build());
        client.deleteReportTemplate(DeleteReportTemplateArgs.builder()
                .reportTemplateId(id2)
                .build());
    }

    @Test
    public void testGetReportTemplateByIdNotFound() {
        assertReturns404(() -> client.getReportTemplateById(GetReportTemplateByIdArgs.builder()
                .reportTemplateId(UUID.randomUUID().toString())
                .build()));
    }

    private ReportTemplate buildTemplate(String name) {
        TimeseriesChartComponent timeseriesChartComponent = getTimeseriesChartComponent();
        PdfReportTemplateConfig config = new PdfReportTemplateConfig()
                .components(List.of(timeseriesChartComponent));

        ReportTemplate template = new ReportTemplate();
        template.setName(name);
        template.setFormat(TbReportFormat.PDF);
        template.setType(ReportTemplateType.REPORT);
        template.setConfiguration(config);
        return template;
    }

    private TimeseriesChartComponent getTimeseriesChartComponent() {
        TimeseriesChartComponent timeseriesChartComponent = new TimeseriesChartComponent();
        timeseriesChartComponent.setSubType(ReportComponentSubType.LINE_CHART);
        ReportTimeSeriesChartSettings timeSeriesChartSettings = new ReportTimeSeriesChartSettings();
        timeSeriesChartSettings.setTitle(
                "Time series chart"
        );
        timeseriesChartComponent.setTimeSeriesChartSettings(timeSeriesChartSettings);
        timeseriesChartComponent.setSubType(ReportComponentSubType.LINE_CHART);
        return timeseriesChartComponent;
    }

    private ReportTemplate createTemplate(String name) throws ApiException {
        return client.saveReportTemplate(SaveReportTemplateArgs.builder()
                .reportTemplate(buildTemplate(name))
                .build());
    }

}

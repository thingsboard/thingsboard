// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.report.ReportConfig;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.ReportTemplateInfo;
import org.thingsboard.server.common.data.report.ReportTemplateQuery;
import org.thingsboard.server.common.data.report.ReportTemplateType;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.common.data.report.configuration.PdfReportTemplateConfig;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartYAxisSettings;
import org.thingsboard.server.common.data.report.configuration.chart.ValueSourceType;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.TimeseriesChartComponent;
import org.thingsboard.server.common.data.scheduler.MonthlyRepeat;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.dao.model.sql.ReportTemplateEntity;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.report.ReportTemplateService;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.dao.sql.report.ReportTemplateRepository;
import org.thingsboard.server.exception.DataValidationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

@DaoSqlTest
public class ReportTemplateServiceTest extends AbstractServiceTest {

    private static final String OLD_FORMAT_REPORT_TEMPLATE = "{\n" +
            "  \"format\": \"PDF\",\n" +
            "  \"namePattern\": \"report-%d{yyyy-MM-dd_HH:mm:ss}\",\n" +
            "  \"timeDataPattern\": \"yyyy-MM-dd HH:mm:ss\",\n" +
            "  \"entityAliases\": [\n" +
            "    {\n" +
            "      \"id\": \"e77969e5-dfa4-e296-b821-cdfa5a877265\",\n" +
            "      \"alias\": \"One dot device\",\n" +
            "      \"filter\": {\n" +
            "        \"type\": \"singleEntity\",\n" +
            "        \"singleEntity\": {\n" +
            "          \"entityType\": \"DEVICE\",\n" +
            "          \"id\": \"f2066e90-d1bc-11f0-9895-1d90d4be80f6\"\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  ],\n" +
            "  \"filters\": [],\n" +
            "  \"components\": [\n" +
            "    {\n" +
            "      \"subType\": \"lineChart\",\n" +
            "      \"dataSources\": [\n" +
            "        {\n" +
            "          \"type\": \"entity\",\n" +
            "          \"deviceId\": null,\n" +
            "          \"entityAliasId\": \"e77969e5-dfa4-e296-b821-cdfa5a877265\",\n" +
            "          \"filterId\": null,\n" +
            "          \"dataKeys\": [\n" +
            "            {\n" +
            "              \"name\": \"temperature\",\n" +
            "              \"type\": \"timeseries\",\n" +
            "              \"label\": \"Temperature\",\n" +
            "              \"color\": \"#F321F3\",\n" +
            "              \"decimals\": 0,\n" +
            "              \"units\": \"°C\",\n" +
            "              \"aggregationType\": null,\n" +
            "              \"timewindow\": null,\n" +
            "              \"usePostProcessing\": false,\n" +
            "              \"postFuncBody\": null,\n" +
            "              \"settings\": {\n" +
            "                \"showInLegend\": true,\n" +
            "                \"seriesType\": \"line\",\n" +
            "                \"lineSettings\": {\n" +
            "                  \"showLine\": true,\n" +
            "                  \"step\": false,\n" +
            "                  \"stepType\": \"start\",\n" +
            "                  \"smooth\": false,\n" +
            "                  \"lineType\": \"solid\",\n" +
            "                  \"lineWidth\": 2,\n" +
            "                  \"showPoints\": false,\n" +
            "                  \"showPointLabel\": false,\n" +
            "                  \"pointLabelPosition\": \"top\",\n" +
            "                  \"pointLabelFont\": {\n" +
            "                    \"size\": 11,\n" +
            "                    \"weight\": \"normal\",\n" +
            "                    \"style\": \"normal\",\n" +
            "                    \"family\": \"Roboto\"\n" +
            "                  },\n" +
            "                  \"pointLabelColor\": \"rgba(0, 0, 0, 0.76)\",\n" +
            "                  \"enablePointLabelBackground\": false,\n" +
            "                  \"pointLabelBackground\": \"rgba(255,255,255,0.56)\",\n" +
            "                  \"pointShape\": \"emptyCircle\",\n" +
            "                  \"pointSize\": 4,\n" +
            "                  \"fillAreaSettings\": {\n" +
            "                    \"type\": \"none\",\n" +
            "                    \"opacity\": 0.4,\n" +
            "                    \"gradient\": {\n" +
            "                      \"start\": 100,\n" +
            "                      \"end\": 0\n" +
            "                    }\n" +
            "                  }\n" +
            "                },\n" +
            "                \"barSettings\": {\n" +
            "                  \"showBorder\": false,\n" +
            "                  \"borderWidth\": 2,\n" +
            "                  \"borderRadius\": 0,\n" +
            "                  \"barWidth\": null,\n" +
            "                  \"showLabel\": false,\n" +
            "                  \"labelPosition\": \"top\",\n" +
            "                  \"labelFont\": {\n" +
            "                    \"size\": 11,\n" +
            "                    \"weight\": \"normal\",\n" +
            "                    \"style\": \"normal\",\n" +
            "                    \"family\": \"Roboto\"\n" +
            "                  },\n" +
            "                  \"labelColor\": \"rgba(0, 0, 0, 0.76)\",\n" +
            "                  \"enableLabelBackground\": false,\n" +
            "                  \"labelBackground\": \"rgba(255,255,255,0.56)\",\n" +
            "                  \"backgroundSettings\": {\n" +
            "                    \"type\": \"none\",\n" +
            "                    \"opacity\": 0.4,\n" +
            "                    \"gradient\": {\n" +
            "                      \"start\": 100,\n" +
            "                      \"end\": 0\n" +
            "                    }\n" +
            "                  }\n" +
            "                },\n" +
            "                \"comparisonSettings\": {\n" +
            "                  \"showValuesForComparison\": false,\n" +
            "                  \"comparisonValuesLabel\": \"\",\n" +
            "                  \"color\": \"\"\n" +
            "                },\n" +
            "                \"type\": \"TIME_SERIES_CHART\",\n" +
            "                \"yAxisId\": \"default\"\n" +
            "              }\n" +
            "            }\n" +
            "          ],\n" +
            "          \"latestDataKeys\": null,\n" +
            "          \"alarmFilterConfig\": {\n" +
            "            \"typeList\": null,\n" +
            "            \"statusList\": [\n" +
            "              \"ACTIVE\"\n" +
            "            ],\n" +
            "            \"severityList\": null,\n" +
            "            \"assigneeId\": null,\n" +
            "            \"searchPropagatedAlarms\": false\n" +
            "          }\n" +
            "        }\n" +
            "      ],\n" +
            "      \"margins\": null,\n" +
            "      \"paddings\": null,\n" +
            "      \"background\": null,\n" +
            "      \"borderWidth\": null,\n" +
            "      \"borderRadius\": null,\n" +
            "      \"borderColor\": null,\n" +
            "      \"widthType\": \"fitWidth\",\n" +
            "      \"customWidth\": 100,\n" +
            "      \"alignment\": \"center\",\n" +
            "      \"height\": 400,\n" +
            "      \"timewindow\": {\n" +
            "        \"history\": {\n" +
            "          \"historyType\": 0,\n" +
            "          \"timewindowMs\": 1800000\n" +
            "        },\n" +
            "        \"aggregation\": {\n" +
            "          \"type\": \"NONE\",\n" +
            "          \"limit\": 25000\n" +
            "        },\n" +
            "        \"timezone\": null\n" +
            "      },\n" +
            "      \"timeSeriesChartSettings\": {\n" +
            "        \"showTitle\": true,\n" +
            "        \"title\": \"Line chart\",\n" +
            "        \"titleFont\": {\n" +
            "          \"size\": 18,\n" +
            "          \"weight\": \"500\",\n" +
            "          \"style\": \"normal\",\n" +
            "          \"family\": \"Roboto\"\n" +
            "        },\n" +
            "        \"titleColor\": \"rgba(0, 0, 0, 0.87)\",\n" +
            "        \"titleAlignment\": \"center\",\n" +
            "        \"stack\": false,\n" +
            "        \"comparisonEnabled\": false,\n" +
            "        \"timeForComparison\": \"previousInterval\",\n" +
            "        \"comparisonCustomIntervalValue\": 7200000,\n" +
            "        \"showLegend\": true,\n" +
            "        \"legendColumnTitleFont\": {\n" +
            "          \"size\": 12,\n" +
            "          \"weight\": \"normal\",\n" +
            "          \"style\": \"normal\",\n" +
            "          \"family\": \"Roboto\"\n" +
            "        },\n" +
            "        \"legendColumnTitleColor\": \"rgba(0, 0, 0, 0.38)\",\n" +
            "        \"legendLabelFont\": {\n" +
            "          \"size\": 12,\n" +
            "          \"weight\": \"normal\",\n" +
            "          \"style\": \"normal\",\n" +
            "          \"family\": \"Roboto\"\n" +
            "        },\n" +
            "        \"legendLabelColor\": \"rgba(0, 0, 0, 0.76)\",\n" +
            "        \"legendValueFont\": {\n" +
            "          \"size\": 12,\n" +
            "          \"weight\": \"500\",\n" +
            "          \"style\": \"normal\",\n" +
            "          \"family\": \"Roboto\"\n" +
            "        },\n" +
            "        \"legendValueColor\": \"rgba(0, 0, 0, 0.87)\",\n" +
            "        \"thresholds\": [],\n" +
            "        \"grid\": {\n" +
            "          \"show\": false,\n" +
            "          \"backgroundColor\": null,\n" +
            "          \"borderWidth\": 1,\n" +
            "          \"borderColor\": \"#ccc\"\n" +
            "        },\n" +
            "        \"yAxes\": {\n" +
            "          \"default\": {\n" +
            "            \"show\": true,\n" +
            "            \"label\": \"\",\n" +
            "            \"labelFont\": {\n" +
            "              \"size\": 12,\n" +
            "              \"weight\": \"bold\",\n" +
            "              \"style\": \"normal\",\n" +
            "              \"family\": \"Roboto\"\n" +
            "            },\n" +
            "            \"labelColor\": \"rgba(0, 0, 0, 0.54)\",\n" +
            "            \"position\": \"left\",\n" +
            "            \"showTickLabels\": true,\n" +
            "            \"tickLabelFont\": {\n" +
            "              \"size\": 12,\n" +
            "              \"weight\": \"normal\",\n" +
            "              \"style\": \"normal\",\n" +
            "              \"family\": \"Roboto\"\n" +
            "            },\n" +
            "            \"tickLabelColor\": \"rgba(0, 0, 0, 0.54)\",\n" +
            "            \"showTicks\": true,\n" +
            "            \"ticksColor\": \"rgba(0, 0, 0, 0.54)\",\n" +
            "            \"showLine\": true,\n" +
            "            \"lineColor\": \"rgba(0, 0, 0, 0.54)\",\n" +
            "            \"showSplitLines\": true,\n" +
            "            \"splitLinesColor\": \"rgba(0, 0, 0, 0.12)\",\n" +
            "            \"id\": \"default\",\n" +
            "            \"order\": 0,\n" +
            "            \"units\": null,\n" +
            "            \"decimals\": 0,\n" +
            "            \"interval\": null,\n" +
            "            \"splitNumber\": null,\n" +
            "            \"min\": 0,\n" +
            "            \"max\": 120\n" +
            "          }\n" +
            "        },\n" +
            "        \"xAxis\": {\n" +
            "          \"show\": true,\n" +
            "          \"label\": \"\",\n" +
            "          \"labelFont\": {\n" +
            "            \"size\": 12,\n" +
            "            \"weight\": \"bold\",\n" +
            "            \"style\": \"normal\",\n" +
            "            \"family\": \"Roboto\"\n" +
            "          },\n" +
            "          \"labelColor\": \"rgba(0, 0, 0, 0.54)\",\n" +
            "          \"position\": \"bottom\",\n" +
            "          \"showTickLabels\": true,\n" +
            "          \"tickLabelFont\": {\n" +
            "            \"size\": 10,\n" +
            "            \"weight\": \"normal\",\n" +
            "            \"style\": \"normal\",\n" +
            "            \"family\": \"Roboto\"\n" +
            "          },\n" +
            "          \"tickLabelColor\": \"rgba(0, 0, 0, 0.54)\",\n" +
            "          \"showTicks\": true,\n" +
            "          \"ticksColor\": \"rgba(0, 0, 0, 0.54)\",\n" +
            "          \"showLine\": true,\n" +
            "          \"lineColor\": \"rgba(0, 0, 0, 0.54)\",\n" +
            "          \"showSplitLines\": true,\n" +
            "          \"splitLinesColor\": \"rgba(0, 0, 0, 0.12)\",\n" +
            "          \"ticksFormat\": {}\n" +
            "        },\n" +
            "        \"barWidthSettings\": null,\n" +
            "        \"noAggregationBarWidthSettings\": {\n" +
            "          \"strategy\": \"group\",\n" +
            "          \"groupWidth\": {\n" +
            "            \"relative\": true,\n" +
            "            \"relativeWidth\": 2,\n" +
            "            \"absoluteWidth\": 1000\n" +
            "          },\n" +
            "          \"barWidth\": {\n" +
            "            \"relative\": true,\n" +
            "            \"relativeWidth\": 2,\n" +
            "            \"absoluteWidth\": 1000\n" +
            "          }\n" +
            "        },\n" +
            "        \"states\": null,\n" +
            "        \"comparisonXAxis\": {\n" +
            "          \"show\": true,\n" +
            "          \"label\": \"\",\n" +
            "          \"labelFont\": {\n" +
            "            \"size\": 12,\n" +
            "            \"weight\": \"bold\",\n" +
            "            \"style\": \"normal\",\n" +
            "            \"family\": \"Roboto\"\n" +
            "          },\n" +
            "          \"labelColor\": \"rgba(0, 0, 0, 0.54)\",\n" +
            "          \"position\": \"top\",\n" +
            "          \"showTickLabels\": true,\n" +
            "          \"tickLabelFont\": {\n" +
            "            \"size\": 10,\n" +
            "            \"weight\": \"normal\",\n" +
            "            \"style\": \"normal\",\n" +
            "            \"family\": \"Roboto\"\n" +
            "          },\n" +
            "          \"tickLabelColor\": \"rgba(0, 0, 0, 0.54)\",\n" +
            "          \"showTicks\": true,\n" +
            "          \"ticksColor\": \"rgba(0, 0, 0, 0.54)\",\n" +
            "          \"showLine\": true,\n" +
            "          \"lineColor\": \"rgba(0, 0, 0, 0.54)\",\n" +
            "          \"showSplitLines\": true,\n" +
            "          \"splitLinesColor\": \"rgba(0, 0, 0, 0.12)\",\n" +
            "          \"ticksFormat\": {}\n" +
            "        },\n" +
            "        \"legendConfig\": {\n" +
            "          \"position\": \"top\",\n" +
            "          \"sortDataKeys\": false,\n" +
            "          \"showMin\": false,\n" +
            "          \"showMax\": false,\n" +
            "          \"showAvg\": true,\n" +
            "          \"showTotal\": false,\n" +
            "          \"showLatest\": false\n" +
            "        }\n" +
            "      },\n" +
            "      \"type\": \"TIME_SERIES_CHART\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"pageSize\": \"A4\",\n" +
            "  \"pageOrientation\": \"PORTRAIT\",\n" +
            "  \"pageMargins\": {\n" +
            "    \"left\": 20,\n" +
            "    \"right\": 20,\n" +
            "    \"top\": 20,\n" +
            "    \"bottom\": 20\n" +
            "  },\n" +
            "  \"pageBackground\": \"#fff\",\n" +
            "  \"header\": {\n" +
            "    \"enabled\": true,\n" +
            "    \"components\": [],\n" +
            "    \"firstPage\": {\n" +
            "      \"enabled\": false,\n" +
            "      \"components\": [],\n" +
            "      \"firstPage\": null\n" +
            "    }\n" +
            "  },\n" +
            "  \"footer\": {\n" +
            "    \"enabled\": true,\n" +
            "    \"components\": [],\n" +
            "    \"firstPage\": {\n" +
            "      \"enabled\": false,\n" +
            "      \"components\": [],\n" +
            "      \"firstPage\": null\n" +
            "    }\n" +
            "  }\n" +
            "}";
    @Autowired
    ReportTemplateService reportTemplateService;
    @Autowired
    ReportTemplateRepository reportTemplateRepository;
    @Autowired
    RelationService relationService;
    @Autowired
    SchedulerEventService schedulerEventService;

    private final IdComparator<ReportTemplateInfo> idComparator = new IdComparator<>();

    @Test
    public void testSaveReportTemplate() {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setTenantId(tenantId);
        reportTemplate.setName("My report");
        reportTemplate.setFormat(TbReportFormat.PDF);
        reportTemplate.setType(ReportTemplateType.REPORT);
        reportTemplate.setDescription("My report");
        reportTemplate.setConfiguration(PdfReportTemplateConfig.builder().components(Collections.emptyList()).build());
        ReportTemplate savedReportTemplate = reportTemplateService.saveReportTemplate(reportTemplate);

        Assert.assertNotNull(savedReportTemplate);
        Assert.assertNotNull(savedReportTemplate.getId());
        Assert.assertTrue(savedReportTemplate.getCreatedTime() > 0);
        Assert.assertEquals(reportTemplate.getTenantId(), savedReportTemplate.getTenantId());
        Assert.assertNotNull(savedReportTemplate.getCustomerId());
        Assert.assertEquals(NULL_UUID, savedReportTemplate.getCustomerId().getId());
        Assert.assertEquals(reportTemplate.getName(), savedReportTemplate.getName());

        savedReportTemplate.setName("My new report");

        reportTemplateService.saveReportTemplate(savedReportTemplate);
        ReportTemplate foundReportTemplate = reportTemplateService.findReportTemplateById(tenantId, savedReportTemplate.getId());
        Assert.assertEquals(foundReportTemplate.getName(), savedReportTemplate.getName());

        reportTemplateService.deleteReportTemplate(tenantId, savedReportTemplate.getId());
    }

    @Test
    public void testSaveReportTemplateWithEmptyName() {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setTenantId(tenantId);
        reportTemplate.setFormat(TbReportFormat.PDF);
        reportTemplate.setType(ReportTemplateType.REPORT);
        reportTemplate.setConfiguration(PdfReportTemplateConfig.builder().components(Collections.emptyList()).build());
        Assertions.assertThrows(DataValidationException.class, () -> reportTemplateService.saveReportTemplate(reportTemplate));
    }

    @Test
    public void testSaveReportTemplateWithEmptyFormat() {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setTenantId(tenantId);
        reportTemplate.setName("My report");
        reportTemplate.setFormat(TbReportFormat.PDF);
        reportTemplate.setConfiguration(PdfReportTemplateConfig.builder().components(Collections.emptyList()).build());
        Assertions.assertThrows(DataValidationException.class, () -> reportTemplateService.saveReportTemplate(reportTemplate));
    }

    @Test
    public void testSaveReportTemplateWithEmptyType() {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setTenantId(tenantId);
        reportTemplate.setName("My report");
        reportTemplate.setType(ReportTemplateType.REPORT);
        reportTemplate.setConfiguration(PdfReportTemplateConfig.builder().components(Collections.emptyList()).build());
        Assertions.assertThrows(DataValidationException.class, () -> reportTemplateService.saveReportTemplate(reportTemplate));
    }

    @Test
    public void testSaveReportTemplateWithNameContains0x00_thenDataValidationException() {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setTenantId(tenantId);
        reportTemplate.setConfiguration(new PdfReportTemplateConfig());
        reportTemplate.setName("F0929906\000\000\000\000\000\000\000\000\000");
        reportTemplate.setFormat(TbReportFormat.PDF);
        reportTemplate.setType(ReportTemplateType.REPORT);
        Assertions.assertThrows(DataValidationException.class, () -> reportTemplateService.saveReportTemplate(reportTemplate));
    }

    @Test
    public void testSaveReportTemplateWithEmptyTenant() {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setName("My report");
        reportTemplate.setFormat(TbReportFormat.PDF);
        reportTemplate.setType(ReportTemplateType.REPORT);
        reportTemplate.setConfiguration(PdfReportTemplateConfig.builder().components(Collections.emptyList()).build());
        Assertions.assertThrows(DataValidationException.class, () -> reportTemplateService.saveReportTemplate(reportTemplate));
    }

    @Test
    public void testSaveReportTemplateWithInvalidTenant() {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setName("My report");
        reportTemplate.setType(ReportTemplateType.REPORT);
        reportTemplate.setFormat(TbReportFormat.PDF);
        reportTemplate.setConfiguration(PdfReportTemplateConfig.builder().components(Collections.emptyList()).build());
        reportTemplate.setTenantId(TenantId.fromUUID(Uuids.timeBased()));
        Assertions.assertThrows(DataValidationException.class, () -> reportTemplateService.saveReportTemplate(reportTemplate));
    }

    @Test
    public void testFindReportTemplateById() {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setTenantId(tenantId);
        reportTemplate.setName("My report");
        reportTemplate.setFormat(TbReportFormat.PDF);
        reportTemplate.setType(ReportTemplateType.REPORT);
        reportTemplate.setConfiguration(PdfReportTemplateConfig.builder().components(Collections.emptyList()).build());
        ReportTemplate savedReportTemplate = reportTemplateService.saveReportTemplate(reportTemplate);
        ReportTemplate foundReportTemplate = reportTemplateService.findReportTemplateById(tenantId, savedReportTemplate.getId());
        Assert.assertNotNull(foundReportTemplate);
        Assert.assertEquals(savedReportTemplate, foundReportTemplate);
        reportTemplateService.deleteReportTemplate(tenantId, savedReportTemplate.getId());
    }

    @Test
    public void testDeserializeOldFormatReportTemplate() {
        ReportTemplateEntity entity = new ReportTemplateEntity();
        entity.setConfiguration(JacksonUtil.toJsonNode(OLD_FORMAT_REPORT_TEMPLATE));
        entity.setName("Test Report Template");
        entity.setTenantId(tenantId.getId());
        entity.setCustomerId(CustomerId.NULL_UUID);
        ReportTemplateEntity savedReportTemplate = reportTemplateRepository.save(entity);

        ReportTemplate foundReportTemplate = reportTemplateService.findReportTemplateById(tenantId, new ReportTemplateId(savedReportTemplate.getId()));
        ReportComponent reportComponent = foundReportTemplate.getConfiguration().getComponents().get(0);
        assertThat(reportComponent).isInstanceOf(TimeseriesChartComponent.class);
        TimeSeriesChartYAxisSettings yAxisSettings = ((TimeseriesChartComponent) reportComponent).getTimeSeriesChartSettings().getYAxes().get("default");
        assertThat(yAxisSettings.getMin().getValue()).isEqualTo(0L);
        assertThat(yAxisSettings.getMin().getType()).isEqualTo(ValueSourceType.constant);
        assertThat(yAxisSettings.getMax().getValue()).isEqualTo(120L);
        assertThat(yAxisSettings.getMax().getType()).isEqualTo(ValueSourceType.constant);
    }

    @Test
    public void testDeleteReportTemplate() {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setTenantId(tenantId);
        reportTemplate.setName("My report");
        reportTemplate.setFormat(TbReportFormat.PDF);
        reportTemplate.setType(ReportTemplateType.REPORT);
        reportTemplate.setConfiguration(PdfReportTemplateConfig.builder().components(Collections.emptyList()).build());
        ReportTemplate savedReportTemplate = reportTemplateService.saveReportTemplate(reportTemplate);
        EntityRelation relation = new EntityRelation(tenantId, savedReportTemplate.getId(), EntityRelation.CONTAINS_TYPE);
        relationService.saveRelation(tenantId, relation);

        ReportTemplate foundReportTemplate = reportTemplateService.findReportTemplateById(tenantId, savedReportTemplate.getId());
        Assert.assertNotNull(foundReportTemplate);
        reportTemplateService.deleteReportTemplate(tenantId, savedReportTemplate.getId());
        foundReportTemplate = reportTemplateService.findReportTemplateById(tenantId, savedReportTemplate.getId());
        Assert.assertNull(foundReportTemplate);
        Assert.assertTrue(relationService.findByTo(tenantId, savedReportTemplate.getId(), RelationTypeGroup.COMMON).isEmpty());
    }

    @Test
    public void testDeleteReportTemplateUsedInScheduler() {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setTenantId(tenantId);
        reportTemplate.setName("My report");
        reportTemplate.setFormat(TbReportFormat.PDF);
        reportTemplate.setType(ReportTemplateType.REPORT);
        reportTemplate.setConfiguration(PdfReportTemplateConfig.builder().components(Collections.emptyList()).build());
        ReportTemplate savedReportTemplate = reportTemplateService.saveReportTemplate(reportTemplate);

        SchedulerEvent schedulerEvent = new SchedulerEvent();
        schedulerEvent.setName("Report Scheduler Event");
        schedulerEvent.setType("generateReport");
        ObjectNode schedule = JacksonUtil.newObjectNode();
        schedule.put("startTime", System.currentTimeMillis() + 3000);
        schedule.put("timezone", "UTC");
        MonthlyRepeat schedulerRepeat = new MonthlyRepeat();
        schedule.set("repeat", JacksonUtil.valueToTree(schedulerRepeat));
        schedulerEvent.setSchedule(schedule);
        ReportConfig reportConfig = new ReportConfig();
        reportConfig.setReportTemplateId(savedReportTemplate.getId());
        reportConfig.setTimezone("Europe/Kiev");
        reportConfig.setUserId(new UserId(Uuids.random()));
        schedulerEvent.setConfiguration(JacksonUtil.valueToTree(reportConfig));
        schedulerEvent.setTenantId(tenantId);
        schedulerEventService.saveSchedulerEvent(schedulerEvent);

        Assertions.assertThrows(DataValidationException.class, () -> {
            reportTemplateService.deleteReportTemplate(tenantId, savedReportTemplate.getId());
        });
    }

    @Test
    public void testFindReportTemplatesByTenantId() {
        List<ReportTemplateInfo> reportTemplates = new ArrayList<>();
        for (int i = 0; i < 13; i++) {
            ReportTemplate reportTemplate = new ReportTemplate();
            reportTemplate.setTenantId(tenantId);
            reportTemplate.setName("ReportTemplate" + i);
            reportTemplate.setFormat(TbReportFormat.PDF);
            reportTemplate.setType(ReportTemplateType.REPORT);
            reportTemplate.setConfiguration(PdfReportTemplateConfig.builder().components(Collections.emptyList()).build());
            reportTemplates.add(new ReportTemplateInfo(reportTemplateService.saveReportTemplate(reportTemplate)));
        }

        List<ReportTemplateInfo> loadedReportTemplates = new ArrayList<>();
        PageLink pageLink = new PageLink(3);
        PageData<ReportTemplateInfo> pageData;
        do {
            pageData = reportTemplateService.findReportTemplates(tenantId, ReportTemplateQuery.builder().pageLink(pageLink).build());
            loadedReportTemplates.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        reportTemplates.sort(idComparator);
        loadedReportTemplates.sort(idComparator);

        Assert.assertEquals(reportTemplates, loadedReportTemplates);

        reportTemplateService.deleteReportTemplatesByTenantId(tenantId);

        pageLink = new PageLink(4);
        pageData = reportTemplateService.findReportTemplates(tenantId, ReportTemplateQuery.builder().pageLink(pageLink).build());
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());
    }

    @Test
    public void testFindReportTemplatesByTenantIdAndName() {
        String title1 = "Report title 1";
        List<ReportTemplateInfo> reportTemplatesTitle1 = new ArrayList<>();
        for (int i = 0; i < 13; i++) {
            ReportTemplate reportTemplate = new ReportTemplate();
            reportTemplate.setTenantId(tenantId);
            reportTemplate.setFormat(TbReportFormat.PDF);
            reportTemplate.setType(ReportTemplateType.REPORT);
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            reportTemplate.setName(name);
            reportTemplate.setConfiguration(PdfReportTemplateConfig.builder().components(Collections.emptyList()).build());
            reportTemplatesTitle1.add(new ReportTemplateInfo(reportTemplateService.saveReportTemplate(reportTemplate)));
        }
        String title2 = "Report title 2";
        List<ReportTemplateInfo> reportTemplatesTitle2 = new ArrayList<>();
        for (int i = 0; i < 17; i++) {
            ReportTemplate reportTemplate = new ReportTemplate();
            reportTemplate.setTenantId(tenantId);
            reportTemplate.setFormat(TbReportFormat.PDF);
            reportTemplate.setType(ReportTemplateType.REPORT);
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            reportTemplate.setName(name);
            reportTemplate.setConfiguration(PdfReportTemplateConfig.builder().components(Collections.emptyList()).build());
            reportTemplatesTitle2.add(new ReportTemplateInfo(reportTemplateService.saveReportTemplate(reportTemplate)));
        }

        List<ReportTemplateInfo> loadedReportTemplatesTitle1 = new ArrayList<>();
        PageLink pageLink = new PageLink(3, 0, title1);
        PageData<ReportTemplateInfo> pageData;
        do {
            pageData = reportTemplateService.findReportTemplates(tenantId, ReportTemplateQuery.builder().pageLink(pageLink).build());
            loadedReportTemplatesTitle1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        reportTemplatesTitle1.sort(idComparator);
        loadedReportTemplatesTitle1.sort(idComparator);

        Assert.assertEquals(reportTemplatesTitle1, loadedReportTemplatesTitle1);

        List<ReportTemplateInfo> loadedReportTemplatesTitle2 = new ArrayList<>();
        pageLink = new PageLink(4, 0, title2);
        do {
            pageData = reportTemplateService.findReportTemplates(tenantId, ReportTemplateQuery.builder().pageLink(pageLink).build());
            loadedReportTemplatesTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        reportTemplatesTitle2.sort(idComparator);
        loadedReportTemplatesTitle2.sort(idComparator);

        Assert.assertEquals(reportTemplatesTitle2, loadedReportTemplatesTitle2);

        for (ReportTemplateInfo reportTemplate : reportTemplatesTitle1) {
            reportTemplateService.deleteReportTemplate(tenantId, reportTemplate.getId());
        }

        pageLink = new PageLink(4, 0, title1);
        pageData = reportTemplateService.findReportTemplates(tenantId, ReportTemplateQuery.builder().pageLink(pageLink).build());
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (ReportTemplateInfo reportTemplate : reportTemplatesTitle2) {
            reportTemplateService.deleteReportTemplate(tenantId, reportTemplate.getId());
        }

        pageLink = new PageLink(4, 0, title2);
        pageData = reportTemplateService.findReportTemplates(tenantId, ReportTemplateQuery.builder().pageLink(pageLink).build());
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

}

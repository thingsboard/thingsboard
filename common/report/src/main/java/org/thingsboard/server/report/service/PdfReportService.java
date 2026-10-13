// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.service;

import com.google.common.util.concurrent.SettableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.dashboardreport.DashboardReportConfig;
import org.thingsboard.server.common.data.dashboardreport.DashboardReportData;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.task.ReportTask;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.TsValue;
import org.thingsboard.server.common.data.report.ReportData;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.DataSourceType;
import org.thingsboard.server.common.data.report.configuration.HeaderFooter;
import org.thingsboard.server.common.data.report.configuration.PdfReportTemplateConfig;
import org.thingsboard.server.common.data.report.configuration.chart.ColorRange;
import org.thingsboard.server.common.data.report.configuration.chart.ComparisonDuration;
import org.thingsboard.server.common.data.report.configuration.chart.DataKeyComparisonSettings;
import org.thingsboard.server.common.data.report.configuration.chart.ReportRangeChartSettings;
import org.thingsboard.server.common.data.report.configuration.chart.SimpleValueSourceConfig;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartKeySettings;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartThreshold;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartYAxisSettings;
import org.thingsboard.server.common.data.report.configuration.chart.ValueSourceConfig;
import org.thingsboard.server.common.data.report.configuration.chart.ValueSourceType;
import org.thingsboard.server.common.data.report.configuration.components.AlarmTableComponent;
import org.thingsboard.server.common.data.report.configuration.components.DashboardComponent;
import org.thingsboard.server.common.data.report.configuration.components.DataReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.ErrorComponent;
import org.thingsboard.server.common.data.report.configuration.components.ImageComponent;
import org.thingsboard.server.common.data.report.configuration.components.LatestChartComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.common.data.report.configuration.components.SplitViewComponent;
import org.thingsboard.server.common.data.report.configuration.components.SubReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.TimeseriesChartComponent;
import org.thingsboard.server.common.data.report.configuration.components.TimeseriesTableComponent;
import org.thingsboard.server.common.data.report.configuration.image.ImageSourceType;
import org.thingsboard.server.common.data.report.configuration.style.Insets;
import org.thingsboard.server.common.data.report.configuration.style.PageOrientation;
import org.thingsboard.server.common.data.report.configuration.style.PageSize;
import org.thingsboard.server.common.data.report.configuration.timewindow.History;
import org.thingsboard.server.common.data.report.configuration.timewindow.TimeIntervalCalculator;
import org.thingsboard.server.common.data.report.configuration.timewindow.TimeWindowConfiguration;
import org.thingsboard.server.report.context.ComponentData;
import org.thingsboard.server.report.context.HeaderFooterRenderLayout;
import org.thingsboard.server.report.context.TbReportCtx;
import org.thingsboard.server.report.context.chart.DataPostProcessFunction;
import org.thingsboard.server.report.context.chart.LatestChartData;
import org.thingsboard.server.report.context.chart.LatestChartDataSource;
import org.thingsboard.server.report.context.chart.TsChartData;
import org.thingsboard.server.report.context.chart.TsChartDataSource;
import org.thingsboard.server.report.context.chart.TsChartRangeItem;
import org.thingsboard.server.report.context.chart.TsChartThresholdItem;
import org.thingsboard.server.report.context.chart.YAxisScale;
import org.thingsboard.server.report.renderer.PdfReportComponentRenderer;
import org.thingsboard.server.report.util.ColorUtils;
import org.thingsboard.server.report.util.HtmlRenderUtils;
import org.thingsboard.server.report.util.NonProductionImageNotice;
import org.thingsboard.server.report.util.ThymeleafUtil;
import org.thingsboard.server.report.util.WebReportClient;
import org.w3c.dom.Document;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.awt.Dimension;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.thingsboard.server.common.data.report.configuration.chart.ReportComponentSubType.RANGE_CHART;
import static org.thingsboard.server.common.data.report.configuration.chart.ReportComponentSubType.STATE_CHART;
import static org.thingsboard.server.common.data.report.configuration.components.ReportComponentType.DASHBOARD;
import static org.thingsboard.server.common.data.report.configuration.components.ReportComponentType.ERROR;
import static org.thingsboard.server.common.data.report.configuration.components.ReportComponentType.SUB_REPORT;
import static org.thingsboard.server.common.data.report.configuration.components.ReportComponentType.TIME_SERIES_TABLE;
import static org.thingsboard.server.common.data.report.configuration.style.PageSize.A4;
import static org.thingsboard.server.common.data.report.configuration.timewindow.TimeIntervalCalculator.getComparisonTimeRange;
import static org.thingsboard.server.common.data.report.configuration.timewindow.TimeIntervalCalculator.getTimeRange;
import static org.thingsboard.server.common.data.util.DataSourceUtils.entityDataFromEntityId;
import static org.thingsboard.server.report.renderer.chart.ChartUtils.createValueFormatter;
import static org.thingsboard.server.report.util.NonProductionPdfNotice.addNonProductionNotice;
import static org.thingsboard.server.report.util.ReportQueryUtils.DEFAULT_TS_CHART_SORT_ORDER;
import static org.thingsboard.server.report.util.ReportQueryUtils.resolveAliasId;
import static org.thingsboard.server.report.util.ReportQueryUtils.toAlarmCountQuery;
import static org.thingsboard.server.report.util.ReportQueryUtils.toEntityCountQuery;
import static org.thingsboard.server.report.util.ReportUtils.collectThresholdItems;
import static org.thingsboard.server.report.util.ReportUtils.getMultipleDataSources;
import static org.thingsboard.server.report.util.ReportUtils.getSingleDataSource;
import static org.thingsboard.server.report.util.ReportUtils.prepareReportComponent;
import static org.thingsboard.server.report.util.ReportUtils.prepareReportName;
import static org.thingsboard.server.report.util.ReportUtils.updateDashboardReportStateParamsWithEntity;

@Service
@Slf4j
public class PdfReportService extends AbstractReportService {

    private final Map<ReportComponentType, PdfReportComponentRenderer<ReportComponent>> componentsRenderers = new EnumMap<>(ReportComponentType.class);
    private final WebReportClient webReportClient;

    private PdfReportService(List<PdfReportComponentRenderer> renderers, WebReportClient webReportClient) {
        renderers.forEach(renderer -> {
            ReportComponentType type = renderer.getType();
            if (type != null) {
                this.componentsRenderers.put(type, renderer);
            }
        });
        this.webReportClient = webReportClient;
    }

    public ReportData generateReport(ReportTask task, TbReportCtx ctx) throws Exception {
        TenantId tenantId = task.getTenantId();

        log.trace("[{}] Executing generateReport, reportRequest [{}]", tenantId, task);
        PdfReportTemplateConfig configuration = (PdfReportTemplateConfig) task.getReportTemplateConfig();

        Dimension pageSize = computePageSize(configuration);
        Insets pageMargins = computePageMargins(configuration);
        int usablePageWidthPx = (int)((pageSize.width - pageMargins.getLeft() - pageMargins.getRight()) * 4f / 3f);

        ITextRenderer renderer = HtmlRenderUtils.createRenderer(dataService, ctx, usablePageWidthPx);

        EntityData stateEntity = task.getOriginator() != null ? entityDataFromEntityId(task.getOriginator()) : null;

        HeaderFooterRenderLayout headerLayout = renderHeaderFooter(renderer, ctx, configuration.getHeader(), usablePageWidthPx, stateEntity);
        HeaderFooterRenderLayout footerLayout = renderHeaderFooter(renderer, ctx, configuration.getFooter(), usablePageWidthPx, stateEntity);

        Map<String, Object> reportVariables = new HashMap<>();
        fillPageLayoutVariables(reportVariables, configuration, headerLayout, footerLayout, pageSize, pageMargins);

        reportVariables.put("pageContent", renderContent(usablePageWidthPx, ctx, configuration.getComponents(), stateEntity));

        String renderedHtmlContent = ThymeleafUtil.renderFromHtmlTemplate("html/report-template", reportVariables);

        Document doc = HtmlRenderUtils.parseDom(renderedHtmlContent);
        renderer.setDocument(doc);
        renderer.layout();

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            renderer.createPDF(outputStream);
            byte[] reportBytes = addNonProductionNotice(outputStream.toByteArray(), ctx.isNonProduction());
            String reportName = prepareReportName(configuration.getNamePattern(), new Date(), task.getTimezone(), TbReportFormat.PDF.getExtension());

            return ReportData.builder()
                    .data(reportBytes)
                    .contentType(configuration.getFormat().getContentType())
                    .name(reportName)
                    .build();
        }
    }

    private HeaderFooterRenderLayout renderHeaderFooter(ITextRenderer renderer,
                                                        TbReportCtx ctx, HeaderFooter headerFooter,
                                                        int usablePageWidthPx, EntityData stateEntity) throws Exception {
        if (headerFooter == null) {
            return new HeaderFooterRenderLayout();
        }
        HeaderFooterRenderLayout headerFooterRenderLayout = new HeaderFooterRenderLayout();
        headerFooterRenderLayout.setEnabled(headerFooter.isEnabled());
        if (headerFooter.isEnabled()) {
            String htmlContent = renderContent(usablePageWidthPx, ctx, headerFooter.getComponents(), stateEntity);
            headerFooterRenderLayout.setHtmlContent(htmlContent);
            int heightPx = HtmlRenderUtils.measureHtmlHeight(renderer, htmlContent, usablePageWidthPx);
            headerFooterRenderLayout.setHeightPx(heightPx);
        }
        headerFooterRenderLayout.setFirstPageEnabled(headerFooter.getFirstPage() != null && headerFooter.getFirstPage().isEnabled());
        if (headerFooterRenderLayout.isFirstPageEnabled()) {
            String htmlContent = renderContent(usablePageWidthPx, ctx, headerFooter.getFirstPage().getComponents(), stateEntity);
            headerFooterRenderLayout.setFirstPageHtmlContent(htmlContent);
            int heightPx = HtmlRenderUtils.measureHtmlHeight(renderer, htmlContent, usablePageWidthPx);
            headerFooterRenderLayout.setFirstPageHeightPx(heightPx);
        }
        return headerFooterRenderLayout;
    }

    private String renderContent(int usablePageWidthPx, TbReportCtx ctx, List<ReportComponent> components, EntityData stateEntity) {
        StringBuilder content = new StringBuilder();
        EntityId stateEntityId = stateEntity != null ? stateEntity.getEntityId() : null;
        for (ReportComponent component : components) {
            prepareReportComponent(component);
            ReportComponentType type = component.getType();
            if (type == SUB_REPORT) {
                content.append(renderSubReport(usablePageWidthPx, ctx, (SubReportComponent)component, stateEntityId));
            } else if (type == DASHBOARD) {
                content.append(renderDashboard(usablePageWidthPx, ctx, stateEntityId, (DataReportComponent) component));
            } else if (type == TIME_SERIES_TABLE) {
                content.append(renderTimeseriesTables(usablePageWidthPx, ctx, stateEntityId, (TimeseriesTableComponent) component));
            } else {
                content.append(renderComponent(usablePageWidthPx, ctx, component, stateEntity));
            }
        }
        return content.toString();
    }

    private String renderComponent(int usablePageWidthPx, TbReportCtx ctx, ReportComponent component, EntityData stateEntity) {
        try {
            ComponentData componentData = getComponentData(usablePageWidthPx, ctx, component, stateEntity);
            return componentsRenderers.get(component.getType()).render(component, componentData);
        } catch (Exception e) {
            log.error("[{}] Failed to render component of type [{}]", ctx.getTenantId(), component.getType(), e);
            String componentSubType = component.getSubType() != null ? " [" + component.getSubType() + "]" : "";
            return renderError(usablePageWidthPx, "Failed to render component of type: "
                    + component.getType() + componentSubType, e);
        }
    }

    private ComponentData getComponentData(int usablePageWidthPx, TbReportCtx ctx, ReportComponent component, EntityData stateEntity) {
        ComponentData componentData = switch (component.getType()) {
            case TIME_SERIES_TABLE ->
                    buildTsComponentData(usablePageWidthPx, ctx, (TimeseriesTableComponent) component, stateEntity);
            case TIME_SERIES_CHART ->
                    buildTsChartComponentData(usablePageWidthPx, ctx, (TimeseriesChartComponent) component, stateEntity);
            case LATEST_CHART ->
                    buildLatestChartComponentData(usablePageWidthPx, ctx, (LatestChartComponent) component, stateEntity);
            case ALARM_TABLE ->
                    buildAlarmComponentData(usablePageWidthPx, ctx, (AlarmTableComponent) component, stateEntity);
            case DASHBOARD ->
                    buildDashboardComponentData(usablePageWidthPx, ctx, ((DashboardComponent) component), stateEntity);
            case SPLIT_VIEW -> buildSplitViewComponentData(usablePageWidthPx, ctx, (SplitViewComponent) component, stateEntity);
            case IMAGE -> buildImageComponentData(usablePageWidthPx, ctx, ((ImageComponent) component), stateEntity);
            default -> buildMultipleDataSourceData(usablePageWidthPx, ctx, component, stateEntity);
        };
        populateReportVars(componentData, ctx);
        return componentData;
    }

    private String renderTimeseriesTables(int usablePageWidthPx, TbReportCtx ctx, EntityId stateEntityId, DataReportComponent component) {
        StringBuilder content = new StringBuilder();
        Optional<DataSource> dataSource = getSingleDataSource(component);
        if (dataSource.isEmpty()) {
            return renderError(usablePageWidthPx, "Data source is not configured for time series table");
        }
        DataSource ds = dataSource.get();
        if (ds.getDataKeys().isEmpty()) {
            return renderError(usablePageWidthPx, "At least one time series column should be specified for time series table");
        }
        DataSource latestDataSource = DataSource.builder()
                .type(ds.getType())
                .deviceId(ds.getDeviceId())
                .entityAliasId(ds.getEntityAliasId())
                .filterId(ds.getFilterId())
                .dataKeys(ds.getLatestDataKeys()).build();
        List<EntityData> entityDatas = fetchEntities(ctx, latestDataSource, stateEntityId);
        for (EntityData entity : entityDatas) {
            content.append(renderComponent(usablePageWidthPx, ctx, component, entity));
        }
        return content.toString();
    }

    private String renderDashboard(int usablePageWidthPx, TbReportCtx ctx, EntityId stateEntityId, DataReportComponent component) {
        StringBuilder content = new StringBuilder();
        Optional<DataSource> dataSource = getSingleDataSource(component);
        List<EntityData> entityDatas;
        if (dataSource.isEmpty()) {
            entityDatas = new ArrayList<>();
            entityDatas.add(null);
        } else {
            DataSource dashboardDataSource = dataSource.get();
            entityDatas = fetchEntities(ctx, dashboardDataSource, stateEntityId);
        }
        for (EntityData entity : entityDatas) {
            content.append(renderComponent(usablePageWidthPx, ctx, component, entity));
        }
        return content.toString();
    }

    private String renderSubReport(int usablePageWidthPx, TbReportCtx ctx, DataReportComponent component, EntityId stateEntityId) {
        SubReportComponent subReportComponent = ((SubReportComponent) component);
        ReportTemplateId templateId = subReportComponent.getTemplateId();
        if (templateId == null) {
            return renderError(usablePageWidthPx, "Report template id is not configured for SubReport");
        }
        StringBuilder content = new StringBuilder();
        try {
            ReportTemplate reportTemplate = dataService.findReportTemplate(templateId, ctx);
            if (reportTemplate == null) {
                return renderError(usablePageWidthPx, "Template with id " + templateId + " not found. Please check the configuration.");
            }
            PdfReportTemplateConfig reportConfiguration = (PdfReportTemplateConfig) reportTemplate.getConfiguration();

            TbReportCtx subReportCtx = ctx.createSubReportCxt(reportConfiguration);
            List<EntityData> entities = getSubReportEntities(ctx, component, stateEntityId);
            for (EntityData entity : entities) {
                if (subReportComponent.isAvoidPageBreakInside()) {
                    content.append("<div class=\"no-page-break\">");
                }
                content.append(renderContent(usablePageWidthPx, subReportCtx, reportConfiguration.getComponents(), entity));
                if (subReportComponent.isAvoidPageBreakInside()) {
                    content.append("</div>");
                }
            }
            return content.toString();
        } catch (Exception e) {
            log.error("[{}] Failed to render Subreport, template id: {}", ctx.getTenantId(), templateId, e);
            return renderError(usablePageWidthPx, "Failed to render sub-report " + templateId, e);
        }
    }

    private String renderError(int usablePageWidthPx, String errorMessage) {
        return renderError(usablePageWidthPx, errorMessage, null);
    }

    private String renderError(int usablePageWidthPx, String errorMessage, Exception e) {
        if (e instanceof InterruptedException || ExceptionUtils.getRootCause(e) instanceof InterruptedException) {
            throw new RuntimeException(e);
        }
        return componentsRenderers.get(ERROR).render(new ErrorComponent(errorMessage, e), new ComponentData(usablePageWidthPx));
    }

    private ComponentData buildLatestChartComponentData(int usablePageWidthPx, TbReportCtx ctx, LatestChartComponent component, EntityData stateEntity) {
        Optional<DataSource> dataSource = getSingleDataSource(component);
        if (dataSource.isEmpty()) {
            return new ComponentData(usablePageWidthPx, "Data source is not configured for the chart [" + component.getSubType() + "]");
        }
        DataSource ds = dataSource.get();
        if (ds.getDataKeys().isEmpty()) {
            return new ComponentData(usablePageWidthPx, "At least one series should be specified for the chart [" + component.getSubType() + "]");
        }
        List<EntityData> entityDatas = fetchEntities(ctx, ds, stateEntity != null ? stateEntity.getEntityId() : null, DEFAULT_TS_CHART_SORT_ORDER);
        List<LatestChartDataSource> chartData = new ArrayList<>();
        int dataIndex = 0;
        DataPostProcessFunction dataPostProcessFunction = (dataKey, timestamp, value) -> this.postProcess(ctx, dataKey, timestamp, value, true);
        for (EntityData entity : entityDatas) {
            LatestChartDataSource chartDataSource = new LatestChartDataSource(ds, entity, dataPostProcessFunction, dataIndex);
            chartData.add(chartDataSource);
            dataIndex++;
        }
        int keyIndex = 0;

        for (LatestChartDataSource chartDataSource : chartData) {
            for (DataKey dataKey : chartDataSource.getDataKeys()) {
                if (chartDataSource.isGenerated()) {
                    dataKey.setColor(ColorUtils.getMaterialColor(keyIndex));
                }
                keyIndex++;
            }
        }
        LatestChartData latestChartData = new LatestChartData(chartData);
        Map<String, String> variables = new HashMap<>();
        if (stateEntity != null) {
            putEntityInfoData(stateEntity, variables);
        } else if (!entityDatas.isEmpty()) {
            putEntityInfoData(entityDatas.get(0), variables);
        }
        return new ComponentData(usablePageWidthPx, latestChartData, new HashMap<>(variables));
    }

    private ComponentData buildTsChartComponentData(int usablePageWidthPx, TbReportCtx ctx, TimeseriesChartComponent component, EntityData stateEntity) {
        Optional<DataSource> dataSource = getSingleDataSource(component);
        if (dataSource.isEmpty()) {
            return new ComponentData(usablePageWidthPx, "Data source is not configured for time series chart [" + component.getSubType() + "]");
        }
        DataSource ds = dataSource.get();
        if (ds.getDataKeys().isEmpty()) {
            return new ComponentData(usablePageWidthPx, "At least one series should be specified for time series chart [" + component.getSubType() + "]");
        }

        List<TimeSeriesChartThreshold> thresholds = null;
        boolean comparisonEnabled = false;
        if (component.getTimeSeriesChartSettings() != null) {
            thresholds = component.getTimeSeriesChartSettings().getThresholds();
            comparisonEnabled = component.getTimeSeriesChartSettings().getComparisonEnabled() != null ?
                    component.getTimeSeriesChartSettings().getComparisonEnabled() : false;
        }
        if (thresholds == null) {
            thresholds = new ArrayList<>();
        }
        thresholds = thresholds.stream().filter(ValueSourceConfig::isValidSource).toList();

        List<TsChartThresholdItem> thresholdItems = new ArrayList<>(thresholds.stream()
                .filter(t -> ValueSourceType.constant.equals(t.getType())).map(t -> new TsChartThresholdItem(t, t.getValue())).toList());

        DataSource latestDataSource = DataSource.builder()
                .type(ds.getType())
                .deviceId(ds.getDeviceId())
                .entityAliasId(ds.getEntityAliasId())
                .filterId(ds.getFilterId())
                .dataKeys(ds.getLatestDataKeys()).build();

        boolean singleEntity = RANGE_CHART == component.getSubType();

        List<EntityData> entityDatas = fetchEntities(ctx, latestDataSource, stateEntity != null ? stateEntity.getEntityId() : null, DEFAULT_TS_CHART_SORT_ORDER, singleEntity);

        List<TimeSeriesChartThreshold> latestKeyThresholds = thresholds.stream()
                .filter(t -> ValueSourceType.latestKey.equals(t.getType())).toList();

        List<TsChartThresholdItem> latestThresholdItems = collectThresholdItems(latestKeyThresholds, entityDatas, true);
        thresholdItems.addAll(latestThresholdItems);

        Map<String, List<TimeSeriesChartThreshold>> thresholdsByAlias = thresholds.stream().filter(t -> ValueSourceType.entity.equals(t.getType()))
                .collect(Collectors.groupingBy(TimeSeriesChartThreshold::getEntityAlias));

        thresholdsByAlias.forEach((alias, entityThresholds) -> {
            Optional<String> aliasId = resolveAliasId(ctx, alias);
            if (aliasId.isPresent()) {
                List<DataKey> dataKeys = entityThresholds.stream().map(TimeSeriesChartThreshold::toEntityDataKey).distinct().toList();
                DataSource thresholdDataSource = DataSource.builder().type(DataSourceType.ENTITY)
                        .entityAliasId(aliasId.get())
                        .dataKeys(dataKeys)
                        .build();
                List<EntityData> foundEntities = fetchEntities(ctx, thresholdDataSource, stateEntity != null ? stateEntity.getEntityId() : null, DEFAULT_TS_CHART_SORT_ORDER);
                List<TsChartThresholdItem> entityThresholdItems = collectThresholdItems(entityThresholds, foundEntities, false);
                thresholdItems.addAll(entityThresholdItems);
            }
        });

        List<TsChartRangeItem> rangeItems = new ArrayList<>();
        if (RANGE_CHART == component.getSubType()) {
            if (component.getTimeSeriesChartSettings() != null) {
                ReportRangeChartSettings rangeChartSettings = (ReportRangeChartSettings) component.getTimeSeriesChartSettings();
                List<ColorRange> colorRanges = rangeChartSettings.getRangeColors();
                if (colorRanges != null) {
                    int decimals = rangeChartSettings.getRangeDecimals() != null ? rangeChartSettings.getRangeDecimals() : 2;
                    String units = rangeChartSettings.getRangeUnits() != null ? rangeChartSettings.getRangeUnits() : "";
                    NumberFormat valueFormat = createValueFormatter(decimals, "");
                    rangeItems = TsChartRangeItem.toRangeItems(colorRanges, valueFormat);
                    if (rangeChartSettings.getShowRangeThresholds() == null || rangeChartSettings.getShowRangeThresholds()) {
                        TimeSeriesChartThreshold rangeThreshold = new TimeSeriesChartThreshold(rangeChartSettings.getRangeThreshold());
                        rangeThreshold.setType(ValueSourceType.constant);
                        rangeThreshold.setYAxisId("default");
                        rangeThreshold.setDecimals(rangeThreshold.getDecimals() != null ? rangeThreshold.getDecimals() : decimals);
                        rangeThreshold.setUnits(rangeThreshold.getUnits() != null ? rangeThreshold.getUnits() : units);
                        thresholdItems.addAll(
                            TsChartRangeItem.toMarkPoints(rangeItems).stream().map(item -> new TsChartThresholdItem(rangeThreshold, item)).toList()
                        );
                    }
                }
            }
        }

        List<TsChartDataSource> chartData = new ArrayList<>();
        List<DataKey> dataKeys = ds.getDataKeys();
        List<String> keys = dataKeys.stream().map(DataKey::getName).distinct().toList();

        TimeWindowConfiguration timeWindowConf = component.getTimewindow();
        History historyConf = timeWindowConf.getHistory();
        String targetTimezone = StringUtils.isNotBlank(timeWindowConf.getTimezone()) ?
                timeWindowConf.getTimezone() : ctx.getTimeZone();
        TimeIntervalCalculator.TimeRange timeRange = getTimeRange(timeWindowConf, targetTimezone);
        ZoneId zoneId = targetTimezone != null ? ZoneId.of(targetTimezone) : ZoneId.systemDefault();

        int dataIndex = 0;
        int startDataIndex = 0;
        boolean stateData = STATE_CHART == component.getSubType();
        DataPostProcessFunction dataPostProcessFunction = (dataKey, timestamp, value) -> this.postProcess(ctx, dataKey, timestamp, value, true);

        for (EntityData entity : entityDatas) {
            Aggregation aggregation = timeWindowConf.getAggregation().getType();
            if (stateData) {
                aggregation = Aggregation.NONE;
            }
            List<TsKvEntry> tsKvEntries = dataService.getTimeseries(entity.getEntityId(), keys, timeRange.startTs, timeRange.endTs,
                    historyConf.getInterval(), targetTimezone, aggregation, SortOrder.Direction.ASC,
                    timeWindowConf.getAggregation().getLimit(), false, ctx);
            if (stateData) {
                List<TsKvEntry> prevTsKvEntries = dataService.getTimeseries(entity.getEntityId(), keys, timeRange.startTs - TimeUnit.DAYS.toMillis(365), timeRange.startTs,
                        historyConf.getInterval(), targetTimezone, aggregation, SortOrder.Direction.DESC,
                        1, false, ctx);
                if (!prevTsKvEntries.isEmpty()) {
                    TsKvEntry prev = prevTsKvEntries.get(0);
                    if (tsKvEntries.isEmpty() || tsKvEntries.get(0).getTs() > timeRange.startTs) {
                        TsKvEntry startEntry = new BasicTsKvEntry(timeRange.startTs, prev);
                        tsKvEntries.add(0, startEntry);
                    }
                }
                if (!tsKvEntries.isEmpty() && tsKvEntries.get(tsKvEntries.size() - 1).getTs() < timeRange.endTs) {
                    TsKvEntry last = tsKvEntries.get(tsKvEntries.size() - 1);
                    TsKvEntry endEntry = new BasicTsKvEntry(timeRange.endTs, last);
                    tsKvEntries.add(endEntry);
                }
            }
            TsChartDataSource chartDataSource = new TsChartDataSource(ds, entity, tsKvEntries, timeRange,
                    historyConf.getInterval(), aggregation, zoneId, false, null, dataPostProcessFunction, dataIndex, startDataIndex);
            chartData.add(chartDataSource);
            dataIndex++;
            startDataIndex += chartDataSource.getDataKeys().size();
        }

        int keyIndex = 0;

        for (TsChartDataSource chartDataSource : chartData) {
            for (DataKey dataKey : chartDataSource.getDataKeys()) {
                if (chartDataSource.isGenerated()) {
                    dataKey.setColor(ColorUtils.getMaterialColor(keyIndex));
                }
                keyIndex++;
            }
        }
        TimeIntervalCalculator.TimeRange comparisonTimeRange = null;
        if (comparisonEnabled) {
            ComparisonDuration timeForComparison = ComparisonDuration.previousInterval;
            Long comparisonCustomIntervalValue = 7200000L;
            if (component.getTimeSeriesChartSettings() != null) {
                if (component.getTimeSeriesChartSettings().getTimeForComparison() != null) {
                    timeForComparison = component.getTimeSeriesChartSettings().getTimeForComparison();
                }
                if (component.getTimeSeriesChartSettings().getComparisonCustomIntervalValue() != null) {
                    comparisonCustomIntervalValue = component.getTimeSeriesChartSettings().getComparisonCustomIntervalValue();
                }
            }
            comparisonTimeRange = getComparisonTimeRange(timeRange, timeWindowConf,
                    targetTimezone, timeForComparison, comparisonCustomIntervalValue);

            List<DataKey> comparisionDataKeys = dataKeys.stream().filter(DataKey::isComparisonKey).toList();
            if (!comparisionDataKeys.isEmpty()) {
                List<String> comparisonKeys = comparisionDataKeys.stream().map(DataKey::getName).distinct().toList();
                List<TsChartDataSource> comparisonChartData = new ArrayList<>();
                for (EntityData entity : entityDatas) {
                    List<TsKvEntry> tsKvEntries = dataService.getTimeseries(entity.getEntityId(), comparisonKeys, comparisonTimeRange.startTs, comparisonTimeRange.endTs,
                            historyConf.getInterval(), targetTimezone, timeWindowConf.getAggregation().getType(), SortOrder.Direction.ASC,
                            timeWindowConf.getAggregation().getLimit(), false, ctx);

                    TsChartDataSource chartDataSource = new TsChartDataSource(ds, entity, tsKvEntries, comparisonTimeRange,
                            historyConf.getInterval(), timeWindowConf.getAggregation().getType(), zoneId, true, timeForComparison, dataPostProcessFunction, dataIndex, startDataIndex);
                    comparisonChartData.add(chartDataSource);
                    dataIndex++;
                    startDataIndex += chartDataSource.getDataKeys().size();
                }

                for (TsChartDataSource chartDataSource : comparisonChartData) {
                    for (DataKey dataKey : chartDataSource.getDataKeys()) {
                        String color = ColorUtils.getMaterialColor(keyIndex);
                        TimeSeriesChartKeySettings timeSeriesChartKeySettings = (TimeSeriesChartKeySettings)dataKey.getSettings();
                        DataKeyComparisonSettings comparisonSettings = timeSeriesChartKeySettings.getComparisonSettings();
                        if (StringUtils.isNotBlank(comparisonSettings.getColor())) {
                            color = comparisonSettings.getColor();
                        }
                        dataKey.setColor(color);
                        keyIndex++;
                    }
                }
                chartData.addAll(comparisonChartData);
            }
        }
        TimeZone timeZone = TimeZone.getTimeZone(zoneId.getId());

        Map<String, YAxisScale> yAxisScales = getYAxisScalesByYAxisId(ctx, component, stateEntity, entityDatas);
        TsChartData tsChartData = new TsChartData(timeZone, timeRange, Aggregation.NONE.equals(timeWindowConf.getAggregation().getType()),
                chartData, thresholdItems, yAxisScales, rangeItems, comparisonEnabled, comparisonTimeRange);
        Map<String, String> variables = new HashMap<>();
        if (stateEntity != null) {
            putEntityInfoData(stateEntity, variables);
        } else if (!entityDatas.isEmpty()) {
            putEntityInfoData(entityDatas.get(0), variables);
        }
        return new ComponentData(usablePageWidthPx, tsChartData, new HashMap<>(variables));
    }

    private Map<String, YAxisScale> getYAxisScalesByYAxisId(TbReportCtx ctx, TimeseriesChartComponent component, EntityData stateEntity, List<EntityData> entityDatas) {
        Collection<TimeSeriesChartYAxisSettings> yAxes =
                component.getTimeSeriesChartSettings().getYAxes().values();

        Map<String, List<SimpleValueSourceConfig>> yBoundaryValueSourcesByAlias = yAxes.stream()
                .flatMap(yAxis -> Stream.of(yAxis.getMin(), yAxis.getMax()))
                .filter(Objects::nonNull)
                .filter(cfg -> ValueSourceType.entity.equals(cfg.getType()) && cfg.getEntityAlias() != null)
                .collect(Collectors.groupingBy(SimpleValueSourceConfig::getEntityAlias));

        Map<String, List<EntityData>> yEntitySourceEntities = yBoundaryValueSourcesByAlias.entrySet().stream()
                .map(entry -> {
                    String alias = entry.getKey();
                    Optional<String> aliasId = resolveAliasId(ctx, alias);
                    if (aliasId.isEmpty()) {
                        return null;
                    }

                    List<DataKey> yDataKeys = entry.getValue().stream()
                            .map(SimpleValueSourceConfig::toEntityDataKey)
                            .distinct()
                            .toList();

                    DataSource yEntitySource = DataSource.builder()
                            .type(DataSourceType.ENTITY)
                            .entityAliasId(aliasId.get())
                            .dataKeys(yDataKeys)
                            .build();

                    List<EntityData> entities = fetchEntities(
                            ctx,
                            yEntitySource,
                            stateEntity != null ? stateEntity.getEntityId() : null,
                            DEFAULT_TS_CHART_SORT_ORDER
                    );

                    return Map.entry(alias, entities);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Map<String, YAxisScale> yAxisScales = yAxes.stream()
                .collect(Collectors.toMap(
                        TimeSeriesChartYAxisSettings::getId,
                        yAxisSettings -> {
                            Double minValue = fetchYAxisBound(yAxisSettings.getMin(), entityDatas, yEntitySourceEntities);
                            Double maxValue = fetchYAxisBound(yAxisSettings.getMax(), entityDatas, yEntitySourceEntities);
                            return new YAxisScale(minValue, maxValue);
                        }
                ));
        return yAxisScales;
    }

    private Double fetchYAxisBound(SimpleValueSourceConfig config, List<EntityData> entityDatas, Map<String, List<EntityData>> yEntitySourceEntities) {
        if (config == null || config.getType() == null) {
            return null;
        }
        return switch (config.getType()) {
            case constant -> config.getValue();
            case latestKey -> getEntityKeyDoubleValue(config, entityDatas, true);
            case entity -> {
                String entityAlias = config.getEntityAlias();
                List<EntityData> entityDataList = yEntitySourceEntities.get(entityAlias);
                if (entityDataList != null) {
                    yield getEntityKeyDoubleValue(config, entityDataList, false);
                }
                yield null;
            }
        };
    }

    private Double getEntityKeyDoubleValue(SimpleValueSourceConfig config, List<EntityData> entityDatas, boolean latestElseEntityKey) {
        if (entityDatas == null || entityDatas.isEmpty()) {
            return null;
        }
        EntityData entity = entityDatas.get(0);
        Map<EntityKeyType, Map<String, TsValue>> latestValues = entity.getLatest();
        Double value = null;
        EntityKeyType keyType = EntityKeyType.fromName(latestElseEntityKey ? config.getLatestKeyType() : config.getEntityKeyType());
        Map<String, TsValue> valuesByType = latestValues.get(keyType);
        if (valuesByType != null) {
            TsValue tsValue = valuesByType.get(latestElseEntityKey ? config.getLatestKey() : config.getEntityKey());
            if (tsValue != null) {
                try {
                    value = Double.parseDouble(tsValue.getValue());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return value;
    }

    private ComponentData buildImageComponentData(int usablePageWidthPx, TbReportCtx ctx, ImageComponent component, EntityData stateEntity) {
        if (ImageSourceType.ENTITY_KEY == component.getSourceType()) {
            Optional<DataSource> dataSource = getSingleDataSource(component);
            if (dataSource.isEmpty()) {
                return new ComponentData(usablePageWidthPx);
            }
            return buildSingleComponentData(usablePageWidthPx, ctx, dataSource.get(), stateEntity != null ? stateEntity.getEntityId() : null);
        } else {
            return new ComponentData(usablePageWidthPx);
        }
    }

    private ComponentData buildSingleComponentData(int usablePageWidthPx, TbReportCtx ctx, DataSource dataSource, EntityId stateEntityId) {
        return switch (dataSource.getType()) {
            case DEVICE, ENTITY -> buildEntityDataSource(usablePageWidthPx, ctx, dataSource, stateEntityId);
            case ENTITY_COUNT -> buildEntityCountDataSource(usablePageWidthPx, ctx, dataSource, stateEntityId);
            case ALARM_COUNT -> buildAlarmCountDataSource(usablePageWidthPx, ctx, dataSource, stateEntityId);
            default -> throw new IllegalArgumentException("Unknown data source type: " + dataSource.getType());
        };
    }

    private ComponentData buildEntityDataSource(int usablePageWidthPx, TbReportCtx ctx, DataSource dataSource, EntityId stateEntityId) {
        List<Map<String, String>> entityDatas = collectEntityDatas(ctx, dataSource, stateEntityId);
        Map<String, Object> variables = new HashMap<>();
        if (stateEntityId == null && !entityDatas.isEmpty()) {
            variables.putAll(entityDatas.get(0));
        }
        return new ComponentData(usablePageWidthPx, dataSource, entityDatas, variables);
    }

    private ComponentData buildEntityCountDataSource(int usablePageWidthPx, TbReportCtx ctx, DataSource dataSource, EntityId stateEntityId) {
        Map<String, Object> map = new HashMap<>();
        String label = resolveSingleLabel(dataSource, "count");
        map.put(label, dataService.countEntitiesByQuery(toEntityCountQuery(dataSource, ctx, stateEntityId), ctx));
        return new ComponentData(usablePageWidthPx, map);
    }

    private ComponentData buildAlarmCountDataSource(int usablePageWidthPx, TbReportCtx ctx, DataSource dataSource, EntityId stateEntityId) {
        Map<String, Object> map = new HashMap<>();
        String label = resolveSingleLabel(dataSource, "count");
        map.put(label, dataService.countAlarmsByQuery(toAlarmCountQuery(dataSource, ctx, stateEntityId), ctx));
        return new ComponentData(usablePageWidthPx, map);
    }

    private String resolveSingleLabel(DataSource dataSource, String fallback) {
        String label = null;
        if (dataSource.getDataKeys() != null && !dataSource.getDataKeys().isEmpty()) {
            label = dataSource.getDataKeys().get(0).getLabel();
        }
        if (StringUtils.isNotBlank(label)) {
            return label;
        }
        return fallback;
    }

    private ComponentData buildDashboardComponentData(int usablePageWidthPx, TbReportCtx ctx, DashboardComponent component, EntityData stateEntity) {
        if (component.getConfig() == null) {
            return new ComponentData(usablePageWidthPx, "Dashboard report config is empty");
        }
        if (StringUtils.isBlank(component.getConfig().getBaseUrl())) {
            return new ComponentData(usablePageWidthPx, "Base URL is not configured for dashboard report");
        }
        if (StringUtils.isBlank(component.getConfig().getDashboardId())) {
            return new ComponentData(usablePageWidthPx, "Dashboard id is not configured for dashboard report");
        }
        SettableFuture<DashboardReportData> futureToSet = SettableFuture.create();
        DashboardReportConfig config = component.getConfig();
        config.setType("png");
        if (stateEntity != null) {
            config.setState(updateDashboardReportStateParamsWithEntity(config.getState(), stateEntity));
        }
        webReportClient.requestDashboardReport(config, null,
                ctx.getAccessToken(), ctx.getAccessTokenExpTs(),
                futureToSet::set, error -> {
                    log.error("[{}] Failed to generate dashboard report", ctx.getTenantId(), error);
                    futureToSet.setException(error);
                });
        try {
            // The capture is embedded as raw pixels, so the page-level stamp applied in generateReport does not
            // mark the bitmap itself: cropping the page, or pulling the image out with any PDF tool, would
            // otherwise yield a clean, shareable dashboard picture.
            byte[] dashboardImage = NonProductionImageNotice.addNonProductionNotice(futureToSet.get().getData(),
                    NonProductionImageNotice.PNG_IMAGE_FORMAT, ctx.isNonProduction());
            return new ComponentData(usablePageWidthPx, dashboardImage);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ComponentData buildSplitViewComponentData(int usablePageWidthPx, TbReportCtx ctx, SplitViewComponent component, EntityData stateEntity) {
        float splitPosition = component.getSplitPosition();
        float splitGap = component.getSplitGap() * 4f / 3f;
        int leftWidth = (int)(usablePageWidthPx * splitPosition / 100 - splitGap / 2f);
        int rightWidth = (int)(usablePageWidthPx * (100 - splitPosition) / 100 - splitGap / 2f);
        int centerWidth = (int)splitGap;
        String leftContent = "";
        if (component.getLeftView() != null) {
            leftContent = this.renderContent(leftWidth, ctx, Collections.singletonList(component.getLeftView()), stateEntity);
        }
        String rightContent = "";
        if (component.getRightView() != null) {
            rightContent = this.renderContent(rightWidth, ctx, Collections.singletonList(component.getRightView()), stateEntity);
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put("leftWidth", leftWidth);
        variables.put("rightWidth", rightWidth);
        variables.put("centerWidth", centerWidth);
        variables.put("leftContent", leftContent);
        variables.put("rightContent", rightContent);
        return new ComponentData(usablePageWidthPx, variables);
    }

    private ComponentData buildMultipleDataSourceData(int usablePageWidthPx, TbReportCtx ctx, ReportComponent component, EntityData stateEntity) {
        List<DataSource> dataSources = null;
        if (component instanceof DataReportComponent) {
            dataSources = getMultipleDataSources((DataReportComponent) component);
        }
        if (dataSources == null || dataSources.isEmpty()) {
            return new ComponentData(usablePageWidthPx);
        }
        Map<String, String> variables = new HashMap<>();
        if (stateEntity != null) {
            putEntityInfoData(stateEntity, variables);
        }
        ComponentData mainDataSource = new ComponentData(usablePageWidthPx,null, new ArrayList<>(), new HashMap<>(variables));
        for (DataSource dataSource : dataSources) {
            ComponentData singleDataSource = buildSingleComponentData(usablePageWidthPx, ctx, dataSource, stateEntity != null ? stateEntity.getEntityId() : null);
            mainDataSource.merge(singleDataSource);
        }
        return mainDataSource;
    }

    private Dimension computePageSize(PdfReportTemplateConfig config) {
        PageSize pageSize = Optional.ofNullable(config.getPageSize()).orElse(A4);
        return config.getPageOrientation() == PageOrientation.LANDSCAPE
                ? new Dimension(pageSize.getHeight(), pageSize.getWidth())
                : new Dimension(pageSize.getWidth(), pageSize.getHeight());
    }

    private Insets computePageMargins(PdfReportTemplateConfig configuration) {
        if (configuration.getPageMargins() != null) {
            return configuration.getPageMargins();
        } else {
            return new Insets(20, 20, 20, 20);
        }
    }

    private void fillPageLayoutVariables(Map<String, Object> reportVariables,
                                         PdfReportTemplateConfig configuration,
                                         HeaderFooterRenderLayout headerLayout,
                                         HeaderFooterRenderLayout footerLayout,
                                         Dimension pageSize, Insets pageMargins) {
        reportVariables.put("pageWidth", pageSize.getWidth() + "pt" );
        reportVariables.put("pageHeight", pageSize.getHeight() + "pt" );
        reportVariables.put("pageMarginLeft", pageMargins.getLeft() + "pt");
        reportVariables.put("pageMarginRight", pageMargins.getRight() + "pt");

        String pageBackground = configuration.getPageBackground() != null ? ColorUtils.normalizeCssColor(configuration.getPageBackground()) : "#fff";
        reportVariables.put("pageBackground", pageBackground);

        int minContentHeight = 100;

        int minHalfPageContentHeight = Math.max((pageSize.height - pageMargins.getTop() - pageMargins.getBottom() - minContentHeight) / 2, 0);
        int maxTopMargin = pageMargins.getTop() + minHalfPageContentHeight;
        int maxBottomMargin = pageMargins.getBottom() + minHalfPageContentHeight;

        int pageMarginTop = this.fillHeaderFooterVariables(reportVariables, headerLayout, pageMargins, maxTopMargin, true);
        reportVariables.put("pageMarginTop", pageMarginTop + "pt");

        int pageMarginBottom = this.fillHeaderFooterVariables(reportVariables, footerLayout, pageMargins, maxBottomMargin, false);
        reportVariables.put("pageMarginBottom", pageMarginBottom + "pt");
    }

    private int fillHeaderFooterVariables(Map<String, Object> reportVariables,
                                          HeaderFooterRenderLayout headerFooterLayout,
                                          Insets pageMargins,
                                          int maxMargin,
                                          boolean headerElseFooter) {
        String prefix = headerElseFooter ? "Header" : "Footer";
        String marginPrefix = headerElseFooter ? "Top" : "Bottom";
        reportVariables.put("enable" + prefix, headerFooterLayout.isEnabled());
        int startMargin = headerElseFooter ? pageMargins.getTop() : pageMargins.getBottom();
        int margin = startMargin;
        reportVariables.put("page" + prefix + "Padding", startMargin + "pt");
        if (headerFooterLayout.isEnabled()) {
            reportVariables.put("page" + prefix, headerFooterLayout.getHtmlContent());
            margin = Math.min((int)(startMargin + headerFooterLayout.getHeightPx() * 3f / 4f), maxMargin);
            int height = margin - startMargin;
            reportVariables.put("page" + prefix + "Height", height + "pt");
        }
        reportVariables.put("enableFirstPage" + prefix, headerFooterLayout.isFirstPageEnabled());
        if (headerFooterLayout.isFirstPageEnabled()) {
            int firstPageMargin = Math.min((int)(startMargin + headerFooterLayout.getFirstPageHeightPx() * 3f / 4f), maxMargin);
            reportVariables.put("firstPageMargin" + marginPrefix, firstPageMargin + "pt");
            reportVariables.put("firstPage" + prefix, headerFooterLayout.getFirstPageHtmlContent());
            int height = firstPageMargin - startMargin;
            reportVariables.put("firstPage" + prefix + "Height", height + "pt");
        }
        return margin;
    }

    @Override
    public TbReportFormat getFormat() {
        return TbReportFormat.PDF;
    }

}

// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.dashboardreport.DashboardReportConfig;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.HeaderFooter;
import org.thingsboard.server.common.data.report.configuration.PdfReportTemplateConfig;
import org.thingsboard.server.common.data.report.configuration.ReportTemplateConfig;
import org.thingsboard.server.common.data.report.configuration.components.DashboardComponent;
import org.thingsboard.server.common.data.report.configuration.components.HeadingComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.SplitViewComponent;
import org.thingsboard.server.common.data.report.configuration.components.SubReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.TimeseriesChartComponent;
import org.thingsboard.server.common.data.report.configuration.timewindow.AggregationConfiguration;
import org.thingsboard.server.common.data.report.configuration.timewindow.FixedTimeWindow;
import org.thingsboard.server.common.data.report.configuration.timewindow.History;
import org.thingsboard.server.common.data.report.configuration.timewindow.Interval;
import org.thingsboard.server.common.data.report.configuration.timewindow.QuickTimeInterval;
import org.thingsboard.server.common.data.report.configuration.timewindow.TimeWindowConfiguration;
import org.thingsboard.server.report.context.TbReportCtx;
import org.thingsboard.server.report.util.ReportTimeRangeResolver.ReportTimeRange;
import org.thingsboard.server.report.datasource.ReportDataService;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReportTimeRangeResolverTest {

    private static final String UTC = "UTC";
    private static final long MINUTE_MS = 60 * 1000L;
    private static final long HOUR_MS = 60 * MINUTE_MS;
    private static final long DAY_MS = 24 * HOUR_MS;

    private ReportDataService dataService;
    private ReportTimeRangeResolver resolver;
    private TbReportCtx ctx;

    @BeforeEach
    void setUp() {
        dataService = mock(ReportDataService.class);
        resolver = new ReportTimeRangeResolver(dataService);
        ctx = mock(TbReportCtx.class);
        when(ctx.getTimeZone()).thenReturn(UTC);
    }

    @Test
    void testResolveFixedTimeWindow() {
        ReportTimeRange range = resolver.resolve(config(chart(fixedWindow(1000, 2000))), ctx);

        assertThat(range.startTs()).isEqualTo(1000);
        assertThat(range.endTs()).isEqualTo(2000);
    }

    @Test
    void testResolveLastIntervalTimeWindow() {
        long before = System.currentTimeMillis();
        ReportTimeRange range = resolver.resolve(config(chart(lastIntervalWindow(DAY_MS))), ctx);
        long after = System.currentTimeMillis();

        assertThat(range.endTs()).isBetween(before, after);
        assertThat(range.endTs() - range.startTs()).isEqualTo(DAY_MS);
    }

    @Test
    void testResolveQuickIntervalTimeWindow() {
        long yesterdayStart = LocalDate.now(ZoneOffset.UTC).minusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();

        ReportTimeRange range = resolver.resolve(config(chart(quickWindow(QuickTimeInterval.YESTERDAY))), ctx);

        assertThat(range.startTs()).isEqualTo(yesterdayStart);
        assertThat(range.endTs()).isEqualTo(yesterdayStart + DAY_MS - 1);
    }

    @Test
    void testResolveForAllTimeWindowIsUndefined() {
        ReportTimeRange range = resolver.resolve(config(chart(forAllTimeWindow())), ctx);

        assertUndefined(range);
    }

    @Test
    void testResolveWithoutAnyTimeWindowIsUndefined() {
        assertUndefined(resolver.resolve(config(new HeadingComponent()), ctx));
    }

    @Test
    void testResolveEqualTimeWindowsOfSeveralComponents() {
        ReportTemplateConfig config = config(chart(fixedWindow(1000, 2000)), chart(fixedWindow(1000, 2000)));

        ReportTimeRange range = resolver.resolve(config, ctx);

        assertThat(range.startTs()).isEqualTo(1000);
        assertThat(range.endTs()).isEqualTo(2000);
    }

    @Test
    void testResolveDifferentTimeWindowsIsUndefined() {
        ReportTemplateConfig config = config(chart(fixedWindow(1000, 2000)), chart(fixedWindow(1000, 3000)));

        assertUndefined(resolver.resolve(config, ctx));
    }

    @Test
    void testResolveTakesDataKeyTimeWindowIntoAccount() {
        TimeseriesChartComponent chart = chart(fixedWindow(1000, 2000));
        chart.setDataSources(List.of(dataSource(dataKey(fixedWindow(1000, 3000)))));

        assertUndefined(resolver.resolve(config(chart), ctx));
    }

    @Test
    void testResolveTakesSplitViewTimeWindowIntoAccount() {
        SplitViewComponent splitView = new SplitViewComponent();
        splitView.setLeftView(chart(fixedWindow(1000, 2000)));
        splitView.setRightView(chart(fixedWindow(1000, 3000)));

        assertUndefined(resolver.resolve(config(splitView), ctx));
    }

    @Test
    void testResolveFallsBackToContextTimezoneBeforeDeduplication() {
        TimeWindowConfiguration withContextTimezone = quickWindow(QuickTimeInterval.CURRENT_DAY);
        TimeWindowConfiguration withExplicitTimezone = quickWindow(QuickTimeInterval.CURRENT_DAY);
        withExplicitTimezone.setTimezone(UTC);

        ReportTimeRange range = resolver.resolve(config(chart(withContextTimezone), chart(withExplicitTimezone)), ctx);

        long todayStart = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        assertThat(range.startTs()).isEqualTo(todayStart);
    }

    @Test
    void testResolveIgnoresAggregationDifferenceOfEqualTimeWindows() {
        TimeWindowConfiguration avgWindow = fixedWindow(1000, 2000);
        avgWindow.setAggregation(aggregation(Aggregation.AVG));
        TimeWindowConfiguration sumWindow = fixedWindow(1000, 2000);
        sumWindow.setAggregation(aggregation(Aggregation.SUM));

        ReportTimeRange range = resolver.resolve(config(chart(avgWindow), chart(sumWindow)), ctx);

        assertThat(range.startTs()).isEqualTo(1000);
        assertThat(range.endTs()).isEqualTo(2000);
    }

    @Test
    void testResolveIgnoresResidualFieldsOfOtherHistoryTypes() {
        // the UI keeps the defaults of every history type in one object, so a last interval window carries
        // a fixed timewindow stamped when the component was created, and a quick interval left over from the defaults
        TimeWindowConfiguration olderWindow = lastIntervalWindow(DAY_MS);
        olderWindow.getHistory().setFixedTimewindow(fixedTimeWindow(1000, 2000));
        olderWindow.getHistory().setQuickInterval(QuickTimeInterval.CURRENT_DAY);
        olderWindow.getHistory().setInterval(Interval.of(MINUTE_MS));
        TimeWindowConfiguration newerWindow = lastIntervalWindow(DAY_MS);
        newerWindow.getHistory().setFixedTimewindow(fixedTimeWindow(5000, 6000));
        newerWindow.getHistory().setQuickInterval(QuickTimeInterval.CURRENT_WEEK);
        newerWindow.getHistory().setInterval(Interval.of(HOUR_MS));

        ReportTimeRange range = resolver.resolve(config(chart(olderWindow), chart(newerWindow)), ctx);

        assertThat(range.endTs() - range.startTs()).isEqualTo(DAY_MS);
    }

    @Test
    void testResolveDifferentTimezonesOfEqualTimeWindowsIsUndefined() {
        TimeWindowConfiguration utcWindow = quickWindow(QuickTimeInterval.CURRENT_DAY);
        utcWindow.setTimezone(UTC);
        TimeWindowConfiguration kyivWindow = quickWindow(QuickTimeInterval.CURRENT_DAY);
        kyivWindow.setTimezone("Europe/Kyiv");

        assertUndefined(resolver.resolve(config(chart(utcWindow), chart(kyivWindow)), ctx));
    }

    @Test
    void testResolveTakesSubReportTimeWindowIntoAccount() {
        ReportTemplateId templateId = new ReportTemplateId(UUID.randomUUID());
        givenSubReportTemplate(templateId, config(chart(fixedWindow(1000, 3000))));

        ReportTemplateConfig config = config(chart(fixedWindow(1000, 2000)), subReport(templateId));

        assertUndefined(resolver.resolve(config, ctx));
    }

    @Test
    void testResolveEqualTimeWindowOfSubReport() {
        ReportTemplateId templateId = new ReportTemplateId(UUID.randomUUID());
        givenSubReportTemplate(templateId, config(chart(fixedWindow(1000, 2000))));

        ReportTimeRange range = resolver.resolve(config(chart(fixedWindow(1000, 2000)), subReport(templateId)), ctx);

        assertThat(range.startTs()).isEqualTo(1000);
        assertThat(range.endTs()).isEqualTo(2000);
    }

    @Test
    void testResolveMissingSubReportTemplateIsUndefined() throws Exception {
        ReportTemplateId templateId = new ReportTemplateId(UUID.randomUUID());
        when(dataService.findReportTemplate(eq(templateId), any())).thenReturn(null);

        ReportTemplateConfig config = config(chart(fixedWindow(1000, 2000)), subReport(templateId));

        assertThatCode(() -> assertUndefined(resolver.resolve(config, ctx))).doesNotThrowAnyException();
    }

    @Test
    void testResolveFailingSubReportTemplateFetchIsUndefined() throws Exception {
        ReportTemplateId templateId = new ReportTemplateId(UUID.randomUUID());
        when(dataService.findReportTemplate(eq(templateId), any())).thenThrow(new RuntimeException("failed to fetch"));

        ReportTemplateConfig config = config(chart(fixedWindow(1000, 2000)), subReport(templateId));

        assertThatCode(() -> assertUndefined(resolver.resolve(config, ctx))).doesNotThrowAnyException();
    }

    @Test
    void testResolveSubReportCycle() {
        ReportTemplateId templateId = new ReportTemplateId(UUID.randomUUID());
        givenSubReportTemplate(templateId, config(chart(fixedWindow(1000, 2000)), subReport(templateId)));

        ReportTimeRange range = resolver.resolve(config(subReport(templateId)), ctx);

        assertThat(range.startTs()).isEqualTo(1000);
        assertThat(range.endTs()).isEqualTo(2000);
    }

    @Test
    void testResolveIgnoresDashboardTimeWindow() {
        ReportTemplateConfig config = config(chart(fixedWindow(1000, 2000)), dashboard(DAY_MS));

        ReportTimeRange range = resolver.resolve(config, ctx);

        assertThat(range.startTs()).isEqualTo(1000);
        assertThat(range.endTs()).isEqualTo(2000);
    }

    @Test
    void testResolveTakesEnabledHeaderTimeWindowIntoAccount() {
        ReportTemplateConfig config = config(headerFooter(true, chart(fixedWindow(1000, 3000))), null,
                chart(fixedWindow(1000, 2000)));

        assertUndefined(resolver.resolve(config, ctx));
    }

    @Test
    void testResolveIgnoresDisabledHeaderTimeWindow() {
        ReportTemplateConfig config = config(headerFooter(false, chart(fixedWindow(1000, 3000))), null,
                chart(fixedWindow(1000, 2000)));

        ReportTimeRange range = resolver.resolve(config, ctx);

        assertThat(range.startTs()).isEqualTo(1000);
        assertThat(range.endTs()).isEqualTo(2000);
    }

    @Test
    void testResolveIgnoresDisabledFooterTimeWindow() {
        ReportTemplateConfig config = config(null, headerFooter(false, chart(fixedWindow(1000, 3000))),
                chart(fixedWindow(1000, 2000)));

        ReportTimeRange range = resolver.resolve(config, ctx);

        assertThat(range.startTs()).isEqualTo(1000);
        assertThat(range.endTs()).isEqualTo(2000);
    }

    @Test
    void testResolveTakesEnabledFirstPageHeaderTimeWindowIntoAccount() {
        HeaderFooter header = headerFooter(false, chart(fixedWindow(1000, 4000)));
        header.setFirstPage(headerFooter(true, chart(fixedWindow(1000, 3000))));

        ReportTemplateConfig config = config(header, null, chart(fixedWindow(1000, 2000)));

        assertUndefined(resolver.resolve(config, ctx));
    }

    @Test
    void testResolveIgnoresDisabledFirstPageHeaderTimeWindow() {
        HeaderFooter header = headerFooter(true, chart(fixedWindow(1000, 2000)));
        header.setFirstPage(headerFooter(false, chart(fixedWindow(1000, 3000))));

        ReportTemplateConfig config = config(header, null, chart(fixedWindow(1000, 2000)));

        ReportTimeRange range = resolver.resolve(config, ctx);

        assertThat(range.startTs()).isEqualTo(1000);
        assertThat(range.endTs()).isEqualTo(2000);
    }

    @Test
    void testResolveTakesSubReportOfHeaderIntoAccount() {
        ReportTemplateId templateId = new ReportTemplateId(UUID.randomUUID());
        givenSubReportTemplate(templateId, config(chart(fixedWindow(1000, 3000))));

        ReportTemplateConfig config = config(headerFooter(true, subReport(templateId)), null,
                chart(fixedWindow(1000, 2000)));

        assertUndefined(resolver.resolve(config, ctx));
    }

    @Test
    void testResolveIgnoresHeaderOfSubReportTemplate() {
        ReportTemplateId templateId = new ReportTemplateId(UUID.randomUUID());
        givenSubReportTemplate(templateId, config(headerFooter(true, chart(fixedWindow(1000, 3000))), null,
                chart(fixedWindow(1000, 2000))));

        ReportTimeRange range = resolver.resolve(config(chart(fixedWindow(1000, 2000)), subReport(templateId)), ctx);

        assertThat(range.startTs()).isEqualTo(1000);
        assertThat(range.endTs()).isEqualTo(2000);
    }

    @Test
    void testResolveKeepsTimezoneOfTimeWindow() {
        TimeWindowConfiguration window = quickWindow(QuickTimeInterval.CURRENT_DAY);
        window.setTimezone("Asia/Tokyo");

        ReportTimeRange range = resolver.resolve(config(chart(window)), ctx);

        assertThat(range.timezone()).isEqualTo("Asia/Tokyo");
        assertThat(range.startTs()).isEqualTo(LocalDate.now(ZoneId.of("Asia/Tokyo"))
                .atStartOfDay(ZoneId.of("Asia/Tokyo")).toInstant().toEpochMilli());
    }

    @Test
    void testResolveFallsBackToContextTimezone() {
        ReportTimeRange range = resolver.resolve(config(chart(quickWindow(QuickTimeInterval.CURRENT_DAY))), ctx);

        assertThat(range.timezone()).isEqualTo(UTC);
    }

    private void givenSubReportTemplate(ReportTemplateId templateId, ReportTemplateConfig configuration) {
        ReportTemplate template = new ReportTemplate(templateId);
        template.setConfiguration(configuration);
        try {
            when(dataService.findReportTemplate(eq(templateId), any())).thenReturn(template);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void assertUndefined(ReportTimeRange range) {
        assertThat(range.startTs()).isZero();
        assertThat(range.endTs()).isZero();
    }

    private static ReportTemplateConfig config(ReportComponent... components) {
        return PdfReportTemplateConfig.builder().components(List.of(components)).build();
    }

    private static ReportTemplateConfig config(HeaderFooter header, HeaderFooter footer, ReportComponent... components) {
        return PdfReportTemplateConfig.builder().components(List.of(components)).header(header).footer(footer).build();
    }

    private static HeaderFooter headerFooter(boolean enabled, ReportComponent... components) {
        HeaderFooter headerFooter = new HeaderFooter();
        headerFooter.setEnabled(enabled);
        headerFooter.setComponents(List.of(components));
        return headerFooter;
    }

    private static DashboardComponent dashboard(long timewindowMs) {
        DashboardReportConfig config = new DashboardReportConfig();
        config.setUseDashboardTimewindow(true);
        config.setTimewindow(JacksonUtil.toJsonNode(
                "{\"selectedTab\":1,\"history\":{\"historyType\":0,\"timewindowMs\":" + timewindowMs + "}}"));
        DashboardComponent component = new DashboardComponent();
        component.setConfig(config);
        return component;
    }

    private static TimeseriesChartComponent chart(TimeWindowConfiguration timewindow) {
        TimeseriesChartComponent component = new TimeseriesChartComponent();
        component.setTimewindow(timewindow);
        return component;
    }

    private static SubReportComponent subReport(ReportTemplateId templateId) {
        SubReportComponent component = new SubReportComponent();
        component.setTemplateId(templateId);
        return component;
    }

    private static DataSource dataSource(DataKey... dataKeys) {
        return DataSource.builder().dataKeys(List.of(dataKeys)).build();
    }

    private static DataKey dataKey(TimeWindowConfiguration timewindow) {
        DataKey dataKey = new DataKey("temperature", "timeseries", "Temperature");
        dataKey.setTimewindow(timewindow);
        return dataKey;
    }

    private static TimeWindowConfiguration fixedWindow(long startTimeMs, long endTimeMs) {
        History history = new History();
        history.setHistoryType(1);
        history.setFixedTimewindow(fixedTimeWindow(startTimeMs, endTimeMs));
        return timeWindow(history);
    }

    private static FixedTimeWindow fixedTimeWindow(long startTimeMs, long endTimeMs) {
        FixedTimeWindow fixedTimeWindow = new FixedTimeWindow();
        fixedTimeWindow.setStartTimeMs(startTimeMs);
        fixedTimeWindow.setEndTimeMs(endTimeMs);
        return fixedTimeWindow;
    }

    private static TimeWindowConfiguration lastIntervalWindow(long timewindowMs) {
        History history = new History();
        history.setHistoryType(0);
        history.setTimewindowMs(timewindowMs);
        return timeWindow(history);
    }

    private static TimeWindowConfiguration quickWindow(QuickTimeInterval quickInterval) {
        History history = new History();
        history.setHistoryType(2);
        history.setQuickInterval(quickInterval);
        return timeWindow(history);
    }

    private static TimeWindowConfiguration forAllTimeWindow() {
        History history = new History();
        history.setHistoryType(3);
        return timeWindow(history);
    }

    private static AggregationConfiguration aggregation(Aggregation type) {
        AggregationConfiguration aggregation = new AggregationConfiguration();
        aggregation.setType(type);
        return aggregation;
    }

    private static TimeWindowConfiguration timeWindow(History history) {
        TimeWindowConfiguration timeWindow = new TimeWindowConfiguration();
        timeWindow.setHistory(history);
        return timeWindow;
    }

}

// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.HeaderFooter;
import org.thingsboard.server.common.data.report.configuration.PdfReportTemplateConfig;
import org.thingsboard.server.common.data.report.configuration.ReportTemplateConfig;
import org.thingsboard.server.common.data.report.configuration.components.AlarmTableComponent;
import org.thingsboard.server.common.data.report.configuration.components.DashboardComponent;
import org.thingsboard.server.common.data.report.configuration.components.DataReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.SplitViewComponent;
import org.thingsboard.server.common.data.report.configuration.components.SubReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.TimeseriesChartComponent;
import org.thingsboard.server.common.data.report.configuration.components.TimeseriesTableComponent;
import org.thingsboard.server.common.data.report.configuration.timewindow.FixedTimeWindow;
import org.thingsboard.server.common.data.report.configuration.timewindow.History;
import org.thingsboard.server.common.data.report.configuration.timewindow.QuickTimeInterval;
import org.thingsboard.server.common.data.report.configuration.timewindow.TimeIntervalCalculator;
import org.thingsboard.server.common.data.report.configuration.timewindow.TimeWindowConfiguration;
import org.thingsboard.server.report.context.TbReportCtx;
import org.thingsboard.server.report.datasource.ReportDataService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@Slf4j
public class ReportTimeRangeResolver {

    private static final int LAST_INTERVAL_HISTORY_TYPE = 0;
    private static final int FIXED_HISTORY_TYPE = 1;
    private static final int QUICK_INTERVAL_HISTORY_TYPE = 2;
    private static final ReportTimeRange UNDEFINED_TIME_RANGE = new ReportTimeRange(0, 0, null);

    private final ReportDataService dataService;

    public ReportTimeRangeResolver(@Lazy ReportDataService dataService) {
        this.dataService = dataService;
    }

    public ReportTimeRange resolve(ReportTemplateConfig reportTemplateConfig, TbReportCtx ctx) {
        Map<TimeWindowKey, History> timeWindows = new HashMap<>();
        if (!collect(renderedComponents(reportTemplateConfig), ctx, timeWindows, new HashSet<>()) || timeWindows.size() != 1) {
            return UNDEFINED_TIME_RANGE;
        }
        Map.Entry<TimeWindowKey, History> timeWindow = timeWindows.entrySet().iterator().next();
        TimeWindowConfiguration config = new TimeWindowConfiguration();
        config.setHistory(timeWindow.getValue());
        String timezone = timeWindow.getKey().timezone();
        TimeIntervalCalculator.TimeRange range = TimeIntervalCalculator.getTimeRange(config, timezone);
        return new ReportTimeRange(range.startTs, range.endTs, timezone);
    }

    private boolean collect(List<ReportComponent> components, TbReportCtx ctx, Map<TimeWindowKey, History> timeWindows, Set<ReportTemplateId> visitedTemplates) {
        for (ReportComponent component : components) {
            if (!collectComponent(component, ctx, timeWindows, visitedTemplates)) {
                return false;
            }
        }
        return true;
    }

    private static List<ReportComponent> renderedComponents(ReportTemplateConfig config) {
        List<ReportComponent> components = new ArrayList<>();
        addComponents(components, config.getComponents());
        if (config instanceof PdfReportTemplateConfig pdfConfig) {
            addEnabledComponents(components, pdfConfig.getHeader());
            addEnabledComponents(components, pdfConfig.getFooter());
        }
        return components;
    }

    private static void addEnabledComponents(List<ReportComponent> components, HeaderFooter headerFooter) {
        if (headerFooter == null) {
            return;
        }
        if (headerFooter.isEnabled()) {
            addComponents(components, headerFooter.getComponents());
        }
        HeaderFooter firstPage = headerFooter.getFirstPage();
        if (firstPage != null && firstPage.isEnabled()) {
            addComponents(components, firstPage.getComponents());
        }
    }

    private static List<ReportComponent> subReportComponents(ReportTemplateConfig config) {
        return config.getComponents() != null ? config.getComponents() : List.of();
    }

    private static void addComponents(List<ReportComponent> components, List<ReportComponent> toAdd) {
        if (toAdd != null) {
            components.addAll(toAdd);
        }
    }

    private boolean collectComponent(ReportComponent component, TbReportCtx ctx, Map<TimeWindowKey, History> timeWindows, Set<ReportTemplateId> visitedTemplates) {
        if (component instanceof SplitViewComponent splitView) {
            return (splitView.getLeftView() == null || collectComponent(splitView.getLeftView(), ctx, timeWindows, visitedTemplates))
                    && (splitView.getRightView() == null || collectComponent(splitView.getRightView(), ctx, timeWindows, visitedTemplates));
        }
        if (component instanceof DashboardComponent) {
            return true;
        }
        if (!collectTimeWindows(component, ctx, timeWindows)) {
            return false;
        }
        if (component instanceof SubReportComponent subReport) {
            return collectSubReport(subReport, ctx, timeWindows, visitedTemplates);
        }
        return true;
    }

    private static boolean collectTimeWindows(ReportComponent component, TbReportCtx ctx, Map<TimeWindowKey, History> timeWindows) {
        for (TimeWindowConfiguration timeWindow : queriedTimeWindows(component)) {
            History history = timeWindow.getHistory();
            if (history == null) {
                continue;
            }
            String timezone = StringUtils.isNotBlank(timeWindow.getTimezone()) ? timeWindow.getTimezone() : ctx.getTimeZone();
            TimeWindowKey key = toKey(history, timezone);
            if (key == null) {
                continue;
            }
            timeWindows.putIfAbsent(key, history);
            if (timeWindows.size() > 1) {
                return false;
            }
        }
        return true;
    }

    private static List<TimeWindowConfiguration> queriedTimeWindows(ReportComponent component) {
        List<TimeWindowConfiguration> result = new ArrayList<>();
        addIfPresent(result, componentTimeWindow(component));
        for (DataSource dataSource : dataSources(component)) {
            if (dataSource != null) {
                addAggregatedKeyTimeWindows(result, dataSource.getDataKeys());
                addAggregatedKeyTimeWindows(result, dataSource.getLatestDataKeys());
            }
        }
        return result;
    }

    private static void addAggregatedKeyTimeWindows(List<TimeWindowConfiguration> timeWindows, List<DataKey> dataKeys) {
        if (dataKeys != null) {
            dataKeys.stream()
                    .filter(dataKey -> dataKey != null && dataKey.isAggregated())
                    .forEach(dataKey -> addIfPresent(timeWindows, dataKey.getTimewindow()));
        }
    }

    private static TimeWindowConfiguration componentTimeWindow(ReportComponent component) {
        if (component instanceof TimeseriesChartComponent chart) {
            return chart.getTimewindow();
        }
        if (component instanceof TimeseriesTableComponent table) {
            return table.getTimewindow();
        }
        if (component instanceof AlarmTableComponent alarmTable) {
            return alarmTable.getTimewindow();
        }
        return null;
    }

    private static List<DataSource> dataSources(ReportComponent component) {
        if (component instanceof AlarmTableComponent alarmTable) {
            return alarmTable.getAlarmSource() != null ? List.of(alarmTable.getAlarmSource()) : List.of();
        }
        if (component instanceof DataReportComponent dataComponent && dataComponent.getDataSources() != null) {
            return dataComponent.getDataSources();
        }
        return List.of();
    }

    private static void addIfPresent(List<TimeWindowConfiguration> timeWindows, TimeWindowConfiguration timeWindow) {
        if (timeWindow != null) {
            timeWindows.add(timeWindow);
        }
    }

    private boolean collectSubReport(SubReportComponent subReport, TbReportCtx ctx, Map<TimeWindowKey, History> timeWindows, Set<ReportTemplateId> visitedTemplates) {
        ReportTemplateId templateId = subReport.getTemplateId();
        if (templateId == null) {
            return true;
        }
        if (!visitedTemplates.add(templateId)) {
            return true;
        }
        ReportTemplate template;
        try {
            template = dataService.findReportTemplate(templateId, ctx);
        } catch (Exception e) {
            log.debug("[{}] Failed to fetch sub-report template {} while resolving report time range", ctx.getTenantId(), templateId, e);
            return false;
        }
        if (template == null || template.getConfiguration() == null) {
            return false;
        }
        return collect(subReportComponents(template.getConfiguration()), ctx, timeWindows, visitedTemplates);
    }

    private static TimeWindowKey toKey(History history, String timezone) {
        return switch (history.getHistoryType()) {
            case LAST_INTERVAL_HISTORY_TYPE ->
                    new TimeWindowKey(LAST_INTERVAL_HISTORY_TYPE, history.getTimewindowMs(), 0, 0, null, timezone);
            case FIXED_HISTORY_TYPE -> {
                FixedTimeWindow fixedTimewindow = history.getFixedTimewindow();
                yield fixedTimewindow == null ? null : new TimeWindowKey(FIXED_HISTORY_TYPE, 0,
                        fixedTimewindow.getStartTimeMs(), fixedTimewindow.getEndTimeMs(), null, timezone);
            }
            case QUICK_INTERVAL_HISTORY_TYPE -> history.getQuickInterval() == null ? null
                    : new TimeWindowKey(QUICK_INTERVAL_HISTORY_TYPE, 0, 0, 0, history.getQuickInterval(), timezone);
            default -> null;
        };
    }

    private record TimeWindowKey(int historyType, long timewindowMs, long fixedStartTs, long fixedEndTs,
                                 QuickTimeInterval quickInterval, String timezone) {}

    public record ReportTimeRange(long startTs, long endTs, String timezone) {}

}

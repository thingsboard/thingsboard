// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.util;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.configuration.HeaderFooter;
import org.thingsboard.server.common.data.report.configuration.PdfReportTemplateConfig;
import org.thingsboard.server.common.data.report.configuration.ReportTemplateConfig;
import org.thingsboard.server.common.data.report.configuration.components.DashboardComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.SplitViewComponent;
import org.thingsboard.server.common.data.report.configuration.components.SubReportComponent;
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

    /**
     * Collects distinct time windows of the given components and of all sub-report templates they reference.
     * Returns false as soon as the time range is known to be undefined: either the collected time windows
     * differ, or a referenced sub-report template can't be fetched.
     */
    private boolean collect(List<ReportComponent> components, TbReportCtx ctx, Map<TimeWindowKey, History> timeWindows, Set<ReportTemplateId> visitedTemplates) {
        for (ReportComponent component : components) {
            if (!collectComponent(component, ctx, timeWindows, visitedTemplates)) {
                return false;
            }
        }
        return true;
    }

    /**
     * The components that end up on the page: the body ones, plus the header and footer ones of the blocks that
     * are enabled. Disabling a header or a footer in the UI only flips the flag and keeps the components, and
     * PdfReportService renders a block only while it is enabled, so the components of a disabled one carry time
     * windows nothing on the page uses. The first page block is enabled on its own, as it is rendered on its own.
     */
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

    /**
     * A sub-report puts only the body components of its template on the page: PdfReportService.renderSubReport
     * passes exactly those to renderContent, so the header and the footer of a sub-report template are never
     * rendered and the time windows they carry are none of this report's business.
     */
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
        for (JsonNode node : JacksonUtil.valueToTree(component).findValues("timewindow")) {
            if (!node.isObject()) {
                continue;
            }
            TimeWindowConfiguration timeWindow = JacksonUtil.treeToValue(node, TimeWindowConfiguration.class);
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

    /**
     * Keys a time window by the fields getTimeRange actually reads for the given history type, plus the
     * effective timezone. The UI keeps the defaults of every history type in one object - a last interval
     * window still carries a fixed timewindow stamped when the component was created - so keying by the whole
     * History would split time windows that resolve to the very same range. Returns null for the time windows
     * that carry no range at all: for all time, and anything this version doesn't know how to resolve.
     */
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

    /**
     * The resolved time range together with the timezone it was resolved in. A time window can carry a timezone
     * of its own, which is the one the range was computed in, so it is also the one it has to be formatted in.
     */
    public record ReportTimeRange(long startTs, long endTs, String timezone) {}

}

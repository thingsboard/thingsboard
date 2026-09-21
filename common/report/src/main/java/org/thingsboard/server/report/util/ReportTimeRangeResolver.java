/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2026 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.server.report.util;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.configuration.ReportTemplateConfig;
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
@Slf4j
public class ReportTimeRangeResolver {

    private static final int LAST_INTERVAL_HISTORY_TYPE = 0;
    private static final int FIXED_HISTORY_TYPE = 1;
    private static final int QUICK_INTERVAL_HISTORY_TYPE = 2;
    private static final TimeIntervalCalculator.TimeRange UNDEFINED_TIME_RANGE = new TimeIntervalCalculator.TimeRange(0, 0);

    private final ReportDataService dataService;

    public ReportTimeRangeResolver(@Lazy ReportDataService dataService) {
        this.dataService = dataService;
    }

    public TimeIntervalCalculator.TimeRange resolve(ReportTemplateConfig reportTemplateConfig, TbReportCtx ctx) {
        Map<TimeWindowKey, History> timeWindows = new HashMap<>();
        if (!collect(reportTemplateConfig, ctx, timeWindows, new HashSet<>()) || timeWindows.size() != 1) {
            return UNDEFINED_TIME_RANGE;
        }
        Map.Entry<TimeWindowKey, History> timeWindow = timeWindows.entrySet().iterator().next();
        TimeWindowConfiguration config = new TimeWindowConfiguration();
        config.setHistory(timeWindow.getValue());
        return TimeIntervalCalculator.getTimeRange(config, timeWindow.getKey().timezone());
    }

    /**
     * Collects distinct time windows of the template and of all sub-report templates it references.
     * Returns false as soon as the time range is known to be undefined: either the collected time windows
     * differ, or a referenced sub-report template can't be fetched.
     */
    private boolean collect(ReportTemplateConfig config, TbReportCtx ctx, Map<TimeWindowKey, History> timeWindows, Set<ReportTemplateId> visitedTemplates) {
        for (JsonNode node : JacksonUtil.valueToTree(config).findValues("timewindow")) {
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
        for (ReportComponent component : config.getComponents()) {
            if (!collectSubReports(component, ctx, timeWindows, visitedTemplates)) {
                return false;
            }
        }
        return true;
    }

    private boolean collectSubReports(ReportComponent component, TbReportCtx ctx, Map<TimeWindowKey, History> timeWindows, Set<ReportTemplateId> visitedTemplates) {
        if (component instanceof SplitViewComponent splitView) {
            return (splitView.getLeftView() == null || collectSubReports(splitView.getLeftView(), ctx, timeWindows, visitedTemplates))
                    && (splitView.getRightView() == null || collectSubReports(splitView.getRightView(), ctx, timeWindows, visitedTemplates));
        }
        if (!(component instanceof SubReportComponent subReport)) {
            return true;
        }
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
        return collect(template.getConfiguration(), ctx, timeWindows, visitedTemplates);
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

}

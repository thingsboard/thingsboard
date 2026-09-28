// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer;

import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.report.configuration.components.AlarmTableComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;

import java.util.HashMap;
import java.util.Map;


@Component
public class AlarmTableRenderer extends TableWithLayoutComponentRenderer<AlarmTableComponent> {

    private static final Map<String, String> SEVERITY_COLOR = new HashMap<>();
    private static final Map<String, String> DISPLAY_STATUS = new HashMap<>();

    static {
        SEVERITY_COLOR.put("CRITICAL", "rgb(209, 39, 48)");
        SEVERITY_COLOR.put("MAJOR", "rgb(246, 103, 22)");
        SEVERITY_COLOR.put("MINOR", "rgb(250, 164, 5)");
        SEVERITY_COLOR.put("WARNING", "rgb(242, 218, 5)");
        SEVERITY_COLOR.put("INDETERMINATE", "rgba(0, 0, 0, 0.38)");

        DISPLAY_STATUS.put("ACTIVE_UNACK", "Active Unacknowledged");
        DISPLAY_STATUS.put("ACTIVE_ACK", "Active Acknowledged");
        DISPLAY_STATUS.put("CLEARED_UNACK", "Cleared Unacknowledged");
        DISPLAY_STATUS.put("CLEARED_ACK", "Cleared Acknowledged");
    }

    @Override
    protected String dataSourceName() {
        return "alarm source";
    }

    @Override
    protected String noDataMessage() {
        return "No alarms found";
    }

    @Override
    protected String defaultFontWeight(String key) {
        if ("severity".equals(key)) {
            return "bold";
        }
        return null;
    }

    @Override
    protected String defaultColor(String key, String value) {
        if ("severity".equals(key)) {
            return SEVERITY_COLOR.getOrDefault(value, value);
        }
        return null;
    }

    @Override
    protected String defaultValue(String key, String value) {
        if ("status".equals(key)) {
            return DISPLAY_STATUS.getOrDefault(value, value);
        }
        return super.defaultValue(key, value);
    }

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.ALARM_TABLE;
    }

}

// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer;

import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.report.configuration.components.AlarmTableComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;


@Component
public class CsvAlarmTableRenderer extends AbstractCsvComponentRenderer<AlarmTableComponent> {

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.ALARM_TABLE;
    }

}

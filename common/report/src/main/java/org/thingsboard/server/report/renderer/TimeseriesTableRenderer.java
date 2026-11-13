// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.renderer;

import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.common.data.report.configuration.components.TimeseriesTableComponent;

import java.util.LinkedList;
import java.util.List;

@Component
public class TimeseriesTableRenderer extends TableWithLayoutComponentRenderer<TimeseriesTableComponent> {

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.TIME_SERIES_TABLE;
    }

    @Override
    protected String noDataMessage() {
        return "No time series data found";
    }

    @Override
    protected List<DataKey> getColumns(TimeseriesTableComponent component, DataSource dataSource) {
        List<DataKey> columns = new LinkedList<>();
        if (component.isShowTimestamp()) {
            DataKey tsDataKey = DataKey.builder()
                    .name("ts")
                    .label(component.getTimestampLabel())
                    .settings(component.getTimestampColumnSettings())
                    .usePostProcessing(false)
                    .build();
            columns.add(tsDataKey);
        }
        columns.addAll(super.getColumns(component, dataSource));
        return columns;
    }

}

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
import java.util.Optional;


@Component
public class CsvTimeseriesTableRenderer extends AbstractCsvComponentRenderer<TimeseriesTableComponent> {

    @Override
    public List<DataKey> getColumns(TimeseriesTableComponent component, DataSource dataSource) {
        List<DataKey> dataKeys = new LinkedList<>();
        if (component.isShowTimestamp()) {
            DataKey tsDataKey = DataKey.builder()
                    .name("ts")
                    .label(component.getTimestampLabel())
                    .settings(component.getTimestampColumnSettings())
                    .usePostProcessing(false)
                    .build();
            dataKeys.add(tsDataKey);
        }
        Optional.ofNullable(dataSource.getDataKeys())
                .ifPresent(dataKeys::addAll);

        Optional.ofNullable(dataSource.getLatestDataKeys())
                .ifPresent(dataKeys::addAll);

        return dataKeys;
    }

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.TIME_SERIES_TABLE;
    }

}

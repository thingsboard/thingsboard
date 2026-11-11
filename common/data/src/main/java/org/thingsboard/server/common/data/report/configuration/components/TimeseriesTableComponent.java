// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.components;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.report.configuration.ColumnSettings;
import org.thingsboard.server.common.data.report.configuration.timewindow.TimeWindowConfiguration;

@Schema
@Data
@EqualsAndHashCode
@NoArgsConstructor
public class TimeseriesTableComponent extends AbstractTableWithLayoutReportComponent {

    private TimeWindowConfiguration timewindow;
    private boolean showTimestamp;
    private String timestampLabel;
    private String timestampPattern;
    private ColumnSettings timestampColumnSettings;

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.TIME_SERIES_TABLE;
    }

}

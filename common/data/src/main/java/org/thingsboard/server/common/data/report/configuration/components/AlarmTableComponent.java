// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.components;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.timewindow.TimeWindowConfiguration;

import java.util.List;

@Schema
@Data
@EqualsAndHashCode
@NoArgsConstructor
public class AlarmTableComponent extends AbstractTableWithLayoutReportComponent {

    private DataSource alarmSource;
    private TimeWindowConfiguration timewindow;

    @Override
    @JsonIgnore
    public List<DataSource> getDataSources() {
        return List.of(alarmSource);
    }

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.ALARM_TABLE;
    }

}

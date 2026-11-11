// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.components;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.report.configuration.chart.ReportLatestChartSettings;

@Schema
@Data
@EqualsAndHashCode
@NoArgsConstructor
public class LatestChartComponent extends AbstractChartComponent {

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
            property = "subType")
    private ReportLatestChartSettings latestChartSettings;

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.LATEST_CHART;
    }

}

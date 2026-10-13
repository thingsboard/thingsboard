// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.chart;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import org.thingsboard.server.common.data.report.configuration.style.Font;
import org.thingsboard.server.common.data.report.configuration.style.FontStyle;
import org.thingsboard.server.common.data.report.configuration.style.FontWeight;

@Schema
@Data
public class TimeSeriesChartYAxisSettings extends TimeSeriesChartAxisSettings {

    private String id;
    private Integer order;
    private String units;
    private Integer decimals;
    private Double interval;
    private Integer splitNumber;
    @JsonDeserialize(using = SimpleValueSourceDeserializer.class)
    private SimpleValueSourceConfig min;
    @JsonDeserialize(using = SimpleValueSourceDeserializer.class)
    private SimpleValueSourceConfig max;

    public TimeSeriesChartYAxisSettings() {}

    public TimeSeriesChartYAxisSettings(TimeSeriesChartYAxisSettings input) {
        super(input != null ? input : new TimeSeriesChartYAxisSettings());
        if (input == null) {
            input = new TimeSeriesChartYAxisSettings();
        }
        this.setPosition(input.getPosition() != null ? input.getPosition() : AxisPosition.left);
        this.setTickLabelFont(input.getTickLabelFont() != null ? input.getTickLabelFont() : Font.builder().family("Roboto")
                .size(12f)
                .weight(FontWeight.NORMAL)
                .style(FontStyle.NORMAL)
                .build());
        this.id = input.getId() != null ? input.getId() : "default";
        this.order = input.getOrder() != null ? input.getOrder() : 0;
        this.units = input.getUnits();
        this.decimals = input.getDecimals() != null ? input.getDecimals() : 0;
        this.interval = input.getInterval();
        this.splitNumber = input.getSplitNumber();
        this.min = input.getMin();
        this.max = input.getMax();
    }

}

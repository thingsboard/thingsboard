// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.components;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.report.configuration.chart.ReportComponentSubType;

import java.io.Serializable;

@Schema(
        discriminatorProperty = "type",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "HEADING", schema = HeadingComponent.class),
                @DiscriminatorMapping(value = "RICH_TEXT", schema = RichTextComponent.class),
                @DiscriminatorMapping(value = "ENTITY_TABLE", schema = EntityTableComponent.class),
                @DiscriminatorMapping(value = "PAGE_BREAK", schema = PageBreakComponent.class),
                @DiscriminatorMapping(value = "TIME_SERIES_TABLE", schema = TimeseriesTableComponent.class),
                @DiscriminatorMapping(value = "ALARM_TABLE", schema = AlarmTableComponent.class),
                @DiscriminatorMapping(value = "TIME_SERIES_CHART", schema = TimeseriesChartComponent.class),
                @DiscriminatorMapping(value = "LATEST_CHART", schema = LatestChartComponent.class),
                @DiscriminatorMapping(value = "DASHBOARD", schema = DashboardComponent.class),
                @DiscriminatorMapping(value = "IMAGE", schema = ImageComponent.class),
                @DiscriminatorMapping(value = "SUB_REPORT", schema = SubReportComponent.class),
                @DiscriminatorMapping(value = "ERROR", schema = ErrorComponent.class),
                @DiscriminatorMapping(value = "DIVIDER", schema = DividerComponent.class),
                @DiscriminatorMapping(value = "SPLIT_VIEW", schema = SplitViewComponent.class),
        }
)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = HeadingComponent.class, name = "HEADING"),
        @JsonSubTypes.Type(value = RichTextComponent.class, name = "RICH_TEXT"),
        @JsonSubTypes.Type(value = EntityTableComponent.class, name = "ENTITY_TABLE"),
        @JsonSubTypes.Type(value = PageBreakComponent.class, name = "PAGE_BREAK"),
        @JsonSubTypes.Type(value = TimeseriesTableComponent.class, name = "TIME_SERIES_TABLE"),
        @JsonSubTypes.Type(value = AlarmTableComponent.class, name = "ALARM_TABLE"),
        @JsonSubTypes.Type(value = TimeseriesChartComponent.class, name = "TIME_SERIES_CHART"),
        @JsonSubTypes.Type(value = LatestChartComponent.class, name = "LATEST_CHART"),
        @JsonSubTypes.Type(value = DashboardComponent.class, name = "DASHBOARD"),
        @JsonSubTypes.Type(value = ImageComponent.class, name = "IMAGE"),
        @JsonSubTypes.Type(value = SubReportComponent.class, name = "SUB_REPORT"),
        @JsonSubTypes.Type(value = ErrorComponent.class, name = "ERROR"),
        @JsonSubTypes.Type(value = DividerComponent.class, name = "DIVIDER"),
        @JsonSubTypes.Type(value = SplitViewComponent.class, name = "SPLIT_VIEW")
})
public interface ReportComponent extends Serializable {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    ReportComponentType getType();

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    ReportComponentSubType getSubType();

}

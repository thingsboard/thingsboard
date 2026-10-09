// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartKeySettings;
import org.thingsboard.server.common.data.report.configuration.style.DataKeySettingsType;

@Schema(
        discriminatorProperty = "type",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "COLUMN", schema = ColumnSettings.class),
                @DiscriminatorMapping(value = "TIME_SERIES_CHART", schema = TimeSeriesChartKeySettings.class),
                @DiscriminatorMapping(value = "DEFAULT", schema = DefaultDataKeySettings.class)
        }
)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        defaultImpl = DefaultDataKeySettings.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ColumnSettings.class, name = "COLUMN"),
        @JsonSubTypes.Type(value = TimeSeriesChartKeySettings.class, name = "TIME_SERIES_CHART"),
        @JsonSubTypes.Type(value = DefaultDataKeySettings.class, name = "DEFAULT")
})
public interface DataKeySettings {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Data key settings type")
    DataKeySettingsType getType();
}

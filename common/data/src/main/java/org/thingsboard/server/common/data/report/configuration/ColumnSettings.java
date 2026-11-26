// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.report.configuration.style.DataKeySettingsType;

// allOf is explicit because ColumnSettings is also a field type
// (TimeseriesTableComponent.timestampColumnSettings): resolved there first, it
// lost the DataKeySettings parent and generated clients did not treat it as one.
@Schema(allOf = DataKeySettings.class)
@Data
public class ColumnSettings implements DataKeySettings {

    private String columnWidth;
    private CellSettings header;
    private CellSettings cell;

    @Override
    public DataKeySettingsType getType() {
        return DataKeySettingsType.COLUMN;
    }
}

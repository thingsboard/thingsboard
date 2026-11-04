// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration;

import lombok.Data;
import org.thingsboard.server.common.data.report.configuration.style.DataKeySettingsType;

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

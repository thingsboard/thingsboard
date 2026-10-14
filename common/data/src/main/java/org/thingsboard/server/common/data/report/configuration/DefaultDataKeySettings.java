// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration;

import org.thingsboard.server.common.data.report.configuration.style.DataKeySettingsType;

public class DefaultDataKeySettings implements DataKeySettings {

    @Override
    public DataKeySettingsType getType() {
        return DataKeySettingsType.DEFAULT;
    }
}

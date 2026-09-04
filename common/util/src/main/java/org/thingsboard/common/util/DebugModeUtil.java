// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.common.util;

import org.thingsboard.server.common.data.HasDebugSettings;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;

import java.util.Set;

public final class DebugModeUtil {

    public static final int DEBUG_MODE_DEFAULT_DURATION_MINUTES = 15;

    private DebugModeUtil() {
    }

    public static int getMaxDebugAllDuration(int tenantProfileDuration, int systemDefaultDuration) {
        if (tenantProfileDuration > 0) {
            return tenantProfileDuration;
        } else {
            return systemDefaultDuration > 0 ? systemDefaultDuration : DEBUG_MODE_DEFAULT_DURATION_MINUTES;
        }
    }

    public static boolean isDebugAllAvailable(HasDebugSettings debugSettingsAware) {
        var debugSettings = debugSettingsAware.getDebugSettings();
        return debugSettings != null && debugSettings.getAllEnabledUntil() > System.currentTimeMillis();
    }

    public static boolean isDebugAvailable(HasDebugSettings debugSettingsAware, String nodeConnection) {
        if (isDebugAllAvailable(debugSettingsAware)) {
            return true;
        } else {
            var debugSettings = debugSettingsAware.getDebugSettings();
            return debugSettings != null && debugSettings.isFailuresEnabled() && TbNodeConnectionType.FAILURE.equals(nodeConnection);
        }
    }

    public static boolean isDebugFailuresAvailable(HasDebugSettings debugSettingsAware, Set<String> nodeConnections) {
        if (isDebugAllAvailable(debugSettingsAware)) {
            return true;
        } else {
            var debugSettings = debugSettingsAware.getDebugSettings();
            return debugSettings != null && nodeConnections != null && debugSettings.isFailuresEnabled() && nodeConnections.contains(TbNodeConnectionType.FAILURE);
        }
    }

    public static boolean isDebugFailuresAvailable(HasDebugSettings debugSettingsAware) {
        if (isDebugAllAvailable(debugSettingsAware)) {
            return true;
        } else {
            var debugSettings = debugSettingsAware.getDebugSettings();
            return debugSettings != null && debugSettings.isFailuresEnabled();
        }
    }

}

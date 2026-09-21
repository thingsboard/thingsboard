// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data;

import org.thingsboard.server.common.data.debug.DebugSettings;

public interface HasDebugSettings {

    @Deprecated
    boolean isDebugMode();

    @Deprecated
    void setDebugMode(boolean debugMode);

    DebugSettings getDebugSettings();

    void setDebugSettings(DebugSettings debugSettings);

}

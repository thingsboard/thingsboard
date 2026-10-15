// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.settings;

import lombok.Getter;

public enum UserSettingsType {

    GENERAL,
    VISITED_DASHBOARDS(true),
    QUICK_LINKS,
    DOC_LINKS,
    DASHBOARDS,
    GETTING_STARTED,
    NOTIFICATIONS,
    MOBILE(true);

    @Getter
    private final boolean reserved;

    UserSettingsType() {
        this.reserved = false;
    }

    UserSettingsType(boolean reserved) {
        this.reserved = reserved;
    }
}

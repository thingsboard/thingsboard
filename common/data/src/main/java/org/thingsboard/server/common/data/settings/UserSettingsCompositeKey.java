// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.settings;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class UserSettingsCompositeKey implements Serializable {

    private static final long serialVersionUID = -7883642552545291489L;

    private UUID userId;
    private String type;

    public UserSettingsCompositeKey(UserSettings userSettings) {
        this.userId = userSettings.getUserId().getId();
        this.type = userSettings.getType().name();
    }

    @Override
    public String toString() {
        return userId.toString() + "_" + type;
    }
}

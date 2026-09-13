// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.user;

import lombok.Data;
import org.thingsboard.server.common.data.settings.UserSettingsCompositeKey;

@Data
public class UserSettingsEvictEvent {
    private final UserSettingsCompositeKey key;
}

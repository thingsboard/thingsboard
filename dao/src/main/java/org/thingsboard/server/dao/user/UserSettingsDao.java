// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.user;

import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.settings.UserSettings;
import org.thingsboard.server.common.data.settings.UserSettingsCompositeKey;
import org.thingsboard.server.common.data.settings.UserSettingsType;

import java.util.List;

public interface UserSettingsDao {

    UserSettings save(TenantId tenantId, UserSettings userSettings);

    UserSettings findById(TenantId tenantId, UserSettingsCompositeKey key);

    void removeById(TenantId tenantId, UserSettingsCompositeKey key);

    void removeByUserId(TenantId tenantId, UserId userId);

    List<UserSettings> findByTypeAndPath(TenantId tenantId, UserSettingsType type, String... path);

}

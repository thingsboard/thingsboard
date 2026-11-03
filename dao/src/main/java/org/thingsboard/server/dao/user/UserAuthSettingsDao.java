// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.user;

import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.UserAuthSettings;
import org.thingsboard.server.dao.Dao;

public interface UserAuthSettingsDao extends Dao<UserAuthSettings> {

    UserAuthSettings findByUserId(UserId userId);

    void removeByUserId(UserId userId);

}

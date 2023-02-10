/**
 * Copyright © 2016-2023 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.UserSettings;

import java.util.List;

public interface UserSettingsService {

    void updateUserSettings(TenantId tenantId, UserId userId, JsonNode settings);
    void updateUserSettings(TenantId tenantId, UserId userId, String path, JsonNode settings);

    UserSettings saveUserSettings(TenantId tenantId, UserSettings userSettings);

    UserSettings findUserSettings(TenantId tenantId, UserId userId);

    void deleteUserSettings(TenantId tenantId, UserId userId, List<String> jsonPaths);

}

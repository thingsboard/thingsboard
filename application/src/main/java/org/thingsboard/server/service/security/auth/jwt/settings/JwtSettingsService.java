/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.service.security.auth.jwt.settings;

import org.thingsboard.server.common.data.security.model.JwtSettings;

public interface JwtSettingsService {

    String ADMIN_SETTINGS_JWT_KEY = "jwt";
    String TOKEN_SIGNING_KEY_DEFAULT = "thingsboardDefaultSigningKey";

    JwtSettings getJwtSettings();

    JwtSettings reloadJwtSettings();

    JwtSettings saveJwtSettings(JwtSettings jwtSettings);

}

/**
 * Copyright © 2016-2026 The Thingsboard Authors
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

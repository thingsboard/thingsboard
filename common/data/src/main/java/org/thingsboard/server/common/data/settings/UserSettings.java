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

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.io.Serializable;

@Schema
@Data
public class UserSettings implements Serializable {

    private static final long serialVersionUID = 2628320657987010348L;

    @Schema(description = "JSON object with User id.", accessMode = Schema.AccessMode.READ_ONLY)
    private UserId userId;

    @Schema(description = "Type of the settings.")
    @NoXss
    @Length(fieldName = "type", max = 50)
    private UserSettingsType type;

    @Schema(description = "JSON object with user settings.", implementation = com.fasterxml.jackson.databind.JsonNode.class)
    @NoXss
    @Length(fieldName = "settings", max = 100000)
    private JsonNode settings;

}

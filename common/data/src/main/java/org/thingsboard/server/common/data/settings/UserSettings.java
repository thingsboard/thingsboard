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
package org.thingsboard.server.common.data.settings;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.thingsboard.server.common.data.SearchTextBasedWithAdditionalInfo.getJson;
import static org.thingsboard.server.common.data.SearchTextBasedWithAdditionalInfo.setJson;

@ApiModel
@Data
public class UserSettings implements Serializable {

    private static final long serialVersionUID = 2628320657987010348L;

    @ApiModelProperty(position = 1, value = "JSON object with User id.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private UserId userId;

    @ApiModelProperty(position = 2, value = "Type of the settings.")
    @NoXss
    @Length(fieldName = "type", max = 50)
    private UserSettingsType type;

    @ApiModelProperty(position = 3, value = "JSON object with user settings.", dataType = "com.fasterxml.jackson.databind.JsonNode")
    @NoXss
    @Length(fieldName = "settings", max = 100000)
    private transient JsonNode settings;

    @JsonIgnore
    private byte[] settingsBytes;

    public JsonNode getSettings() {
        return getJson(() -> settings, () -> settingsBytes);
    }

    public void setSettings(JsonNode settings) {
        setJson(settings, json -> this.settings = json, bytes -> this.settingsBytes = bytes);
    }
}

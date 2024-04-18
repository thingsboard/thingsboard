/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.common.data.mobile;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.io.Serializable;

import static org.thingsboard.server.common.data.BaseDataWithAdditionalInfo.getJson;
import static org.thingsboard.server.common.data.BaseDataWithAdditionalInfo.setJson;

@Data
public class MobileAppSettings implements Serializable {

    private static final long serialVersionUID = 2628323657987010348L;

    private TenantId tenantId;

    @NoXss
    @Length(fieldName = "appPackage")
    private String appPackage;

    @NoXss
    @Length(fieldName = "sha256CertFingerprints")
    private String sha256CertFingerprints;

    @NoXss
    @Length(fieldName = "appId")
    private String appId;

    @NoXss
    @Length(fieldName = "settings", max = 10000000)
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

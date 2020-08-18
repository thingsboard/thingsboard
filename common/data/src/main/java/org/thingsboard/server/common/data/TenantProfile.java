/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.common.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.TenantProfileId;

import static org.thingsboard.server.common.data.SearchTextBasedWithAdditionalInfo.getJson;
import static org.thingsboard.server.common.data.SearchTextBasedWithAdditionalInfo.setJson;

@Data
@EqualsAndHashCode(callSuper = true)
public class TenantProfile extends SearchTextBased<TenantProfileId> implements HasName {

    private String name;
    private String description;
    private boolean isDefault;
    private boolean isolatedTbCore;
    private boolean isolatedTbRuleEngine;
    private transient JsonNode profileData;
    @JsonIgnore
    private byte[] profileDataBytes;

    public TenantProfile() {
        super();
    }

    public TenantProfile(TenantProfileId tenantProfileId) {
        super(tenantProfileId);
    }

    public TenantProfile(TenantProfile tenantProfile) {
        super(tenantProfile);
        this.name = tenantProfile.getName();
        this.description = tenantProfile.getDescription();
        this.isDefault = tenantProfile.isDefault();
        this.isolatedTbCore = tenantProfile.isIsolatedTbCore();
        this.isolatedTbRuleEngine = tenantProfile.isIsolatedTbRuleEngine();
        this.setProfileData(tenantProfile.getProfileData());
    }

    @Override
    public String getSearchText() {
        return getName();
    }

    @Override
    public String getName() {
        return name;
    }

    public JsonNode getProfileData() {
        return getJson(() -> profileData, () -> profileDataBytes);
    }

    public void setProfileData(JsonNode data) {
        setJson(data, json -> this.profileData = json, bytes -> this.profileDataBytes = bytes);
    }

}

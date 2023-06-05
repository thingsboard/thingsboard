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
package org.thingsboard.server.common.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;

@ApiModel
@Data
@ToString
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class TenantProfile extends BaseData<TenantProfileId> implements HasName, TbSerializable {

    @NoXss
    @Length(fieldName = "name")
    @ApiModelProperty(position = 3, value = "Name of the tenant profile", example = "High Priority Tenants")
    private String name;
    @NoXss
    @ApiModelProperty(position = 4, value = "Description of the tenant profile", example = "Any text")
    private String description;
    @ApiModelProperty(position = 5, value = "Default Tenant profile to be used.", example = "true")
    @JsonProperty("default")
    private boolean defaultProfile;
    @ApiModelProperty(position = 6, value = "If enabled, will push all messages related to this tenant and processed by the rule engine into separate queue. " +
            "Useful for complex microservices deployments, to isolate processing of the data for specific tenants", example = "true")
    private boolean isolatedTbRuleEngine;
    @ApiModelProperty(position = 7, value = "Complex JSON object that contains profile settings: queue configs, max devices, max assets, rate limits, etc.")
    private TenantProfileData profileData;

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
        this.defaultProfile = tenantProfile.isDefaultProfile();
        this.isolatedTbRuleEngine = tenantProfile.isIsolatedTbRuleEngine();
        this.setProfileData(tenantProfile.getProfileData());
    }

    @ApiModelProperty(position = 1, value = "JSON object with the tenant profile Id. " +
            "Specify this field to update the tenant profile. " +
            "Referencing non-existing tenant profile Id will cause error. " +
            "Omit this field to create new tenant profile.")
    @Override
    public TenantProfileId getId() {
        return super.getId();
    }

    @ApiModelProperty(position = 2, value = "Timestamp of the tenant profile creation, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @Override
    public String getName() {
        return name;
    }

    public TenantProfileData getProfileData() {
        if (profileData != null) {
            return profileData;
        } else {
            return createDefaultTenantProfileData();
        }
    }

    @JsonIgnore
    public Optional<DefaultTenantProfileConfiguration> getProfileConfiguration() {
        return Optional.ofNullable(getProfileData().getConfiguration())
                .filter(profileConfiguration -> profileConfiguration instanceof DefaultTenantProfileConfiguration)
                .map(profileConfiguration -> (DefaultTenantProfileConfiguration) profileConfiguration);
    }

    @JsonIgnore
    public DefaultTenantProfileConfiguration getDefaultProfileConfiguration() {
        return getProfileConfiguration().orElse(null);
    }

    public TenantProfileData createDefaultTenantProfileData() {
        TenantProfileData tpd = new TenantProfileData();
        tpd.setConfiguration(new DefaultTenantProfileConfiguration());
        this.profileData = tpd;
        return tpd;
    }

}

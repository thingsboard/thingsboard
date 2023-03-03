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
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@ApiModel
@EqualsAndHashCode(callSuper = true)
public class Tenant extends ContactBased<TenantId> implements HasTenantId {

    private static final long serialVersionUID = 8057243243859922101L;

    @Length(fieldName = "title")
    @NoXss
    @ApiModelProperty(position = 3, value = "Title of the tenant", example = "Company A")
    private String title;
    @NoXss
    @Length(fieldName = "region")
    @ApiModelProperty(position = 5, value = "Geo region of the tenant", example = "North America")
    private String region;

    @ApiModelProperty(position = 6, required = true, value = "JSON object with Tenant Profile Id")
    private TenantProfileId tenantProfileId;

    public Tenant() {
        super();
    }

    public Tenant(TenantId id) {
        super(id);
    }

    public Tenant(Tenant tenant) {
        super(tenant);
        this.title = tenant.getTitle();
        this.region = tenant.getRegion();
        this.tenantProfileId = tenant.getTenantProfileId();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    @JsonIgnore
    public TenantId getTenantId() {
        return getId();
    }

    @Override
    @ApiModelProperty(position = 4, value = "Name of the tenant. Read-only, duplicated from title for backward compatibility", example = "Company A", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getName() {
        return title;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public TenantProfileId getTenantProfileId() {
        return tenantProfileId;
    }

    public void setTenantProfileId(TenantProfileId tenantProfileId) {
        this.tenantProfileId = tenantProfileId;
    }

    @Override
    public String getSearchText() {
        return getTitle();
    }

    @ApiModelProperty(position = 1, value = "JSON object with the tenant Id. " +
            "Specify this field to update the tenant. " +
            "Referencing non-existing tenant Id will cause error. " +
            "Omit this field to create new tenant." )
    @Override
    public TenantId getId() {
        return super.getId();
    }

    @ApiModelProperty(position = 2, value = "Timestamp of the tenant creation, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @ApiModelProperty(position = 7, required = true, value = "Country", example = "US")
    @Override
    public String getCountry() {
        return super.getCountry();
    }

    @ApiModelProperty(position = 8, required = true, value = "State", example = "NY")
    @Override
    public String getState() {
        return super.getState();
    }

    @ApiModelProperty(position = 9, required = true, value = "City", example = "New York")
    @Override
    public String getCity() {
        return super.getCity();
    }

    @ApiModelProperty(position = 10, required = true, value = "Address Line 1", example = "42 Broadway Suite 12-400")
    @Override
    public String getAddress() {
        return super.getAddress();
    }

    @ApiModelProperty(position = 11, required = true, value = "Address Line 2", example = "")
    @Override
    public String getAddress2() {
        return super.getAddress2();
    }

    @ApiModelProperty(position = 12, required = true, value = "Zip code", example = "10004")
    @Override
    public String getZip() {
        return super.getZip();
    }

    @ApiModelProperty(position = 13, required = true, value = "Phone number", example = "+1(415)777-7777")
    @Override
    public String getPhone() {
        return super.getPhone();
    }

    @ApiModelProperty(position = 14, required = true, value = "Email", example = "example@company.com")
    @Override
    public String getEmail() {
        return super.getEmail();
    }

    @ApiModelProperty(position = 15, value = "Additional parameters of the device", dataType = "com.fasterxml.jackson.databind.JsonNode")
    @Override
    public JsonNode getAdditionalInfo() {
        return super.getAdditionalInfo();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Tenant [title=");
        builder.append(title);
        builder.append(", region=");
        builder.append(region);
        builder.append(", tenantProfileId=");
        builder.append(tenantProfileId);
        builder.append(", additionalInfo=");
        builder.append(getAdditionalInfo());
        builder.append(", country=");
        builder.append(country);
        builder.append(", state=");
        builder.append(state);
        builder.append(", city=");
        builder.append(city);
        builder.append(", address=");
        builder.append(address);
        builder.append(", address2=");
        builder.append(address2);
        builder.append(", zip=");
        builder.append(zip);
        builder.append(", phone=");
        builder.append(phone);
        builder.append(", email=");
        builder.append(email);
        builder.append(", createdTime=");
        builder.append(createdTime);
        builder.append(", id=");
        builder.append(id);
        builder.append("]");
        return builder.toString();
    }

}

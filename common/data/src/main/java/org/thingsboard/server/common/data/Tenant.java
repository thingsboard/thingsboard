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
package org.thingsboard.server.common.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@Schema
@EqualsAndHashCode(callSuper = true)
public class Tenant extends ContactBased<TenantId> implements HasTenantId, HasTitle, HasVersion {

    private static final long serialVersionUID = 8057243243859922101L;

    @Length(fieldName = "title")
    @NoXss
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Title of the tenant", example = "Company A")
    private String title;
    @NoXss
    @Length(fieldName = "region")
    @Schema(description = "Geo region of the tenant", example = "North America")
    private String region;

    @Schema(description = "JSON object with Tenant Profile Id")
    private TenantProfileId tenantProfileId;

    @Getter @Setter
    private Long version;

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
        this.version = tenant.getVersion();
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
    @Schema(description = "Name of the tenant. Read-only, duplicated from title for backward compatibility", example = "Company A", accessMode = Schema.AccessMode.READ_ONLY)
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

    @Schema(description = "JSON object with the tenant Id. " +
            "Specify this field to update the tenant. " +
            "Referencing non-existing tenant Id will cause error. " +
            "Omit this field to create new tenant.")
    @Override
    public TenantId getId() {
        return super.getId();
    }

    @Schema(description = "Timestamp of the tenant creation, in milliseconds", example = "1609459200000", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @Schema(description = "Country", example = "US")
    @Override
    public String getCountry() {
        return super.getCountry();
    }

    @Schema(description = "State", example = "NY")
    @Override
    public String getState() {
        return super.getState();
    }

    @Schema(description = "City", example = "New York")
    @Override
    public String getCity() {
        return super.getCity();
    }

    @Schema(description = "Address Line 1", example = "42 Broadway Suite 12-400")
    @Override
    public String getAddress() {
        return super.getAddress();
    }

    @Schema(description = "Address Line 2", example = "")
    @Override
    public String getAddress2() {
        return super.getAddress2();
    }

    @Schema(description = "Zip code", example = "10004")
    @Override
    public String getZip() {
        return super.getZip();
    }

    @Schema(description = "Phone number", example = "+1(415)777-7777")
    @Override
    public String getPhone() {
        return super.getPhone();
    }

    @Schema(description = "Email", example = "example@company.com")
    @Override
    public String getEmail() {
        return super.getEmail();
    }

    @Schema(description = "Additional parameters of the device", implementation = com.fasterxml.jackson.databind.JsonNode.class)
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

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
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@Schema
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class TbResourceInfo extends BaseData<TbResourceId> implements HasName, HasTenantId {

    private static final long serialVersionUID = 7282664529021651736L;

    @Schema(description = "JSON object with Tenant Id. Tenant Id of the resource can't be changed.", accessMode = Schema.AccessMode.READ_ONLY)
    private TenantId tenantId;
    @NoXss
    @Length(fieldName = "title")
    @Schema(description = "Resource title.", example = "BinaryAppDataContainer id=19 v1.0")
    private String title;
    @Schema(description = "Resource type.", example = "LWM2M_MODEL", accessMode = Schema.AccessMode.READ_ONLY)
    private ResourceType resourceType;
    @NoXss
    @Length(fieldName = "resourceKey")
    @Schema(description = "Resource key.", example = "19_1.0", accessMode = Schema.AccessMode.READ_ONLY)
    private String resourceKey;
    @Schema(description = "Resource search text.", example = "19_1.0:binaryappdatacontainer", accessMode = Schema.AccessMode.READ_ONLY)
    private String searchText;
    @Schema(description = "Resource etag.", example = "33a64df551425fcc55e4d42a148795d9f25f89d4", accessMode = Schema.AccessMode.READ_ONLY)
    private String etag;

    public TbResourceInfo() {
        super();
    }

    public TbResourceInfo(TbResourceId id) {
        super(id);
    }

    public TbResourceInfo(TbResourceInfo resourceInfo) {
        super(resourceInfo);
        this.tenantId = resourceInfo.getTenantId();
        this.title = resourceInfo.getTitle();
        this.resourceType = resourceInfo.getResourceType();
        this.resourceKey = resourceInfo.getResourceKey();
        this.searchText = resourceInfo.getSearchText();
        this.etag = resourceInfo.getEtag();
    }

    @Schema(description = "JSON object with the Resource Id. " +
            "Specify this field to update the Resource. " +
            "Referencing non-existing Resource Id will cause error. " +
            "Omit this field to create new Resource.")
    @Override
    public TbResourceId getId() {
        return super.getId();
    }

    @Schema(description = "Timestamp of the resource creation, in milliseconds", example = "1609459200000", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @Override
    @JsonIgnore
    public String getName() {
        return title;
    }

    @JsonIgnore
    public String getSearchText() {
        return searchText != null ? searchText : title;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ResourceInfo [tenantId=");
        builder.append(tenantId);
        builder.append(", id=");
        builder.append(getUuidId());
        builder.append(", createdTime=");
        builder.append(createdTime);
        builder.append(", title=");
        builder.append(title);
        builder.append(", resourceType=");
        builder.append(resourceType);
        builder.append(", resourceKey=");
        builder.append(resourceKey);
        builder.append(", hashCode=");
        builder.append(etag);
        builder.append("]");
        return builder.toString();
    }
}

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
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@ApiModel
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class TbResourceInfo extends SearchTextBased<TbResourceId> implements HasName, HasTenantId {

    private static final long serialVersionUID = 7282664529021651736L;

    @ApiModelProperty(position = 3, value = "JSON object with Tenant Id. Tenant Id of the resource can't be changed.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private TenantId tenantId;
    @NoXss
    @Length(fieldName = "title")
    @ApiModelProperty(position = 4, value = "Resource title.", example = "BinaryAppDataContainer id=19 v1.0")
    private String title;
    @ApiModelProperty(position = 5, value = "Resource type.", example = "LWM2M_MODEL", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private ResourceType resourceType;
    @NoXss
    @Length(fieldName = "resourceKey")
    @ApiModelProperty(position = 6, value = "Resource key.", example = "19_1.0", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private String resourceKey;
    @ApiModelProperty(position = 7, value = "Resource search text.", example = "19_1.0:binaryappdatacontainer", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private String searchText;

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
    }

    @ApiModelProperty(position = 1, value = "JSON object with the Resource Id. " +
            "Specify this field to update the Resource. " +
            "Referencing non-existing Resource Id will cause error. " +
            "Omit this field to create new Resource." )
    @Override
    public TbResourceId getId() {
        return super.getId();
    }

    @ApiModelProperty(position = 2, value = "Timestamp of the resource creation, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @Override
    @JsonIgnore
    public String getName() {
        return title;
    }

    @Override
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
        builder.append("]");
        return builder.toString();
    }
}

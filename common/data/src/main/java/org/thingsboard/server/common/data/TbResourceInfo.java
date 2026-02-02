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
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.util.function.UnaryOperator;

@Schema
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class TbResourceInfo extends BaseData<TbResourceId> implements HasName, HasTenantId, ExportableEntity<TbResourceId> {

    private static final long serialVersionUID = 7282664529021651736L;

    @Schema(description = "JSON object with Tenant Id. Tenant Id of the resource can't be changed.", accessMode = Schema.AccessMode.READ_ONLY)
    private TenantId tenantId;
    @NoXss
    @Length(fieldName = "title")
    @Schema(description = "Resource title.", example = "BinaryAppDataContainer id=19 v1.0")
    private String title;
    @Schema(description = "Resource type.", example = "LWM2M_MODEL", accessMode = Schema.AccessMode.READ_ONLY)
    private ResourceType resourceType;
    @Schema(description = "Resource sub type.", example = "IOT_SVG", accessMode = Schema.AccessMode.READ_ONLY)
    private ResourceSubType resourceSubType;
    @NoXss
    @Length(fieldName = "resourceKey")
    @Schema(description = "Resource key.", example = "19_1.0", accessMode = Schema.AccessMode.READ_ONLY)
    private String resourceKey;
    private boolean isPublic;
    private String publicResourceKey;
    @Schema(description = "Resource search text.", example = "19_1.0:binaryappdatacontainer", accessMode = Schema.AccessMode.READ_ONLY)
    private String searchText;

    @Schema(description = "Resource etag.", example = "33a64df551425fcc55e4d42a148795d9f25f89d4", accessMode = Schema.AccessMode.READ_ONLY)
    private String etag;
    @NoXss
    @Length(fieldName = "file name")
    @Schema(description = "Resource file name.", example = "19.xml", accessMode = Schema.AccessMode.READ_ONLY)
    private String fileName;
    private JsonNode descriptor;

    private TbResourceId externalId;

    public TbResourceInfo() {
        super();
    }

    public TbResourceInfo(TbResourceId id) {
        super(id);
    }

    public TbResourceInfo(TbResourceInfo resourceInfo) {
        super(resourceInfo);
        this.tenantId = resourceInfo.tenantId;
        this.title = resourceInfo.title;
        this.resourceType = resourceInfo.resourceType;
        this.resourceSubType = resourceInfo.resourceSubType;
        this.resourceKey = resourceInfo.resourceKey;
        this.searchText = resourceInfo.searchText;
        this.isPublic = resourceInfo.isPublic;
        this.publicResourceKey = resourceInfo.publicResourceKey;
        this.etag = resourceInfo.etag;
        this.fileName = resourceInfo.fileName;
        this.descriptor = resourceInfo.descriptor != null ? resourceInfo.descriptor.deepCopy() : null;
        this.externalId = resourceInfo.externalId;
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
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getName() {
        return title;
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getLink() {
        String scope = (tenantId != null && tenantId.isSysTenantId()) ? "system" : "tenant"; // tenantId is null in case of export to git
        if (resourceType == ResourceType.IMAGE) {
            return "/api/images/" + scope + "/" + resourceKey;
        } else {
            return "/api/resource/" + resourceType.name().toLowerCase() + "/" + scope + "/" + resourceKey;
        }
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getPublicLink() {
        if (resourceType == ResourceType.IMAGE && isPublic) {
            return "/api/images/public/" + getPublicResourceKey();
        }
        return null;
    }

    @JsonIgnore
    public String getSearchText() {
        return title;
    }

    @SneakyThrows
    public <T> T getDescriptor(Class<T> type) {
        return descriptor != null ? mapper.treeToValue(descriptor, type) : null;
    }

    public <T> void updateDescriptor(Class<T> type, UnaryOperator<T> updater) {
        T descriptor = getDescriptor(type);
        descriptor = updater.apply(descriptor);
        setDescriptorValue(descriptor);
    }

    @JsonIgnore
    public void setDescriptorValue(Object value) {
        this.descriptor = value != null ? mapper.valueToTree(value) : null;
    }

}

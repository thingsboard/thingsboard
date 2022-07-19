/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.common.data.widget;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@Data
public class BaseWidgetType extends BaseData<WidgetTypeId> implements HasTenantId {

    private static final long serialVersionUID = 8388684344603660756L;

    @ApiModelProperty(position = 3, value = "JSON object with Tenant Id.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private TenantId tenantId;
    @NoXss
    @Length(fieldName = "bundleAlias")
    @ApiModelProperty(position = 4, value = "Reference to widget bundle", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private String bundleAlias;
    @NoXss
    @Length(fieldName = "alias")
    @ApiModelProperty(position = 5, value = "Unique alias that is used in dashboards as a reference widget type", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private String alias;
    @NoXss
    @Length(fieldName = "name")
    @ApiModelProperty(position = 6, value = "Widget name used in search and UI", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private String name;

    public BaseWidgetType() {
        super();
    }

    public BaseWidgetType(WidgetTypeId id) {
        super(id);
    }

    public BaseWidgetType(BaseWidgetType widgetType) {
        super(widgetType);
        this.tenantId = widgetType.getTenantId();
        this.bundleAlias = widgetType.getBundleAlias();
        this.alias = widgetType.getAlias();
        this.name = widgetType.getName();
    }

    @ApiModelProperty(position = 1, value = "JSON object with the Widget Type Id. " +
            "Specify this field to update the Widget Type. " +
            "Referencing non-existing Widget Type Id will cause error. " +
            "Omit this field to create new Widget Type." )
    @Override
    public WidgetTypeId getId() {
        return super.getId();
    }

    @ApiModelProperty(position = 2, value = "Timestamp of the Widget Type creation, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }
}

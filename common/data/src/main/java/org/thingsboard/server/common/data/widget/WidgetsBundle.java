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

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.SearchTextBased;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@ApiModel
public class WidgetsBundle extends SearchTextBased<WidgetsBundleId> implements HasTenantId {

    private static final long serialVersionUID = -7627368878362410489L;

    @Getter
    @Setter
    @ApiModelProperty(position = 3, value = "JSON object with Tenant Id.", readOnly = true)
    private TenantId tenantId;

    @NoXss
    @Length(fieldName = "alias")
    @Getter
    @Setter
    @ApiModelProperty(position = 4, value = "Unique alias that is used in widget types as a reference widget bundle", readOnly = true)
    private String alias;

    @NoXss
    @Length(fieldName = "title")
    @Getter
    @Setter
    @ApiModelProperty(position = 5, value = "Title used in search and UI", readOnly = true)
    private String title;

    @Length(fieldName = "image", max = 1000000)
    @Getter
    @Setter
    @ApiModelProperty(position = 6, value = "Base64 encoded thumbnail", readOnly = true)
    private String image;

    @NoXss
    @Length(fieldName = "description")
    @Getter
    @Setter
    @ApiModelProperty(position = 7, value = "Description", readOnly = true)
    private String description;

    public WidgetsBundle() {
        super();
    }

    public WidgetsBundle(WidgetsBundleId id) {
        super(id);
    }

    public WidgetsBundle(WidgetsBundle widgetsBundle) {
        super(widgetsBundle);
        this.tenantId = widgetsBundle.getTenantId();
        this.alias = widgetsBundle.getAlias();
        this.title = widgetsBundle.getTitle();
        this.image = widgetsBundle.getImage();
        this.description = widgetsBundle.getDescription();
    }

    @ApiModelProperty(position = 1, value = "JSON object with the Widget Bundle Id. " +
            "Specify this field to update the Widget Bundle. " +
            "Referencing non-existing Widget Bundle Id will cause error. " +
            "Omit this field to create new Widget Bundle." )
    @Override
    public WidgetsBundleId getId() {
        return super.getId();
    }

    @ApiModelProperty(position = 2, value = "Timestamp of the Widget Bundle creation, in milliseconds", example = "1609459200000", readOnly = true)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @Override
    public String getSearchText() {
        return getTitle();
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (tenantId != null ? tenantId.hashCode() : 0);
        result = 31 * result + (alias != null ? alias.hashCode() : 0);
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (image != null ? image.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        WidgetsBundle that = (WidgetsBundle) o;

        if (tenantId != null ? !tenantId.equals(that.tenantId) : that.tenantId != null) return false;
        if (alias != null ? !alias.equals(that.alias) : that.alias != null) return false;
        if (title != null ? !title.equals(that.title) : that.title != null) return false;
        if (image != null ? !image.equals(that.image) : that.image != null) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("WidgetsBundle{");
        sb.append("tenantId=").append(tenantId);
        sb.append(", alias='").append(alias).append('\'');
        sb.append(", title='").append(title).append('\'');
        sb.append(", image='").append(image).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append('}');
        return sb.toString();
    }

}

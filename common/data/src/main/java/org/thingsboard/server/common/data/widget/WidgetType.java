/**
 * Copyright © 2016-2017 The Thingsboard Authors
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

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetTypeId;

public class WidgetType extends BaseData<WidgetTypeId> {

    private static final long serialVersionUID = 8388684344603660756L;

    private TenantId tenantId;
    private String bundleAlias;
    private String alias;
    private String name;
    private JsonNode descriptor;

    public WidgetType() {
        super();
    }

    public WidgetType(WidgetTypeId id) {
        super(id);
    }

    public WidgetType(WidgetType widgetType) {
        super(widgetType);
        this.tenantId = widgetType.getTenantId();
        this.bundleAlias = widgetType.getBundleAlias();
        this.alias = widgetType.getAlias();
        this.name = widgetType.getName();
        this.descriptor = widgetType.getDescriptor();
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public void setTenantId(TenantId tenantId) {
        this.tenantId = tenantId;
    }

    public String getBundleAlias() {
        return bundleAlias;
    }

    public void setBundleAlias(String bundleAlias) {
        this.bundleAlias = bundleAlias;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public JsonNode getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(JsonNode descriptor) {
        this.descriptor = descriptor;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (tenantId != null ? tenantId.hashCode() : 0);
        result = 31 * result + (bundleAlias != null ? bundleAlias.hashCode() : 0);
        result = 31 * result + (alias != null ? alias.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (descriptor != null ? descriptor.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        WidgetType that = (WidgetType) o;

        if (tenantId != null ? !tenantId.equals(that.tenantId) : that.tenantId != null) return false;
        if (bundleAlias != null ? !bundleAlias.equals(that.bundleAlias) : that.bundleAlias != null) return false;
        if (alias != null ? !alias.equals(that.alias) : that.alias != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        return descriptor != null ? descriptor.equals(that.descriptor) : that.descriptor == null;

    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("WidgetType{");
        sb.append("tenantId=").append(tenantId);
        sb.append(", bundleAlias='").append(bundleAlias).append('\'');
        sb.append(", alias='").append(alias).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", descriptor=").append(descriptor);
        sb.append('}');
        return sb.toString();
    }

}

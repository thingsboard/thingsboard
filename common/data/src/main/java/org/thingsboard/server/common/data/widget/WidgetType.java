/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetTypeId;

@EqualsAndHashCode(callSuper = true)
public class WidgetType extends BaseData<WidgetTypeId> implements HasTenantId {

    private static final long serialVersionUID = 8388684344603660756L;

    private TenantId tenantId;
    private String bundleAlias;
    private String alias;
    private String name;
    private transient JsonNode descriptor;

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

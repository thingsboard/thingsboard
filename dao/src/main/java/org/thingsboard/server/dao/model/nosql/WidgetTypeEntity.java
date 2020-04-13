/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.dao.model.nosql;

import com.datastax.driver.core.utils.UUIDs;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.type.JsonCodec;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.WIDGET_TYPE_ALIAS_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.WIDGET_TYPE_BUNDLE_ALIAS_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.WIDGET_TYPE_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.WIDGET_TYPE_DESCRIPTOR_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.WIDGET_TYPE_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.WIDGET_TYPE_TENANT_ID_PROPERTY;

@Table(name = WIDGET_TYPE_COLUMN_FAMILY_NAME)
@EqualsAndHashCode
@ToString
public final class WidgetTypeEntity implements BaseEntity<WidgetType> {

    @PartitionKey(value = 0)
    @Column(name = ID_PROPERTY)
    private UUID id;

    @PartitionKey(value = 1)
    @Column(name = WIDGET_TYPE_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @PartitionKey(value = 2)
    @Column(name = WIDGET_TYPE_BUNDLE_ALIAS_PROPERTY)
    private String bundleAlias;

    @Column(name = WIDGET_TYPE_ALIAS_PROPERTY)
    private String alias;

    @Column(name = WIDGET_TYPE_NAME_PROPERTY)
    private String name;

    @Column(name = WIDGET_TYPE_DESCRIPTOR_PROPERTY, codec = JsonCodec.class)
    private JsonNode descriptor;

    public WidgetTypeEntity() {
        super();
    }

    public WidgetTypeEntity(WidgetType widgetType) {
        if (widgetType.getId() != null) {
            this.id = widgetType.getId().getId();
        }
        if (widgetType.getTenantId() != null) {
            this.tenantId = widgetType.getTenantId().getId();
        }
        this.bundleAlias = widgetType.getBundleAlias();
        this.alias = widgetType.getAlias();
        this.name = widgetType.getName();
        this.descriptor = widgetType.getDescriptor();
    }

    @Override
    public UUID getUuid() {
        return id;
    }

    @Override
    public void setUuid(UUID id) {
        this.id = id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
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
    public WidgetType toData() {
        WidgetType widgetType = new WidgetType(new WidgetTypeId(id));
        widgetType.setCreatedTime(UUIDs.unixTimestamp(id));
        if (tenantId != null) {
            widgetType.setTenantId(new TenantId(tenantId));
        }
        widgetType.setBundleAlias(bundleAlias);
        widgetType.setAlias(alias);
        widgetType.setName(name);
        widgetType.setDescriptor(descriptor);
        return widgetType;
    }

}

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
package org.thingsboard.server.dao.model.sql;

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ModelConstants.WIDGET_TYPE_COLUMN_FAMILY_NAME)
public final class WidgetTypeEntity  extends BaseSqlEntity<WidgetType> implements BaseEntity<WidgetType> {

    @Column(name = ModelConstants.WIDGET_TYPE_TENANT_ID_PROPERTY)
    private String tenantId;

    @Column(name = ModelConstants.WIDGET_TYPE_BUNDLE_ALIAS_PROPERTY)
    private String bundleAlias;

    @Column(name = ModelConstants.WIDGET_TYPE_ALIAS_PROPERTY)
    private String alias;

    @Column(name = ModelConstants.WIDGET_TYPE_NAME_PROPERTY)
    private String name;

    @Type(type="json")
    @Column(name = ModelConstants.WIDGET_TYPE_DESCRIPTOR_PROPERTY)
    private JsonNode descriptor;

    public WidgetTypeEntity() {
        super();
    }

    public WidgetTypeEntity(WidgetType widgetType) {
        if (widgetType.getId() != null) {
            this.setUuid(widgetType.getId().getId());
        }
        if (widgetType.getTenantId() != null) {
            this.tenantId = toString(widgetType.getTenantId().getId());
        }
        this.bundleAlias = widgetType.getBundleAlias();
        this.alias = widgetType.getAlias();
        this.name = widgetType.getName();
        this.descriptor = widgetType.getDescriptor();
    }

    @Override
    public WidgetType toData() {
        WidgetType widgetType = new WidgetType(new WidgetTypeId(this.getUuid()));
        widgetType.setCreatedTime(UUIDs.unixTimestamp(this.getUuid()));
        if (tenantId != null) {
            widgetType.setTenantId(new TenantId(toUUID(tenantId)));
        }
        widgetType.setBundleAlias(bundleAlias);
        widgetType.setAlias(alias);
        widgetType.setName(name);
        widgetType.setDescriptor(descriptor);
        return widgetType;
    }

}

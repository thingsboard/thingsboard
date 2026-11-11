// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.group;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.thingsboard.server.common.data.EntityType;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EntityGroupConfiguration {

    private List<ColumnConfiguration> columns;

    public EntityGroupConfiguration() {
    }

    public List<ColumnConfiguration> getColumns() {
        return columns;
    }

    public void setColumns(List<ColumnConfiguration> columns) {
        this.columns = columns;
    }

    public static EntityGroupConfiguration createDefaultEntityGroupConfiguration(EntityType groupType) {
        EntityGroupConfiguration entityGroupConfiguration = new EntityGroupConfiguration();
        List<ColumnConfiguration> columns = new ArrayList<>();
        EntityField[] entityFields = EntityField.defaultFieldsByEntityType.get(groupType);
        if (entityFields != null) {
            for (EntityField entityField : entityFields) {
                ColumnConfiguration columnConfiguration = new ColumnConfiguration(ColumnType.ENTITY_FIELD, entityField.name().toLowerCase());
                if (entityField == EntityField.CREATED_TIME) {
                    columnConfiguration.setSortOrder(SortOrder.DESC);
                }
                columns.add(columnConfiguration);
            }
        }
        entityGroupConfiguration.setColumns(columns);
        return entityGroupConfiguration;
    }
}

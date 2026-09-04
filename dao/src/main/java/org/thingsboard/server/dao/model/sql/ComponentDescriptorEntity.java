// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.ComponentDescriptorId;
import org.thingsboard.server.common.data.plugin.ComponentClusteringMode;
import org.thingsboard.server.common.data.plugin.ComponentDescriptor;
import org.thingsboard.server.common.data.plugin.ComponentScope;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.COMPONENT_DESCRIPTOR_TABLE_NAME)
public class ComponentDescriptorEntity extends BaseSqlEntity<ComponentDescriptor> {

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.COMPONENT_DESCRIPTOR_TYPE_PROPERTY)
    private ComponentType type;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.COMPONENT_DESCRIPTOR_SCOPE_PROPERTY)
    private ComponentScope scope;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.COMPONENT_DESCRIPTOR_CLUSTERING_MODE_PROPERTY)
    private ComponentClusteringMode clusteringMode;

    @Column(name = ModelConstants.COMPONENT_DESCRIPTOR_NAME_PROPERTY)
    private String name;

    @Column(name = ModelConstants.COMPONENT_DESCRIPTOR_CLASS_PROPERTY, unique = true)
    private String clazz;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.COMPONENT_DESCRIPTOR_CONFIGURATION_DESCRIPTOR_PROPERTY)
    private JsonNode configurationDescriptor;

    @Column(name = ModelConstants.COMPONENT_DESCRIPTOR_CONFIGURATION_VERSION_PROPERTY)
    private int configurationVersion;

    @Column(name = ModelConstants.COMPONENT_DESCRIPTOR_ACTIONS_PROPERTY)
    private String actions;

    @Column(name = ModelConstants.COMPONENT_DESCRIPTOR_HAS_QUEUE_NAME_PROPERTY)
    private boolean hasQueueName;

    public ComponentDescriptorEntity() {
    }

    public ComponentDescriptorEntity(ComponentDescriptor component) {
        if (component.getId() != null) {
            this.setUuid(component.getId().getId());
        }
        this.setCreatedTime(component.getCreatedTime());
        this.actions = component.getActions();
        this.type = component.getType();
        this.scope = component.getScope();
        this.clusteringMode = component.getClusteringMode();
        this.name = component.getName();
        this.clazz = component.getClazz();
        this.configurationDescriptor = component.getConfigurationDescriptor();
        this.configurationVersion = component.getConfigurationVersion();
        this.hasQueueName = component.isHasQueueName();
    }

    @Override
    public ComponentDescriptor toData() {
        ComponentDescriptor data = new ComponentDescriptor(new ComponentDescriptorId(this.getUuid()));
        data.setCreatedTime(createdTime);
        data.setType(type);
        data.setScope(scope);
        data.setClusteringMode(clusteringMode);
        data.setName(this.getName());
        data.setClazz(this.getClazz());
        data.setActions(this.getActions());
        data.setConfigurationDescriptor(configurationDescriptor);
        data.setConfigurationVersion(configurationVersion);
        data.setHasQueueName(hasQueueName);
        return data;
    }
}

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

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.server.common.data.id.ComponentDescriptorId;
import org.thingsboard.server.common.data.plugin.ComponentDescriptor;
import org.thingsboard.server.common.data.plugin.ComponentScope;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.dao.model.SearchTextEntity;
import org.thingsboard.server.dao.model.type.JsonCodec;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.COMPONENT_DESCRIPTOR_ACTIONS_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.COMPONENT_DESCRIPTOR_CLASS_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.COMPONENT_DESCRIPTOR_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.COMPONENT_DESCRIPTOR_CONFIGURATION_DESCRIPTOR_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.COMPONENT_DESCRIPTOR_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.COMPONENT_DESCRIPTOR_SCOPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.COMPONENT_DESCRIPTOR_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.SEARCH_TEXT_PROPERTY;

/**
 * @author Andrew Shvayka
 */
@Table(name = COMPONENT_DESCRIPTOR_COLUMN_FAMILY_NAME)
public class ComponentDescriptorEntity implements SearchTextEntity<ComponentDescriptor> {

    @PartitionKey
    @Column(name = ID_PROPERTY)
    private UUID id;

    @Column(name = COMPONENT_DESCRIPTOR_TYPE_PROPERTY)
    private ComponentType type;

    @Column(name = COMPONENT_DESCRIPTOR_SCOPE_PROPERTY)
    private ComponentScope scope;

    @Column(name = COMPONENT_DESCRIPTOR_NAME_PROPERTY)
    private String name;

    @Column(name = COMPONENT_DESCRIPTOR_CLASS_PROPERTY)
    private String clazz;

    @Column(name = COMPONENT_DESCRIPTOR_CONFIGURATION_DESCRIPTOR_PROPERTY, codec = JsonCodec.class)
    private JsonNode configurationDescriptor;

    @Column(name = COMPONENT_DESCRIPTOR_ACTIONS_PROPERTY)
    private String actions;

    @Column(name = SEARCH_TEXT_PROPERTY)
    private String searchText;

    public ComponentDescriptorEntity() {
    }

    public ComponentDescriptorEntity(ComponentDescriptor component) {
        if (component.getId() != null) {
            this.id = component.getId().getId();
        }
        this.actions = component.getActions();
        this.type = component.getType();
        this.scope = component.getScope();
        this.name = component.getName();
        this.clazz = component.getClazz();
        this.configurationDescriptor = component.getConfigurationDescriptor();
        this.searchText = component.getName();
    }

    @Override
    public ComponentDescriptor toData() {
        ComponentDescriptor data = new ComponentDescriptor(new ComponentDescriptorId(id));
        data.setType(type);
        data.setScope(scope);
        data.setName(this.getName());
        data.setClazz(this.getClazz());
        data.setActions(this.getActions());
        data.setConfigurationDescriptor(this.getConfigurationDescriptor());
        return data;
    }

    @Override
    public UUID getUuid() {
        return id;
    }

    @Override
    public void setUuid(UUID id) {
        this.id = id;
    }

    public String getActions() {
        return actions;
    }

    public void setActions(String actions) {
        this.actions = actions;
    }

    public ComponentType getType() {
        return type;
    }

    public void setType(ComponentType type) {
        this.type = type;
    }

    public ComponentScope getScope() {
        return scope;
    }

    public void setScope(ComponentScope scope) {
        this.scope = scope;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    public JsonNode getConfigurationDescriptor() {
        return configurationDescriptor;
    }

    public void setConfigurationDescriptor(JsonNode configurationDescriptor) {
        this.configurationDescriptor = configurationDescriptor;
    }

    public String getSearchText() {
        return searchText;
    }

    @Override
    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    @Override
    public String getSearchTextSource() {
        return getSearchText();
    }
}

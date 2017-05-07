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
package org.thingsboard.server.dao.model.sql;

import com.datastax.driver.core.utils.UUIDs;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.PluginId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleState;
import org.thingsboard.server.common.data.plugin.PluginMetaData;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.SearchTextEntity;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
@Slf4j
@Data
@Entity
@Table(name = ModelConstants.PLUGIN_COLUMN_FAMILY_NAME)
public class PluginMetaDataEntity implements SearchTextEntity<PluginMetaData> {

    @Transient
    private static final long serialVersionUID = -6164321050824823149L;
    @Id
    @Column(name = ModelConstants.ID_PROPERTY)
    private UUID id;

    @Column(name = ModelConstants.PLUGIN_API_TOKEN_PROPERTY)
    private String apiToken;

    @Column(name = ModelConstants.PLUGIN_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ModelConstants.PLUGIN_NAME_PROPERTY)
    private String name;

    @Column(name = ModelConstants.PLUGIN_CLASS_PROPERTY)
    private String clazz;

    @Column(name = ModelConstants.PLUGIN_ACCESS_PROPERTY)
    private boolean publicAccess;

    @Column(name = ModelConstants.PLUGIN_STATE_PROPERTY)
    private ComponentLifecycleState state;

    @Column(name = ModelConstants.PLUGIN_CONFIGURATION_PROPERTY)
    private String configuration;

    @Column(name = ModelConstants.SEARCH_TEXT_PROPERTY)
    private String searchText;

    @Column(name = ModelConstants.ADDITIONAL_INFO_PROPERTY)
    private String additionalInfo;

    public PluginMetaDataEntity() {
    }

    public PluginMetaDataEntity(PluginMetaData pluginMetaData) {
        if (pluginMetaData.getId() != null) {
            this.id = pluginMetaData.getId().getId();
        }
        this.tenantId = pluginMetaData.getTenantId().getId();
        this.apiToken = pluginMetaData.getApiToken();
        this.clazz = pluginMetaData.getClazz();
        this.name = pluginMetaData.getName();
        this.publicAccess = pluginMetaData.isPublicAccess();
        this.state = pluginMetaData.getState();
        this.searchText = pluginMetaData.getName();
        if (pluginMetaData.getConfiguration() != null) {
            this.configuration = pluginMetaData.getConfiguration().toString();
        }
        if (pluginMetaData.getAdditionalInfo() != null) {
            this.additionalInfo = pluginMetaData.getAdditionalInfo().toString();
        }
    }

    @Override
    public String getSearchTextSource() {
        return searchText;
    }

    @Override
    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public void setId(UUID id) {
        this.id = id;
    }

    @Override
    public PluginMetaData toData() {
        PluginMetaData data = new PluginMetaData(new PluginId(id));
        data.setTenantId(new TenantId(tenantId));
        data.setCreatedTime(UUIDs.unixTimestamp(id));
        data.setName(name);
        data.setClazz(clazz);
        data.setPublicAccess(publicAccess);
        data.setState(state);
        data.setApiToken(apiToken);
        ObjectMapper mapper = new ObjectMapper();
        if (configuration != null) {
            try {
                JsonNode jsonNode = mapper.readTree(configuration);
                data.setConfiguration(jsonNode);
            } catch (IOException e) {
                log.warn(String.format("Error parsing JsonNode: %s. Reason: %s ", configuration, e.getMessage()), e);
            }
        }
        if (additionalInfo != null) {
            try {
                JsonNode jsonNode = mapper.readTree(additionalInfo);
                data.setAdditionalInfo(jsonNode);
            } catch (IOException e) {
                log.warn(String.format("Error parsing JsonNode: %s. Reason: %s ", additionalInfo, e.getMessage()), e);
            }
        }
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PluginMetaDataEntity entity = (PluginMetaDataEntity) o;
        return Objects.equals(id, entity.id) && Objects.equals(apiToken, entity.apiToken) && Objects.equals(tenantId, entity.tenantId)
                && Objects.equals(name, entity.name) && Objects.equals(clazz, entity.clazz) && Objects.equals(state, entity.state)
                && Objects.equals(configuration, entity.configuration)
                && Objects.equals(searchText, entity.searchText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, apiToken, tenantId, name, clazz, state, configuration, searchText);
    }

    @Override
    public String toString() {
        return "PluginMetaDataEntity{" + "id=" + id + ", apiToken='" + apiToken + '\'' + ", tenantId=" + tenantId + ", name='" + name + '\'' + ", clazz='"
                + clazz + '\'' + ", state=" + state + ", configuration=" + configuration + ", searchText='" + searchText + '\'' + '}';
    }
}

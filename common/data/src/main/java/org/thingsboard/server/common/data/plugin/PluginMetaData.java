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
package org.thingsboard.server.common.data.plugin;

import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.SearchTextBased;
import org.thingsboard.server.common.data.id.PluginId;
import org.thingsboard.server.common.data.id.TenantId;

import com.fasterxml.jackson.databind.JsonNode;

@EqualsAndHashCode(callSuper = true)
public class PluginMetaData extends SearchTextBased<PluginId> implements HasName {

    private static final long serialVersionUID = 1L;

    private String apiToken;
    private TenantId tenantId;
    private String name;
    private String clazz;
    private boolean publicAccess;
    private ComponentLifecycleState state;
    private transient JsonNode configuration;
    private transient JsonNode additionalInfo;

    public PluginMetaData() {
        super();
    }

    public PluginMetaData(PluginId id) {
        super(id);
    }

    public PluginMetaData(PluginMetaData plugin) {
        super(plugin);
        this.apiToken = plugin.getApiToken();
        this.tenantId = plugin.getTenantId();
        this.name = plugin.getName();
        this.clazz = plugin.getClazz();
        this.publicAccess = plugin.isPublicAccess();
        this.state = plugin.getState();
        this.configuration = plugin.getConfiguration();
        this.additionalInfo = plugin.getAdditionalInfo();
    }

    @Override
    public String getSearchText() {
        return getName();
    }

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public void setTenantId(TenantId tenantId) {
        this.tenantId = tenantId;
    }

    @Override
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

    public JsonNode getConfiguration() {
        return configuration;
    }

    public void setConfiguration(JsonNode configuration) {
        this.configuration = configuration;
    }

    public boolean isPublicAccess() {
        return publicAccess;
    }

    public void setPublicAccess(boolean publicAccess) {
        this.publicAccess = publicAccess;
    }

    public void setState(ComponentLifecycleState state) {
        this.state = state;
    }

    public ComponentLifecycleState getState() {
        return state;
    }

    public JsonNode getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(JsonNode additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    @Override
    public String toString() {
        return "PluginMetaData [apiToken=" + apiToken + ", tenantId=" + tenantId + ", name=" + name + ", clazz=" + clazz + ", publicAccess=" + publicAccess
                + ", configuration=" + configuration + "]";
    }

}

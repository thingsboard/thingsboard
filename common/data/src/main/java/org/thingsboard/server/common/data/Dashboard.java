/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.common.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Streams;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.id.DashboardId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({"title", "image", "mobileHide", "mobileOrder", "configuration", "name", "resources"})
public class Dashboard extends DashboardInfo implements ExportableEntity<DashboardId> {

    private static final long serialVersionUID = 872682138346187503L;

    private transient JsonNode configuration;

    @Getter
    @Setter
    private DashboardId externalId;

    @Getter
    @Setter
    private List<ResourceExportData> resources;

    public Dashboard() {
        super();
    }

    public Dashboard(DashboardId id) {
        super(id);
    }

    public Dashboard(DashboardInfo dashboardInfo) {
        super(dashboardInfo);
    }

    public Dashboard(Dashboard dashboard) {
        super(dashboard);
        this.configuration = dashboard.getConfiguration();
        this.externalId = dashboard.getExternalId();
        this.resources = dashboard.getResources() != null ? new ArrayList<>(dashboard.getResources()) : null;
    }

    @Schema(description = "JSON object with main configuration of the dashboard: layouts, widgets, aliases, etc. " +
                          "The JSON structure of the dashboard configuration is quite complex. " +
                          "The easiest way to learn it is to export existing dashboard to JSON."
            , implementation = com.fasterxml.jackson.databind.JsonNode.class)
    public JsonNode getConfiguration() {
        return configuration;
    }

    public void setConfiguration(JsonNode configuration) {
        this.configuration = configuration;
    }

    @JsonIgnore
    public List<ObjectNode> getEntityAliasesConfig() {
        return getChildObjects("entityAliases");
    }

    @JsonIgnore
    public List<ObjectNode> getWidgetsConfig() {
        return getChildObjects("widgets");
    }

    @JsonIgnore
    private List<ObjectNode> getChildObjects(String propertyName) {
        return Optional.ofNullable(configuration)
                .map(config -> config.get(propertyName))
                .filter(node -> !node.isEmpty() && (node.isObject() || node.isArray()))
                .map(node -> Streams.stream(node.elements())
                        .filter(JsonNode::isObject)
                        .map(jsonNode -> (ObjectNode) jsonNode)
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Dashboard [tenantId=");
        builder.append(getTenantId());
        builder.append(", title=");
        builder.append(getTitle());
        builder.append(", configuration=");
        builder.append(configuration);
        builder.append("]");
        return builder.toString();
    }

}

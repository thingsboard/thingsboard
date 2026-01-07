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
package org.thingsboard.server.common.data.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.id.ComponentDescriptorId;
import org.thingsboard.server.common.data.validation.Length;

import java.util.Objects;

/**
 * @author Andrew Shvayka
 */
@Schema
@ToString
public class ComponentDescriptor extends BaseData<ComponentDescriptorId> {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Type of the Rule Node", accessMode = Schema.AccessMode.READ_ONLY)
    @Getter @Setter private ComponentType type;
    @Schema(description = "Scope of the Rule Node. Always set to 'TENANT', since no rule chains on the 'SYSTEM' level yet.", accessMode = Schema.AccessMode.READ_ONLY, allowableValues = "TENANT", example = "TENANT")
    @Getter @Setter private ComponentScope scope;
    @Schema(description = "Clustering mode of the RuleNode. This mode represents the ability to start Rule Node in multiple microservices.", accessMode = Schema.AccessMode.READ_ONLY, allowableValues = {"USER_PREFERENCE", "ENABLED", "SINGLETON"}, example = "ENABLED")
    @Getter @Setter private ComponentClusteringMode clusteringMode;
    @Length(fieldName = "name")
    @Schema(description = "Name of the Rule Node. Taken from the @RuleNode annotation.", accessMode = Schema.AccessMode.READ_ONLY, example = "Custom Rule Node")
    @Getter @Setter private String name;
    @Schema(description = "Full name of the Java class that implements the Rule Engine Node interface.", accessMode = Schema.AccessMode.READ_ONLY, example = "com.mycompany.CustomRuleNode")
    @Getter @Setter private String clazz;
    @Schema(description = "Complex JSON object that represents the Rule Node configuration.", accessMode = Schema.AccessMode.READ_ONLY)
    @Getter @Setter private transient JsonNode configurationDescriptor;
    @Schema(description = "Rule node configuration version. By default, this value is 0. If the rule node is a versioned node, this value might be greater than 0.", accessMode = Schema.AccessMode.READ_ONLY)
    @Getter @Setter private int configurationVersion;
    @Length(fieldName = "actions")
    @Schema(description = "Rule Node Actions. Deprecated. Always null.", accessMode = Schema.AccessMode.READ_ONLY)
    @Getter @Setter private String actions;
    @Schema(description = "Indicates that the RuleNode supports queue name configuration.", accessMode = Schema.AccessMode.READ_ONLY, example = "true")
    @Getter @Setter private boolean hasQueueName;

    public ComponentDescriptor() {
        super();
    }

    public ComponentDescriptor(ComponentDescriptorId id) {
        super(id);
    }

    public ComponentDescriptor(ComponentDescriptor plugin) {
        super(plugin);
        this.type = plugin.getType();
        this.scope = plugin.getScope();
        this.clusteringMode = plugin.getClusteringMode();
        this.name = plugin.getName();
        this.clazz = plugin.getClazz();
        this.configurationDescriptor = plugin.getConfigurationDescriptor();
        this.configurationVersion = plugin.getConfigurationVersion();
        this.actions = plugin.getActions();
        this.hasQueueName = plugin.isHasQueueName();
    }

    @Schema(description = "JSON object with the descriptor Id. " +
            "Specify existing descriptor id to update the descriptor. " +
            "Referencing non-existing descriptor Id will cause error. " +
            "Omit this field to create new descriptor." )
    @Override
    public ComponentDescriptorId getId() {
        return super.getId();
    }

    @Schema(description = "Timestamp of the descriptor creation, in milliseconds", example = "1609459200000", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ComponentDescriptor that = (ComponentDescriptor) o;

        if (type != that.type) return false;
        if (scope != that.scope) return false;
        if (!Objects.equals(name, that.name)) return false;
        if (!Objects.equals(actions, that.actions)) return false;
        if (!Objects.equals(configurationDescriptor, that.configurationDescriptor)) return false;
        if (configurationVersion != that.configurationVersion) return false;
        if (clusteringMode != that.clusteringMode) return false;
        if (hasQueueName != that.isHasQueueName()) return false;
        return Objects.equals(clazz, that.clazz);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (scope != null ? scope.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (clazz != null ? clazz.hashCode() : 0);
        result = 31 * result + (actions != null ? actions.hashCode() : 0);
        result = 31 * result + (clusteringMode != null ? clusteringMode.hashCode() : 0);
        result = 31 * result + (hasQueueName ? 1 : 0);
        return result;
    }

}

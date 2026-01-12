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
package org.thingsboard.server.common.data.cf;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasDebugSettings;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.cf.configuration.CalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.SimpleCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.io.Serial;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
public class CalculatedField extends BaseData<CalculatedFieldId> implements HasName, HasTenantId, HasVersion, HasDebugSettings {

    @Serial
    private static final long serialVersionUID = 4491966747773381420L;

    private TenantId tenantId;
    private EntityId entityId;

    @NoXss
    @Length(fieldName = "type")
    private CalculatedFieldType type;
    @NoXss
    @Length(fieldName = "name")
    @Schema(description = "User defined name of the calculated field.")
    private String name;
    @Deprecated
    @Schema(description = "Enable/disable debug. ", example = "false", deprecated = true)
    private boolean debugMode;
    @Schema(description = "Debug settings object.")
    private DebugSettings debugSettings;
    @Schema(description = "Version of calculated field configuration.", example = "0")
    private int configurationVersion;
    @Schema(implementation = SimpleCalculatedFieldConfiguration.class)
    private CalculatedFieldConfiguration configuration;
    @Getter
    @Setter
    private Long version;

    public CalculatedField() {
        super();
    }

    public CalculatedField(CalculatedFieldId id) {
        super(id);
    }

    public CalculatedField(TenantId tenantId, EntityId entityId, CalculatedFieldType type, String name, int configurationVersion, CalculatedFieldConfiguration configuration, Long version) {
        this.tenantId = tenantId;
        this.entityId = entityId;
        this.type = type;
        this.name = name;
        this.configurationVersion = configurationVersion;
        this.configuration = configuration;
        this.version = version;
    }

    public CalculatedField(CalculatedField calculatedField) {
        super(calculatedField);
        this.tenantId = calculatedField.tenantId;
        this.entityId = calculatedField.entityId;
        this.type = calculatedField.type;
        this.name = calculatedField.name;
        this.debugMode = calculatedField.debugMode;
        this.debugSettings = calculatedField.debugSettings;
        this.configurationVersion = calculatedField.configurationVersion;
        this.configuration = calculatedField.configuration;
        this.version = calculatedField.version;
    }

    @Schema(description = "JSON object with the Calculated Field Id. Referencing non-existing Calculated Field Id will cause error.")
    @Override
    public CalculatedFieldId getId() {
        return super.getId();
    }

    @Schema(description = "Timestamp of the calculated field creation, in milliseconds", example = "1609459200000", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    // Getter is ignored for serialization
    @JsonIgnore
    public boolean isDebugMode() {
        return debugMode;
    }

    // Setter is annotated for deserialization
    @JsonSetter
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("CalculatedField[")
                .append("tenantId=").append(tenantId)
                .append(", entityId=").append(entityId)
                .append(", type='").append(type)
                .append(", name='").append(name)
                .append(", configurationVersion=").append(configurationVersion)
                .append(", configuration=").append(configuration)
                .append(", version=").append(version)
                .append(", createdTime=").append(createdTime)
                .append(", id=").append(id).append(']')
                .toString();
    }

}

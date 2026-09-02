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
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasAdditionalInfo;
import org.thingsboard.server.common.data.HasDebugSettings;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.cf.configuration.AlarmCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
public class AlarmRuleDefinition extends BaseData<CalculatedFieldId> implements HasName, HasTenantId, HasVersion, HasDebugSettings, HasAdditionalInfo {

    private TenantId tenantId;
    private EntityId entityId;

    @NoXss
    @Length(fieldName = "name")
    @Schema(description = "User defined name of the alarm rule.")
    private String name;
    @Deprecated
    @Schema(description = "Enable/disable debug. ", example = "false", deprecated = true)
    private boolean debugMode;
    @Schema(description = "Debug settings object.")
    private DebugSettings debugSettings;
    @Schema(description = "Version of alarm rule configuration.", example = "0")
    private int configurationVersion;
    @Schema(implementation = AlarmCalculatedFieldConfiguration.class)
    @Valid
    @NotNull
    private AlarmCalculatedFieldConfiguration configuration;
    private Long version;
    @NoXss
    @Schema(description = "Additional parameters of the alarm rule. " +
                          "May include: 'description' (string).",
            implementation = com.fasterxml.jackson.databind.JsonNode.class,
            example = "{\"description\":\"High temperature alarm rule\"}")
    private JsonNode additionalInfo;

    public AlarmRuleDefinition() {}

    public AlarmRuleDefinition(CalculatedFieldId id) {
        super(id);
    }

    public AlarmRuleDefinition(AlarmRuleDefinition alarmRuleDefinition) {
        super(alarmRuleDefinition);
        this.tenantId = alarmRuleDefinition.tenantId;
        this.entityId = alarmRuleDefinition.entityId;
        this.name = alarmRuleDefinition.name;
        this.debugMode = alarmRuleDefinition.debugMode;
        this.debugSettings = alarmRuleDefinition.debugSettings;
        this.configurationVersion = alarmRuleDefinition.configurationVersion;
        this.configuration = alarmRuleDefinition.configuration;
        this.version = alarmRuleDefinition.version;
        this.additionalInfo = alarmRuleDefinition.additionalInfo;
    }

    @Schema(description = "JSON object with the Alarm Rule Id. Referencing non-existing Alarm Rule Id will cause error.")
    @Override
    public CalculatedFieldId getId() {
        return super.getId();
    }

    @Schema(description = "Timestamp of the alarm rule creation, in milliseconds", example = "1609459200000", accessMode = Schema.AccessMode.READ_ONLY)
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

    public CalculatedField toCalculatedField() {
        CalculatedField cf = new CalculatedField();
        cf.setId(this.id);
        cf.setCreatedTime(this.createdTime);
        cf.setTenantId(this.tenantId);
        cf.setEntityId(this.entityId);
        cf.setType(CalculatedFieldType.ALARM);
        cf.setName(this.name);
        cf.setDebugMode(this.debugMode);
        cf.setDebugSettings(this.debugSettings);
        cf.setConfigurationVersion(this.configurationVersion);
        cf.setConfiguration(this.configuration);
        cf.setVersion(this.version);
        cf.setAdditionalInfo(this.additionalInfo);
        return cf;
    }

    public static AlarmRuleDefinition fromCalculatedField(CalculatedField cf) {
        AlarmRuleDefinition def = new AlarmRuleDefinition();
        def.setId(cf.getId());
        def.setCreatedTime(cf.getCreatedTime());
        def.setTenantId(cf.getTenantId());
        def.setEntityId(cf.getEntityId());
        def.setName(cf.getName());
        def.setDebugMode(cf.isDebugMode());
        def.setDebugSettings(cf.getDebugSettings());
        def.setConfigurationVersion(cf.getConfigurationVersion());
        if (!(cf.getConfiguration() instanceof AlarmCalculatedFieldConfiguration config)) {
            throw new IllegalArgumentException("Expected ALARM calculated field, got " + cf.getType());
        }
        def.setConfiguration(config);
        def.setVersion(cf.getVersion());
        def.setAdditionalInfo(cf.getAdditionalInfo());
        return def;
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("AlarmRuleDefinition[")
                .append("tenantId=").append(tenantId)
                .append(", entityId=").append(entityId)
                .append(", name='").append(name)
                .append(", configurationVersion=").append(configurationVersion)
                .append(", configuration=").append(configuration)
                .append(", additionalInfo=").append(additionalInfo)
                .append(", version=").append(version)
                .append(", createdTime=").append(createdTime)
                .append(", id=").append(id).append(']')
                .toString();
    }

}

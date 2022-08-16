/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.common.data.rule;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.SearchTextBasedWithAdditionalInfo;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@ApiModel
@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class RuleChain extends SearchTextBasedWithAdditionalInfo<RuleChainId> implements HasName, HasTenantId, ExportableEntity<RuleChainId> {

    private static final long serialVersionUID = -5656679015121935465L;

    @ApiModelProperty(position = 3, required = true, value = "JSON object with Tenant Id.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private TenantId tenantId;
    @NoXss
    @Length(fieldName = "name")
    @ApiModelProperty(position = 4, required = true, value = "Rule Chain name", example = "Humidity data processing")
    private String name;
    @ApiModelProperty(position = 5, value = "Rule Chain type. 'EDGE' rule chains are processing messages on the edge devices only.", example = "A4B72CCDFF33")
    private RuleChainType type;
    @ApiModelProperty(position = 6, value = "JSON object with Rule Chain Id. Pointer to the first rule node that should receive all messages pushed to this rule chain.")
    private RuleNodeId firstRuleNodeId;
    @ApiModelProperty(position = 7, value = "Indicates root rule chain. The root rule chain process messages from all devices and entities by default. User may configure default rule chain per device profile.")
    private boolean root;
    @ApiModelProperty(position = 8, value = "Reserved for future usage.")
    private boolean debugMode;
    @ApiModelProperty(position = 9, value = "Reserved for future usage. The actual list of rule nodes and their relations is stored in the database separately.")
    private transient JsonNode configuration;

    private RuleChainId externalId;

    @JsonIgnore
    private byte[] configurationBytes;

    public RuleChain() {
        super();
    }

    public RuleChain(RuleChainId id) {
        super(id);
    }

    public RuleChain(RuleChain ruleChain) {
        super(ruleChain);
        this.tenantId = ruleChain.getTenantId();
        this.name = ruleChain.getName();
        this.type = ruleChain.getType();
        this.firstRuleNodeId = ruleChain.getFirstRuleNodeId();
        this.root = ruleChain.isRoot();
        this.setConfiguration(ruleChain.getConfiguration());
        this.setExternalId(ruleChain.getExternalId());
    }

    @Override
    public String getSearchText() {
        return getName();
    }

    @Override
    public String getName() {
        return name;
    }

    @ApiModelProperty(position = 1, value = "JSON object with the Rule Chain Id. " +
            "Specify this field to update the Rule Chain. " +
            "Referencing non-existing Rule Chain Id will cause error. " +
            "Omit this field to create new rule chain." )
    @Override
    public RuleChainId getId() {
        return super.getId();
    }

    @ApiModelProperty(position = 2, value = "Timestamp of the rule chain creation, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    public JsonNode getConfiguration() {
        return SearchTextBasedWithAdditionalInfo.getJson(() -> configuration, () -> configurationBytes);
    }

    public void setConfiguration(JsonNode data) {
        setJson(data, json -> this.configuration = json, bytes -> this.configurationBytes = bytes);
    }

}

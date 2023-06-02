/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
import org.thingsboard.server.common.data.BaseDataWithAdditionalInfo;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@ApiModel
@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class RuleNode extends BaseDataWithAdditionalInfo<RuleNodeId> implements HasName {

    private static final long serialVersionUID = -5656679015121235465L;

    @ApiModelProperty(position = 3, value = "JSON object with the Rule Chain Id. ", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private RuleChainId ruleChainId;
    @Length(fieldName = "type")
    @ApiModelProperty(position = 4, value = "Full Java Class Name of the rule node implementation. ", example = "com.mycompany.iot.rule.engine.ProcessingNode")
    private String type;
    @NoXss
    @Length(fieldName = "name")
    @ApiModelProperty(position = 5, value = "User defined name of the rule node. Used on UI and for logging. ", example = "Process sensor reading")
    private String name;
    @ApiModelProperty(position = 6, value = "Enable/disable debug. ", example = "false")
    private boolean debugMode;
    @ApiModelProperty(position = 7, value = "Enable/disable singleton mode. ", example = "false")
    private boolean singletonMode;
    @ApiModelProperty(position = 8, value = "JSON with the rule node configuration. Structure depends on the rule node implementation.", dataType = "com.fasterxml.jackson.databind.JsonNode")
    private transient JsonNode configuration;
    @JsonIgnore
    private byte[] configurationBytes;

    private RuleNodeId externalId;

    public RuleNode() {
        super();
    }

    public RuleNode(RuleNodeId id) {
        super(id);
    }

    public RuleNode(RuleNode ruleNode) {
        super(ruleNode);
        this.ruleChainId = ruleNode.getRuleChainId();
        this.type = ruleNode.getType();
        this.name = ruleNode.getName();
        this.debugMode = ruleNode.isDebugMode();
        this.singletonMode = ruleNode.isSingletonMode();
        this.setConfiguration(ruleNode.getConfiguration());
        this.externalId = ruleNode.getExternalId();
    }

    @Override
    public String getName() {
        return name;
    }

    public JsonNode getConfiguration() {
        return BaseDataWithAdditionalInfo.getJson(() -> configuration, () -> configurationBytes);
    }

    public void setConfiguration(JsonNode data) {
        setJson(data, json -> this.configuration = json, bytes -> this.configurationBytes = bytes);
    }

    @ApiModelProperty(position = 1, value = "JSON object with the Rule Node Id. " +
            "Specify this field to update the Rule Node. " +
            "Referencing non-existing Rule Node Id will cause error. " +
            "Omit this field to create new rule node." )
    @Override
    public RuleNodeId getId() {
        return super.getId();
    }

    @ApiModelProperty(position = 2, value = "Timestamp of the rule node creation, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @ApiModelProperty(position = 8, value = "Additional parameters of the rule node. Contains 'layoutX' and 'layoutY' properties for visualization.", dataType = "com.fasterxml.jackson.databind.JsonNode")
    @Override
    public JsonNode getAdditionalInfo() {
        return super.getAdditionalInfo();
    }

}

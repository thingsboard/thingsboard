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

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;

import java.util.Set;

@ApiModel
@Data
@Slf4j
public class RuleChainOutputLabelsUsage {

    @ApiModelProperty(position = 1, required = true, value = "Rule Chain Id", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private RuleChainId ruleChainId;
    @ApiModelProperty(position = 2, required = true, value = "Rule Node Id", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private RuleNodeId ruleNodeId;

    @ApiModelProperty(position = 3, required = true, value = "Rule Chain Name", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private String ruleChainName;
    @ApiModelProperty(position = 4, required = true, value = "Rule Node Name", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private String ruleNodeName;
    @ApiModelProperty(position = 5, required = true, value = "Output labels", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private Set<String> labels;

}

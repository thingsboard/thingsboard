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

/**
 * Created by ashvayka on 21.03.18.
 */
@ApiModel
@Data
public class NodeConnectionInfo {
    @ApiModelProperty(position = 1, required = true, value = "Index of rule node in the 'nodes' array of the RuleChainMetaData. Indicates the 'from' part of the connection.")
    private int fromIndex;
    @ApiModelProperty(position = 2, required = true, value = "Index of rule node in the 'nodes' array of the RuleChainMetaData. Indicates the 'to' part of the connection.")
    private int toIndex;
    @ApiModelProperty(position = 3, required = true, value = "Type of the relation. Typically indicated the result of processing by the 'from' rule node. For example, 'Success' or 'Failure'")
    private String type;
}

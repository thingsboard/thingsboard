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
package org.thingsboard.server.common.data.lwm2m;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel
@Data
public class LwM2mObject {
    @ApiModelProperty(position = 1, value = "LwM2M Object id.", example = "19")
    int id;
    @ApiModelProperty(position = 2, value = "LwM2M Object key id.", example = "19_1.0")
    String keyId;
    @ApiModelProperty(position = 3, value = "LwM2M Object name.", example = "BinaryAppDataContainer")
    String name;
    @ApiModelProperty(position = 4, value = "LwM2M Object multiple.", example = "true")
    boolean multiple;
    @ApiModelProperty(position = 5, value = "LwM2M Object mandatory.", example = "false")
    boolean mandatory;
    @ApiModelProperty(position = 6, value = "LwM2M Object instances.")
    LwM2mInstance [] instances;
}

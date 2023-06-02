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
package org.thingsboard.server.common.data;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class SystemInfoData {
    @ApiModelProperty(position = 1, value = "Service Id.")
    private String serviceId;
    @ApiModelProperty(position = 2, value = "Service type.")
    private String serviceType;
    @ApiModelProperty(position = 3, value = "CPU usage, in percent.")
    private Long cpuUsage;
    @ApiModelProperty(position = 4, value = "Total CPU usage.")
    private Long cpuCount;
    @ApiModelProperty(position = 5, value = "Memory usage, in percent.")
    private Long memoryUsage;
    @ApiModelProperty(position = 6, value = "Total memory in bytes.")
    private Long totalMemory;
    @ApiModelProperty(position = 7, value = "Disk usage, in percent.")
    private Long discUsage;
    @ApiModelProperty(position = 8, value = "Total disc space in bytes.")
    private Long totalDiscSpace;

}

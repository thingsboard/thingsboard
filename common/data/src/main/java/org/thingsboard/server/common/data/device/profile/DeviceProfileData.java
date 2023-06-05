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
package org.thingsboard.server.common.data.device.profile;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import jakarta.validation.Valid;
import java.io.Serializable;
import java.util.List;

@ApiModel
@Data
public class DeviceProfileData implements Serializable {

    @ApiModelProperty(position = 1, value = "JSON object of device profile configuration")
    private DeviceProfileConfiguration configuration;
    @Valid
    @ApiModelProperty(position = 2, value = "JSON object of device profile transport configuration")
    private DeviceProfileTransportConfiguration transportConfiguration;
    @ApiModelProperty(position = 3, value = "JSON object of provisioning strategy type per device profile")
    private DeviceProfileProvisionConfiguration provisionConfiguration;
    @Valid
    @ApiModelProperty(position = 4, value = "JSON array of alarm rules configuration per device profile")
    private List<DeviceProfileAlarm> alarms;

}

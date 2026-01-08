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
package org.thingsboard.server.common.data.device.data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Schema
@Data
public class DeviceData implements Serializable {

    private static final long serialVersionUID = -3771567735290681274L;

    @Schema(description = "Device configuration for device profile type. DEFAULT is only supported value for now")
    private DeviceConfiguration configuration;
    @Schema(description = "Device transport configuration used to connect the device")
    private DeviceTransportConfiguration transportConfiguration;

}

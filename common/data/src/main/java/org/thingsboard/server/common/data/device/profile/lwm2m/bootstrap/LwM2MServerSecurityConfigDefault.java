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
package org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel
@Data
public class LwM2MServerSecurityConfigDefault extends LwM2MServerSecurityConfig {
    @ApiModelProperty(position = 5, value = "Host for 'Security' mode (DTLS)", example = "0.0.0.0", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    protected String securityHost;
    @ApiModelProperty(position = 6, value = "Port for 'Security' mode (DTLS): Lwm2m Server or Bootstrap Server", example = "5686 or 5688", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    protected Integer securityPort;
}

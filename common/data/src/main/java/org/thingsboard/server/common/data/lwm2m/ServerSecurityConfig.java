/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
public class ServerSecurityConfig {
    @ApiModelProperty(position = 1, value = "Is Bootstrap Server", example = "true", readOnly = true)
    boolean bootstrapServerIs = true;
    @ApiModelProperty(position = 2, value = "Host for 'No Security' mode", example = "0.0.0.0", readOnly = true)
    String host;
    @ApiModelProperty(position = 3, value = "Port for 'No Security' mode", example = "5687", readOnly = true)
    Integer port;
    @ApiModelProperty(position = 4, value = "Host for 'Security' mode (DTLS)", example = "0.0.0.0", readOnly = true)
    String securityHost;
    @ApiModelProperty(position = 5, value = "Port for 'Security' mode (DTLS)", example = "5688", readOnly = true)
    Integer securityPort;
    @ApiModelProperty(position = 5, value = "Server short Id", example = "111", readOnly = true)
    Integer serverId = 111;
    @ApiModelProperty(position = 7, value = "Client Hold Off Time", example = "1", readOnly = true)
    Integer clientHoldOffTime = 1;
    @ApiModelProperty(position = 8, value = "Server Public Key (base64 encoded)", example = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEAZ0pSaGKHk/GrDaUDnQZpeEdGwX7m3Ws+U/kiVat\n" +
            "+44sgk3c8g0LotfMpLlZJPhPwJ6ipXV+O1r7IZUjBs3LNA==", readOnly = true)
    String serverPublicKey;
    @ApiModelProperty(position = 9, value = "Bootstrap Server Account Timeout", example = "0", readOnly = true)
    Integer bootstrapServerAccountTimeout = 0;
}

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
package org.thingsboard.server.common.data.device.profile.lwm2mTransportConfiguration;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel
@Data
public class BootstrapServerConfigDao {
    @ApiModelProperty(position = 1, value = "Is Bootstrap Server or Lwm2m Server", example = "true or false", readOnly = true)
    boolean bootstrapServerIs = true;
    @ApiModelProperty(position = 2, value = "Host for 'No Security' mode", example = "0.0.0.0", readOnly = true)
    String host;
    @ApiModelProperty(position = 3, value = "Port for  Lwm2m Server: 'No Security' mode: Lwm2m Server or Bootstrap Server", example = "'5685' or '5687'", readOnly = true)
    Integer port;
    @ApiModelProperty(position = 4, value = "Host for 'Security' mode (DTLS)", example = "0.0.0.0", readOnly = true)
    String securityHost;
    @ApiModelProperty(position = 5, value = "Port for 'Security' mode (DTLS): Lwm2m Server or Bootstrap Server", example = "5686 or 5688", readOnly = true)
    Integer securityPort;
    @ApiModelProperty(position = 6, value = "Server short Id", example = "111", readOnly = true)
    Integer serverId = 111;
    @ApiModelProperty(position = 7, value = "Client Hold Off Time. The number of seconds to wait before initiating a Client Initiated Bootstrap once the LwM2M Client has determined it should initiate this bootstrap mode. (This information is relevant for use with a Bootstrap-Server only.)", example = "1", readOnly = true)
    Integer clientHoldOffTime = 1;
    @ApiModelProperty(position = 8, value = "Server Public Key for 'Security' mode (DTLS): RPK or X509. Format: base64 encoded", example = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEAZ0pSaGKHk/GrDaUDnQZpeEdGwX7m3Ws+U/kiVat\n" +
            "+44sgk3c8g0LotfMpLlZJPhPwJ6ipXV+O1r7IZUjBs3LNA==", readOnly = true)
    String serverPublicKey;
    @ApiModelProperty(position = 9, value = "Bootstrap Server Account Timeout (If the value is set to 0, or if this resource is not instantiated, the Bootstrap-Server Account lifetime is infinite.)", example = "0", readOnly = true)
    Integer bootstrapServerAccountTimeout = 0;
}

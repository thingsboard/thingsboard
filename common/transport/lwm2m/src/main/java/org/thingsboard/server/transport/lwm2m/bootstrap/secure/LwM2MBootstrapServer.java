/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.transport.lwm2m.bootstrap.secure;

import lombok.Builder;
import lombok.Data;
import org.eclipse.leshan.core.SecurityMode;

@Data
public class LwM2MBootstrapServer {
    @Builder.Default
    private String host = "0.0.0.0";
    @Builder.Default
    private Integer port = 0;
    @Builder.Default
    private boolean bootstrapServerIs = false;
    @Builder.Default
    private String securityMode = SecurityMode.NO_SEC.name();
    @Builder.Default
    private String clientPublicKeyOrId;
    @Builder.Default
    private String clientSecretKey;
    @Builder.Default
    private String serverPublicKey;
    @Builder.Default
    private Integer clientHoldOffTime = 1;
    @Builder.Default
    private Integer serverId = 123;
    @Builder.Default
    private Integer bootstrapServerAccountTimeout = 0;

}

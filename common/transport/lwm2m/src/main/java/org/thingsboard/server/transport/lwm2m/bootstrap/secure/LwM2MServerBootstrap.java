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
package org.thingsboard.server.transport.lwm2m.bootstrap.secure;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.SecurityMode;

@Slf4j
@Data
public class LwM2MServerBootstrap {

    @Builder.Default
    String clientPublicKeyOrId = "";
    @Builder.Default
    String clientSecretKey = "";
    @Builder.Default
    String serverPublicKey = "";
    @Builder.Default
    Integer clientHoldOffTime = 1;
    @Builder.Default
    Integer bootstrapServerAccountTimeout = 0;

    @Builder.Default
    String host = "0.0.0.0";
    @Builder.Default
    Integer port = 0;

    @Builder.Default
    String securityMode = SecurityMode.NO_SEC.name();

    @Builder.Default
    Integer serverId = 123;
    @Builder.Default
    boolean bootstrapServerIs = false;

    public LwM2MServerBootstrap(){};

    public LwM2MServerBootstrap(LwM2MServerBootstrap bootstrapFromCredential, LwM2MServerBootstrap profileServerBootstrap) {
            this.clientPublicKeyOrId = bootstrapFromCredential.getClientPublicKeyOrId();
            this.clientSecretKey = bootstrapFromCredential.getClientSecretKey();
            this.serverPublicKey = profileServerBootstrap.getServerPublicKey();
            this.clientHoldOffTime = profileServerBootstrap.getClientHoldOffTime();
            this.bootstrapServerAccountTimeout = profileServerBootstrap.getBootstrapServerAccountTimeout();
            this.host = (profileServerBootstrap.getHost().equals("0.0.0.0")) ? "localhost" : profileServerBootstrap.getHost();
            this.port = profileServerBootstrap.getPort();
            this.securityMode = profileServerBootstrap.getSecurityMode();
            this.serverId = profileServerBootstrap.getServerId();
            this.bootstrapServerIs = profileServerBootstrap.bootstrapServerIs;
    }
}

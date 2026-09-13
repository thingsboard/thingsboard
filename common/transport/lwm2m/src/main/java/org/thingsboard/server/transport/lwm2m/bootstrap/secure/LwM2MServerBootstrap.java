// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.bootstrap.secure;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.SecurityMode;

@Slf4j
@Data
public class LwM2MServerBootstrap {

    String clientPublicKeyOrId = "";
    String clientSecretKey = "";
    String serverPublicKey = "";
    Integer clientHoldOffTime = 1;
    Integer bootstrapServerAccountTimeout = 0;

    String host = "0.0.0.0";
    Integer port = 0;
    String securityHost = "0.0.0.0";
    Integer securityPort = 0;

    SecurityMode securityMode = SecurityMode.NO_SEC;

    Integer serverId = 123;
    boolean bootstrapServerIs = false;

    public LwM2MServerBootstrap() {
    }

    public LwM2MServerBootstrap(LwM2MServerBootstrap bootstrapFromCredential, LwM2MServerBootstrap profileServerBootstrap) {
        this.clientPublicKeyOrId = bootstrapFromCredential.getClientPublicKeyOrId();
        this.clientSecretKey = bootstrapFromCredential.getClientSecretKey();
        this.serverPublicKey = profileServerBootstrap.getServerPublicKey();
        this.clientHoldOffTime = profileServerBootstrap.getClientHoldOffTime();
        this.bootstrapServerAccountTimeout = profileServerBootstrap.getBootstrapServerAccountTimeout();
        this.host = (profileServerBootstrap.getHost().equals("0.0.0.0")) ? "localhost" : profileServerBootstrap.getHost();
        this.port = profileServerBootstrap.getPort();
        this.securityHost = (profileServerBootstrap.getSecurityHost().equals("0.0.0.0")) ? "localhost" : profileServerBootstrap.getSecurityHost();
        this.securityPort = profileServerBootstrap.getSecurityPort();
        this.securityMode = profileServerBootstrap.getSecurityMode();
        this.serverId = profileServerBootstrap.getServerId();
        this.bootstrapServerIs = profileServerBootstrap.bootstrapServerIs;
    }
}

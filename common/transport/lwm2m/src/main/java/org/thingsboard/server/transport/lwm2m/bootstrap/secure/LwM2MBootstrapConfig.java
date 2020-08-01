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
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig;

import java.nio.charset.StandardCharsets;

@Data
public class LwM2MBootstrapConfig {
    /**
     * interface BootstrapSecurityConfig
     *   servers: BootstrapServersSecurityConfig,
     *   bootstrapServer: ServerSecurityConfig,
     *   lwm2mServer: ServerSecurityConfig
     * }
     */
    /** -servers
     *   shortId: number,
     *   lifetime: number,
     *   defaultMinPeriod: number,
     *   notifIfDisabled: boolean,
     *   binding: string
     * */
    @Builder.Default
    LwM2MBootstrapServers servers;

    /** -bootstrapServer, lwm2mServer
     * interface ServerSecurityConfig
     *   host?: string,
     *   port?: number,
     *   isBootstrapServer?: boolean,
     *   securityMode: string,
     *   clientPublicKeyOrId?: string,
     *   clientSecretKey?: string,
     *   serverPublicKey?: string;
     *   clientHoldOffTime?: number,
     *   serverId?: number,
     *   bootstrapServerAccountTimeout: number
     * */
    LwM2MServerBootstrap bootstrapServer;

    LwM2MServerBootstrap lwm2mServer;

    public BootstrapConfig getLwM2MBootstrapConfig()  {
        BootstrapConfig configBs = new BootstrapConfig();
        /** Delete old security objects */
        configBs.toDelete.add("/0");
        configBs.toDelete.add("/1");
        /** Server Configuration (object 1) as defined in LWM2M 1.0.x TS. */
        BootstrapConfig.ServerConfig server0 = new BootstrapConfig.ServerConfig();
        server0.shortId = servers.getShortId();
        server0.lifetime = servers.getLifetime();
        server0.defaultMinPeriod = servers.getDefaultMinPeriod();
        server0.notifIfDisabled = servers.isNotifIfDisabled();
        server0.binding = BindingMode.valueOf(servers.getBinding());
        configBs.servers.put(0, server0);
        /** Security Configuration (object 0) as defined in LWM2M 1.0.x TS. Bootstrap instance = 0 */
        configBs.security.put(0, setServerSecuruty(this.bootstrapServer.getHost(), this.bootstrapServer.getPort(), this.bootstrapServer.isBootstrapServerIs(), this.bootstrapServer.getSecurityMode(), this.bootstrapServer.getClientPublicKeyOrId(), this.bootstrapServer.getServerPublicKey(), this.bootstrapServer.getClientSecretKey(), this.bootstrapServer.getServerId()));
        /** Security Configuration (object 0) as defined in LWM2M 1.0.x TS. Server instance = 1 */
        configBs.security.put(1, setServerSecuruty(this.lwm2mServer.getHost(), this.lwm2mServer.getPort(), this.lwm2mServer.isBootstrapServerIs(), this.lwm2mServer.getSecurityMode(), this.lwm2mServer.getClientPublicKeyOrId(), this.lwm2mServer.getServerPublicKey(), this.lwm2mServer.getClientSecretKey(), this.lwm2mServer.getServerId()));
        return configBs;
    }

    private BootstrapConfig.ServerSecurity setServerSecuruty(String host, Integer port, boolean bootstrapServer, String securityMode, String clientPublicKey, String serverPublicKey, String secretKey, int serverId) {
        BootstrapConfig.ServerSecurity serverSecurity = new BootstrapConfig.ServerSecurity();
        serverSecurity.uri =  "coaps://" + host + ":" + Integer.toString(port);
        serverSecurity.bootstrapServer = bootstrapServer;
        serverSecurity.securityMode =  SecurityMode.valueOf(securityMode);
        serverSecurity.publicKeyOrId = setPublicKeyOrId (clientPublicKey, securityMode);
        serverSecurity.serverPublicKey = (serverPublicKey != null && !serverPublicKey.isEmpty()) ? Hex.decodeHex(serverPublicKey.toCharArray()) : new byte[] {};
        serverSecurity.secretKey = (secretKey != null && !secretKey.isEmpty()) ? Hex.decodeHex(secretKey.toCharArray())  : new byte[] {};
        serverSecurity.serverId = serverId;

        return serverSecurity;
    }

    private byte[] setPublicKeyOrId (String publicKeyOrIdStr, String securityMode) {
        byte [] publicKey = (publicKeyOrIdStr == null || publicKeyOrIdStr.isEmpty()) ?  new byte[] {} :
                        SecurityMode.valueOf(securityMode).equals(SecurityMode.PSK) ? publicKeyOrIdStr.getBytes(StandardCharsets.UTF_8)  :
                        Hex.decodeHex(publicKeyOrIdStr.toCharArray());
        return publicKey;
    }


}

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
package org.thingsboard.server.transport.lwm2m.bootstrap.secure;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.SecurityMode;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig;
import org.thingsboard.server.common.data.device.credentials.lwm2m.AbstractLwM2MBootstrapClientCredentialWithKeys;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MBootstrapClientCredential;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MSecurityMode;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.AbstractLwM2MBootstrapServerCredential;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.LwM2MBootstrapServerCredential;

import java.io.Serializable;
import java.util.List;

@Slf4j
@Data
public class LwM2MBootstrapConfig implements Serializable {

    private static final long serialVersionUID = -4729088085817468640L;

    List<LwM2MBootstrapServerCredential> serverConfiguration;

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
    @Getter
    @Setter
    private LwM2MBootstrapClientCredential bootstrapServer;

    @Getter
    @Setter
    private LwM2MBootstrapClientCredential lwm2mServer;

    public LwM2MBootstrapConfig(){};

    public LwM2MBootstrapConfig(List<LwM2MBootstrapServerCredential> serverConfiguration, LwM2MBootstrapClientCredential bootstrapClientServer, LwM2MBootstrapClientCredential lwm2mClientServer) {
        this.serverConfiguration = serverConfiguration;
        this.bootstrapServer = bootstrapClientServer;
        this.lwm2mServer = lwm2mClientServer;

    }

    @JsonIgnore
    public BootstrapConfig getLwM2MBootstrapConfig() {
        BootstrapConfig configBs = new BootstrapConfig();
        configBs.autoIdForSecurityObject = true;
        int id = 0;
        for (LwM2MBootstrapServerCredential serverCredential : serverConfiguration) {
            BootstrapConfig.ServerConfig serverConfig = setServerConfig((AbstractLwM2MBootstrapServerCredential) serverCredential);
            configBs.servers.put(id, serverConfig);
            BootstrapConfig.ServerSecurity serverSecurity = setServerSecurity((AbstractLwM2MBootstrapServerCredential) serverCredential, serverCredential.getSecurityMode());
            configBs.security.put(id, serverSecurity);
            id++;
        }
        /** in LwM2mDefaultBootstrapSessionManager -> initTasks
         * Delete all security/config objects if update bootstrap server and lwm2m server
         * if other: del or update only instances */

        return configBs;
    }

    private BootstrapConfig.ServerSecurity setServerSecurity(AbstractLwM2MBootstrapServerCredential serverCredential, LwM2MSecurityMode securityMode) {
        BootstrapConfig.ServerSecurity serverSecurity = new BootstrapConfig.ServerSecurity();
        String serverUri = "coap://";
        byte[] publicKeyOrId = new byte[]{};
        byte[] secretKey = new byte[]{};
        byte[] serverPublicKey = new byte[]{};
        serverSecurity.serverId = serverCredential.getShortServerId();
        serverSecurity.securityMode = SecurityMode.valueOf(securityMode.name());
        serverSecurity.bootstrapServer = serverCredential.isBootstrapServerIs();
        if (!LwM2MSecurityMode.NO_SEC.equals(securityMode)) {
            AbstractLwM2MBootstrapClientCredentialWithKeys server;
            if (serverSecurity.bootstrapServer) {
                server = (AbstractLwM2MBootstrapClientCredentialWithKeys) this.bootstrapServer;

            } else {
                server = (AbstractLwM2MBootstrapClientCredentialWithKeys) this.lwm2mServer;
            }
            serverUri = "coaps://";
            if (LwM2MSecurityMode.PSK.equals(securityMode)) {
                publicKeyOrId = server.getClientPublicKeyOrId().getBytes();
                secretKey = Hex.decodeHex(server.getClientSecretKey().toCharArray());
            } else {
                publicKeyOrId = server.getDecodedClientPublicKeyOrId();
                secretKey = server.getDecodedClientSecretKey();
            }
            serverPublicKey = serverCredential.getDecodedCServerPublicKey();
        }
        serverUri += (((serverCredential.getHost().equals("0.0.0.0") ? "localhost" : serverCredential.getHost()) + ":" + serverCredential.getPort()));
        serverSecurity.uri = serverUri;
        serverSecurity.publicKeyOrId = publicKeyOrId;
        serverSecurity.secretKey = secretKey;
        serverSecurity.serverPublicKey = serverPublicKey;
        return serverSecurity;
    }

    private BootstrapConfig.ServerConfig setServerConfig (AbstractLwM2MBootstrapServerCredential serverCredential) {
        BootstrapConfig.ServerConfig serverConfig = new BootstrapConfig.ServerConfig();
        if (serverCredential.getShortServerId() != null) {
            serverConfig.shortId = serverCredential.getShortServerId();
        }
        serverConfig.lifetime = serverCredential.getLifetime();
        serverConfig.defaultMinPeriod = serverCredential.getDefaultMinPeriod();
        serverConfig.notifIfDisabled = serverCredential.isNotifIfDisabled();
        serverConfig.binding = BindingMode.parse(serverCredential.getBinding());
        return serverConfig;
    }
}

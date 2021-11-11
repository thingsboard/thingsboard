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
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Data
public class LwM2MBootstrapConfig implements Serializable {

    List<LwM2MBootstrapServerCredential> serverConfiguration;
    /*
      interface BootstrapSecurityConfig
        servers: BootstrapServersSecurityConfig,
        bootstrapServer: ServerSecurityConfig,
        lwm2mServer: ServerSecurityConfig
      }
     */
    /** -servers
     *   shortId: number,
     *   lifetime: number,
     *   defaultMinPeriod: number,
     *   notifIfDisabled: boolean,
     *   binding: string
     * */
//    @Getter
//    @Setter
//    private LwM2MBootstrapServers servers;

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
        AtomicInteger index = new AtomicInteger();
        /** Delete old security/config objects in LwM2mDefaultBootstrapSessionManager -> initTasks */
        configBs.toDelete.add("/0");
        configBs.toDelete.add("/1");
        serverConfiguration.forEach(serverCredential -> {
            BootstrapConfig.ServerConfig serverConfig = new BootstrapConfig.ServerConfig();
            serverConfig.shortId = ((AbstractLwM2MBootstrapServerCredential)serverCredential).getShortServerId();
            serverConfig.lifetime = ((AbstractLwM2MBootstrapServerCredential)serverCredential).getLifetime();
            serverConfig.defaultMinPeriod = ((AbstractLwM2MBootstrapServerCredential)serverCredential).getDefaultMinPeriod();
            serverConfig.notifIfDisabled = ((AbstractLwM2MBootstrapServerCredential)serverCredential).isNotifIfDisabled();
            serverConfig.binding = BindingMode.parse(((AbstractLwM2MBootstrapServerCredential)serverCredential).getBinding());
            int k = index.get();
            configBs.servers.put(k, serverConfig);
            BootstrapConfig.ServerSecurity serverSecurity = setServerSecurity((AbstractLwM2MBootstrapServerCredential)serverCredential, serverCredential.getSecurityMode());
            configBs.security.put(k, serverSecurity);
            index.getAndIncrement();

        });
        return configBs;
    }

    private BootstrapConfig.ServerSecurity setServerSecurity(AbstractLwM2MBootstrapServerCredential serverCredential, LwM2MSecurityMode securityMode) {
        BootstrapConfig.ServerSecurity serverSecurity = new BootstrapConfig.ServerSecurity();
        String serverUri = "coap://";
        byte[] publicKeyOrId = new byte[]{};;
        byte[] secretKey = new byte[]{};;
        serverSecurity.serverId = serverCredential.getShortServerId();
        serverSecurity.securityMode = SecurityMode.valueOf(securityMode.name());
        serverSecurity.bootstrapServer = serverCredential.isBootstrapServerIs();
        if (!LwM2MSecurityMode.NO_SEC.equals(securityMode)) {
            serverUri = "coaps://";
            if (serverSecurity.bootstrapServer) {
                publicKeyOrId = ((AbstractLwM2MBootstrapClientCredentialWithKeys)this.bootstrapServer).getDecodedClientPublicKeyOrId();
                secretKey = ((AbstractLwM2MBootstrapClientCredentialWithKeys)this.bootstrapServer).getDecodedClientSecretKey();

            } else {
                publicKeyOrId = ((AbstractLwM2MBootstrapClientCredentialWithKeys)this.lwm2mServer).getDecodedClientPublicKeyOrId();
                secretKey = ((AbstractLwM2MBootstrapClientCredentialWithKeys)this.lwm2mServer).getDecodedClientSecretKey();
            }

        }
        serverUri += (((serverCredential.getHost().equals("0.0.0.0") ? "localhost" : serverCredential.getHost()) + ":" + serverCredential.getPort()));
        log.info("serverSecurity.uri = [{}]", serverUri);
        log.info("publicKeyOrId  [{}]", Hex.encodeHexString(publicKeyOrId));
        log.info("secretKey [{}]", Hex.encodeHexString(secretKey));
        log.info("server [{}]", Hex.encodeHexString(serverCredential.getDecodedCServerPublicKey()));
        serverSecurity.uri = serverUri;
        serverSecurity.publicKeyOrId = publicKeyOrId;
        serverSecurity.secretKey = secretKey;
        serverSecurity.serverPublicKey = serverCredential.getDecodedCServerPublicKey();
        return serverSecurity;
    }
}

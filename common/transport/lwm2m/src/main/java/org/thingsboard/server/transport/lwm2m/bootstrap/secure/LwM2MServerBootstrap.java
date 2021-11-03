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

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.SecurityMode;
import org.thingsboard.server.common.data.device.credentials.lwm2m.AbstractLwM2MServerCredentialsWithKeys;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MServerCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.NoSecServerCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.PSKServerCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.RPKServerCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.X509ServerCredentials;
import org.thingsboard.server.common.data.device.profile.lwm2mTransportConfiguration.AbstractBootstrapServerConfig;
import org.thingsboard.server.common.data.device.profile.lwm2mTransportConfiguration.BootstrapServerConfig;
import org.thingsboard.server.common.data.device.profile.lwm2mTransportConfiguration.NoSecBootstrapServerConfig;
import org.thingsboard.server.common.data.device.profile.lwm2mTransportConfiguration.PSKBootstrapServerConfig;
import org.thingsboard.server.common.data.device.profile.lwm2mTransportConfiguration.RPKBootstrapServerConfig;
import org.thingsboard.server.common.data.device.profile.lwm2mTransportConfiguration.X509BootstrapServerConfig;

@Slf4j
@Data
public class LwM2MServerBootstrap {
    SecurityMode securityMode = SecurityMode.NO_SEC;

        // AbstractLwM2MServerCredentialsWithKeys -> LwM2MServerCredentials
        LwM2MServerCredentials bootstrapClientCredentials;
//    String clientPublicKeyOrId = "";
//    String clientSecretKey = "";

        // AbstractBootstrapServerConfig -> BootstrapServerConfig
        BootstrapServerConfig bootstrapServerConfig;
//    Integer clientHoldOffTime = 1;
//    Integer bootstrapServerAccountTimeout = 0;
//    String serverPublicKey = "";
//    String host = "0.0.0.0";
//    Integer port = 0;
//    String securityHost = "0.0.0.0";
//    Integer securityPort = 0;
//    Integer serverId = 123;
//    boolean bootstrapServerIs = false;

    public LwM2MServerBootstrap() {
    }

//    public LwM2MServerBootstrap(LwM2MServerBootstrap bootstrapFromCredential, LwM2MServerBootstrap profileServerBootstrap) {
    public LwM2MServerBootstrap(LwM2MServerCredentials bootstrapFromClientCredential, BootstrapServerConfig bootstrapServerFromProfile) {
        this.securityMode = SecurityMode.valueOf(bootstrapServerFromProfile.getSecurityMode().name());
        this.bootstrapClientCredentials = bootstrapFromClientCredential;
        this.bootstrapServerConfig = bootstrapServerFromProfile;
//        if (!this.securityMode.equals(SecurityMode.NO_SEC)) {
//            setBootstrapClientCredentials (bootstrapFromClientCredential);
//        } else {
//            bootstrapClientCredentials = new NoSecServerCredentials();
//        }
//
//        setServerConfig (bootstrapServerFromProfile);


    }

//    private void setBootstrapClientCredentials (LwM2MServerCredentials bootstrapFromClientCredential) {
//        switch (bootstrapFromClientCredential.getSecurityMode()) {
//            case PSK:
//                bootstrapClientCredentials = new PSKServerCredentials();
//                ((PSKServerCredentials)bootstrapClientCredentials).setClientPublicKeyOrId(((PSKServerCredentials) bootstrapFromClientCredential).getClientPublicKeyOrId());
//                ((PSKServerCredentials)bootstrapClientCredentials).setClientSecretKey(((PSKServerCredentials) bootstrapFromClientCredential).getClientSecretKey());
//            case RPK:
//                ((RPKServerCredentials)bootstrapClientCredentials).setClientPublicKeyOrId(((RPKServerCredentials) bootstrapFromClientCredential).getClientPublicKeyOrId());
//                ((RPKServerCredentials)bootstrapClientCredentials).setClientSecretKey(((RPKServerCredentials) bootstrapFromClientCredential).getClientSecretKey());
//            case X509:
//                ((X509ServerCredentials)bootstrapClientCredentials).setClientPublicKeyOrId(((X509ServerCredentials) bootstrapFromClientCredential).getClientPublicKeyOrId());
//                ((X509ServerCredentials)bootstrapClientCredentials).setClientSecretKey(((X509ServerCredentials) bootstrapFromClientCredential).getClientSecretKey());
//        }
//    }
//
//    private void setServerConfig (BootstrapServerConfig bootstrapServerFromProfile) {
//        switch (bootstrapServerFromProfile.getSecurityMode()) {
//            case NO_SEC:
//                serverConfig = new NoSecBootstrapServerConfig();
//                serverConfig.setServerPublicKey("");
//                serverConfig.setClientHoldOffTime(((NoSecBootstrapServerConfig)bootstrapServerFromProfile).getClientHoldOffTime());
//                serverConfig.setBootstrapServerAccountTimeout(((NoSecBootstrapServerConfig)bootstrapServerFromProfile).getBootstrapServerAccountTimeout());
//                serverConfig.setHost((((NoSecBootstrapServerConfig)bootstrapServerFromProfile).getHost().equals("0.0.0.0")) ? "localhost" : ((NoSecBootstrapServerConfig)bootstrapServerFromProfile).getHost());
//                serverConfig.setPort(((NoSecBootstrapServerConfig)bootstrapServerFromProfile).getPort());
//                serverConfig.setSecurityHost(serverConfig.getHost());
//                serverConfig.setSecurityPort(serverConfig.getPort());
//                serverConfig.setServerId(((NoSecBootstrapServerConfig)bootstrapServerFromProfile).getServerId());
//                serverConfig.setBootstrapServerIs(((NoSecBootstrapServerConfig)bootstrapServerFromProfile).isBootstrapServerIs());
//                break;
//            case PSK:
//                serverConfig = new PSKBootstrapServerConfig();
//                serverConfig.setServerPublicKey("");
//                serverConfig.setClientHoldOffTime(((PSKBootstrapServerConfig)bootstrapServerFromProfile).getClientHoldOffTime());
//                serverConfig.setBootstrapServerAccountTimeout(((PSKBootstrapServerConfig)bootstrapServerFromProfile).getBootstrapServerAccountTimeout());
//                serverConfig.setHost((((PSKBootstrapServerConfig)bootstrapServerFromProfile).getHost().equals("0.0.0.0")) ? "localhost" : ((PSKBootstrapServerConfig)bootstrapServerFromProfile).getHost());
//                serverConfig.setPort(((PSKBootstrapServerConfig)bootstrapServerFromProfile).getPort());
//                serverConfig.setSecurityHost(serverConfig.getHost());
//                serverConfig.setSecurityPort(serverConfig.getPort());
//                serverConfig.setServerId(((PSKBootstrapServerConfig)bootstrapServerFromProfile).getServerId());
//                serverConfig.setBootstrapServerIs(((PSKBootstrapServerConfig)bootstrapServerFromProfile).isBootstrapServerIs());
//                break;
//            case RPK:
//                serverConfig = new RPKBootstrapServerConfig();
//                serverConfig.setServerPublicKey(((RPKBootstrapServerConfig)bootstrapServerFromProfile).getServerPublicKey());
//                serverConfig.setClientHoldOffTime(((RPKBootstrapServerConfig)bootstrapServerFromProfile).getClientHoldOffTime());
//                serverConfig.setBootstrapServerAccountTimeout(((RPKBootstrapServerConfig)bootstrapServerFromProfile).getBootstrapServerAccountTimeout());
//                serverConfig.setHost((((RPKBootstrapServerConfig)bootstrapServerFromProfile).getHost().equals("0.0.0.0")) ? "localhost" : ((RPKBootstrapServerConfig)bootstrapServerFromProfile).getHost());
//                serverConfig.setPort(((RPKBootstrapServerConfig)bootstrapServerFromProfile).getPort());
//                serverConfig.setSecurityHost(serverConfig.getHost());
//                serverConfig.setSecurityPort(serverConfig.getPort());
//                serverConfig.setServerId(((RPKBootstrapServerConfig)bootstrapServerFromProfile).getServerId());
//                serverConfig.setBootstrapServerIs(((RPKBootstrapServerConfig)bootstrapServerFromProfile).isBootstrapServerIs());
//                break;
//            case X509:
//                serverConfig = new X509BootstrapServerConfig();
//                serverConfig.setServerPublicKey(((X509BootstrapServerConfig)bootstrapServerFromProfile).getServerPublicKey());
//                serverConfig.setClientHoldOffTime(((X509BootstrapServerConfig)bootstrapServerFromProfile).getClientHoldOffTime());
//                serverConfig.setBootstrapServerAccountTimeout(((X509BootstrapServerConfig)bootstrapServerFromProfile).getBootstrapServerAccountTimeout());
//                serverConfig.setHost((((X509BootstrapServerConfig)bootstrapServerFromProfile).getHost().equals("0.0.0.0")) ? "localhost" : ((X509BootstrapServerConfig)bootstrapServerFromProfile).getHost());
//                serverConfig.setPort(((X509BootstrapServerConfig)bootstrapServerFromProfile).getPort());
//                serverConfig.setSecurityHost(serverConfig.getHost());
//                serverConfig.setSecurityPort(serverConfig.getPort());
//                serverConfig.setServerId(((X509BootstrapServerConfig)bootstrapServerFromProfile).getServerId());
//                serverConfig.setBootstrapServerIs(((X509BootstrapServerConfig)bootstrapServerFromProfile).isBootstrapServerIs());
//                break;
//        }
//    }
}

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
package org.thingsboard.server.service.lwm2m;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.LwM2MServerSecurityConfigDefault;
import org.thingsboard.server.common.transport.config.ssl.SslCredentials;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.transport.lwm2m.config.LwM2MSecureServerConfig;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportBootstrapConfig;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@TbCoreComponent
public class LwM2MServiceImpl implements LwM2MService {

    private final LwM2MTransportServerConfig serverConfig;
    private final Optional<LwM2MTransportBootstrapConfig> bootstrapConfig;

    @Override
    public LwM2MServerSecurityConfigDefault getServerSecurityInfo(boolean bootstrapServer) {
        LwM2MSecureServerConfig bsServerConfig = bootstrapServer ? bootstrapConfig.orElse(null) : serverConfig;
        if (bsServerConfig!= null) {
            LwM2MServerSecurityConfigDefault result = getServerSecurityConfig(bsServerConfig);
            result.setBootstrapServerIs(bootstrapServer);
            return result;
        }
        else {
            return  null;
        }
    }

    private LwM2MServerSecurityConfigDefault getServerSecurityConfig(LwM2MSecureServerConfig bsServerConfig) {
        LwM2MServerSecurityConfigDefault bsServ = new LwM2MServerSecurityConfigDefault();
        bsServ.setShortServerId(bsServerConfig.getId());
        bsServ.setHost(bsServerConfig.getHost());
        bsServ.setPort(bsServerConfig.getPort());
        bsServ.setSecurityHost(bsServerConfig.getSecureHost());
        bsServ.setSecurityPort(bsServerConfig.getSecurePort());
        byte[] publicKeyBase64 = getPublicKey(bsServerConfig);
        if (publicKeyBase64 == null) {
            bsServ.setServerPublicKey("");
        } else {
            bsServ.setServerPublicKey(Base64.encodeBase64String(publicKeyBase64));
        }
        byte[] certificateBase64 = getCertificate(bsServerConfig);
        if (certificateBase64 == null) {
            bsServ.setServerCertificate("");
        } else {
            bsServ.setServerCertificate(Base64.encodeBase64String(certificateBase64));
        }
        return bsServ;
    }

    private byte[] getPublicKey(LwM2MSecureServerConfig config) {
        try {
            SslCredentials sslCredentials = config.getSslCredentials();
            if (sslCredentials != null) {
                return sslCredentials.getPublicKey().getEncoded();
            }
        } catch (Exception e) {
            log.trace("Failed to fetch public key from key store!", e);
        }
        return null;
    }

    private byte[] getCertificate(LwM2MSecureServerConfig config) {
        try {
            SslCredentials sslCredentials = config.getSslCredentials();
            if (sslCredentials != null) {
                return sslCredentials.getCertificateChain()[0].getEncoded();
            }
        } catch (Exception e) {
            log.trace("Failed to fetch certificate from key store!", e);
        }
        return null;
    }
}


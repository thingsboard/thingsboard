// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
            return getServerSecurityConfig(bsServerConfig, bootstrapServer);
        }
        else {
            return  null;
        }
    }

    private LwM2MServerSecurityConfigDefault getServerSecurityConfig(LwM2MSecureServerConfig bsServerConfig, boolean bootstrapServer) {
        LwM2MServerSecurityConfigDefault bsServ = new LwM2MServerSecurityConfigDefault();
        bsServ.setHost(bsServerConfig.getHost());
        bsServ.setPort(bsServerConfig.getPort());
        bsServ.setBootstrapServerIs(bootstrapServer);
        if (bootstrapServer) {
            if (bsServerConfig.getId() != null) {
                log.warn("Ignoring Short Server ID [{}] on the Bootstrap Server entry: cleared to null for backward compatibility (ThingsBoard <= 4.2).",
                        bsServerConfig.getId());
            }
            bsServ.setShortServerId(null);
        } else {
            Integer configId = bsServerConfig.getId();
            if (configId == null || configId <= 0 || configId >= 65535) {
                log.warn("Invalid Short Server ID [{}] for LwM2M Server entry: defaulting to [1] for backward compatibility (ThingsBoard <= 4.2).",
                        configId);
                configId = 1;
            }
            bsServ.setShortServerId(configId);
        }
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


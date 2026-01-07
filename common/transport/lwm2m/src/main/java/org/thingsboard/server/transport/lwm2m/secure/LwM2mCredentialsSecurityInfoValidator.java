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
package org.thingsboard.server.transport.lwm2m.secure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.eclipse.leshan.core.security.util.SecurityUtil;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MClientCredential;
import org.thingsboard.server.common.data.device.credentials.lwm2m.PSKClientCredential;
import org.thingsboard.server.common.data.device.credentials.lwm2m.RPKClientCredential;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceLwM2MCredentialsRequestMsg;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.bootstrap.secure.LwM2MBootstrapConfig;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.transport.lwm2m.secure.credentials.LwM2MClientCredentials;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportContext;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2MAuthException;
import org.thingsboard.server.transport.lwm2m.server.uplink.LwM2mTypeServer;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.eclipse.leshan.core.SecurityMode.NO_SEC;
import static org.eclipse.leshan.core.SecurityMode.PSK;
import static org.eclipse.leshan.core.SecurityMode.RPK;
import static org.eclipse.leshan.core.SecurityMode.X509;
import static org.thingsboard.server.transport.lwm2m.server.uplink.LwM2mTypeServer.BOOTSTRAP;

@Slf4j
@Component
@TbLwM2mTransportComponent
@RequiredArgsConstructor
public class LwM2mCredentialsSecurityInfoValidator {

    private final LwM2mTransportContext context;
    private final LwM2MTransportServerConfig config;

    public TbLwM2MSecurityInfo getEndpointSecurityInfoByCredentialsId(String credentialsId, LwM2mTypeServer keyValue) {
        CountDownLatch latch = new CountDownLatch(1);
        final TbLwM2MSecurityInfo[] resultSecurityStore = new TbLwM2MSecurityInfo[1];
        log.trace("Validating credentials [{}]", credentialsId);
        context.getTransportService().process(ValidateDeviceLwM2MCredentialsRequestMsg.newBuilder().setCredentialsId(credentialsId).build(),
                new TransportServiceCallback<>() {
                    @Override
                    public void onSuccess(ValidateDeviceCredentialsResponse msg) {
                        log.trace("Validated credentials: [{}] [{}]", credentialsId, msg);
                        resultSecurityStore[0] = createSecurityInfo(credentialsId, msg, keyValue);
                        latch.countDown();
                    }

                    @Override
                    public void onError(Throwable e) {
                        log.info("[{}] [{}] Failed to process credentials ", credentialsId, e);
                        TbLwM2MSecurityInfo result = new TbLwM2MSecurityInfo();
                        result.setEndpoint(credentialsId);
                        resultSecurityStore[0] = result;
                        latch.countDown();
                    }
                });
        try {
            latch.await(config.getTimeout(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.error("Failed to await credentials!", e);
        }

        TbLwM2MSecurityInfo securityInfo = resultSecurityStore[0];
        if (securityInfo != null && securityInfo.getSecurityMode() == null) {
            throw new LwM2MAuthException();
        }
        return securityInfo;
    }

    /**
     * Create new SecurityInfo
     *
     * @return SecurityInfo
     */
    private TbLwM2MSecurityInfo createSecurityInfo(String endpoint, ValidateDeviceCredentialsResponse msg, LwM2mTypeServer keyValue) {
        TbLwM2MSecurityInfo result = new TbLwM2MSecurityInfo();
        LwM2MClientCredentials credentials = JacksonUtil.fromString(msg.getCredentials(), LwM2MClientCredentials.class);
        if (credentials != null) {
            result.setMsg(msg);
            result.setDeviceProfile(msg.getDeviceProfile());
            result.setEndpoint(credentials.getClient().getEndpoint());
//            if ((keyValue.equals(CLIENT))) {
            switch (credentials.getClient().getSecurityConfigClientMode()) {
                case NO_SEC:
                    createClientSecurityInfoNoSec(result);
                    break;
                case PSK:
                    createClientSecurityInfoPSK(result, endpoint, credentials.getClient());
                    break;
                case RPK:
                    createClientSecurityInfoRPK(result, endpoint, credentials.getClient());
                    break;
                case X509:
                    createClientSecurityInfoX509(result, endpoint);
                    break;
                default:
                    break;
            }
            if (keyValue.equals(BOOTSTRAP)) {
                LwM2MBootstrapConfig bootstrapCredentialConfig = new LwM2MBootstrapConfig(((Lwm2mDeviceProfileTransportConfiguration) msg.getDeviceProfile().getProfileData().getTransportConfiguration()).getBootstrap(),
                        credentials.getBootstrap().getBootstrapServer(), credentials.getBootstrap().getLwm2mServer());
                result.setBootstrapCredentialConfig(bootstrapCredentialConfig);
            }
        }
        return result;
    }

    private void createClientSecurityInfoNoSec(TbLwM2MSecurityInfo result) {
        result.setSecurityInfo(null);
        result.setSecurityMode(NO_SEC);
    }

    private void createClientSecurityInfoPSK(TbLwM2MSecurityInfo result, String endpoint, LwM2MClientCredential clientCredentialsConfig) {
        PSKClientCredential pskConfig = (PSKClientCredential) clientCredentialsConfig;
        if (StringUtils.isNotEmpty(pskConfig.getIdentity())) {
            try {
                if (pskConfig.getDecoded() != null && pskConfig.getDecoded().length > 0) {
                    endpoint = StringUtils.isNotEmpty(pskConfig.getEndpoint()) ? pskConfig.getEndpoint() : endpoint;
                    if (endpoint != null && !endpoint.isEmpty()) {
                        result.setSecurityInfo(SecurityInfo.newPreSharedKeyInfo(endpoint, pskConfig.getIdentity(), pskConfig.getDecoded()));
                        result.setSecurityMode(PSK);
                    }
                }
            } catch (IllegalArgumentException | DecoderException e) {
                log.error("Missing PSK key: " + e.getMessage());
            }
        } else {
            log.error("Missing PSK identity");
        }
    }

    private void createClientSecurityInfoRPK(TbLwM2MSecurityInfo result, String endpoint, LwM2MClientCredential clientCredentialsConfig) {
        RPKClientCredential rpkConfig = (RPKClientCredential) clientCredentialsConfig;
        try {
            if (rpkConfig.getDecoded() != null) {
                PublicKey key = SecurityUtil.publicKey.decode(rpkConfig.getDecoded());
                result.setSecurityInfo(SecurityInfo.newRawPublicKeyInfo(endpoint, key));
                result.setSecurityMode(RPK);
            } else {
                log.error("Missing RPK key");
            }
        } catch (IllegalArgumentException | IOException | GeneralSecurityException | DecoderException e) {
            log.error("RPK: Invalid security info content: " + e.getMessage());
        }
    }

    private void createClientSecurityInfoX509(TbLwM2MSecurityInfo result, String endpoint) {
        result.setSecurityInfo(SecurityInfo.newX509CertInfo(endpoint));
        result.setSecurityMode(X509);
    }
}

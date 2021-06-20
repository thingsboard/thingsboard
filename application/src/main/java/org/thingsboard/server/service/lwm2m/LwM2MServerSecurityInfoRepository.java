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
package org.thingsboard.server.service.lwm2m;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.util.Hex;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.lwm2m.ServerSecurityConfig;
import org.thingsboard.server.transport.lwm2m.config.LwM2MSecureServerConfig;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportBootstrapConfig;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;

import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStoreException;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.KeySpec;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnExpression("('${service.type:null}'=='tb-transport' && '${transport.lwm2m.enabled:false}'=='true') || '${service.type:null}'=='monolith' || '${service.type:null}'=='tb-core'")
public class LwM2MServerSecurityInfoRepository {

    private final LwM2MTransportServerConfig serverConfig;
    private final LwM2MTransportBootstrapConfig bootstrapConfig;

    public ServerSecurityConfig getServerSecurityInfo(boolean bootstrapServer) {
        ServerSecurityConfig result = getServerSecurityConfig(bootstrapServer ? bootstrapConfig : serverConfig);
        result.setBootstrapServerIs(bootstrapServer);
        return result;
    }

    private ServerSecurityConfig getServerSecurityConfig(LwM2MSecureServerConfig serverConfig) {
        ServerSecurityConfig bsServ = new ServerSecurityConfig();
        bsServ.setServerId(serverConfig.getId());
        bsServ.setHost(serverConfig.getHost());
        bsServ.setPort(serverConfig.getPort());
        bsServ.setSecurityHost(serverConfig.getSecureHost());
        bsServ.setSecurityPort(serverConfig.getSecurePort());
        bsServ.setServerPublicKey(getPublicKey(serverConfig.getCertificateAlias(), this.serverConfig.getPublicX(), this.serverConfig.getPublicY()));
        return bsServ;
    }

    private String getPublicKey(String alias, String publicServerX, String publicServerY) {
        String publicKey = getServerPublicKeyX509(alias);
        return publicKey != null ? publicKey : getRPKPublicKey(publicServerX, publicServerY);
    }

    private String getServerPublicKeyX509(String alias) {
        try {
            X509Certificate serverCertificate = (X509Certificate) serverConfig.getKeyStoreValue().getCertificate(alias);
            return Hex.encodeHexString(serverCertificate.getEncoded());
        } catch (CertificateEncodingException | KeyStoreException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getRPKPublicKey(String publicServerX, String publicServerY) {
        try {
            /** Get Elliptic Curve Parameter spec for secp256r1 */
            AlgorithmParameters algoParameters = AlgorithmParameters.getInstance("EC");
            algoParameters.init(new ECGenParameterSpec("secp256r1"));
            ECParameterSpec parameterSpec = algoParameters.getParameterSpec(ECParameterSpec.class);
            if (publicServerX != null && !publicServerX.isEmpty() && publicServerY != null && !publicServerY.isEmpty()) {
                /** Get point values */
                byte[] publicX = Hex.decodeHex(publicServerX.toCharArray());
                byte[] publicY = Hex.decodeHex(publicServerY.toCharArray());
                /** Create key specs */
                KeySpec publicKeySpec = new ECPublicKeySpec(new ECPoint(new BigInteger(publicX), new BigInteger(publicY)),
                        parameterSpec);
                /** Get keys */
                PublicKey publicKey = KeyFactory.getInstance("EC").generatePublic(publicKeySpec);
                if (publicKey != null && publicKey.getEncoded().length > 0) {
                    return Hex.encodeHexString(publicKey.getEncoded());
                }
            }
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            log.error("[{}] Failed generate Server RPK for profile", e.getMessage());
            throw new RuntimeException(e);
        }
        return null;
    }
}


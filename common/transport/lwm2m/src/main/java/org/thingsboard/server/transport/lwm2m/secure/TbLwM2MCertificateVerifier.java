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
package org.thingsboard.server.transport.lwm2m.secure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.scandium.dtls.x509.StaticCertificateVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2MAuthException;
import org.thingsboard.server.transport.lwm2m.server.store.TbLwM2MDtlsSessionStore;
import org.thingsboard.server.transport.lwm2m.server.store.TbMainSecurityStore;
import org.thingsboard.server.transport.lwm2m.server.uplink.LwM2mTypeServer;

import javax.annotation.PostConstruct;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;

@Slf4j
@Component
@TbLwM2mTransportComponent
@RequiredArgsConstructor
public class TbLwM2MCertificateVerifier {

    private final LwM2MTransportServerConfig config;
    private final LwM2mCredentialsSecurityInfoValidator securityInfoValidator;

    public TbLwM2MSecurityInfo verifyCertificate(X509Certificate cert, String sha3Hash, LwM2mTypeServer typeServer) {
        TbLwM2MSecurityInfo securityInfo = null;
        // verify if trust
        if (config.getTrustSslCredentials() != null && config.getTrustSslCredentials().getTrustedCertificates().length > 0) {
            if (verifyTrust(cert, config.getTrustSslCredentials().getTrustedCertificates()) != null) {
                String endpoint = config.getTrustSslCredentials().getValueFromSubjectNameByKey(cert.getSubjectX500Principal().getName(), "CN");
                securityInfo = StringUtils.isNotEmpty(endpoint) ? securityInfoValidator.getEndpointSecurityInfoByCredentialsId(endpoint, typeServer) : null;
            }
        }
        // if not trust or cert trust securityInfo == null
        if (securityInfo == null) {
            try {
                securityInfo = securityInfoValidator.getEndpointSecurityInfoByCredentialsId(sha3Hash, typeServer);
            } catch (LwM2MAuthException e) {
                log.trace("Failed find security info: {}", sha3Hash, e);
            }
        }
        return securityInfo;
    }

    private X509Certificate verifyTrust(X509Certificate certificate, X509Certificate[] certificates) {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            CertPath cp = cf.generateCertPath(Arrays.asList(new X509Certificate[]{certificate}));
            for (int index = 0; index < certificates.length; ++index) {
                X509Certificate caCert = certificates[index];
                try {
                    TrustAnchor trustAnchor = new TrustAnchor(caCert, null);
                    CertPathValidator cpv = CertPathValidator.getInstance("PKIX");
                    PKIXParameters pkixParams = new PKIXParameters(
                            Collections.singleton(trustAnchor));
                    pkixParams.setRevocationEnabled(false);
                    if (cpv.validate(cp, pkixParams) != null) return certificate;
                } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | CertPathValidatorException e) {
                    log.trace("[{}]. [{}]", certificate.getSubjectDN(), e.getMessage());
                }
            }
        } catch (CertificateException e) {
            log.trace("[{}] certPath not valid. [{}]", certificate.getSubjectDN(), e.getMessage());
        }
        return null;
    }
}

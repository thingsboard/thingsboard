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
package org.thingsboard.monitoring.util;

import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.config.SystemConfig;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConfig;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.SignatureAndHashAlgorithm;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite;
import org.thingsboard.common.util.SslUtil;

import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Builds a verify-only ({@code DTLS_ROLE = CLIENT_ONLY}) DTLS client connector for probing a
 * CoAPS endpoint. Trust is scoped to the JVM's default trust store (cacerts) only — a server
 * whose certificate chains to a private/custom CA will fail the handshake. Hostname/wildcard
 * verification is delegated to {@link WildcardAwareCertificateVerifier}.
 */
public class DtlsClientConnectorFactory {

    // Californium only exposes named constants for SHA256_WITH_ECDSA/SHA384_WITH_ECDSA/SHA256_WITH_RSA;
    // the other RSA/ECDSA combinations below have no named constant in this version and must be
    // looked up by name instead.
    private static final List<SignatureAndHashAlgorithm> CLIENT_SIG_ALGS = List.of(
            SignatureAndHashAlgorithm.SHA256_WITH_ECDSA,
            SignatureAndHashAlgorithm.SHA384_WITH_ECDSA,
            SignatureAndHashAlgorithm.valueOf("SHA512withECDSA"),
            SignatureAndHashAlgorithm.SHA256_WITH_RSA,
            SignatureAndHashAlgorithm.valueOf("SHA384withRSA"),
            SignatureAndHashAlgorithm.valueOf("SHA512withRSA"),
            SignatureAndHashAlgorithm.INTRINSIC_WITH_ED25519);

    // default implicitly limits a cert-verifier-only client to ECDSA suites, breaking RSA-cert servers
    private static final List<CipherSuite> CLIENT_CIPHER_SUITES = List.of(
            CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
            CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,
            CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
            CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,
            CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8,
            CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CCM_8,
            CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CCM,
            CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CCM);

    private static final X509Certificate[] TRUSTED_CERTIFICATES = SslUtil.getDefaultTrustedCertificates();

    static {
        SystemConfig.register();
        CoapConfig.register();
    }

    private DtlsClientConnectorFactory() {
    }

    public static DTLSConnector jvmTrustedDtlsClientConnector() {
        Configuration config = new Configuration();
        config.set(DtlsConfig.DTLS_ROLE, DtlsConfig.DtlsRole.CLIENT_ONLY); // no client cert required
        config.set(DtlsConfig.DTLS_USE_SERVER_NAME_INDICATION, true); // select the correct server cert
        config.set(DtlsConfig.DTLS_SIGNATURE_AND_HASH_ALGORITHMS, CLIENT_SIG_ALGS);
        config.set(DtlsConfig.DTLS_CIPHER_SUITES, CLIENT_CIPHER_SUITES);
        // hostname is validated by WildcardAwareCertificateVerifier, since Scandium's own
        // DTLS_VERIFY_SERVER_CERTIFICATES_SUBJECT check doesn't support wildcard SANs
        return new DTLSConnector(DtlsConnectorConfig.builder(config)
                .setAdvancedCertificateVerifier(new WildcardAwareCertificateVerifier(TRUSTED_CERTIFICATES))
                .build());
    }

}

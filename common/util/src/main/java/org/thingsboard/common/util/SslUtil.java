/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.common.util;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.jcajce.JcePKCSPBEInputDecryptorProviderBuilder;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.thingsboard.server.common.data.StringUtils;

import java.io.StringReader;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_CONNECTION_ID_LENGTH;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_CONNECTION_ID_NODE_ID;

@Slf4j
public class SslUtil {

    public static final char[] EMPTY_PASS = {};

    public static final BouncyCastleProvider DEFAULT_PROVIDER = new BouncyCastleProvider();

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(DEFAULT_PROVIDER);
        }
    }

    private SslUtil() {
    }

    @SneakyThrows
    public static List<X509Certificate> readCertFile(String fileContent) {
        List<X509Certificate> certificates = new ArrayList<>();
        JcaX509CertificateConverter certConverter = new JcaX509CertificateConverter();
        try (PEMParser pemParser = new PEMParser(new StringReader(fileContent))) {
            Object object;
            while ((object = pemParser.readObject()) != null) {
                if (object instanceof X509CertificateHolder) {
                    X509Certificate x509Cert = certConverter.getCertificate((X509CertificateHolder) object);
                    certificates.add(x509Cert);
                }
            }
        }
        return certificates;
    }

    @SneakyThrows
    public static PrivateKey readPrivateKey(String fileContent, String passStr) {
        char[] password = StringUtils.isEmpty(passStr) ? EMPTY_PASS : passStr.toCharArray();

        PrivateKey privateKey = null;
        JcaPEMKeyConverter keyConverter = new JcaPEMKeyConverter();
        if (StringUtils.isNotEmpty(fileContent)) {
            try (PEMParser pemParser = new PEMParser(new StringReader(fileContent))) {
                Object object;
                while ((object = pemParser.readObject()) != null) {
                    if (object instanceof PEMEncryptedKeyPair) {
                        PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder().build(password);
                        privateKey = keyConverter.getKeyPair(((PEMEncryptedKeyPair) object).decryptKeyPair(decProv)).getPrivate();
                        break;
                    } else if (object instanceof PKCS8EncryptedPrivateKeyInfo) {
                        InputDecryptorProvider decProv =
                                new JcePKCSPBEInputDecryptorProviderBuilder().setProvider(DEFAULT_PROVIDER).build(password);
                        privateKey = keyConverter.getPrivateKey(((PKCS8EncryptedPrivateKeyInfo) object).decryptPrivateKeyInfo(decProv));
                        break;
                    } else if (object instanceof PEMKeyPair) {
                        privateKey = keyConverter.getKeyPair((PEMKeyPair) object).getPrivate();
                        break;
                    } else if (object instanceof PrivateKeyInfo) {
                        privateKey = keyConverter.getPrivateKey((PrivateKeyInfo) object);
                    }
                }
            }
        }
        return privateKey;
    }

    public static void setDtlsConnectorConfigCidLength (DtlsConnectorConfig.Builder configBuilder, Integer dtlsCidLength) {
        configBuilder.set(DTLS_CONNECTION_ID_LENGTH, dtlsCidLength);
        if (dtlsCidLength != null) {
            if (dtlsCidLength > 4) {
                configBuilder.set(DTLS_CONNECTION_ID_NODE_ID, 0);
            } else {
                configBuilder.set(DTLS_CONNECTION_ID_NODE_ID, null);
            }
        }
    }

    public static boolean isUsedNodeConnectionIdGenerator(Integer dtlsCidLength){
        return dtlsCidLength != null && dtlsCidLength > 0;
    }
}

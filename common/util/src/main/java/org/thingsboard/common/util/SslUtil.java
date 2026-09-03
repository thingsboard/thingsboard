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
package org.thingsboard.common.util;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.PKCSException;
import org.bouncycastle.pkcs.jcajce.JcePKCSPBEInputDecryptorProviderBuilder;
import org.thingsboard.server.common.data.StringUtils;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class SslUtil {

    public static final char[] EMPTY_PASS = {};

    /**
     * JCE provider used for PEM/PKCS8 decryption. Defaults to the standard
     * BouncyCastle provider ("BC"); override with -Dthingsboard.bc.provider=BCFIPS
     * once a FIPS-certified provider is registered on the classpath instead.
     */
    public static final String PROVIDER_NAME = System.getProperty("thingsboard.bc.provider", "BC");

    static {
        registerProviderIfAbsent(PROVIDER_NAME);
    }

    private SslUtil() {
    }

    @SneakyThrows
    public static List<X509Certificate> readCertFile(String fileContent) {
        return readCertFile(new StringReader(fileContent));
    }

    @SneakyThrows
    public static List<X509Certificate> readCertFileByPath(String filePath) {
        return readCertFile( new FileReader(filePath));
    }

    private static List<X509Certificate> readCertFile(Reader reader) throws IOException, CertificateException {
        List<X509Certificate> certificates = new ArrayList<>();
        JcaX509CertificateConverter certConverter = new JcaX509CertificateConverter();
        try (PEMParser pemParser = new PEMParser(reader)) {
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
        if (StringUtils.isNotEmpty(fileContent)) {
            StringReader reader = new StringReader(fileContent);
            return readPrivateKey(reader, passStr);
        }
        return null;
    }

    @SneakyThrows
    public static PrivateKey readPrivateKeyByFilePath(String filePath, String passStr) {
        if (StringUtils.isNotEmpty(filePath)) {
            FileReader fileReader = new FileReader(filePath);
            return readPrivateKey(fileReader, passStr);
        }
        return null;
    }

    private static PrivateKey readPrivateKey(Reader reader, String passStr) throws IOException, PKCSException {
        char[] password = getPassword(passStr);
        PrivateKey privateKey = null;
        JcaPEMKeyConverter keyConverter = new JcaPEMKeyConverter();
        try (PEMParser pemParser = new PEMParser(reader)) {
            Object object;
            while ((object = pemParser.readObject()) != null) {
                if (object instanceof PEMEncryptedKeyPair) {
                    PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder().build(password);
                    privateKey = keyConverter.getKeyPair(((PEMEncryptedKeyPair) object).decryptKeyPair(decProv)).getPrivate();
                    break;
                } else if (object instanceof PKCS8EncryptedPrivateKeyInfo) {
                    InputDecryptorProvider decProv =
                            new JcePKCSPBEInputDecryptorProviderBuilder().setProvider(PROVIDER_NAME).build(password);
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
        return privateKey;
    }

    public static char[] getPassword(String passStr) {
        return StringUtils.isEmpty(passStr) ? EMPTY_PASS : passStr.toCharArray();
    }

    private static void registerProviderIfAbsent(String providerName) {
        if (Security.getProvider(providerName) != null) {
            return;
        }
        String providerClass = switch (providerName) {
            case "BC" -> "org.bouncycastle.jce.provider.BouncyCastleProvider";
            case "BCFIPS" -> "org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider";
            default -> throw new IllegalStateException("Unknown JCE provider name '" + providerName
                    + "'; no provider class mapping and no provider already registered under that name");
        };
        try {
            Security.addProvider((Provider) Class.forName(providerClass).getDeclaredConstructor().newInstance());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to instantiate JCE provider '" + providerName + "' (" + providerClass + ")", e);
        }
    }

}

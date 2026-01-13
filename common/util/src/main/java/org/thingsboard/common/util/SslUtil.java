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
import org.bouncycastle.jce.provider.BouncyCastleProvider;
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
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

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
        JcaPEMKeyConverter keyConverter = new JcaPEMKeyConverter();
        try (PEMParser pemParser = new PEMParser(reader)) {
            Object object;
            while ((object = pemParser.readObject()) != null) {
                PrivateKey key = extractPrivateKeyFromObject(object, keyConverter, password);
                if (key != null) {
                    return key;
                }
            }
        }
        return null;
    }

    /**
     * Extracts a PrivateKey from a PEM object based on its type.
     * This method handles different PEM object types (encrypted/unencrypted key pairs and key info).
     *
     * @param object the PEM object to extract the key from
     * @param keyConverter the converter to use for key extraction
     * @param password the password for decryption (if needed)
     * @return the extracted PrivateKey, or null if the object type is not recognized
     * @throws PKCSException if there's an error during decryption
     */
    private static PrivateKey extractPrivateKeyFromObject(Object object, JcaPEMKeyConverter keyConverter, char[] password) throws PKCSException {
        if (object instanceof PEMEncryptedKeyPair) {
            return extractFromEncryptedKeyPair((PEMEncryptedKeyPair) object, keyConverter, password);
        } else if (object instanceof PKCS8EncryptedPrivateKeyInfo) {
            return extractFromPKCS8EncryptedInfo((PKCS8EncryptedPrivateKeyInfo) object, keyConverter, password);
        } else if (object instanceof PEMKeyPair) {
            return extractFromKeyPair((PEMKeyPair) object, keyConverter);
        } else if (object instanceof PrivateKeyInfo) {
            return extractFromPrivateKeyInfo((PrivateKeyInfo) object, keyConverter);
        }
        return null;
    }

    /**
     * Extracts a PrivateKey from an encrypted PEM key pair.
     */
    private static PrivateKey extractFromEncryptedKeyPair(PEMEncryptedKeyPair encryptedKeyPair, JcaPEMKeyConverter keyConverter, char[] password) throws PKCSException {
        PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder().build(password);
        return keyConverter.getKeyPair(encryptedKeyPair.decryptKeyPair(decProv)).getPrivate();
    }

    /**
     * Extracts a PrivateKey from a PKCS8 encrypted private key info.
     */
    private static PrivateKey extractFromPKCS8EncryptedInfo(PKCS8EncryptedPrivateKeyInfo encryptedInfo, JcaPEMKeyConverter keyConverter, char[] password) throws PKCSException {
        InputDecryptorProvider decProv = new JcePKCSPBEInputDecryptorProviderBuilder()
                .setProvider(DEFAULT_PROVIDER)
                .build(password);
        return keyConverter.getPrivateKey(encryptedInfo.decryptPrivateKeyInfo(decProv));
    }

    /**
     * Extracts a PrivateKey from an unencrypted PEM key pair.
     */
    private static PrivateKey extractFromKeyPair(PEMKeyPair keyPair, JcaPEMKeyConverter keyConverter) {
        return keyConverter.getKeyPair(keyPair).getPrivate();
    }

    /**
     * Extracts a PrivateKey from a private key info object.
     */
    private static PrivateKey extractFromPrivateKeyInfo(PrivateKeyInfo keyInfo, JcaPEMKeyConverter keyConverter) throws PKCSException {
        return keyConverter.getPrivateKey(keyInfo);
    }

    public static char[] getPassword(String passStr) {
        return StringUtils.isEmpty(passStr) ? EMPTY_PASS : passStr.toCharArray();
    }

}

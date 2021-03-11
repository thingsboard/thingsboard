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
package org.thingsboard.rule.engine.credentials;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.AlgorithmParameters;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
@Slf4j
@JsonIgnoreProperties(ignoreUnknown = true)
public class CertPemCredentials implements ClientCredentials {
    private static final String TLS_VERSION = "TLSv1.2";

    protected String caCert;
    private String cert;
    private String privateKey;
    private String password;

    static final String OPENSSL_ENCRYPTED_RSA_PRIVATEKEY_REGEX = "\\s*"
            + "-----BEGIN RSA PRIVATE KEY-----" + "\\s*"
            + "Proc-Type: 4,ENCRYPTED" + "\\s*"
            + "DEK-Info:" + "\\s*([^\\s]+)" + "\\s+"
            + "([\\s\\S]*)"
            + "-----END RSA PRIVATE KEY-----" + "\\s*";

    static final Pattern OPENSSL_ENCRYPTED_RSA_PRIVATEKEY_PATTERN = Pattern.compile(OPENSSL_ENCRYPTED_RSA_PRIVATEKEY_REGEX);

    @Override
    public CredentialsType getType() {
        return CredentialsType.CERT_PEM;
    }

    @Override
    public SslContext initSslContext() {
        try {
            Security.addProvider(new BouncyCastleProvider());
            SslContextBuilder builder = SslContextBuilder.forClient();
            if (StringUtils.hasLength(caCert)) {
                builder.trustManager(createAndInitTrustManagerFactory());
            }
            if (StringUtils.hasLength(cert) && StringUtils.hasLength(privateKey)) {
                builder.keyManager(createAndInitKeyManagerFactory());
            }
            return builder.build();
        } catch (Exception e) {
            log.error("[{}:{}] Creating TLS factory failed!", caCert, cert, e);
            throw new RuntimeException("Creating TLS factory failed!", e);
        }
    }

    private KeyManagerFactory createAndInitKeyManagerFactory() throws Exception {
        X509Certificate certHolder = readCertFile(cert);
        Object keyObject = readPrivateKeyFile(privateKey);
        char[] passwordCharArray = "".toCharArray();
        if (!StringUtils.isEmpty(password)) {
            passwordCharArray = password.toCharArray();
        }

        JcaPEMKeyConverter keyConverter = new JcaPEMKeyConverter().setProvider("BC");

        PrivateKey privateKey;
        if (keyObject instanceof PEMEncryptedKeyPair) {
            PEMDecryptorProvider provider = new JcePEMDecryptorProviderBuilder().build(passwordCharArray);
            KeyPair key = keyConverter.getKeyPair(((PEMEncryptedKeyPair) keyObject).decryptKeyPair(provider));
            privateKey = key.getPrivate();
        } else if (keyObject instanceof PEMKeyPair) {
            KeyPair key = keyConverter.getKeyPair((PEMKeyPair) keyObject);
            privateKey = key.getPrivate();
        } else if (keyObject instanceof PrivateKey) {
            privateKey = (PrivateKey) keyObject;
        } else {
            throw new RuntimeException("Unable to get private key from object: " + keyObject.getClass());
        }

        KeyStore clientKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        clientKeyStore.load(null, null);
        clientKeyStore.setCertificateEntry("cert", certHolder);
        clientKeyStore.setKeyEntry("private-key",
                privateKey,
                passwordCharArray,
                new Certificate[]{certHolder});

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(clientKeyStore, passwordCharArray);
        return keyManagerFactory;
    }

    protected TrustManagerFactory createAndInitTrustManagerFactory() throws Exception {
        X509Certificate caCertHolder;
        caCertHolder = readCertFile(caCert);

        KeyStore caKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        caKeyStore.load(null, null);
        caKeyStore.setCertificateEntry("caCert-cert", caCertHolder);

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(caKeyStore);
        return trustManagerFactory;
    }

    private X509Certificate readCertFile(String fileContent) throws Exception {
        X509Certificate certificate = null;
        if (fileContent != null && !fileContent.trim().isEmpty()) {
            fileContent = fileContent.replace("-----BEGIN CERTIFICATE-----", "")
                    .replace("-----END CERTIFICATE-----", "")
                    .replaceAll("\\s", "");
            byte[] decoded = Base64.decodeBase64(fileContent);
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            try (InputStream inStream = new ByteArrayInputStream(decoded)) {
                certificate = (X509Certificate) certFactory.generateCertificate(inStream);
            }
        }
        return certificate;
    }

    private PrivateKey readPrivateKeyFile(String fileContent) throws Exception {
        PrivateKey privateKey = null;
        if (fileContent != null && !fileContent.isEmpty()) {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            KeySpec keySpec = getKeySpec(fileContent);
            privateKey = keyFactory.generatePrivate(keySpec);
        }
        return privateKey;
    }

    private KeySpec getKeySpec(String encodedKey) throws Exception {
        KeySpec keySpec = null;
        Matcher matcher = OPENSSL_ENCRYPTED_RSA_PRIVATEKEY_PATTERN.matcher(encodedKey);
        if (matcher.matches()) {
            String encryptionDetails = matcher.group(1).trim();
            String encryptedKey = matcher.group(2).replaceAll("\\s", "");
            byte[] encryptedBinaryKey = java.util.Base64.getDecoder().decode(encryptedKey);
            String[] encryptionDetailsParts = encryptionDetails.split(",");
            if (encryptionDetailsParts.length == 2) {
                String encryptionAlgorithm = encryptionDetailsParts[0];
                String encryptedAlgorithmParams = encryptionDetailsParts[1];
                byte[] pw = password.getBytes();
                byte[] iv = Hex.decode(encryptedAlgorithmParams);

                MessageDigest digest = MessageDigest.getInstance("MD5");
                digest.update(pw);
                digest.update(iv, 0, 8);

                byte[] round1Digest = digest.digest();
                digest.update(round1Digest);
                digest.update(pw);
                digest.update(iv, 0, 8);

                byte[] round2Digest = digest.digest();
                Cipher cipher = null;
                SecretKey secretKey = null;
                byte[] key = null;

                switch(encryptionAlgorithm) {
                    case "AES-256-CBC":
                        cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                        key = new byte[32];
                        System.arraycopy(round1Digest, 0, key, 0, 16);
                        System.arraycopy(round2Digest, 0, key, 16, 16);
                        secretKey = new SecretKeySpec(key, "AES");
                        break;
                    case "AES-192-CBC":
                        cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                        key = new byte[24];
                        System.arraycopy(round1Digest, 0, key, 0, 16);
                        System.arraycopy(round2Digest, 0, key, 16, 8);
                        secretKey = new SecretKeySpec(key, "AES");
                        break;
                    case "AES-128-CBC":
                        cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                        key = new byte[16];
                        System.arraycopy(round1Digest, 0, key, 0, 16);
                        secretKey = new SecretKeySpec(key, "AES");
                        break;
                    case "DES-EDE3-CBC":
                        cipher = Cipher.getInstance("DESede/CBC/PKCS5Padding");
                        key = new byte[24];
                        System.arraycopy(round1Digest, 0, key, 0, 16);
                        System.arraycopy(round2Digest, 0, key, 16, 8);
                        secretKey = new SecretKeySpec(key, "DESede");
                        break;
                    case "DES-CBC":
                        cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
                        key = new byte[8];
                        System.arraycopy(round1Digest, 0, key, 0, 8);
                        secretKey = new SecretKeySpec(key, "DES");
                        break;
                    }
                if (cipher != null) {
                    cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
                    byte[] pkcs1 = cipher.doFinal(encryptedBinaryKey);
                    keySpec = decodeRSAPrivatePKCS1(pkcs1);
                } else {
                    throw new RuntimeException("Unknown Encryption algorithm!");
                }
            } else {
                throw new RuntimeException("Wrong encryption details!");
            }
        } else {
            encodedKey = encodedKey.replaceAll(".*BEGIN.*PRIVATE KEY.*", "")
                    .replaceAll(".*END.*PRIVATE KEY.*", "")
                    .replaceAll("\\s", "");
            byte[] decoded = Base64.decodeBase64(encodedKey);
            if (password == null || password.isEmpty()) {
                keySpec = new PKCS8EncodedKeySpec(decoded);
            } else {
                PBEKeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray());

                EncryptedPrivateKeyInfo privateKeyInfo = new EncryptedPrivateKeyInfo(decoded);
                String algorithmName = privateKeyInfo.getAlgName();
                Cipher cipher = Cipher.getInstance(algorithmName);
                SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(algorithmName);

                Key pbeKey = secretKeyFactory.generateSecret(pbeKeySpec);
                AlgorithmParameters algParams = privateKeyInfo.getAlgParameters();
                cipher.init(Cipher.DECRYPT_MODE, pbeKey, algParams);
                keySpec = privateKeyInfo.getKeySpec(cipher);
            }
        }
        return keySpec;
    }

    private static BigInteger derint(ByteBuffer input) {
        int len = der(input, 0x02);
        byte[] value = new byte[len];
        input.get(value);
        return new BigInteger(+1, value);
    }

    private static int der(ByteBuffer input, int exp) {
        int tag = input.get() & 0xFF;
        if (tag != exp) throw new IllegalArgumentException("Unexpected tag");
        int n = input.get() & 0xFF;
        if (n < 128) return n;
        n &= 0x7F;
        if ((n < 1) || (n > 2)) throw new IllegalArgumentException("Invalid length");
        int len = 0;
        while (n-- > 0) {
            len <<= 8;
            len |= input.get() & 0xFF;
        }
        return len;
    }

    static RSAPrivateCrtKeySpec decodeRSAPrivatePKCS1(byte[] encoded) {
        ByteBuffer input = ByteBuffer.wrap(encoded);
        if (der(input, 0x30) != input.remaining()) throw new IllegalArgumentException("Excess data");
        if (!BigInteger.ZERO.equals(derint(input))) throw new IllegalArgumentException("Unsupported version");
        BigInteger n = derint(input);
        BigInteger e = derint(input);
        BigInteger d = derint(input);
        BigInteger p = derint(input);
        BigInteger q = derint(input);
        BigInteger ep = derint(input);
        BigInteger eq = derint(input);
        BigInteger c = derint(input);
        return new RSAPrivateCrtKeySpec(n, e, d, p, q, ep, eq, c);
    }
}

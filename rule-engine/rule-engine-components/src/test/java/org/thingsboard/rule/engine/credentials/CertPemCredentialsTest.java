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
package org.thingsboard.rule.engine.credentials;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.thingsboard.common.util.SslUtil;

import java.io.File;
import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.stream.Stream;

import static org.thingsboard.rule.engine.credentials.CertPemCredentials.CERT_ALIAS_PREFIX;
import static org.thingsboard.rule.engine.credentials.CertPemCredentials.PRIVATE_KEY_ALIAS;

public class CertPemCredentialsTest {

    private static final String PASS = "test";
    private static final String RSA = "RSA";
    private static final String EC = "EC";

    @Test
    public void testChainOfCertificates() throws Exception {
        String fileContent = fileContent("pem/tb-cloud-chain.pem");

        List<X509Certificate> x509Certificates = SslUtil.readCertFile(fileContent);

        Assertions.assertEquals(4, x509Certificates.size());
        Assertions.assertEquals("CN=*.thingsboard.cloud, O=\"ThingsBoard, Inc.\", ST=New York, C=US",
                x509Certificates.get(0).getSubjectDN().getName());
        Assertions.assertEquals("CN=Sectigo ECC Organization Validation Secure Server CA, O=Sectigo Limited, L=Salford, ST=Greater Manchester, C=GB",
                x509Certificates.get(1).getSubjectDN().getName());
        Assertions.assertEquals("CN=USERTrust ECC Certification Authority, O=The USERTRUST Network, L=Jersey City, ST=New Jersey, C=US",
                x509Certificates.get(2).getSubjectDN().getName());
        Assertions.assertEquals("CN=AAA Certificate Services, O=Comodo CA Limited, L=Salford, ST=Greater Manchester, C=GB",
                x509Certificates.get(3).getSubjectDN().getName());
    }

    @Test
    public void testSingleCertificate() throws Exception {
        String fileContent = fileContent("pem/tb-cloud.pem");

        List<X509Certificate> x509Certificates = SslUtil.readCertFile(fileContent);

        Assertions.assertEquals(1, x509Certificates.size());
        Assertions.assertEquals("CN=*.thingsboard.cloud, O=\"ThingsBoard, Inc.\", ST=New York, C=US",
                x509Certificates.get(0).getSubjectDN().getName());
    }

    @Test
    public void testEmptyFileContent() throws Exception {
        String fileContent = fileContent("pem/empty.pem");

        List<X509Certificate> x509Certificates = SslUtil.readCertFile(fileContent);

        Assertions.assertEquals(0, x509Certificates.size());
    }

    private static Stream<Arguments> testLoadKeyStore() {
        return Stream.of(
                Arguments.of("pem/rsa_cert.pem", "pem/rsa_key.pem", null, RSA),
                Arguments.of("pem/rsa_encrypted_cert.pem", "pem/rsa_encrypted_key.pem", PASS, RSA),
                Arguments.of("pem/rsa_encrypted_traditional_cert.pem", "pem/rsa_encrypted_traditional_key.pem", PASS, RSA),
                Arguments.of("pem/ec_cert.pem", "pem/ec_key.pem", null, EC)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testLoadKeyStore(String certPath, String keyPath, String password, String algorithm) throws Exception {
        CertPemCredentials certPemCredentials = new CertPemCredentials();
        String certContent = fileContent(certPath);
        certPemCredentials.setCert(certContent);
        certPemCredentials.setPrivateKey(fileContent(keyPath));
        certPemCredentials.setPassword(password);
        KeyStore keyStore = certPemCredentials.loadKeyStore();
        Assertions.assertNotNull(keyStore);
        Key key = keyStore.getKey(PRIVATE_KEY_ALIAS, SslUtil.getPassword(password));
        Assertions.assertNotNull(key);
        Assertions.assertEquals(algorithm, key.getAlgorithm());

        List<X509Certificate> certs = SslUtil.readCertFile(certContent);
        for (X509Certificate cert : certs) {
            String alias = CERT_ALIAS_PREFIX + cert.getIssuerDN().getName();
            Certificate certificate = keyStore.getCertificate(alias);
            Assertions.assertNotNull(certificate);
            Assertions.assertEquals(new String(cert.getEncoded()), new String(certificate.getEncoded()));
        }
    }

    private String fileContent(String fileName) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(fileName).getFile());
        return FileUtils.readFileToString(file, "UTF-8");
    }
}

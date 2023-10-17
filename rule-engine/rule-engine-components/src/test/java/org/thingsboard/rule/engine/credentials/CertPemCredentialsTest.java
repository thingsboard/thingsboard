/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.stream.Stream;

public class CertPemCredentialsTest {

    private final CertPemCredentials credentials = new CertPemCredentials();

    private static final String PASS = "test";
    private static final String EMPTY_PASS = "";
    private static final String RSA = "RSA";
    private static final String ECDSA = "ECDSA";

    @Test
    public void testChainOfCertificates() throws Exception {
        String fileContent = fileContent("pem/tb-cloud-chain.pem");

        List<X509Certificate> x509Certificates = credentials.readCertFile(fileContent);

        Assert.assertEquals(4, x509Certificates.size());
        Assert.assertEquals("CN=*.thingsboard.cloud, O=\"ThingsBoard, Inc.\", ST=New York, C=US",
                x509Certificates.get(0).getSubjectDN().getName());
        Assert.assertEquals("CN=Sectigo ECC Organization Validation Secure Server CA, O=Sectigo Limited, L=Salford, ST=Greater Manchester, C=GB",
                x509Certificates.get(1).getSubjectDN().getName());
        Assert.assertEquals("CN=USERTrust ECC Certification Authority, O=The USERTRUST Network, L=Jersey City, ST=New Jersey, C=US",
                x509Certificates.get(2).getSubjectDN().getName());
        Assert.assertEquals("CN=AAA Certificate Services, O=Comodo CA Limited, L=Salford, ST=Greater Manchester, C=GB",
                x509Certificates.get(3).getSubjectDN().getName());
    }

    @Test
    public void testSingleCertificate() throws Exception {
        String fileContent = fileContent("pem/tb-cloud.pem");

        List<X509Certificate> x509Certificates = credentials.readCertFile(fileContent);

        Assert.assertEquals(1, x509Certificates.size());
        Assert.assertEquals("CN=*.thingsboard.cloud, O=\"ThingsBoard, Inc.\", ST=New York, C=US",
                x509Certificates.get(0).getSubjectDN().getName());
    }

    @Test
    public void testEmptyFileContent() throws Exception {
        String fileContent = fileContent("pem/empty.pem");

        List<X509Certificate> x509Certificates = credentials.readCertFile(fileContent);

        Assert.assertEquals(0, x509Certificates.size());
    }

    private static Stream<Arguments> testReadPrivateKey() {
        return Stream.of(
                Arguments.of("pem/rsa_key.pem", EMPTY_PASS, RSA),
                Arguments.of("pem/rsa_encrypted_key.pem", PASS, RSA),
                Arguments.of("pem/rsa_encrypted_traditional_key.pem", PASS, RSA),
                Arguments.of("pem/ec_key.pem", EMPTY_PASS, ECDSA)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testReadPrivateKey(String keyPath, String password, String algorithm) throws Exception {
        PrivateKey privateKey = credentials.readPrivateKey(fileContent(keyPath), password);
        Assertions.assertNotNull(privateKey);
        Assertions.assertEquals(algorithm, privateKey.getAlgorithm());
    }

    private String fileContent(String fileName) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(fileName).getFile());
        return FileUtils.readFileToString(file, "UTF-8");
    }
}

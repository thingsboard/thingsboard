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
package org.thingsboard.server.transport.coap.x509;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.thingsboard.common.util.SslUtil;
import java.io.File;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.List;

public class CertPrivateKey {
    private final X509Certificate cert;
    private PrivateKey privateKey;

    public CertPrivateKey(String certFilePathPem, String keyFilePathPem) throws Exception {
        List<X509Certificate> certs = SslUtil.readCertFile(fileRead(certFilePathPem));
        this.cert = certs.get(0);
        this.privateKey = SslUtil.readPrivateKey(fileRead(keyFilePathPem), null);
        if (this.privateKey instanceof BCECPrivateKey) {
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(this.privateKey.getEncoded());
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            this.privateKey = keyFactory.generatePrivate(keySpec);
        }
        if (!(this.privateKey instanceof ECPrivateKey)) {
            throw new RuntimeException("Private key generation must be of type java.security.interfaces.ECPrivateKey, which is used in the standard Java API!");
        }
    }

    public CertPrivateKey(X509Certificate cert, PrivateKey privateKey) {
        this.cert = cert;
        this.privateKey = privateKey;
    }

    public X509Certificate getCert() {
        return this.cert;
    }

    public PrivateKey getPrivateKey() {
        return this.privateKey;
    }

    private String fileRead(String fileName) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(fileName).getFile());
        return FileUtils.readFileToString(file, "UTF-8");
    }

    public static String convertCertToPEM(X509Certificate certificate) throws Exception {
        StringBuilder pemBuilder = new StringBuilder();
        pemBuilder.append("-----BEGIN CERTIFICATE-----\n");
        // Copy cert to Base64
        String base64EncodedCert = Base64.getEncoder().encodeToString(certificate.getEncoded());
        int index = 0;
        while (index < base64EncodedCert.length()) {
            pemBuilder.append(base64EncodedCert, index, Math.min(index + 64, base64EncodedCert.length()));
            pemBuilder.append("\n");
            index += 64;
        }
        pemBuilder.append("-----END CERTIFICATE-----\n");
        return pemBuilder.toString();
    }
}
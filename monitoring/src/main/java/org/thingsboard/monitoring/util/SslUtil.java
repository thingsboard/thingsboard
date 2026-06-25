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

import org.thingsboard.monitoring.config.transport.TransportType;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Collection;

public class SslUtil {

    public static KeyStore loadKeyStore(String filePath, String password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        try (InputStream is = new FileInputStream(filePath)) {
            keyStore.load(is, password != null ? password.toCharArray() : null);
        }
        return keyStore;
    }

    public static Certificate[] getCertificatesFromUrl(TransportType transportType, String domain) throws Exception {
        String url = String.format("https://%s:443/api/device-connectivity/%s/certificate/download", domain, transportType.name().toLowerCase());
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .build();
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() != 200) {
            throw new IOException("Failed to download certificate from " + url + ", status code: " + response.statusCode());
        }

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        @SuppressWarnings("unchecked")
        Collection<Certificate> certs = (Collection<Certificate>) cf.generateCertificates(new ByteArrayInputStream(response.body()));
        return certs.toArray(new Certificate[0]);
    }
}

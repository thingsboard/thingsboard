// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.mqtt.credentials;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.thingsboard.common.util.SslUtil;
import org.thingsboard.mqtt.MqttClientConfig;
import org.thingsboard.server.common.data.integration.template.FormFieldType;
import org.thingsboard.server.common.data.integration.template.TemplateField;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import javax.security.auth.x500.X500Principal;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Data
@Slf4j
@JsonIgnoreProperties(ignoreUnknown = true)
public class CertPemClientCredentials implements MqttClientCredentials {

    public static final String PRIVATE_KEY_ALIAS = "private-key";
    public static final String X_509 = "X.509";
    public static final String CERT_ALIAS_PREFIX = "cert-";
    public static final String CA_CERT_CERT_ALIAS_PREFIX = "caCert-cert-";

    @TemplateField(key = "mqttCaCert", label = "CA certificate (PEM)", type = FormFieldType.PASSWORD, secret = true, group = "MQTT Auth", required = true)
    private String caCert;
    @TemplateField(key = "mqttCert", label = "Client certificate (PEM)", type = FormFieldType.PASSWORD, secret = true, group = "MQTT Auth", required = true)
    private String cert;
    @TemplateField(key = "mqttPrivateKey", label = "Private key (PEM)", type = FormFieldType.PASSWORD, secret = true, group = "MQTT Auth", required = true)
    private String privateKey;
    @TemplateField(key = "mqttPrivateKeyPassword", label = "Private key password", type = FormFieldType.PASSWORD, secret = true, group = "MQTT Auth")
    private String privateKeyPassword;

    @Override
    public Optional<SslContext> initSslContext() {
        try {
            Security.addProvider(new BouncyCastleProvider());
            return Optional.of(SslContextBuilder.forClient()
                    .keyManager(createAndInitKeyManagerFactory())
                    .trustManager(createAndInitTrustManagerFactory())
                    .clientAuth(ClientAuth.REQUIRE)
                    .build());
        } catch (Exception e) {
            log.error("[{}:{}] Creating TLS factory failed!", caCert, cert, e);
            throw new RuntimeException("Creating TLS factory failed!", e);
        }
    }

    @Override
    public void configure(MqttClientConfig config) {

    }

    protected TrustManagerFactory createAndInitTrustManagerFactory() throws Exception {
        List<X509Certificate> caCerts = SslUtil.readCertFile(caCert);

        KeyStore caKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        caKeyStore.load(null, null);
        for (X509Certificate caCert : caCerts) {
            caKeyStore.setCertificateEntry(CA_CERT_CERT_ALIAS_PREFIX + caCert.getSubjectX500Principal().getName(X500Principal.RFC1779), caCert);
        }

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(caKeyStore);
        return trustManagerFactory;
    }

    private KeyManagerFactory createAndInitKeyManagerFactory() throws Exception {
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(loadKeyStore(), SslUtil.getPassword(privateKeyPassword));
        return kmf;
    }

    protected KeyStore loadKeyStore() throws Exception {
        List<X509Certificate> certificates = SslUtil.readCertFile(this.cert);
        PrivateKey privateKey = SslUtil.readPrivateKey(this.privateKey, privateKeyPassword);

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null);
        List<X509Certificate> unique = certificates.stream().distinct().collect(Collectors.toList());
        for (X509Certificate cert : unique) {
            keyStore.setCertificateEntry(CERT_ALIAS_PREFIX + cert.getSubjectX500Principal().getName(X500Principal.RFC1779), cert);
        }

        if (privateKey != null) {
            CertificateFactory factory = CertificateFactory.getInstance(X_509);
            CertPath certPath = factory.generateCertPath(certificates);
            List<? extends Certificate> path = certPath.getCertificates();
            Certificate[] x509Certificates = path.toArray(new Certificate[0]);
            keyStore.setKeyEntry(PRIVATE_KEY_ALIAS, privateKey, SslUtil.getPassword(privateKeyPassword), x509Certificates);
        }
        return keyStore;
    }
}

// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.transport.mqtt;

import io.netty.handler.ssl.SslHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.transport.config.ssl.SslCredentials;
import org.thingsboard.server.common.transport.config.ssl.SslCredentialsConfig;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

@Slf4j
@Component("MqttSslHandlerProvider")
@TbMqttSslTransportComponent
public class MqttSslHandlerProvider implements SmartInitializingSingleton {

    @Value("${transport.mqtt.ssl.protocol}")
    private String sslProtocol;

    @Bean
    @ConfigurationProperties(prefix = "transport.mqtt.ssl.credentials")
    public SslCredentialsConfig mqttSslCredentials() {
        return new SslCredentialsConfig("MQTT SSL Credentials", false);
    }

    @Autowired
    @Qualifier("mqttSslCredentials")
    private SslCredentialsConfig mqttSslCredentialsConfig;

    private volatile SSLContext sslContext;

    @Override
    public void afterSingletonsInstantiated() {
        // Eagerly build the initial context so the handshake path is a lock-free volatile read.
        this.sslContext = createSslContext();
        mqttSslCredentialsConfig.registerReloadCallback(() -> {
            log.info("MQTT SSL certificates reloaded. Rebuilding SSL context...");
            // Build the new context first; if it fails, the old one stays in place, and
            // the exception propagates to CertificateReloadManager's retry/backoff logic.
            this.sslContext = createSslContext();
            log.info("MQTT SSL context rebuilt. New connections will use the new certificate.");
        });
    }

    public SslHandler getSslHandler() {
        SSLContext ctx = sslContext;
        // Defensive lazy init in case afterSingletonsInstantiated hasn't run yet (e.g., test wiring).
        // In normal operation ctx is non-null here, so the handshake path is lock-free.
        if (ctx == null) {
            synchronized (this) {
                ctx = sslContext;
                if (ctx == null) {
                    ctx = createSslContext();
                    sslContext = ctx;
                }
            }
        }
        SSLEngine sslEngine = ctx.createSSLEngine();
        sslEngine.setUseClientMode(false);
        sslEngine.setNeedClientAuth(false);
        sslEngine.setWantClientAuth(true);
        sslEngine.setEnabledProtocols(sslEngine.getSupportedProtocols());
        sslEngine.setEnabledCipherSuites(sslEngine.getSupportedCipherSuites());
        sslEngine.setEnableSessionCreation(true);
        return new SslHandler(sslEngine);
    }

    private SSLContext createSslContext() {
        try {
            SslCredentials sslCredentials = this.mqttSslCredentialsConfig.getCredentials();
            TrustManagerFactory tmFactory = sslCredentials.createTrustManagerFactory();
            KeyManagerFactory kmf = sslCredentials.createKeyManagerFactory();

            KeyManager[] km = kmf.getKeyManagers();
            TrustManager x509wrapped = getX509TrustManager(tmFactory);
            TrustManager[] tm = {x509wrapped};
            if (StringUtils.isEmpty(sslProtocol)) {
                sslProtocol = "TLS";
            }
            SSLContext sslContext = SSLContext.getInstance(sslProtocol);
            sslContext.init(km, tm, null);
            return sslContext;
        } catch (Exception e) {
            log.error("Unable to set up SSL context. Reason: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get SSL context", e);
        }
    }

    private TrustManager getX509TrustManager(TrustManagerFactory tmf) throws Exception {
        X509TrustManager x509Tm = null;
        for (TrustManager tm : tmf.getTrustManagers()) {
            if (tm instanceof X509TrustManager x509TrustManager) {
                x509Tm = x509TrustManager;
                break;
            }
        }
        return new ThingsboardMqttX509TrustManager(x509Tm);
    }

    /**
     * Accepts any client certificate chain whose certificates are signed by each other: the device is authenticated at CONNECT.
     * A trust manager runs before the client proves that it holds the private key of the certificate, so it must not ask the core
     * about it: X.509 chain provisioning creates devices and replaces their credentials, and anybody who has seen the certificate
     * could trigger it.
     */
    static class ThingsboardMqttX509TrustManager implements X509TrustManager {

        private final X509TrustManager trustManager;

        ThingsboardMqttX509TrustManager(X509TrustManager trustManager) {
            this.trustManager = trustManager;
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return trustManager.getAcceptedIssuers();
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain,
                                       String authType) throws CertificateException {
            trustManager.checkServerTrusted(chain, authType);
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            if (!validateCertificateChain(chain)) {
                throw new CertificateException("Invalid Chain of X509 Certificates. ");
            }
        }

        private boolean validateCertificateChain(X509Certificate[] chain) {
            try {
                if (chain.length > 1) {
                    X509Certificate leafCert = chain[0];
                    for (int i = 1; i < chain.length; i++) {
                        X509Certificate intermediateCert = chain[i];
                        leafCert.verify(intermediateCert.getPublicKey());
                        leafCert = intermediateCert;
                    }
                }
                return true;
            } catch (Exception e) {
                return false;
            }
        }

    }

}

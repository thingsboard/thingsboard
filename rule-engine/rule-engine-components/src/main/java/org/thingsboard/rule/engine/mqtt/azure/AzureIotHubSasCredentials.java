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
package org.thingsboard.rule.engine.mqtt.azure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.AzureIotHubUtil;
import org.thingsboard.rule.engine.credentials.CertPemCredentials;
import org.thingsboard.rule.engine.credentials.CredentialsType;

import java.security.Provider;
import java.security.Security;

@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureIotHubSasCredentials extends CertPemCredentials {

    /**
     * JCE provider used for TLS trust material. Defaults to the standard
     * BouncyCastle provider ("BC"); override with -Dthingsboard.bc.provider=BCFIPS
     * once a FIPS-certified provider is registered on the classpath instead.
     */
    private static final String PROVIDER_NAME = System.getProperty("thingsboard.bc.provider", "BC");

    private String sasKey;

    @Override
    public SslContext initSslContext() {
        try {
            registerProviderIfAbsent(PROVIDER_NAME);
            if (caCert == null || caCert.isEmpty()) {
                caCert = AzureIotHubUtil.getDefaultCaCert();
            }
            return SslContextBuilder.forClient()
                    .trustManager(createAndInitTrustManagerFactory())
                    .clientAuth(ClientAuth.REQUIRE)
                    .build();
        } catch (Exception e) {
            log.error("[{}] Creating TLS factory failed!", caCert, e);
            throw new RuntimeException("Creating TLS factory failed!", e);
        }
    }

    @Override
    public CredentialsType getType() {
        return CredentialsType.SAS;
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

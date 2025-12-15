/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.common.transport.config.ssl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.ssl.SslStoreBundle;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Slf4j
@Component
@ConditionalOnExpression("'${spring.main.web-environment:true}'=='true' && '${server.ssl.enabled:false}'=='true'")
public class SslCredentialsWebServerCustomizer implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory>, SmartInitializingSingleton {

    private static final String DEFAULT_BUNDLE_NAME = "default";

    private final ServerProperties serverProperties;
    private final List<Consumer<SslBundle>> updateHandlers = new CopyOnWriteArrayList<>();

    @Autowired
    @Qualifier("httpServerSslCredentials")
    private SslCredentialsConfig httpServerSslCredentialsConfig;

    @Autowired
    SslBundles sslBundles;

    public SslCredentialsWebServerCustomizer(ServerProperties serverProperties) {
        this.serverProperties = serverProperties;
    }

    @Bean
    @ConfigurationProperties(prefix = "server.ssl.credentials")
    public SslCredentialsConfig httpServerSslCredentials() {
        return new SslCredentialsConfig("HTTP Server SSL Credentials", false);
    }

    @Bean
    public SslBundles sslBundles() {
        return new DynamicSslBundles();
    }

    @Override
    public void customize(ConfigurableServletWebServerFactory factory) {
        SslCredentials credentials = httpServerSslCredentialsConfig.getCredentials();

        Ssl ssl = serverProperties.getSsl();
        ssl.setBundle(DEFAULT_BUNDLE_NAME);
        ssl.setKeyAlias(credentials.getKeyAlias());
        ssl.setKeyPassword(credentials.getKeyPassword());

        factory.setSsl(ssl);
        factory.setSslBundles(sslBundles);
    }

    @Override
    public void afterSingletonsInstantiated() {
        httpServerSslCredentialsConfig.registerReloadCallback(this::reloadSslCertificates);
    }

    private void reloadSslCertificates() {
        try {
            log.info("Reloading HTTP Server SSL certificates...");

            SslBundle newBundle = createSslBundle();
            notifyUpdateHandlers(newBundle);

            log.info("HTTP Server SSL certificates reloaded successfully");
        } catch (Exception e) {
            log.error("Failed to reload HTTP Server SSL certificates", e);
        }
    }

    private SslBundle createSslBundle() {
        SslCredentials credentials = httpServerSslCredentialsConfig.getCredentials();

        SslStoreBundle storeBundle = SslStoreBundle.of(
                credentials.getKeyStore(),
                credentials.getKeyPassword(),
                null
        );
        return SslBundle.of(storeBundle);
    }

    private void notifyUpdateHandlers(SslBundle newBundle) {
        for (Consumer<SslBundle> handler : updateHandlers) {
            try {
                handler.accept(newBundle);
            } catch (Exception e) {
                log.error("Failed to notify SSL bundle update handler", e);
            }
        }
    }

    private class DynamicSslBundles implements SslBundles {

        @Override
        public SslBundle getBundle(String name) {
            if (!DEFAULT_BUNDLE_NAME.equals(name)) {
                throw new IllegalArgumentException("Unknown SSL bundle: " + name);
            }
            return createSslBundle();
        }

        @Override
        public List<String> getBundleNames() {
            return List.of(DEFAULT_BUNDLE_NAME);
        }

        @Override
        public void addBundleUpdateHandler(String name, Consumer<SslBundle> handler) {
            if (DEFAULT_BUNDLE_NAME.equals(name)) {
                updateHandlers.add(handler);
                log.debug("Registered SSL bundle update handler for bundle: {}", name);
            } else {
                log.warn("Attempted to register update handler for unknown bundle: {}", name);
            }
        }

    }

}

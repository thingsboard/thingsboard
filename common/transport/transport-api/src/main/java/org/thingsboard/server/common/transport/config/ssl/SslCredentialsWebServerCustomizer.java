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
package org.thingsboard.server.common.transport.config.ssl;

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
import java.util.function.Consumer;

@Component
@ConditionalOnExpression("'${spring.main.web-environment:true}'=='true' && '${server.ssl.enabled:false}'=='true'")
public class SslCredentialsWebServerCustomizer implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {

    @Bean
    @ConfigurationProperties(prefix = "server.ssl.credentials")
    public SslCredentialsConfig httpServerSslCredentials() {
        return new SslCredentialsConfig("HTTP Server SSL Credentials", false);
    }

    @Autowired
    @Qualifier("httpServerSslCredentials")
    private SslCredentialsConfig httpServerSslCredentialsConfig;

    @Autowired
    SslBundles sslBundles;

    private final ServerProperties serverProperties;

    public SslCredentialsWebServerCustomizer(ServerProperties serverProperties) {
        this.serverProperties = serverProperties;
    }

    @Override
    public void customize(ConfigurableServletWebServerFactory factory) {
        SslCredentials sslCredentials = this.httpServerSslCredentialsConfig.getCredentials();
        Ssl ssl = serverProperties.getSsl();
        ssl.setBundle("default");
        ssl.setKeyAlias(sslCredentials.getKeyAlias());
        ssl.setKeyPassword(sslCredentials.getKeyPassword());
        factory.setSsl(ssl);
        factory.setSslBundles(sslBundles);
    }

    @Bean
    public SslBundles sslBundles() {
        SslStoreBundle storeBundle = SslStoreBundle.of(
                httpServerSslCredentialsConfig.getCredentials().getKeyStore(),
                httpServerSslCredentialsConfig.getCredentials().getKeyPassword(),
                null
        );
        return new SslBundles() {
            @Override
            public SslBundle getBundle(String name) {
                return SslBundle.of(storeBundle);
            }

            @Override
            public List<String> getBundleNames() {
                return List.of("default");
            }

            @Override
            public void addBundleUpdateHandler(String name, Consumer<SslBundle> handler) {
                // no-op
            }
        };
    }

}

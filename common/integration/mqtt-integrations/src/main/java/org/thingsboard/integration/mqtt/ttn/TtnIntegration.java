// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.mqtt.ttn;

import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.thingsboard.integration.mqtt.MqttClientConfiguration;
import org.thingsboard.integration.mqtt.basic.BasicMqttIntegration;
import org.thingsboard.integration.mqtt.credentials.BasicCredentials;
import org.thingsboard.integration.mqtt.credentials.MqttClientCredentials;
import org.thingsboard.server.common.data.StringUtils;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLException;
import java.io.File;
import java.security.Security;
import java.util.Optional;

@Slf4j
public class TtnIntegration extends BasicMqttIntegration {

    private static final String TTN_ENDPOINT = "thethings.network";
    private static final String TTI_ENDPOINT = "thethings.industries";

    @Override
    protected String getDownlinkTopicPattern() {
        return this.configuration.getConfiguration().get("downlinkTopicPattern").asText();
    }

    @Override
    protected void setupConfiguration(MqttClientConfiguration mqttClientConfiguration) {
        String integrationType = this.configuration.getType().name();
        mqttClientConfiguration.setCleanSession(true);
        MqttClientCredentials credentials = mqttClientConfiguration.getCredentials();
        if (!(credentials instanceof BasicCredentials basicCredentials)) {
            throw new RuntimeException("Can't setup TheThingsNetwork integration without Application Credentials!");
        }
        if (StringUtils.isEmpty(basicCredentials.getUsername()) ||
                StringUtils.isEmpty(basicCredentials.getPassword())) {
            throw new RuntimeException("Can't setup TheThingsNetwork integration. Required TheThingsNetwork Application Credentials values are missing!");
        }

        if (!mqttClientConfiguration.isCustomHost()) {
            String region = mqttClientConfiguration.getHost();
            if (integrationType.equals("TTN") && !region.endsWith(TTN_ENDPOINT)){
                mqttClientConfiguration.setHost(region + "." + TTN_ENDPOINT);
            } else if (integrationType.equals("TTI") && !region.endsWith(TTI_ENDPOINT)) {
                mqttClientConfiguration.setHost(region + "." + TTI_ENDPOINT);
            }
        }
    }

    @Override
    protected Optional<SslContext> initSslContext(MqttClientConfiguration configuration) throws SSLException {
        try {
            if (configuration.isSsl()) {
                Security.addProvider(new BouncyCastleProvider());
                return Optional.of(SslContextBuilder.forClient()
                        .keyManager((KeyManagerFactory) null)
                        .trustManager((File) null)
                        .clientAuth(ClientAuth.NONE)
                        .build());
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Creating TLS factory failed!", e);
            throw new RuntimeException("Creating TLS factory failed!", e);
        }
    }

}

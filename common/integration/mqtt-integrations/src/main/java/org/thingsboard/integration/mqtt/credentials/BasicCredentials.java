// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.mqtt.credentials;

import io.netty.handler.ssl.SslContext;
import lombok.Data;
import org.thingsboard.mqtt.MqttClientConfig;
import org.thingsboard.server.common.data.integration.template.FormFieldType;
import org.thingsboard.server.common.data.integration.template.TemplateField;

import java.util.Optional;

@Data
public class BasicCredentials implements MqttClientCredentials {

    @TemplateField(key = "mqttUsername", label = "MQTT username", secret = true, group = "MQTT Auth", required = true)
    private String username;
    @TemplateField(key = "mqttPassword", label = "MQTT password", type = FormFieldType.PASSWORD, secret = true, group = "MQTT Auth", required = true)
    private String password;

    @Override
    public Optional<SslContext> initSslContext() {
        return Optional.empty();
    }

    @Override
    public void configure(MqttClientConfig config) {
        config.setUsername(username);
        config.setPassword(password);
    }

}

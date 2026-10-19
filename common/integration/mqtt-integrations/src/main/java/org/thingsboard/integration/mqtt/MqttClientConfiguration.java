// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.mqtt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.netty.handler.codec.mqtt.MqttVersion;
import lombok.Data;
import org.thingsboard.integration.mqtt.credentials.MqttClientCredentials;
import org.thingsboard.server.common.data.integration.template.FormFieldType;
import org.thingsboard.server.common.data.integration.template.TemplateField;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MqttClientConfiguration {

    @TemplateField(key = "mqttHost", label = "Broker host", group = "MQTT Connection", required = true)
    private String host;
    private boolean customHost;
    @TemplateField(key = "mqttPort", label = "Broker port", type = FormFieldType.INTEGER, group = "MQTT Connection")
    private int port;
    @TemplateField(key = "mqttConnectTimeoutSec", label = "Connect timeout (s)", type = FormFieldType.INTEGER, group = "MQTT Connection")
    private int connectTimeoutSec;
    @TemplateField(key = "mqttClientId", label = "Client ID", group = "MQTT Connection")
    private String clientId;
    private Integer maxBytesInMessage;
    private boolean retainedMessage;
    @TemplateField(key = "mqttCleanSession", label = "Clean session", type = FormFieldType.BOOLEAN, group = "MQTT Connection")
    private boolean cleanSession;
    @TemplateField(key = "mqttSsl", label = "Use TLS", type = FormFieldType.BOOLEAN, group = "MQTT Connection")
    private boolean ssl;
    private MqttVersion protocolVersion;
    private MqttClientCredentials credentials;

}

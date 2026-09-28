// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.rabbitmq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.thingsboard.server.common.data.integration.template.FormFieldType;
import org.thingsboard.server.common.data.integration.template.TemplateField;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RabbitMQConsumerConfiguration {

    @TemplateField(key = "rabbitmqExchangeName", label = "Exchange name",
                   group = "RabbitMQ Connection", required = true)
    private String exchangeName;

    @TemplateField(key = "rabbitmqHost", label = "Host",
                   group = "RabbitMQ Connection", required = true)
    private String host;

    @TemplateField(key = "rabbitmqPort", label = "Port",
                   type = FormFieldType.INTEGER, group = "RabbitMQ Connection", required = true)
    private int port;

    private String virtualHost;

    @TemplateField(key = "rabbitmqUsername", label = "Username",
                   secret = true, group = "RabbitMQ Connection", required = true)
    private String username;

    @TemplateField(key = "rabbitmqPassword", label = "Password",
                   type = FormFieldType.PASSWORD, secret = true, group = "RabbitMQ Connection", required = true)
    private String password;

    private String downlinkTopic;

    @TemplateField(key = "rabbitmqQueues", label = "Queues",
                   group = "RabbitMQ Connection", required = true)
    private String queues;
    private int connectionTimeout;
    private int handshakeTimeout;
    private long pollPeriod;
    private Boolean durable;
    private Boolean exclusive;
    private Boolean autoDelete;
}

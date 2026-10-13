// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api.util;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.integration.api.ThingsboardPlatformIntegration;
import org.thingsboard.server.common.data.integration.IntegrationType;

public class IntegrationUtil {

    public static ThingsboardPlatformIntegration<?> createPlatformIntegration(IntegrationType type, JsonNode configuration, boolean remote, Object param) throws Exception {
        switch (type) {
            case HTTP:
                return newInstance("org.thingsboard.integration.http.basic.BasicHttpIntegration");
            case LORIOT:
                return newInstance("org.thingsboard.integration.http.loriot.LoriotIntegration");
            case SIGFOX:
                return newInstance("org.thingsboard.integration.http.sigfox.SigFoxIntegration");
            case OCEANCONNECT:
                return newInstance("org.thingsboard.integration.http.oc.OceanConnectIntegration");
            case THINGPARK:
                return newInstance("org.thingsboard.integration.http.thingpark.ThingParkIntegration");
            case TPE:
                return newInstance("org.thingsboard.integration.http.thingpark.ThingParkIntegrationEnterprise");
            case TMOBILE_IOT_CDP:
                return newInstance("org.thingsboard.integration.http.tmobile.TMobileIotCdpIntegration");
            case CHIRPSTACK:
                return newInstance("org.thingsboard.integration.http.chirpstack.ChirpStackIntegration");
            case PARTICLE:
                return newInstance("org.thingsboard.integration.http.particle.ParticleIntegration");
            case KPN:
                return newInstance("org.thingsboard.integration.http.kpn.KpnIntegration");
            case MQTT:
                return newInstance("org.thingsboard.integration.mqtt.basic.BasicMqttIntegration");
            case AWS_IOT:
                return newInstance("org.thingsboard.integration.mqtt.aws.AwsIotIntegration");
            case PUB_SUB:
                return newInstance("org.thingsboard.gcloud.pubsub.PubSubIntegration");
            case TTI:
            case TTN:
                return newInstance("org.thingsboard.integration.mqtt.ttn.TtnIntegration");
            case AZURE_EVENT_HUB:
                return newInstance("org.thingsboard.integration.azure.AzureEventHubIntegration");
            case AZURE_IOT_HUB:
                return newInstance("org.thingsboard.integration.mqtt.azure.AzureIotHubIntegration");
            case OPC_UA:
                return newInstance("org.thingsboard.integration.opcua.OpcUaIntegration");
            case AWS_SQS:
                return newInstance("org.thingsboard.integration.aws.sqs.AwsSqsIntegration");
            case AWS_KINESIS:
                return newInstance("org.thingsboard.integration.aws.kinesis.AwsKinesisIntegration");
            case KAFKA:
                return newInstance("org.thingsboard.integration.kafka.basic.BasicKafkaIntegration");
            case RABBITMQ:
                return newInstance("org.thingsboard.integration.rabbitmq.RabbitMQIntegration");
            case APACHE_PULSAR:
                return newInstance("org.thingsboard.integration.apache.pulsar.basic.BasicPulsarIntegration");
            case TUYA:
                return newInstance("org.thingsboard.integration.tuya.TuyaIntegration");
            case COAP:
                return newInstance("org.thingsboard.integration.coap.CoapIntegration", param);
            case TCP:
                if (remote) {
                    return newInstance("org.thingsboard.integration.tcpip.tcp.BasicTcpIntegration");
                } else {
                    throw new RuntimeException("TCP Integration should be executed remotely!");
                }
            case UDP:
                if (remote) {
                    return newInstance("org.thingsboard.integration.tcpip.udp.BasicUdpIntegration");
                } else {
                    throw new RuntimeException("TCP Integration should be executed remotely!");
                }
            case CUSTOM:
                if (remote) {
                    return newInstance(configuration.get("clazz").asText());
                } else {
                    throw new RuntimeException("Custom Integrations should be executed remotely!");
                }
            case AZURE_SERVICE_BUS:
                return newInstance("org.thingsboard.integration.azure.AzureServiceBusIntegration");
            default:
                throw new RuntimeException("Not Implemented!");
        }
    }

    private static ThingsboardPlatformIntegration<?> newInstance(String clazz) throws Exception {
        return newInstance(clazz, null);
    }

    private static ThingsboardPlatformIntegration<?> newInstance(String clazz, Object param) throws Exception {
        if (param != null) {
            return (ThingsboardPlatformIntegration<?>) Class.forName(clazz).getDeclaredConstructors()[0].newInstance(param);
        } else {
            return (ThingsboardPlatformIntegration<?>) Class.forName(clazz).getDeclaredConstructor().newInstance();
        }
    }

}

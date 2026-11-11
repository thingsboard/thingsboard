// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration.template;

import org.springframework.stereotype.Component;
import org.thingsboard.gcloud.pubsub.PubSubIntegrationConfiguration;
import org.thingsboard.integration.apache.pulsar.PulsarConfiguration;
import org.thingsboard.integration.aws.kinesis.KinesisClientConfiguration;
import org.thingsboard.integration.aws.sqs.SqsIntegrationConfiguration;
import org.thingsboard.integration.azure.AzureEventHubClientConfiguration;
import org.thingsboard.integration.azure.AzureServiceBusClientConfiguration;
import org.thingsboard.integration.coap.CoapClientConfiguration;
import org.thingsboard.integration.http.chirpstack.ChirpStackConfiguration;
import org.thingsboard.integration.http.kpn.KpnConfiguration;
import org.thingsboard.integration.http.loriot.LoriotConfiguration;
import org.thingsboard.integration.http.particle.ParticleConfiguration;
import org.thingsboard.integration.http.thingpark.ThingParkConfiguration;
import org.thingsboard.integration.http.thingpark.ThingParkEnterpriseConfiguration;
import org.thingsboard.integration.kafka.KafkaConsumerConfiguration;
import org.thingsboard.integration.mqtt.MqttClientConfiguration;
import org.thingsboard.integration.opcua.OpcUaServerConfiguration;
import org.thingsboard.integration.rabbitmq.RabbitMQConsumerConfiguration;
import org.thingsboard.integration.tuya.TuyaIntegrationConfiguration;
import org.thingsboard.server.common.data.integration.IntegrationType;

import java.util.List;
import java.util.Map;

/**
 * Maps each {@link IntegrationType} to the list of POJO walks the export service runs
 * for it. The implicit {@code Integration.class} spec at prefix {@code ""} is added by
 * the export service for every type to capture the universal {@code Integration#name}
 * annotation.
 *
 * <p>Adding a new annotated integration type is one entry here. No classpath scanning.
 */
@Component
public class IntegrationConfigPojoRegistry {

    private static final Map<IntegrationType, List<IntegrationPojoSpec>> SPECS = Map.ofEntries(
        Map.entry(IntegrationType.LORIOT, List.of(
            new IntegrationPojoSpec(LoriotConfiguration.class, "configuration")
        )),
        Map.entry(IntegrationType.MQTT, List.of(
            new IntegrationPojoSpec(MqttClientConfiguration.class, "configuration.clientConfiguration")
        )),
        Map.entry(IntegrationType.AWS_IOT, List.of(
            new IntegrationPojoSpec(MqttClientConfiguration.class, "configuration.clientConfiguration")
        )),
        Map.entry(IntegrationType.AZURE_IOT_HUB, List.of(
            new IntegrationPojoSpec(MqttClientConfiguration.class, "configuration.clientConfiguration")
        )),
        Map.entry(IntegrationType.TTN, List.of(
            new IntegrationPojoSpec(MqttClientConfiguration.class, "configuration.clientConfiguration")
        )),
        Map.entry(IntegrationType.TTI, List.of(
            new IntegrationPojoSpec(MqttClientConfiguration.class, "configuration.clientConfiguration")
        )),
        Map.entry(IntegrationType.CHIRPSTACK, List.of(
            new IntegrationPojoSpec(ChirpStackConfiguration.class, "configuration.clientConfiguration")
        )),
        Map.entry(IntegrationType.THINGPARK, List.of(
            new IntegrationPojoSpec(ThingParkConfiguration.class, "configuration")
        )),
        Map.entry(IntegrationType.TPE, List.of(
            new IntegrationPojoSpec(ThingParkEnterpriseConfiguration.class, "configuration")
        )),
        Map.entry(IntegrationType.PARTICLE, List.of(
            new IntegrationPojoSpec(ParticleConfiguration.class, "configuration")
        )),
        Map.entry(IntegrationType.KPN, List.of(
            new IntegrationPojoSpec(KpnConfiguration.class, "configuration")
        )),
        Map.entry(IntegrationType.PUB_SUB, List.of(
            new IntegrationPojoSpec(PubSubIntegrationConfiguration.class, "configuration")
        )),
        Map.entry(IntegrationType.AWS_SQS, List.of(
            new IntegrationPojoSpec(SqsIntegrationConfiguration.class, "configuration.sqsConfiguration")
        )),
        Map.entry(IntegrationType.AWS_KINESIS, List.of(
            new IntegrationPojoSpec(KinesisClientConfiguration.class, "configuration")
        )),
        Map.entry(IntegrationType.AZURE_EVENT_HUB, List.of(
            new IntegrationPojoSpec(AzureEventHubClientConfiguration.class, "configuration")
        )),
        Map.entry(IntegrationType.AZURE_SERVICE_BUS, List.of(
            new IntegrationPojoSpec(AzureServiceBusClientConfiguration.class, "configuration")
        )),
        Map.entry(IntegrationType.KAFKA, List.of(
            new IntegrationPojoSpec(KafkaConsumerConfiguration.class, "configuration")
        )),
        Map.entry(IntegrationType.APACHE_PULSAR, List.of(
            new IntegrationPojoSpec(PulsarConfiguration.class, "configuration")
        )),
        Map.entry(IntegrationType.RABBITMQ, List.of(
            new IntegrationPojoSpec(RabbitMQConsumerConfiguration.class, "configuration")
        )),
        Map.entry(IntegrationType.COAP, List.of(
            new IntegrationPojoSpec(CoapClientConfiguration.class, "configuration")
        )),
        Map.entry(IntegrationType.TUYA, List.of(
            new IntegrationPojoSpec(TuyaIntegrationConfiguration.class, "configuration")
        )),
        Map.entry(IntegrationType.OPC_UA, List.of(
            new IntegrationPojoSpec(OpcUaServerConfiguration.class, "configuration")
        ))
    );

    public List<IntegrationPojoSpec> getSpecs(IntegrationType type) {
        return SPECS.getOrDefault(type, List.of());
    }
}

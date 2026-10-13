// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.integration.IntegrationType;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class IntegrationPackageExportServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private IntegrationPackageExportService service;

    @BeforeEach
    void setUp() {
        var registry = new IntegrationConfigPojoRegistry();
        service = new IntegrationPackageExportService(
            registry,
            new PojoFieldWalker(),
            new IntegrationJsonCleaner(),
            new ConverterJsonCleaner(),
            mapper
        );
    }

    @Test
    void tokenize_loriot_replaces_top_level_token_with_clean_form_key() throws Exception {
        ObjectNode integrationJson = (ObjectNode) mapper.readTree("""
            {
              "name": "Acme LoRaWAN Sensor",
              "type": "LORIOT",
              "enabled": true,
              "debugMode": true,
              "configuration": {
                "server": "eu1",
                "domain": "loriot.io",
                "appId": "ACME-1234",
                "token": "real-token-value",
                "sendDownlink": true,
                "metadata": {}
              }
            }
            """);

        IntegrationPackageExportService.TokenizationResult result =
            service.tokenize(IntegrationType.LORIOT, integrationJson);

        assertThat(result.json().get("name").asText()).isEqualTo("${integrationName}");
        assertThat(result.json().get("configuration").get("server").asText()).isEqualTo("${loriotServer}");
        assertThat(result.json().get("configuration").get("appId").asText()).isEqualTo("${loriotAppId}");
        assertThat(result.json().get("configuration").get("token").asText()).isEqualTo("${loriotToken}");

        assertThat(result.formEntries()).extracting(e -> e.get("key"))
            .containsExactlyInAnyOrder("integrationName", "loriotServer", "loriotDomain",
                                       "loriotAppId", "loriotToken");

        var loriotServer = result.formEntries().stream()
            .filter(e -> "loriotServer".equals(e.get("key"))).findFirst().orElseThrow();
        assertThat(loriotServer.get("defaultValue")).isEqualTo("eu1");

        var loriotToken = result.formEntries().stream()
            .filter(e -> "loriotToken".equals(e.get("key"))).findFirst().orElseThrow();
        assertThat(loriotToken.get("secretSupport")).isEqualTo(true);
        assertThat(loriotToken.get("secretType")).isEqualTo("TEXT");
        assertThat(loriotToken.get("defaultValue")).isEqualTo("");
    }

    @Test
    void tokenize_loriot_basic_credentials_replaces_annotated_fields() throws Exception {
        ObjectNode integrationJson = (ObjectNode) mapper.readTree("""
            {
              "name": "Acme LoRaWAN Sensor",
              "type": "LORIOT",
              "configuration": {
                "server": "us1",
                "domain": "loriot.io",
                "appId": "ACME-9876",
                "credentials": { "type": "basic", "username": "user@acme", "password": "real-pwd" },
                "metadata": {}
              }
            }
            """);

        IntegrationPackageExportService.TokenizationResult result =
            service.tokenize(IntegrationType.LORIOT, integrationJson);

        assertThat(result.formEntries()).extracting(e -> e.get("key"))
            .contains("integrationName", "loriotUsername", "loriotPassword")
            .doesNotContain("loriotToken");
        assertThat(result.json().get("configuration").get("credentials").get("username").asText())
            .isEqualTo("${loriotUsername}");
        assertThat(result.json().get("configuration").get("credentials").get("password").asText())
            .isEqualTo("${loriotPassword}");
    }

    @Test
    void tokenize_mqtt_basic_credentials_emits_correct_paths_and_keys() throws Exception {
        ObjectNode integrationJson = (ObjectNode) mapper.readTree("""
            {
              "name": "Acme MQTT Sensor",
              "type": "MQTT",
              "configuration": {
                "clientConfiguration": {
                  "host": "broker.example.com",
                  "port": 1883,
                  "ssl": false,
                  "cleanSession": false,
                  "clientId": "acme-001",
                  "connectTimeoutSec": 10,
                  "credentials": { "type": "basic", "username": "u", "password": "p" }
                },
                "topicFilters": [{ "filter": "sensors/+/up", "qos": 1 }],
                "downlinkTopicPattern": "${topic}",
                "metadata": {}
              }
            }
            """);

        IntegrationPackageExportService.TokenizationResult result =
            service.tokenize(IntegrationType.MQTT, integrationJson);

        assertThat(result.formEntries()).extracting(e -> e.get("key"))
            .contains("integrationName", "mqttHost", "mqttPort", "mqttSsl",
                      "mqttClientId", "mqttCleanSession", "mqttConnectTimeoutSec",
                      "mqttUsername", "mqttPassword");
        // Topic filters preserved as-is (List, not annotated)
        assertThat(result.json().get("configuration").get("topicFilters").get(0).get("filter").asText())
            .isEqualTo("sensors/+/up");
    }

    @Test
    void tokenize_aws_iot_cert_pem_credentials() throws Exception {
        ObjectNode integrationJson = (ObjectNode) mapper.readTree("""
            {
              "name": "Acme AWS Sensor",
              "type": "AWS_IOT",
              "configuration": {
                "clientConfiguration": {
                  "host": "x-ats.iot.us-east-1.amazonaws.com",
                  "port": 8883,
                  "ssl": true,
                  "cleanSession": true,
                  "credentials": {
                    "type": "cert.PEM",
                    "caCert": "CA_PEM",
                    "cert": "CERT_PEM",
                    "privateKey": "KEY_PEM"
                  }
                },
                "metadata": {}
              }
            }
            """);

        IntegrationPackageExportService.TokenizationResult result =
            service.tokenize(IntegrationType.AWS_IOT, integrationJson);

        assertThat(result.formEntries()).extracting(e -> e.get("key"))
            .contains("mqttCaCert", "mqttCert", "mqttPrivateKey")
            .doesNotContain("mqttUsername", "mqttPassword");
    }

    @Test
    void tokenize_azure_iot_hub_sas() throws Exception {
        ObjectNode integrationJson = (ObjectNode) mapper.readTree("""
            {
              "name": "Acme Azure Sensor",
              "type": "AZURE_IOT_HUB",
              "configuration": {
                "clientConfiguration": {
                  "host": "myhub.azure-devices.net",
                  "credentials": { "type": "sas", "sasKey": "real-sas-key" }
                },
                "metadata": {}
              }
            }
            """);

        IntegrationPackageExportService.TokenizationResult result =
            service.tokenize(IntegrationType.AZURE_IOT_HUB, integrationJson);

        assertThat(result.formEntries()).extracting(e -> e.get("key"))
            .contains("azureSasKey");
    }

    @Test
    void tokenize_chirpstack_replaces_application_server_url_token_and_api_flag() throws Exception {
        ObjectNode integrationJson = (ObjectNode) mapper.readTree("""
            {
              "name": "Acme ChirpStack Sensor",
              "type": "CHIRPSTACK",
              "configuration": {
                "clientConfiguration": {
                  "applicationServerUrl": "https://chirpstack.example.com",
                  "applicationServerAPIToken": "real-api-token",
                  "useAPI4Plus": true
                },
                "metadata": {}
              }
            }
            """);

        IntegrationPackageExportService.TokenizationResult result =
            service.tokenize(IntegrationType.CHIRPSTACK, integrationJson);

        JsonNode clientConfig = result.json().get("configuration").get("clientConfiguration");
        assertThat(clientConfig.get("applicationServerUrl").asText())
            .isEqualTo("${chirpstackApplicationServerUrl}");
        assertThat(clientConfig.get("applicationServerAPIToken").asText())
            .isEqualTo("${chirpstackApplicationServerApiToken}");
        assertThat(clientConfig.get("useAPI4Plus").asText())
            .isEqualTo("${chirpstackUseApi4Plus}");

        assertThat(result.formEntries()).extracting(e -> e.get("key"))
            .contains("integrationName", "chirpstackApplicationServerUrl",
                      "chirpstackApplicationServerApiToken", "chirpstackUseApi4Plus");

        var urlEntry = result.formEntries().stream()
            .filter(e -> "chirpstackApplicationServerUrl".equals(e.get("key"))).findFirst().orElseThrow();
        assertThat(urlEntry.get("defaultValue")).isEqualTo("https://chirpstack.example.com");

        var tokenEntry = result.formEntries().stream()
            .filter(e -> "chirpstackApplicationServerApiToken".equals(e.get("key"))).findFirst().orElseThrow();
        assertThat(tokenEntry.get("secretSupport")).isEqualTo(true);
        assertThat(tokenEntry.get("defaultValue")).isEqualTo("");
    }

    @Test
    void tokenize_ttn_with_basic_credentials() throws Exception {
        ObjectNode integrationJson = (ObjectNode) mapper.readTree("""
            {
              "name": "Acme TTN Sensor",
              "type": "TTN",
              "configuration": {
                "clientConfiguration": {
                  "host": "eu1",
                  "port": 1883,
                  "ssl": true,
                  "cleanSession": true,
                  "clientId": "acme-ttn-001",
                  "connectTimeoutSec": 10,
                  "credentials": { "type": "basic", "username": "app@ttn", "password": "ttn-access-key" }
                },
                "downlinkTopicPattern": "v3/{username}/devices/{devEui}/down/push",
                "metadata": {}
              }
            }
            """);

        IntegrationPackageExportService.TokenizationResult result =
            service.tokenize(IntegrationType.TTN, integrationJson);

        assertThat(result.formEntries()).extracting(e -> e.get("key"))
            .contains("integrationName", "mqttHost", "mqttPort", "mqttSsl",
                      "mqttClientId", "mqttCleanSession", "mqttConnectTimeoutSec",
                      "mqttUsername", "mqttPassword");

        var hostEntry = result.formEntries().stream()
            .filter(e -> "mqttHost".equals(e.get("key"))).findFirst().orElseThrow();
        assertThat(hostEntry.get("defaultValue")).isEqualTo("eu1");

        assertThat(result.json().get("configuration").get("clientConfiguration").get("host").asText())
            .isEqualTo("${mqttHost}");

        assertThat(result.json().get("configuration").get("downlinkTopicPattern").asText())
            .isEqualTo("v3/{username}/devices/{devEui}/down/push");
    }

    @Test
    void tokenize_tti_with_basic_credentials() throws Exception {
        ObjectNode integrationJson = (ObjectNode) mapper.readTree("""
            {
              "name": "Acme TTI Sensor",
              "type": "TTI",
              "configuration": {
                "clientConfiguration": {
                  "host": "nam1",
                  "port": 8883,
                  "ssl": true,
                  "cleanSession": true,
                  "credentials": { "type": "basic", "username": "app@tti", "password": "tti-access-key" }
                },
                "metadata": {}
              }
            }
            """);

        IntegrationPackageExportService.TokenizationResult result =
            service.tokenize(IntegrationType.TTI, integrationJson);

        assertThat(result.formEntries()).extracting(e -> e.get("key"))
            .contains("integrationName", "mqttHost", "mqttPort", "mqttSsl",
                      "mqttCleanSession", "mqttUsername", "mqttPassword");

        var hostEntry = result.formEntries().stream()
            .filter(e -> "mqttHost".equals(e.get("key"))).findFirst().orElseThrow();
        assertThat(hostEntry.get("defaultValue")).isEqualTo("nam1");
    }

    @Test
    void tokenize_sigfox_only_replaces_integration_name() throws Exception {
        ObjectNode integrationJson = (ObjectNode) mapper.readTree("""
            {
              "name": "Acme Sigfox Sensor",
              "type": "SIGFOX",
              "configuration": {
                "enableSecurity": false,
                "headersFilter": {},
                "replaceNoContentToOk": false,
                "metadata": {}
              }
            }
            """);

        IntegrationPackageExportService.TokenizationResult result =
            service.tokenize(IntegrationType.SIGFOX, integrationJson);

        assertThat(result.json().get("name").asText()).isEqualTo("${integrationName}");
        assertThat(result.formEntries()).extracting(e -> e.get("key"))
            .containsExactlyInAnyOrder("integrationName");
        assertThat(result.json().get("configuration").get("enableSecurity").asBoolean()).isFalse();
    }

    @Test
    void tokenize_thingpark_replaces_security_and_downlink_fields() throws Exception {
        ObjectNode integrationJson = (ObjectNode) mapper.readTree("""
            {
              "name": "Acme ThingPark Sensor",
              "type": "THINGPARK",
              "configuration": {
                "enableSecurity": true,
                "asId": "AS-1234",
                "asKey": "real-as-key",
                "maxTimeDiffInSeconds": 300,
                "downlinkUrl": "https://api.thingpark.com/thingpark/lrc/rest/downlink",
                "metadata": {}
              }
            }
            """);

        IntegrationPackageExportService.TokenizationResult result =
            service.tokenize(IntegrationType.THINGPARK, integrationJson);

        JsonNode config = result.json().get("configuration");
        assertThat(config.get("asId").asText()).isEqualTo("${thingparkAsId}");
        assertThat(config.get("asKey").asText()).isEqualTo("${thingparkAsKey}");
        assertThat(config.get("maxTimeDiffInSeconds").asText()).isEqualTo("${thingparkMaxTimeDiffInSeconds}");
        assertThat(config.get("downlinkUrl").asText()).isEqualTo("${thingparkDownlinkUrl}");

        assertThat(result.formEntries()).extracting(e -> e.get("key"))
            .contains("integrationName", "thingparkAsId", "thingparkAsKey",
                      "thingparkMaxTimeDiffInSeconds", "thingparkDownlinkUrl");

        var asKeyEntry = result.formEntries().stream()
            .filter(e -> "thingparkAsKey".equals(e.get("key"))).findFirst().orElseThrow();
        assertThat(asKeyEntry.get("secretSupport")).isEqualTo(true);

        var asIdEntry = result.formEntries().stream()
            .filter(e -> "thingparkAsId".equals(e.get("key"))).findFirst().orElseThrow();
        assertThat(asIdEntry.get("defaultValue")).isEqualTo("AS-1234");

        var maxTimeDiffEntry = result.formEntries().stream()
            .filter(e -> "thingparkMaxTimeDiffInSeconds".equals(e.get("key"))).findFirst().orElseThrow();
        assertThat(maxTimeDiffEntry.get("defaultValue")).isEqualTo("300");
    }

    @Test
    void tokenize_tpe_replaces_oauth_credentials() throws Exception {
        ObjectNode integrationJson = (ObjectNode) mapper.readTree("""
            {
              "name": "Acme TPE Sensor",
              "type": "TPE",
              "configuration": {
                "enableSecurityNew": true,
                "asIdNew": "AS-NEW-9999",
                "asKey": "real-as-key",
                "clientIdNew": "tpe-client",
                "clientSecret": "real-client-secret",
                "downlinkUrl": "https://api.thingpark.com/thingpark/lrc/rest/downlink",
                "metadata": {}
              }
            }
            """);

        IntegrationPackageExportService.TokenizationResult result =
            service.tokenize(IntegrationType.TPE, integrationJson);

        JsonNode config = result.json().get("configuration");
        assertThat(config.get("asIdNew").asText()).isEqualTo("${tpeAsId}");
        assertThat(config.get("asKey").asText()).isEqualTo("${tpeAsKey}");
        assertThat(config.get("clientIdNew").asText()).isEqualTo("${tpeClientId}");
        assertThat(config.get("clientSecret").asText()).isEqualTo("${tpeClientSecret}");
        assertThat(config.get("downlinkUrl").asText()).isEqualTo("${tpeDownlinkUrl}");

        assertThat(result.formEntries()).extracting(e -> e.get("key"))
            .contains("integrationName", "tpeAsId", "tpeAsKey",
                      "tpeClientId", "tpeClientSecret", "tpeDownlinkUrl");

        var asKeyEntry = result.formEntries().stream()
            .filter(e -> "tpeAsKey".equals(e.get("key"))).findFirst().orElseThrow();
        assertThat(asKeyEntry.get("secretSupport")).isEqualTo(true);

        var clientSecretEntry = result.formEntries().stream()
            .filter(e -> "tpeClientSecret".equals(e.get("key"))).findFirst().orElseThrow();
        assertThat(clientSecretEntry.get("secretSupport")).isEqualTo(true);

        var asIdEntry = result.formEntries().stream()
            .filter(e -> "tpeAsId".equals(e.get("key"))).findFirst().orElseThrow();
        assertThat(asIdEntry.get("defaultValue")).isEqualTo("AS-NEW-9999");
    }

    @Test
    void tokenize_particle_replaces_token_via_credentials_descent() throws Exception {
        ObjectNode integrationJson = (ObjectNode) mapper.readTree("""
            {
              "name": "Acme Particle Sensor",
              "type": "PARTICLE",
              "configuration": {
                "allowDownlink": true,
                "credentials": { "token": "real-particle-token" }
              }
            }
            """);

        IntegrationPackageExportService.TokenizationResult result =
            service.tokenize(IntegrationType.PARTICLE, integrationJson);

        assertThat(result.json().get("configuration").get("allowDownlink").asText())
            .isEqualTo("${particleAllowDownlink}");
        assertThat(result.json().get("configuration").get("credentials").get("token").asText())
            .isEqualTo("${particleAccessToken}");

        assertThat(result.formEntries()).extracting(e -> e.get("key"))
            .contains("integrationName", "particleAllowDownlink", "particleAccessToken");

        var tokenEntry = result.formEntries().stream()
            .filter(e -> "particleAccessToken".equals(e.get("key"))).findFirst().orElseThrow();
        assertThat(tokenEntry.get("secretSupport")).isEqualTo(true);
        assertThat(tokenEntry.get("defaultValue")).isEqualTo("");
    }

    @Test
    void tokenize_kpn_replaces_grip_id_and_secrets() throws Exception {
        ObjectNode integrationJson = (ObjectNode) mapper.readTree("""
            {
              "name": "Acme KPN Sensor",
              "type": "KPN",
              "configuration": {
                "apiId": "api-id",
                "apiKey": "api-key",
                "destinationSharedSecret": "shared"
              }
            }
            """);

        IntegrationPackageExportService.TokenizationResult result =
            service.tokenize(IntegrationType.KPN, integrationJson);

        assertThat(result.formEntries()).extracting(e -> e.get("key"))
            .contains("integrationName", "kpnApiId", "kpnApiKey", "kpnDestinationSharedSecret");

        for (String secretKey : new String[]{"kpnApiId", "kpnApiKey", "kpnDestinationSharedSecret"}) {
            var entry = result.formEntries().stream()
                .filter(e -> secretKey.equals(e.get("key"))).findFirst().orElseThrow();
            assertThat(entry.get("secretSupport")).isEqualTo(true);
            assertThat(entry.get("defaultValue")).isEqualTo("");
        }
    }

    @Test
    void tokenize_pubsub_replaces_project_subscription_and_service_account_key() throws Exception {
        ObjectNode integrationJson = (ObjectNode) mapper.readTree("""
            {
              "name": "Acme PubSub Sensor",
              "type": "PUB_SUB",
              "configuration": {
                "projectId": "my-proj",
                "subscriptionId": "sub-1",
                "serviceAccountKey": "{json}"
              }
            }
            """);

        IntegrationPackageExportService.TokenizationResult result =
            service.tokenize(IntegrationType.PUB_SUB, integrationJson);

        JsonNode config = result.json().get("configuration");
        assertThat(config.get("projectId").asText()).isEqualTo("${pubsubProjectId}");
        assertThat(config.get("subscriptionId").asText()).isEqualTo("${pubsubSubscriptionId}");
        assertThat(config.get("serviceAccountKey").asText()).isEqualTo("${pubsubServiceAccountKey}");

        assertThat(result.formEntries()).extracting(e -> e.get("key"))
            .contains("integrationName", "pubsubProjectId", "pubsubSubscriptionId", "pubsubServiceAccountKey");

        var sakEntry = result.formEntries().stream()
            .filter(e -> "pubsubServiceAccountKey".equals(e.get("key"))).findFirst().orElseThrow();
        assertThat(sakEntry.get("secretSupport")).isEqualTo(true);
        assertThat(sakEntry.get("defaultValue")).isEqualTo("");

        var projEntry = result.formEntries().stream()
            .filter(e -> "pubsubProjectId".equals(e.get("key"))).findFirst().orElseThrow();
        assertThat(projEntry.get("defaultValue")).isEqualTo("my-proj");
    }

    @Test
    void tokenize_aws_sqs_walks_under_sqs_configuration_prefix() throws Exception {
        ObjectNode integrationJson = (ObjectNode) mapper.readTree("""
            {
              "name": "Acme SQS Sensor",
              "type": "AWS_SQS",
              "configuration": {
                "sqsConfiguration": {
                  "queueUrl": "https://sqs.amazonaws.com/123456789/my-queue",
                  "region": "us-east-1",
                  "accessKeyId": "AKIAIOSFODNN7EXAMPLE",
                  "secretAccessKey": "real-secret"
                }
              }
            }
            """);

        IntegrationPackageExportService.TokenizationResult result =
            service.tokenize(IntegrationType.AWS_SQS, integrationJson);

        assertThat(result.json().get("configuration").get("sqsConfiguration").get("queueUrl").asText())
            .isEqualTo("${sqsQueueUrl}");

        assertThat(result.formEntries()).extracting(e -> e.get("key"))
            .contains("integrationName", "sqsQueueUrl", "sqsRegion", "sqsAccessKeyId", "sqsSecretAccessKey");

        var keyIdEntry = result.formEntries().stream()
            .filter(e -> "sqsAccessKeyId".equals(e.get("key"))).findFirst().orElseThrow();
        assertThat(keyIdEntry.get("secretSupport")).isEqualTo(true);
        assertThat(keyIdEntry.get("defaultValue")).isEqualTo("");

        var secretEntry = result.formEntries().stream()
            .filter(e -> "sqsSecretAccessKey".equals(e.get("key"))).findFirst().orElseThrow();
        assertThat(secretEntry.get("secretSupport")).isEqualTo(true);
        assertThat(secretEntry.get("defaultValue")).isEqualTo("");

        var regionEntry = result.formEntries().stream()
            .filter(e -> "sqsRegion".equals(e.get("key"))).findFirst().orElseThrow();
        assertThat(regionEntry.get("defaultValue")).isEqualTo("us-east-1");
    }

    @Test
    void tokenize_aws_kinesis_replaces_stream_region_and_credentials() throws Exception {
        ObjectNode integrationJson = (ObjectNode) mapper.readTree("""
            {
              "name": "Acme Kinesis Sensor",
              "type": "AWS_KINESIS",
              "configuration": {
                "streamName": "events",
                "region": "us-east-1",
                "accessKeyId": "AKIAIOSFODNN7EXAMPLE",
                "secretAccessKey": "real-secret"
              }
            }
            """);

        IntegrationPackageExportService.TokenizationResult result =
            service.tokenize(IntegrationType.AWS_KINESIS, integrationJson);

        JsonNode config = result.json().get("configuration");
        assertThat(config.get("streamName").asText()).isEqualTo("${kinesisStreamName}");
        assertThat(config.get("region").asText()).isEqualTo("${kinesisRegion}");
        assertThat(config.get("accessKeyId").asText()).isEqualTo("${kinesisAccessKeyId}");
        assertThat(config.get("secretAccessKey").asText()).isEqualTo("${kinesisSecretAccessKey}");

        assertThat(result.formEntries()).extracting(e -> e.get("key"))
            .contains("integrationName", "kinesisStreamName", "kinesisRegion",
                      "kinesisAccessKeyId", "kinesisSecretAccessKey");

        var secretEntry = result.formEntries().stream()
            .filter(e -> "kinesisSecretAccessKey".equals(e.get("key"))).findFirst().orElseThrow();
        assertThat(secretEntry.get("secretSupport")).isEqualTo(true);
        assertThat(secretEntry.get("defaultValue")).isEqualTo("");
    }

    @Test
    void tokenize_azure_event_hub_replaces_connection_string_and_iot_hub_name() throws Exception {
        ObjectNode integrationJson = (ObjectNode) mapper.readTree("""
            {
              "name": "Acme Azure Event Hub Sensor",
              "type": "AZURE_EVENT_HUB",
              "configuration": {
                "connectionString": "Endpoint=sb://myhub.servicebus.windows.net/;SharedAccessKeyName=...",
                "iotHubName": "myhub",
                "consumerGroup": "$Default"
              }
            }
            """);

        IntegrationPackageExportService.TokenizationResult result =
            service.tokenize(IntegrationType.AZURE_EVENT_HUB, integrationJson);

        JsonNode config = result.json().get("configuration");
        assertThat(config.get("connectionString").asText()).isEqualTo("${azureEhConnectionString}");
        assertThat(config.get("iotHubName").asText()).isEqualTo("${azureEhIotHubName}");
        assertThat(config.get("consumerGroup").asText()).isEqualTo("${azureEhConsumerGroup}");

        assertThat(result.formEntries()).extracting(e -> e.get("key"))
            .contains("integrationName", "azureEhConnectionString", "azureEhIotHubName", "azureEhConsumerGroup");

        var connEntry = result.formEntries().stream()
            .filter(e -> "azureEhConnectionString".equals(e.get("key"))).findFirst().orElseThrow();
        assertThat(connEntry.get("secretSupport")).isEqualTo(true);
        assertThat(connEntry.get("defaultValue")).isEqualTo("");

        var hubEntry = result.formEntries().stream()
            .filter(e -> "azureEhIotHubName".equals(e.get("key"))).findFirst().orElseThrow();
        assertThat(hubEntry.get("defaultValue")).isEqualTo("myhub");
    }

    @Test
    void tokenize_azure_service_bus_replaces_connection_string_topic_and_subscription() throws Exception {
        ObjectNode integrationJson = (ObjectNode) mapper.readTree("""
            {
              "name": "Acme Azure Service Bus Sensor",
              "type": "AZURE_SERVICE_BUS",
              "configuration": {
                "connectionString": "Endpoint=sb://myns.servicebus.windows.net/;SharedAccessKeyName=...",
                "topicName": "telemetry",
                "subName": "tb-sub"
              }
            }
            """);

        IntegrationPackageExportService.TokenizationResult result =
            service.tokenize(IntegrationType.AZURE_SERVICE_BUS, integrationJson);

        JsonNode config = result.json().get("configuration");
        assertThat(config.get("connectionString").asText()).isEqualTo("${azureSbConnectionString}");
        assertThat(config.get("topicName").asText()).isEqualTo("${azureSbTopicName}");
        assertThat(config.get("subName").asText()).isEqualTo("${azureSbSubscriptionName}");

        assertThat(result.formEntries()).extracting(e -> e.get("key"))
            .contains("integrationName", "azureSbConnectionString", "azureSbTopicName", "azureSbSubscriptionName");

        var connEntry = result.formEntries().stream()
            .filter(e -> "azureSbConnectionString".equals(e.get("key"))).findFirst().orElseThrow();
        assertThat(connEntry.get("secretSupport")).isEqualTo(true);
        assertThat(connEntry.get("defaultValue")).isEqualTo("");

        var topicEntry = result.formEntries().stream()
            .filter(e -> "azureSbTopicName".equals(e.get("key"))).findFirst().orElseThrow();
        assertThat(topicEntry.get("defaultValue")).isEqualTo("telemetry");
    }

    @Test
    void tokenize_kafka_replaces_brokers_topics_group_and_client_id() throws Exception {
        ObjectNode integrationJson = (ObjectNode) mapper.readTree("""
            {
              "name": "Acme Kafka Sensor",
              "type": "KAFKA",
              "configuration": {
                "bootstrapServers": "kafka1:9092,kafka2:9092",
                "topics": "events",
                "groupId": "tb-group",
                "clientId": "tb-client"
              }
            }
            """);

        IntegrationPackageExportService.TokenizationResult result =
            service.tokenize(IntegrationType.KAFKA, integrationJson);

        JsonNode config = result.json().get("configuration");
        assertThat(config.get("bootstrapServers").asText()).isEqualTo("${kafkaBootstrapServers}");
        assertThat(config.get("topics").asText()).isEqualTo("${kafkaTopics}");
        assertThat(config.get("groupId").asText()).isEqualTo("${kafkaGroupId}");
        assertThat(config.get("clientId").asText()).isEqualTo("${kafkaClientId}");

        assertThat(result.formEntries()).extracting(e -> e.get("key"))
            .contains("integrationName", "kafkaBootstrapServers", "kafkaTopics", "kafkaGroupId", "kafkaClientId");

        var serversEntry = result.formEntries().stream()
            .filter(e -> "kafkaBootstrapServers".equals(e.get("key"))).findFirst().orElseThrow();
        assertThat(serversEntry.get("defaultValue")).isEqualTo("kafka1:9092,kafka2:9092");
    }

    @Test
    void tokenize_pulsar_replaces_service_url_topics_and_subscription() throws Exception {
        ObjectNode integrationJson = (ObjectNode) mapper.readTree("""
            {
              "name": "Acme Pulsar Sensor",
              "type": "APACHE_PULSAR",
              "configuration": {
                "serviceUrl": "pulsar://broker:6650",
                "topics": "events",
                "subscriptionName": "tb-sub"
              }
            }
            """);

        IntegrationPackageExportService.TokenizationResult result =
            service.tokenize(IntegrationType.APACHE_PULSAR, integrationJson);

        JsonNode config = result.json().get("configuration");
        assertThat(config.get("serviceUrl").asText()).isEqualTo("${pulsarServiceUrl}");
        assertThat(config.get("topics").asText()).isEqualTo("${pulsarTopics}");
        assertThat(config.get("subscriptionName").asText()).isEqualTo("${pulsarSubscriptionName}");

        assertThat(result.formEntries()).extracting(e -> e.get("key"))
            .contains("integrationName", "pulsarServiceUrl", "pulsarTopics", "pulsarSubscriptionName");

        var urlEntry = result.formEntries().stream()
            .filter(e -> "pulsarServiceUrl".equals(e.get("key"))).findFirst().orElseThrow();
        assertThat(urlEntry.get("defaultValue")).isEqualTo("pulsar://broker:6650");
    }

    @Test
    void tokenize_rabbitmq_replaces_connection_credentials_and_queues() throws Exception {
        ObjectNode integrationJson = (ObjectNode) mapper.readTree("""
            {
              "name": "Acme RabbitMQ Sensor",
              "type": "RABBITMQ",
              "configuration": {
                "host": "rabbit.example",
                "port": 5672,
                "username": "admin",
                "password": "real-pwd",
                "exchangeName": "tb",
                "queues": "uplink"
              }
            }
            """);

        IntegrationPackageExportService.TokenizationResult result =
            service.tokenize(IntegrationType.RABBITMQ, integrationJson);

        JsonNode config = result.json().get("configuration");
        assertThat(config.get("host").asText()).isEqualTo("${rabbitmqHost}");
        assertThat(config.get("port").asText()).isEqualTo("${rabbitmqPort}");
        assertThat(config.get("username").asText()).isEqualTo("${rabbitmqUsername}");
        assertThat(config.get("password").asText()).isEqualTo("${rabbitmqPassword}");
        assertThat(config.get("exchangeName").asText()).isEqualTo("${rabbitmqExchangeName}");
        assertThat(config.get("queues").asText()).isEqualTo("${rabbitmqQueues}");

        assertThat(result.formEntries()).extracting(e -> e.get("key"))
            .contains("integrationName", "rabbitmqHost", "rabbitmqPort", "rabbitmqUsername",
                      "rabbitmqPassword", "rabbitmqExchangeName", "rabbitmqQueues");

        for (String secretKey : new String[]{"rabbitmqUsername", "rabbitmqPassword"}) {
            var entry = result.formEntries().stream()
                .filter(e -> secretKey.equals(e.get("key"))).findFirst().orElseThrow();
            assertThat(entry.get("secretSupport")).isEqualTo(true);
            assertThat(entry.get("defaultValue")).isEqualTo("");
        }

        var hostEntry = result.formEntries().stream()
            .filter(e -> "rabbitmqHost".equals(e.get("key"))).findFirst().orElseThrow();
        assertThat(hostEntry.get("defaultValue")).isEqualTo("rabbit.example");
    }

    @Test
    void tokenize_coap_replaces_endpoint() throws Exception {
        ObjectNode integrationJson = (ObjectNode) mapper.readTree("""
            {
              "name": "Acme CoAP Sensor",
              "type": "COAP",
              "configuration": {
                "coapEndpoint": "coap://device:5683"
              }
            }
            """);

        IntegrationPackageExportService.TokenizationResult result =
            service.tokenize(IntegrationType.COAP, integrationJson);

        assertThat(result.json().get("configuration").get("coapEndpoint").asText())
            .isEqualTo("${coapEndpoint}");

        assertThat(result.formEntries()).extracting(e -> e.get("key"))
            .contains("integrationName", "coapEndpoint");

        var endpointEntry = result.formEntries().stream()
            .filter(e -> "coapEndpoint".equals(e.get("key"))).findFirst().orElseThrow();
        assertThat(endpointEntry.get("defaultValue")).isEqualTo("coap://device:5683");
    }

    @Test
    void tokenize_tuya_replaces_access_id_and_key() throws Exception {
        ObjectNode integrationJson = (ObjectNode) mapper.readTree("""
            {
              "name": "Acme Tuya Sensor",
              "type": "TUYA",
              "configuration": {
                "accessId": "tuya-id",
                "accessKey": "tuya-key"
              }
            }
            """);

        IntegrationPackageExportService.TokenizationResult result =
            service.tokenize(IntegrationType.TUYA, integrationJson);

        JsonNode config = result.json().get("configuration");
        assertThat(config.get("accessId").asText()).isEqualTo("${tuyaAccessId}");
        assertThat(config.get("accessKey").asText()).isEqualTo("${tuyaAccessKey}");

        assertThat(result.formEntries()).extracting(e -> e.get("key"))
            .contains("integrationName", "tuyaAccessId", "tuyaAccessKey");

        for (String secretKey : new String[]{"tuyaAccessId", "tuyaAccessKey"}) {
            var entry = result.formEntries().stream()
                .filter(e -> secretKey.equals(e.get("key"))).findFirst().orElseThrow();
            assertThat(entry.get("secretSupport")).isEqualTo(true);
            assertThat(entry.get("defaultValue")).isEqualTo("");
        }
    }

    @Test
    void tokenize_opcua_anonymous_identity_with_keystore() throws Exception {
        ObjectNode integrationJson = (ObjectNode) mapper.readTree("""
            {
              "name": "Acme OPC UA Sensor",
              "type": "OPC_UA",
              "configuration": {
                "host": "opc.local",
                "port": 4840,
                "security": "Basic256Sha256",
                "applicationName": "TB-Client",
                "applicationUri": "urn:thingsboard:client",
                "scanPeriodInSeconds": 30,
                "timeoutInMillis": 5000,
                "identity": { "type": "anonymous" },
                "keystore": {
                  "type": "PKCS12",
                  "location": "/etc/keys/store.p12",
                  "fileContent": "<base64-bytes>",
                  "password": "real-store-pwd",
                  "alias": "client-cert",
                  "keyPassword": "real-key-pwd"
                },
                "mapping": []
              }
            }
            """);

        IntegrationPackageExportService.TokenizationResult result =
            service.tokenize(IntegrationType.OPC_UA, integrationJson);

        JsonNode config = result.json().get("configuration");
        assertThat(config.get("host").asText()).isEqualTo("${opcuaHost}");
        assertThat(config.get("port").asText()).isEqualTo("${opcuaPort}");
        assertThat(config.get("security").asText()).isEqualTo("${opcuaSecurity}");

        JsonNode keystore = config.get("keystore");
        assertThat(keystore.get("type").asText()).isEqualTo("${opcuaKeystoreType}");
        assertThat(keystore.get("fileContent").asText()).isEqualTo("${opcuaKeystoreFileContent}");
        assertThat(keystore.get("password").asText()).isEqualTo("${opcuaKeystorePassword}");
        assertThat(keystore.get("alias").asText()).isEqualTo("${opcuaKeystoreAlias}");
        assertThat(keystore.get("keyPassword").asText()).isEqualTo("${opcuaKeystoreKeyPassword}");

        // location is not annotated — must be preserved as-is
        assertThat(keystore.get("location").asText()).isEqualTo("/etc/keys/store.p12");
        // mapping is not annotated — passes through as empty array
        assertThat(config.get("mapping").isArray()).isTrue();
        assertThat(config.get("mapping").size()).isEqualTo(0);

        assertThat(result.formEntries()).extracting(e -> e.get("key"))
            .contains("integrationName", "opcuaHost", "opcuaPort", "opcuaSecurity",
                      "opcuaApplicationName", "opcuaApplicationUri",
                      "opcuaScanPeriodInSeconds", "opcuaTimeoutInMillis",
                      "opcuaKeystoreType", "opcuaKeystoreFileContent",
                      "opcuaKeystorePassword", "opcuaKeystoreAlias", "opcuaKeystoreKeyPassword")
            .doesNotContain("opcuaUsername", "opcuaPassword");

        var keystorePasswordEntry = result.formEntries().stream()
            .filter(e -> "opcuaKeystorePassword".equals(e.get("key"))).findFirst().orElseThrow();
        assertThat(keystorePasswordEntry.get("secretSupport")).isEqualTo(true);
        assertThat(keystorePasswordEntry.get("defaultValue")).isEqualTo("");

        var keystoreFileEntry = result.formEntries().stream()
            .filter(e -> "opcuaKeystoreFileContent".equals(e.get("key"))).findFirst().orElseThrow();
        assertThat(keystoreFileEntry.get("secretSupport")).isEqualTo(true);
        assertThat(keystoreFileEntry.get("secretType")).isEqualTo("TEXT_FILE");

        var hostEntry = result.formEntries().stream()
            .filter(e -> "opcuaHost".equals(e.get("key"))).findFirst().orElseThrow();
        assertThat(hostEntry.get("defaultValue")).isEqualTo("opc.local");
    }

    @Test
    void tokenize_opcua_username_identity_no_keystore() throws Exception {
        ObjectNode integrationJson = (ObjectNode) mapper.readTree("""
            {
              "name": "Acme OPC UA Sensor",
              "type": "OPC_UA",
              "configuration": {
                "host": "opc.local",
                "port": 4840,
                "security": "None",
                "identity": { "type": "username", "username": "admin", "password": "real-pwd" },
                "mapping": []
              }
            }
            """);

        IntegrationPackageExportService.TokenizationResult result =
            service.tokenize(IntegrationType.OPC_UA, integrationJson);

        JsonNode identity = result.json().get("configuration").get("identity");
        assertThat(identity.get("username").asText()).isEqualTo("${opcuaUsername}");
        assertThat(identity.get("password").asText()).isEqualTo("${opcuaPassword}");

        assertThat(result.formEntries()).extracting(e -> e.get("key"))
            .contains("integrationName", "opcuaHost", "opcuaPort", "opcuaSecurity",
                      "opcuaUsername", "opcuaPassword")
            .doesNotContain("opcuaKeystoreType", "opcuaKeystoreFileContent",
                            "opcuaKeystorePassword", "opcuaKeystoreAlias", "opcuaKeystoreKeyPassword");

        for (String secretKey : new String[]{"opcuaUsername", "opcuaPassword"}) {
            var entry = result.formEntries().stream()
                .filter(e -> secretKey.equals(e.get("key"))).findFirst().orElseThrow();
            assertThat(entry.get("secretSupport")).isEqualTo(true);
        }
    }

    @Test
    void tokenize_opcua_security_options_emit_value_label_pairs() throws Exception {
        ObjectNode integrationJson = (ObjectNode) mapper.readTree("""
            {
              "name": "Acme OPC UA Sensor",
              "type": "OPC_UA",
              "configuration": {
                "host": "opc.local",
                "port": 4840,
                "security": "None",
                "identity": { "type": "username", "username": "admin", "password": "real-pwd" },
                "mapping": []
              }
            }
            """);

        IntegrationPackageExportService.TokenizationResult result =
            service.tokenize(IntegrationType.OPC_UA, integrationJson);

        var securityEntry = result.formEntries().stream()
            .filter(e -> "opcuaSecurity".equals(e.get("key"))).findFirst().orElseThrow();
        assertThat(securityEntry.get("type")).isEqualTo("STRING_AUTOCOMPLETE");

        @SuppressWarnings("unchecked")
        List<Map<String, String>> options = (List<Map<String, String>>) securityEntry.get("options");
        assertThat(options).hasSize(6);
        assertThat(options.get(0).get("value")).isEqualTo("None");
        assertThat(options.get(0).get("label")).isEqualTo("None");
    }

    @Test
    void tokenize_chirpstack_strips_source_host_and_uuid_from_clientConfiguration_endpoint() throws Exception {
        ObjectNode integrationJson = (ObjectNode) mapper.readTree("""
            {
              "name": "Acme CHIRPSTACK",
              "type": "CHIRPSTACK",
              "configuration": {
                "metadata": {},
                "clientConfiguration": {
                  "baseUrl": "https://thingsboard.cloud",
                  "httpEndpoint": "https://thingsboard.cloud/api/v1/integrations/chirpstack/11bd221c-3134-391e-e214-45f9a58a1694",
                  "applicationServerUrl": "https://chirpstack.example.com",
                  "applicationServerAPIToken": "real-api-token",
                  "useAPI4Plus": true
                }
              }
            }
            """);

        IntegrationPackageExportService.TokenizationResult result =
            service.tokenize(IntegrationType.CHIRPSTACK, integrationJson);

        JsonNode client = result.json().get("configuration").get("clientConfiguration");
        assertThat(client.get("baseUrl").asText()).isEqualTo("${baseUrl}");
        assertThat(client.get("httpEndpoint").asText())
            .isEqualTo("${baseUrl}/api/v1/integrations/chirpstack/${routingKey}");
        // Annotated fields still get tokenized.
        assertThat(client.get("applicationServerUrl").asText()).isEqualTo("${chirpstackApplicationServerUrl}");
        assertThat(client.get("applicationServerAPIToken").asText()).isEqualTo("${chirpstackApplicationServerApiToken}");
    }

    @Test
    void tokenize_loriot_rewrites_top_level_baseUrl_and_httpEndpoint() throws Exception {
        ObjectNode integrationJson = (ObjectNode) mapper.readTree("""
            {
              "name": "Acme LORIOT",
              "type": "LORIOT",
              "configuration": {
                "metadata": {},
                "baseUrl": "http://localhost:8081",
                "httpEndpoint": "http://localhost:8081/api/v1/integrations/loriot/abb0028e-26ac-aa0e-2901-eca3d94d53d7",
                "server": "eu1",
                "appId": "ACME-1",
                "token": "tok"
              }
            }
            """);

        IntegrationPackageExportService.TokenizationResult result =
            service.tokenize(IntegrationType.LORIOT, integrationJson);

        JsonNode cfg = result.json().get("configuration");
        assertThat(cfg.get("baseUrl").asText()).isEqualTo("${baseUrl}");
        assertThat(cfg.get("httpEndpoint").asText())
            .isEqualTo("${baseUrl}/api/v1/integrations/loriot/${routingKey}");
        // Annotated fields still get tokenized.
        assertThat(cfg.get("server").asText()).isEqualTo("${loriotServer}");
        assertThat(cfg.get("token").asText()).isEqualTo("${loriotToken}");
    }
}

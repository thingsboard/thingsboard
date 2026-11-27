// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.RandomStringUtils;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EventInfo;
import org.thingsboard.server.common.data.SecretType;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.SecretId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.rule.NodeConnectionInfo;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.secret.Secret;
import org.thingsboard.server.msa.AbstractContainerTest;
import org.thingsboard.server.msa.DisableUIListeners;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.thingsboard.server.msa.prototypes.ConverterPrototypes.downlinkConverterPrototype;
import static org.thingsboard.server.msa.prototypes.ConverterPrototypes.uplinkConverterPrototype;
import static org.thingsboard.server.msa.prototypes.DevicePrototypes.defaultDevicePrototype;

@DisableUIListeners
public abstract class AbstractIntegrationTest extends AbstractContainerTest {

    public static final String LOGIN = "tenant@thingsboard.org";
    public static final String PASSWORD = "tenant";
    protected static final String DOWNLINK_DEBUG_EVENT_TYPE = "Downlink";
    protected Device device;
    private RuleChainId defaultRuleChainId;
    protected Integration integration;
    protected Converter uplinkConverter;
    protected Secret secret;

    protected final String JSON_CONVERTER_CONFIG = """
            {
                "isDevice": true,
                "name": "DEVICE_NAME",
                "profile": "default",
                "customer": null,
                "group": null,
                "attributes": [
                    "eui",
                    "fPort",
                    "rssi"
                ],
                "telemetry": [
                    "data",
                    "snr"
                ],
                "scriptLang": "TBEL",
                "decoder": "",
                "tbelDecoder": "var payloadStr = decodeToString(payload);\\nvar result = {\\n    attributes: {},\\n    telemetry: {\\n        ts: metadata.ts,\\n        values: {\\n            temperature: payload[0],\\n            humidity: payload[1]\\n        }\\n    }\\n};\\n\\nreturn result;",
                "encoder": null,
                "tbelEncoder": null,
                "updateOnlyKeys": [
                    "fPort",
                    "eui",
                    "rssi"
                ]
            }
            """;

    abstract protected String getDevicePrototypeSufix();

    @BeforeClass
    public void beforeIntegrationTestClass() {
        testRestClient.login(LOGIN, PASSWORD);
    }

    @BeforeMethod
    public void beforeIntegrationTest() {
        defaultRuleChainId = getDefaultRuleChainId();
        device = testRestClient.postDevice("", defaultDevicePrototype(getDevicePrototypeSufix()));
    }

    @AfterMethod
    public void afterIntegrationTest() {
        testRestClient.setRootRuleChain(defaultRuleChainId);
        if (device != null) {
            testRestClient.deleteDevice(device.getId());
            if (integration != null) {
                if (integration.getId() != null) {
                    testRestClient.deleteIntegration(integration.getId());
                }
                testRestClient.deleteConverter(integration.getDefaultConverterId());
                if (integration.getDownlinkConverterId() != null) {
                    testRestClient.deleteConverter(integration.getDownlinkConverterId());
                }
                integration = null;
            }
            if (secret != null) {
                testRestClient.deleteSecret(secret.getId());
                secret = null;
            }
        }
    }

    protected Integration createIntegration(IntegrationType type, String config, JsonNode uplinkConfig,
                                            String routingKey, String secretKey, boolean isRemote) {
        return createIntegration(type, JacksonUtil.toJsonNode(config), uplinkConfig, routingKey, secretKey, isRemote);
    }

    protected Integration createIntegration(IntegrationType type, JsonNode config, JsonNode uplinkConfig,
                                            String routingKey, String secretKey, boolean isRemote) {
        return createIntegration(type, config, uplinkConfig, null, routingKey, secretKey, isRemote);
    }

    protected Integration createIntegration(IntegrationType type, JsonNode config, JsonNode uplinkConfig, JsonNode downlinkConfig,
                                            String routingKey, String secretKey, boolean isRemote) {
        return createIntegration(type, config, uplinkConfig, downlinkConfig, routingKey, secretKey, isRemote, 1);
    }

    protected Integration createIntegration(IntegrationType type, JsonNode config, JsonNode uplinkConfig, JsonNode downlinkConfig,
                                            String routingKey, String secretKey, boolean isRemote, int converterVersion) {
        Integration integration = new Integration();
        integration.setConfiguration(config);

        uplinkConverter = testRestClient.postConverter(uplinkConverterPrototype(uplinkConfig, type, converterVersion));
        integration.setDefaultConverterId(uplinkConverter.getId());

        if (downlinkConfig != null) {
            integration.setDownlinkConverterId(testRestClient.postConverter(downlinkConverterPrototype(downlinkConfig)).getId());
        }

        integration.setName(type.name().toLowerCase() + "_" + RandomStringUtils.randomAlphanumeric(7));
        integration.setType(type);
        integration.setRoutingKey(routingKey);
        integration.setSecret(secretKey);
        integration.setEnabled(true);
        integration.setRemote(isRemote);
        integration.setDebugSettings(DebugSettings.all());
        integration.setAllowCreateDevicesOrAssets(true);

        integration = testRestClient.postIntegration(integration);

        IntegrationId integrationId = integration.getId();
        TenantId tenantId = integration.getTenantId();

        waitUntilIntegrationStarted(integrationId, tenantId);
        return this.integration = integration;
    }

    protected void waitUntilIntegrationStarted(IntegrationId integrationId, TenantId tenantId) {
        Awaitility
                .await()
                .alias("Get integration events")
                .atMost(20, TimeUnit.SECONDS)
                .until(() -> {
                    PageData<EventInfo> events = testRestClient.getEvents(integrationId, EventType.LC_EVENT, tenantId, new TimePageLink(1024));
                    if (events.getData().isEmpty()) {
                        return false;
                    }

                    EventInfo event = events.getData().stream().max(Comparator.comparingLong(EventInfo::getCreatedTime)).orElse(null);
                    return event != null
                            && "STARTED".equals(event.getBody().get("event").asText())
                            && "true".equals(event.getBody().get("success").asText());
                });
    }

    protected void waitForIntegrationEvent(Integration integration, String eventType, int count) {
        if (containerTestSuite.isActive() && !integration.getType().isSingleton() && !integration.isRemote()) {
            count = count * 2;
        }
        int finalCount = count;
        Awaitility
                .await()
                .alias("Get integration events")
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> {
                    PageData<EventInfo> events = testRestClient.getEvents(integration.getId(), EventType.LC_EVENT, integration.getTenantId(), new TimePageLink(1024));
                    if (events.getData().isEmpty()) {
                        return false;
                    }

                    List<EventInfo> eventInfos = events.getData().stream().filter(eventInfo ->
                            eventType.equals(eventInfo.getBody().get("event").asText()) &&
                                    "true".equals(eventInfo.getBody().get("success").asText())).toList();

                    return eventInfos.size() == finalCount;
                });
    }

    protected void waitForConverterDebugEvent(Converter converter, String eventType, int count) {
        int finalCount = count;
        Awaitility
                .await()
                .alias("Get converter events")
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> {
                    PageData<EventInfo> events = testRestClient.getEvents(converter.getId(), EventType.DEBUG_CONVERTER, converter.getTenantId(), new TimePageLink(finalCount));
                    if (events.getData().isEmpty()) {
                        return false;
                    }

                    List<EventInfo> eventInfos = events.getData().stream().filter(eventInfo ->
                            eventType.equalsIgnoreCase(eventInfo.getBody().get("type").asText())).toList();

                    return eventInfos.size() == finalCount;
                });
    }

    protected RuleChainId getDefaultRuleChainId() {
        PageData<RuleChain> ruleChains = testRestClient.getRuleChains(new PageLink(40, 0));
        Optional<RuleChain> defaultRuleChain = ruleChains.getData()
                .stream()
                .filter(RuleChain::isRoot)
                .findFirst();
        if (!defaultRuleChain.isPresent()) {
            Assert.fail("Root rule chain wasn't found");
        }
        return defaultRuleChain.get().getId();
    }

    protected RuleChainId createRootRuleChainWithIntegrationDownlinkNode(IntegrationId integrationId) throws Exception {
        RuleChain newRuleChain = new RuleChain();
        newRuleChain.setName("testRuleChain");
        RuleChain ruleChain = testRestClient.saveRuleChain(newRuleChain);

        JsonNode configuration = JacksonUtil.OBJECT_MAPPER.readTree(this.getClass().getClassLoader().getResourceAsStream("DownlinkRuleChainMetadata.json"));
        RuleChainMetaData ruleChainMetaData = new RuleChainMetaData();
        ruleChainMetaData.setRuleChainId(ruleChain.getId());
        ruleChainMetaData.setFirstNodeIndex(configuration.get("firstNodeIndex").asInt());
        ruleChainMetaData.setNodes(Arrays.asList(JacksonUtil.OBJECT_MAPPER.treeToValue(configuration.get("nodes"), RuleNode[].class)));
        RuleNode integrationNode = ruleChainMetaData.getNodes().stream().filter(ruleNode -> ruleNode.getType().equals("org.thingsboard.rule.engine.integration.TbIntegrationDownlinkNode")).findFirst().get();
        integrationNode.setConfiguration(JacksonUtil.newObjectNode().put("integrationId", integrationId.toString()));
        ruleChainMetaData.setConnections(Arrays.asList(JacksonUtil.OBJECT_MAPPER.treeToValue(configuration.get("connections"), NodeConnectionInfo[].class)));

        testRestClient.postRuleChainMetadata(ruleChainMetaData);

        // make rule chain root
        testRestClient.setRootRuleChain(ruleChain.getId());

        Awaitility
                .await()
                .alias("Get events from rule chain")
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> {
                    PageData<EventInfo> events = testRestClient.getEvents(ruleChain.getId(), EventType.LC_EVENT, ruleChain.getTenantId(), new TimePageLink(1024));
                    List<EventInfo> eventInfos = events.getData().stream().filter(eventInfo ->
                            "UPDATED".equals(eventInfo.getBody().get("event").asText()) &&
                                    "true".equals(eventInfo.getBody().get("success").asText())).toList();

                    return eventInfos.size() == 4;
                });

        return ruleChain.getId();
    }

    protected void waitTillRuleNodeReceiveMsg(EntityId entityId, EventType eventType, TenantId tenantId, String msgType) {
        Awaitility
                .await()
                .alias("Get events from rule node")
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> {
                    PageData<EventInfo> events = testRestClient.getEvents(entityId, eventType, tenantId, new TimePageLink(1024));
                    if (events.getData().isEmpty()) {
                        return false;
                    }

                    EventInfo event = events.getData().stream().max(Comparator.comparingLong(EventInfo::getCreatedTime)).orElse(null);
                    return event != null
                            && msgType.equals(event.getBody().get("msgType").asText());
                });
    }

    /**
     * Waits until the integration reports that it processed a downlink. The integration persists a
     * {@code Downlink} debug event after it ran the downlink converter and handed the result to its
     * transport, so this separates "the downlink never reached the integration" from "the integration
     * sent it but the external side did not receive it". Requires debug mode on the integration.
     */
    protected EventInfo waitForIntegrationDownlinkDebugEvent(Integration integration) {
        AtomicReference<List<EventInfo>> lastEvents = new AtomicReference<>(List.of());
        try {
            return Awaitility
                    .await()
                    .alias("Get integration downlink debug event")
                    .pollInterval(500, TimeUnit.MILLISECONDS)
                    .atMost(TIMEOUT, TimeUnit.SECONDS)
                    .until(() -> {
                        List<EventInfo> events = testRestClient.getEvents(integration.getId(), EventType.DEBUG_INTEGRATION,
                                integration.getTenantId(), new TimePageLink(1024)).getData();
                        lastEvents.set(events);
                        return events.stream()
                                .filter(event -> DOWNLINK_DEBUG_EVENT_TYPE.equals(bodyText(event, "type")))
                                .findFirst();
                    }, Optional::isPresent)
                    .get();
        } catch (ConditionTimeoutException e) {
            throw new AssertionError("Integration " + integration.getId() + " did not report a '" + DOWNLINK_DEBUG_EVENT_TYPE
                    + "' debug event within " + TIMEOUT + " seconds, so the downlink did not reach the integration's transport. "
                    + "Integration debug events: " + describeDebugEvents(lastEvents.get()), e);
        }
    }

    protected static String describeDebugEvents(List<EventInfo> events) {
        if (events == null || events.isEmpty()) {
            return "[]";
        }
        return events.stream()
                .sorted(Comparator.comparingLong(EventInfo::getCreatedTime))
                .map(event -> "{type=" + bodyText(event, "type") + ", status=" + bodyText(event, "status")
                        + ", error=" + bodyText(event, "error") + ", message=" + bodyText(event, "message") + "}")
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private static String bodyText(EventInfo event, String field) {
        JsonNode body = event.getBody();
        return body != null && body.hasNonNull(field) ? body.get(field).asText() : null;
    }

    protected Secret createSecret(SecretId secretId, String name, String value) {
        Secret secret = new Secret();
        secret.setName(name);
        secret.setValue(value);
        secret.setType(SecretType.TEXT);
        secret.setId(secretId);
        this.secret = testRestClient.saveSecret(secret);
        return this.secret;
    }

    protected String toSecretPlaceholder(String name, SecretType type) {
        return String.format("${secret:%s;type:%s}", name, type);
    }

}

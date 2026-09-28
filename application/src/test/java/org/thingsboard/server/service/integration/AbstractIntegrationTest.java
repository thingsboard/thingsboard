// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EventInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.event.EventFilter;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.event.LifeCycleEventFilter;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationInfo;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.controller.AbstractControllerTest;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public abstract class AbstractIntegrationTest extends AbstractControllerTest {

    @Autowired
    protected TbIntegrationDownlinkService downlinkService;

    protected Converter uplinkConverter;
    protected Converter downlinkConverter;
    protected Integration integration;

    protected void createConverter(String converterName, ConverterType type, JsonNode converterConfig) {
        Converter newConverter = new Converter();
        newConverter.setTenantId(tenantId);
        newConverter.setName(converterName);
        newConverter.setType(type);
        newConverter.setConfiguration(converterConfig);
        newConverter.setDebugSettings(DebugSettings.all());
        switch (type) {
            case UPLINK:
                uplinkConverter = doPost("/api/converter", newConverter, Converter.class);
                Assert.assertNotNull(uplinkConverter);
                break;
            case DOWNLINK:
                downlinkConverter = doPost("/api/converter", newConverter, Converter.class);
                Assert.assertNotNull(downlinkConverter);
                break;
        }
    }

    protected void createIntegration(String integrationName, IntegrationType type) throws InterruptedException {
        Integration newIntegration = new Integration();
        newIntegration.setTenantId(tenantId);
        newIntegration.setDefaultConverterId(uplinkConverter.getId());
        if (downlinkConverter != null) {
            newIntegration.setDownlinkConverterId(downlinkConverter.getId());
        }
        newIntegration.setName(integrationName);
        newIntegration.setRoutingKey(StringUtils.randomAlphanumeric(15));
        newIntegration.setType(type);
        JsonNode clientConfig = createIntegrationClientConfiguration();
        ObjectNode integrationConfiguration = JacksonUtil.newObjectNode();
        integrationConfiguration.set("clientConfiguration", clientConfig);
        integrationConfiguration.set("metadata", JacksonUtil.newObjectNode());
        newIntegration.setConfiguration(integrationConfiguration);
        newIntegration.setDebugSettings(DebugSettings.all());
        newIntegration.setEnabled(false);
        newIntegration.setAllowCreateDevicesOrAssets(true);
        integration = doPost("/api/integration", newIntegration, Integration.class);
        Assert.assertNotNull(integration);
    }

    public PageData<IntegrationInfo> getIntegrationInfos(PageLink pageLink) throws Exception {
        return doGetTypedWithPageLink("/api/integrationInfos?", new TypeReference<>() {}, pageLink);
    }

    public void enableIntegration() {
        if (!integration.isEnabled()) {
            integration.setEnabled(true);
            integration = doPost("/api/integration", integration, Integration.class);
        }
        Assert.assertNotNull(integration);
    }

    public void disableIntegration() {
        if (integration.isEnabled()) {
            integration.setEnabled(false);
            integration = doPost("/api/integration", integration, Integration.class);
        }
        Assert.assertNotNull(integration);
    }

    public void removeIntegration(Integration integration) throws Exception {
        doDelete("/api/integration/" + integration.getId().getId().toString()).andExpect(status().isOk());
    }

    protected abstract JsonNode createIntegrationClientConfiguration();

    public List<EventInfo> getIntegrationDebugMessages(long startTs, String expectedMessageType, IntegrationDebugMessageStatus expectedStatus, long timeout) throws Exception {
        return getIntegrationDebugMessages(startTs, expectedMessageType, expectedStatus.name(), timeout);
    }

    public List<EventInfo> getIntegrationDebugMessages(long startTs, String expectedMessageType, String expectedStatus, long timeout) throws Exception {
        return getEntityDebugMessages(integration.getTenantId(), integration.getId(), startTs, expectedMessageType, expectedStatus, timeout);
    }

    public List<EventInfo> getConverterDebugMessages(long startTs, String expectedMessageType, IntegrationDebugMessageStatus expectedStatus, long timeout) throws Exception {
        return getConverterDebugMessages(startTs, expectedMessageType, expectedStatus.name(), timeout);
    }

    public List<EventInfo> getConverterDebugMessages(long startTs, String expectedMessageType, String expectedStatus, long timeout) throws Exception {
        return getEntityDebugMessages(integration.getTenantId(), integration.getDefaultConverterId(), startTs, expectedMessageType, expectedStatus, timeout);
    }

    private List<EventInfo> getEntityDebugMessages(TenantId tenantId, EntityId entityId, long startTs, String expectedMessageType, String expectedStatus, long timeout) throws Exception {
        long endTs = startTs + timeout * 1000;
        List<EventInfo> targetMsgs;
        List<EventInfo> allMsgs;

        do {
            SortOrder sortOrder = new SortOrder("createdTime", SortOrder.Direction.DESC);
            TimePageLink pageLink = new TimePageLink(100, 0, null, sortOrder, startTs, endTs);
            EntityType entityType = entityId.getEntityType();
            PageData<EventInfo> events = doGetTypedWithTimePageLink(
                    String.format("/api/events/%s/%s/DEBUG_%s?tenantId=%s&", entityType, entityId, entityType, tenantId),
                    new TypeReference<>() {
                    },
                    pageLink);
            allMsgs = events.getData();
            targetMsgs = events.getData().stream().filter(event -> expectedMessageType.equals(event.getBody().get("type").asText())
                    && (IntegrationDebugMessageStatus.ANY.name().equals(expectedStatus)
                    || expectedStatus.equalsIgnoreCase(event.getBody().get("status").asText()))).collect(Collectors.toList());
            if (!targetMsgs.isEmpty()) {
                break;
            }
            Thread.sleep(100);
        }
        while (System.currentTimeMillis() <= endTs);
        if (allMsgs == null || allMsgs.isEmpty()) {
            log.error("[{} - {}] ALL DEBUG EVENTS ARE EMPTY.", startTs, endTs);
        } else {
            log.error("[{} - {}] THERE ARE {} DEBUG EVENTS ", startTs, endTs, allMsgs.size());
            allMsgs.forEach(event -> log.error("DEBUG EVENT: {}", event));
        }
        return targetMsgs;
    }

    protected void waitUntilIntegrationStarted(TenantId tenantId, IntegrationId integrationId) {
        Awaitility
                .await()
                .alias("Get integration events")
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> {
                    PageData<EventInfo> events = getEvents(tenantId, integrationId);
                    if (events.getData().isEmpty()) {
                        return false;
                    }

                    EventInfo event = events.getData().stream().max(Comparator.comparingLong(EventInfo::getCreatedTime)).orElse(null);
                    return event != null
                            && "STARTED".equals(event.getBody().get("event").asText())
                            && "true".equals(event.getBody().get("success").asText());
                });
    }

    protected PageData<EventInfo> getEvents(TenantId tenantId, IntegrationId integrationId) throws Exception {
        return doGetTyped("/api/events/{entityType}/{entityId}/{eventType}?tenantId={tenantId}&pageSize={pageSize}&page={page}",
                new TypeReference<>() {}, EntityType.INTEGRATION, integrationId.toString(), EventType.LC_EVENT, tenantId.toString(), 1024, 0);
    }

    protected void cleanUpEvents(EntityId entityId, EventFilter eventFilter) throws Exception {
        doPost("/api/events/" + entityId.getEntityType().name() + "/" + entityId.getId() + "/clear", eventFilter);
    }

}

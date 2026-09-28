// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.client.ApiException;
import org.thingsboard.client.api.ThingsboardApi.AssignIntegrationToEdgeArgs;
import org.thingsboard.client.api.ThingsboardApi.CheckIntegrationConnectionArgs;
import org.thingsboard.client.api.ThingsboardApi.DeleteConverterArgs;
import org.thingsboard.client.api.ThingsboardApi.DeleteEdgeArgs;
import org.thingsboard.client.api.ThingsboardApi.DeleteIntegrationArgs;
import org.thingsboard.client.api.ThingsboardApi.GetEdgeIntegrationInfosArgs;
import org.thingsboard.client.api.ThingsboardApi.GetEdgeIntegrationsArgs;
import org.thingsboard.client.api.ThingsboardApi.GetIntegrationByIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetIntegrationByRoutingKeyArgs;
import org.thingsboard.client.api.ThingsboardApi.GetIntegrationInfosArgs;
import org.thingsboard.client.api.ThingsboardApi.GetIntegrationsArgs;
import org.thingsboard.client.api.ThingsboardApi.GetIntegrationsByIdsArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveConverterArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveEdgeArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveIntegrationArgs;
import org.thingsboard.client.api.ThingsboardApi.UnassignIntegrationFromEdgeArgs;
import org.thingsboard.client.model.Converter;
import org.thingsboard.client.model.ConverterId;
import org.thingsboard.client.model.ConverterType;
import org.thingsboard.client.model.Edge;
import org.thingsboard.client.model.EntityType;
import org.thingsboard.client.model.Integration;
import org.thingsboard.client.model.IntegrationConvertersInfo;
import org.thingsboard.client.model.IntegrationType;
import org.thingsboard.client.model.PageDataIntegration;
import org.thingsboard.client.model.PageDataIntegrationInfo;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@DaoSqlTest
@TestPropertySource(properties = {
        "service.integrations.supported=ALL"
})
public class IntegrationApiClientTest extends AbstractApiClientTest {

    private static final JsonNode EMPTY_CONFIG = OBJECT_MAPPER.createObjectNode();

    @Test
    public void testIntegrationLifecycle() throws Exception {
        long ts = System.currentTimeMillis();

        Converter converter = createConverter(TEST_PREFIX + ts + "_conv");
        String converterId = converter.getId().getId().toString();

        String routingKey = "rk_" + ts;

        Integration saved = client.saveIntegration(SaveIntegrationArgs.builder()
                .integration(buildIntegration(TEST_PREFIX + ts, routingKey, converterId))
                .build());
        assertNotNull(saved);
        assertNotNull(saved.getId());
        assertEquals(TEST_PREFIX + ts, saved.getName());
        assertEquals(IntegrationType.HTTP, saved.getType());
        assertEquals(routingKey, saved.getRoutingKey());
        String integrationId = saved.getId().getId().toString();

        Integration fetched = client.getIntegrationById(GetIntegrationByIdArgs.builder()
                .integrationId(integrationId)
                .build());
        assertNotNull(fetched);
        assertEquals(integrationId, fetched.getId().getId().toString());
        assertEquals(TEST_PREFIX + ts, fetched.getName());

        Integration byRoutingKey = client.getIntegrationByRoutingKey(GetIntegrationByRoutingKeyArgs.builder()
                .routingKey(routingKey)
                .build());
        assertNotNull(byRoutingKey);
        assertEquals(integrationId, byRoutingKey.getId().getId().toString());

        fetched.setName(TEST_PREFIX + ts + "_updated");
        Integration updated = client.saveIntegration(SaveIntegrationArgs.builder()
                .integration(fetched)
                .build());
        assertEquals(TEST_PREFIX + ts + "_updated", updated.getName());

        client.deleteIntegration(DeleteIntegrationArgs.builder()
                .integrationId(integrationId)
                .build());
        assertReturns404(() -> client.getIntegrationById(GetIntegrationByIdArgs.builder()
                .integrationId(integrationId)
                .build()));

        client.deleteConverter(DeleteConverterArgs.builder()
                .converterId(converterId)
                .build());
    }

    @Test
    public void testGetIntegrations() throws Exception {
        long ts = System.currentTimeMillis();

        Converter converter = createConverter(TEST_PREFIX + ts + "_conv");
        String converterId = converter.getId().getId().toString();

        List<String> createdIds = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Integration saved = client.saveIntegration(SaveIntegrationArgs.builder()
                    .integration(buildIntegration(TEST_PREFIX + ts + "_" + i, "rk_" + ts + "_" + i, converterId))
                    .build());
            createdIds.add(saved.getId().getId().toString());
        }

        PageDataIntegration page = client.getIntegrations(GetIntegrationsArgs.builder()
                .pageSize("100")
                .page("0")
                .textSearch(TEST_PREFIX + ts)
                .build());
        assertNotNull(page);
        assertTrue(page.getTotalElements() >= 3);

        PageDataIntegrationInfo infoPage = client.getIntegrationInfos(GetIntegrationInfosArgs.builder()
                .pageSize("100")
                .page("0")
                .textSearch(TEST_PREFIX + ts)
                .build());
        assertNotNull(infoPage);
        assertTrue(infoPage.getTotalElements() >= 3);

        for (String id : createdIds) client.deleteIntegration(DeleteIntegrationArgs.builder()
                .integrationId(id)
                .build());
        client.deleteConverter(DeleteConverterArgs.builder()
                .converterId(converterId)
                .build());
    }

    @Test
    public void testGetIntegrationsByIds() throws Exception {
        long ts = System.currentTimeMillis();

        Converter converter = createConverter(TEST_PREFIX + ts + "_conv");
        String converterId = converter.getId().getId().toString();

        Integration i1 = client.saveIntegration(SaveIntegrationArgs.builder()
                .integration(buildIntegration(TEST_PREFIX + ts + "_a", "rk_" + ts + "_a", converterId))
                .build());
        Integration i2 = client.saveIntegration(SaveIntegrationArgs.builder()
                .integration(buildIntegration(TEST_PREFIX + ts + "_b", "rk_" + ts + "_b", converterId))
                .build());
        String id1 = i1.getId().getId().toString();
        String id2 = i2.getId().getId().toString();

        List<Integration> result = client.getIntegrationsByIds(GetIntegrationsByIdsArgs.builder()
                .integrationIds(List.of(id1, id2))
                .build());
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(i -> i.getId().getId().toString().equals(id1)));
        assertTrue(result.stream().anyMatch(i -> i.getId().getId().toString().equals(id2)));

        client.deleteIntegration(DeleteIntegrationArgs.builder()
                .integrationId(id1)
                .build());
        client.deleteIntegration(DeleteIntegrationArgs.builder()
                .integrationId(id2)
                .build());
        client.deleteConverter(DeleteConverterArgs.builder()
                .converterId(converterId)
                .build());
    }

    @Test
    public void testCheckIntegrationConnection() throws Exception {
        long ts = System.currentTimeMillis();

        Converter converter = createConverter(TEST_PREFIX + ts + "_conv");
        String converterId = converter.getId().getId().toString();

        Integration saved = client.saveIntegration(SaveIntegrationArgs.builder()
                .integration(buildIntegration(TEST_PREFIX + ts, "rk_" + ts, converterId))
                .build());
        String integrationId = saved.getId().getId().toString();

        try {
            client.checkIntegrationConnection(CheckIntegrationConnectionArgs.builder()
                    .integration(saved)
                    .build());
        } catch (ApiException e) {
            assertTrue("Unexpected client error from checkIntegrationConnection: HTTP " + e.getCode(),
                    e.getCode() >= 500);
        }

        client.deleteIntegration(DeleteIntegrationArgs.builder()
                .integrationId(integrationId)
                .build());
        client.deleteConverter(DeleteConverterArgs.builder()
                .converterId(converterId)
                .build());
    }

    @Test
    public void testGetIntegrationsConvertersInfo() throws Exception {
        Map<String, IntegrationConvertersInfo> info = client.getIntegrationsConvertersInfo();
        assertNotNull(info);
        assertTrue("Expected 'HTTP' key in integrations converters info map",
                info.containsKey("HTTP"));
    }

    @Test
    public void testEdgeIntegrationMethods() throws Exception {
        long ts = System.currentTimeMillis();

        Converter converter = new Converter();
        converter.setName(TEST_PREFIX + ts + "_conv");
        converter.setType(ConverterType.UPLINK);
        converter.setConfiguration(ConverterApiClientTest.TEST_DECODER);
        converter.setEdgeTemplate(true);
        converter = client.saveConverter(SaveConverterArgs.builder()
                .converter(converter)
                .build());
        String converterId = converter.getId().getId().toString();

        Integration integration1 = buildIntegration(TEST_PREFIX + ts, "rk_" + ts, converterId);
        integration1.setEdgeTemplate(true);
        Integration integration = client.saveIntegration(SaveIntegrationArgs.builder()
                .integration(integration1)
                .build());
        String integrationId = integration.getId().getId().toString();

        Edge edge = createEdge(TEST_PREFIX + ts + "_edge", String.valueOf(ts));
        String edgeId = edge.getId().getId().toString();

        Integration assigned = client.assignIntegrationToEdge(AssignIntegrationToEdgeArgs.builder()
                .edgeId(edgeId)
                .integrationId(integrationId)
                .build());
        assertNotNull(assigned);
        assertEquals(integrationId, assigned.getId().getId().toString());

        PageDataIntegration edgeIntegrations = client.getEdgeIntegrations(GetEdgeIntegrationsArgs.builder()
                .edgeId(edgeId)
                .pageSize(100)
                .page(0)
                .build());
        assertNotNull(edgeIntegrations);
        assertTrue(edgeIntegrations.getData().stream()
                .anyMatch(i -> i.getId().getId().toString().equals(integrationId)));

        PageDataIntegrationInfo edgeIntegrationInfos = client.getEdgeIntegrationInfos(GetEdgeIntegrationInfosArgs.builder()
                .edgeId(edgeId)
                .pageSize(100)
                .page(0)
                .build());
        assertNotNull(edgeIntegrationInfos);
        assertTrue(edgeIntegrationInfos.getData().stream()
                .anyMatch(i -> i.getId().getId().toString().equals(integrationId)));

        Integration unassigned = client.unassignIntegrationFromEdge(UnassignIntegrationFromEdgeArgs.builder()
                .edgeId(edgeId)
                .integrationId(integrationId)
                .build());
        assertNotNull(unassigned);
        assertEquals(integrationId, unassigned.getId().getId().toString());

        PageDataIntegration afterUnassign = client.getEdgeIntegrations(GetEdgeIntegrationsArgs.builder()
                .edgeId(edgeId)
                .pageSize(100)
                .page(0)
                .build());
        assertTrue(afterUnassign.getData().stream()
                .noneMatch(i -> i.getId().getId().toString().equals(integrationId)));

        client.deleteEdge(DeleteEdgeArgs.builder()
                .edgeId(edgeId)
                .build());
        client.deleteIntegration(DeleteIntegrationArgs.builder()
                .integrationId(integrationId)
                .build());
        client.deleteConverter(DeleteConverterArgs.builder()
                .converterId(converterId)
                .build());
    }

    private Converter createConverter(String name) throws ApiException {
        Converter converter = new Converter();
        converter.setName(name);
        converter.setType(ConverterType.UPLINK);
        converter.setConfiguration(ConverterApiClientTest.TEST_DECODER);
        return client.saveConverter(SaveConverterArgs.builder()
                .converter(converter)
                .build());
    }

    private Integration buildIntegration(String name, String routingKey, String converterId) {
        Integration integration = new Integration();
        integration.setName(name);
        integration.setType(IntegrationType.HTTP);
        integration.setRoutingKey(routingKey);
        integration.setConfiguration(EMPTY_CONFIG);
        integration.setDefaultConverterId(new ConverterId().id(UUID.fromString(converterId)));
        return integration;
    }

    private Edge createEdge(String name, String routingKey) throws ApiException {
        Edge edge = new Edge();
        edge.setName(name);
        edge.setType("default");
        edge.setRoutingKey(routingKey);
        edge.setSecret("edgeSecret_" + routingKey);
        edge.setEdgeLicenseKey("edgeLicense_" + routingKey);
        edge.setCloudEndpoint("http://localhost:8080");
        return client.saveEdge(SaveEdgeArgs.builder()
                .edge(edge)
                .build());
    }

}

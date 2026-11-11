// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.client;

import org.junit.Test;
import org.thingsboard.client.api.ThingsboardApi.DeleteEdgeArgs;
import org.thingsboard.client.api.ThingsboardApi.GetAllEdgeInfosArgs;
import org.thingsboard.client.api.ThingsboardApi.GetEdgeByIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetEdgeListArgs;
import org.thingsboard.client.api.ThingsboardApi.GetTenantEdgeByNameArgs;
import org.thingsboard.client.api.ThingsboardApi.GetTenantEdgesArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveEdgeArgs;
import org.thingsboard.client.model.Edge;
import org.thingsboard.client.model.PageDataEdge;
import org.thingsboard.client.model.PageDataEdgeInfo;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@DaoSqlTest
public class EdgeApiClientTest extends AbstractApiClientTest {

    @Test
    public void testEdgeLifecycle() throws Exception {
        long timestamp = System.currentTimeMillis();
        List<Edge> createdEdges = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            Edge edge = new Edge();
            edge.setName(TEST_PREFIX + "Edge_" + timestamp + "_" + i);
            edge.setType("gateway");
            edge.setLabel("Test Edge " + i);
            edge.setRoutingKey("routing_key_" + timestamp + "_" + i);
            edge.setSecret("secret_key_" + timestamp + "_" + i);
            edge.setEdgeLicenseKey("license_key_" + timestamp + "_" + i);
            edge.setCloudEndpoint("http://localhost:8080");

            Edge created = client.saveEdge(SaveEdgeArgs.builder()
                    .edge(edge)
                    .build());
            assertNotNull(created);
            assertNotNull(created.getId());
            assertEquals(edge.getName(), created.getName());
            assertEquals("gateway", created.getType());
            assertNotNull(created.getRoutingKey());
            assertNotNull(created.getSecret());

            createdEdges.add(created);
        }

        PageDataEdge filteredEdges = client.getTenantEdges(GetTenantEdgesArgs.builder()
                .pageSize(100)
                .page(0)
                .textSearch(TEST_PREFIX + "Edge_" + timestamp)
                .build());
        assertNotNull(filteredEdges);
        assertEquals(5, filteredEdges.getData().size());

        PageDataEdge typedEdges = client.getTenantEdges(GetTenantEdgesArgs.builder()
                .pageSize(100)
                .page(0)
                .type("gateway")
                .textSearch(TEST_PREFIX + "Edge_" + timestamp)
                .build());
        assertEquals(5, typedEdges.getData().size());

        PageDataEdgeInfo edgeInfos = client.getAllEdgeInfos(GetAllEdgeInfosArgs.builder()
                .pageSize(100)
                .page(0)
                .includeCustomers(true)
                .type("gateway")
                .textSearch(TEST_PREFIX + "Edge_" + timestamp)
                .build());
        assertEquals(5, edgeInfos.getData().size());

        Edge searchEdge = createdEdges.get(2);
        Edge fetchedEdge = client.getEdgeById(GetEdgeByIdArgs.builder()
                .edgeId(searchEdge.getId().getId().toString())
                .build());
        assertEquals(searchEdge.getName(), fetchedEdge.getName());
        assertEquals(searchEdge.getType(), fetchedEdge.getType());
        assertEquals(searchEdge.getRoutingKey(), fetchedEdge.getRoutingKey());

        Edge fetchedByName = client.getTenantEdgeByName(GetTenantEdgeByNameArgs.builder()
                .edgeName(searchEdge.getName())
                .build());
        assertEquals(searchEdge.getId().getId(), fetchedByName.getId().getId());

        List<String> idsToFetch = List.of(
                createdEdges.get(0).getId().getId().toString(),
                createdEdges.get(1).getId().getId().toString()
        );
        List<Edge> edgeList = client.getEdgeList(GetEdgeListArgs.builder()
                .edgeIds(idsToFetch)
                .build());
        assertEquals(2, edgeList.size());

        Edge edgeToUpdate = createdEdges.get(3);
        edgeToUpdate.setLabel("Updated Label");
        Edge updatedEdge = client.saveEdge(SaveEdgeArgs.builder()
                .edge(edgeToUpdate)
                .build());
        assertEquals("Updated Label", updatedEdge.getLabel());

        UUID edgeToDeleteId = createdEdges.get(0).getId().getId();
        client.deleteEdge(DeleteEdgeArgs.builder()
                .edgeId(edgeToDeleteId.toString())
                .build());

        assertReturns404(() ->
                client.getEdgeById(GetEdgeByIdArgs.builder()
                        .edgeId(edgeToDeleteId.toString())
                        .build())
        );

        PageDataEdge edgesAfterDelete = client.getTenantEdges(GetTenantEdgesArgs.builder()
                .pageSize(100)
                .page(0)
                .textSearch(TEST_PREFIX + "Edge_" + timestamp)
                .build());
        assertEquals(4, edgesAfterDelete.getData().size());
    }

}

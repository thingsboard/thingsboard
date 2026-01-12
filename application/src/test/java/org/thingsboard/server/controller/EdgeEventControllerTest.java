/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.dao.edge.EdgeEventDao;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.sqlts.insert.sql.SqlPartitioningRepository;
import org.thingsboard.server.service.ttl.EdgeEventsCleanUpService;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@TestPropertySource(properties = {
        "edges.enabled=true",
        "queue.rule-engine.stats.enabled=false"
})
@Slf4j
@DaoSqlTest
public class EdgeEventControllerTest extends AbstractControllerTest {

    @Autowired
    private EdgeEventDao edgeEventDao;
    @SpyBean
    private SqlPartitioningRepository partitioningRepository;
    @Autowired
    private EdgeEventsCleanUpService edgeEventsCleanUpService;

    @Value("#{${sql.edge_events.partition_size} * 60 * 60 * 1000}")
    private long partitionDurationInMs;
    @Value("${sql.ttl.edge_events.edge_event_ttl}")
    private long edgeEventTtlInSec;

    @Before
    public void beforeTest() throws Exception {
        loginTenantAdmin();
    }

    @After
    public void afterTest() throws Exception {
    }

    @Test
    public void testGetEdgeEvents() throws Exception {
        Edge edge = constructEdge("TestEdge", "default");
        edge = doPost("/api/edge", edge, Edge.class);

        final EdgeId edgeId = edge.getId();

        awaitForEdgeTemplateRootRuleChainToAssignToEdge(edgeId);

        // simulate edge activation
        ObjectNode attributes = JacksonUtil.newObjectNode();
        attributes.put("active", true);
        doPost("/api/plugins/telemetry/EDGE/" + edge.getId() + "/attributes/" + AttributeScope.SERVER_SCOPE, attributes);

        Device device = constructDevice("TestDevice", "default");
        Device savedDevice = doPost("/api/device", device, Device.class);
        doPost("/api/edge/" + edgeId + "/device/" + savedDevice.getId(), Device.class);

        Asset asset = constructAsset("TestAsset", "default");
        Asset savedAsset = doPost("/api/asset", asset, Asset.class);
        doPost("/api/edge/" + edgeId + "/asset/" + savedAsset.getId(), Asset.class);

        EntityRelation relation = new EntityRelation(savedAsset.getId(), savedDevice.getId(), EntityRelation.CONTAINS_TYPE);

        awaitForNumberOfEdgeEvents(edgeId, 2);

        doPost("/api/relation", relation);

        awaitForNumberOfEdgeEvents(edgeId, 3);

        List<EdgeEvent> edgeEvents = findEdgeEvents(edgeId);
        Assert.assertTrue(popEdgeEvent(edgeEvents, EdgeEventType.DEVICE));
        Assert.assertTrue(popEdgeEvent(edgeEvents, EdgeEventType.ASSET));
        Assert.assertTrue(popEdgeEvent(edgeEvents, EdgeEventType.RELATION));
        Assert.assertTrue(edgeEvents.isEmpty());
    }

    @Test
    public void saveEdgeEvent_thenCreatePartitionIfNotExist() {
        reset(partitioningRepository);
        EdgeEvent edgeEvent = createEdgeEvent();
        verify(partitioningRepository).createPartitionIfNotExists(eq("edge_event"), eq(edgeEvent.getCreatedTime()), eq(partitionDurationInMs));
        List<Long> partitions = partitioningRepository.fetchPartitions("edge_event");
        assertThat(partitions).singleElement().satisfies(partitionStartTs -> {
            assertThat(partitionStartTs).isEqualTo(partitioningRepository.calculatePartitionStartTime(edgeEvent.getCreatedTime(), partitionDurationInMs));
        });
    }

    @Test
    public void cleanUpEdgeEventByTtl_dropOldPartitions() {
        long oldEdgeEventTs = LocalDate.of(2020, 10, 1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
        long partitionStartTs = partitioningRepository.calculatePartitionStartTime(oldEdgeEventTs, partitionDurationInMs);
        partitioningRepository.createPartitionIfNotExists("edge_event", oldEdgeEventTs, partitionDurationInMs);
        List<Long> partitions = partitioningRepository.fetchPartitions("edge_event");
        assertThat(partitions).contains(partitionStartTs);

        edgeEventsCleanUpService.cleanUp();
        partitions = partitioningRepository.fetchPartitions("edge_event");
        assertThat(partitions).doesNotContain(partitionStartTs);
        assertThat(partitions).allSatisfy(partitionsStart -> {
            long partitionEndTs = partitionsStart + partitionDurationInMs;
            assertThat(partitionEndTs).isGreaterThan(System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(edgeEventTtlInSec));
        });
    }

    private boolean popEdgeEvent(List<EdgeEvent> edgeEvents, EdgeEventType edgeEventType) {
        for (EdgeEvent edgeEvent : edgeEvents) {
            if (edgeEventType.equals(edgeEvent.getType())) {
                edgeEvents.remove(edgeEvent);
                return true;
            }
        }
        return false;
    }

    private void awaitForNumberOfEdgeEvents(EdgeId edgeId, int expectedNumber) {
        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> {
                    List<EdgeEvent> edgeEvents = findEdgeEvents(edgeId);
                    return edgeEvents.size() == expectedNumber;
                });
    }

    private List<EdgeEvent> findEdgeEvents(EdgeId edgeId) throws Exception {
        return doGetTypedWithTimePageLink("/api/edge/" + edgeId + "/events?",
                new TypeReference<PageData<EdgeEvent>>() {
                }, new TimePageLink(10)).getData();
    }

    private void awaitForEdgeTemplateRootRuleChainToAssignToEdge(EdgeId edgeId) {
        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> {
                    List<RuleChain> ruleChains = getEdgeRuleChains(edgeId);
                    return ruleChains.size() == 1;
                });
    }

    private List<RuleChain> getEdgeRuleChains(EdgeId edgeId) throws Exception {
        return doGetTypedWithTimePageLink("/api/edge/" + edgeId + "/ruleChains?",
                new TypeReference<PageData<RuleChain>>() {
                }, new TimePageLink(10)).getData();
    }

    private Device constructDevice(String name, String type) {
        Device device = new Device();
        device.setName(name);
        device.setType(type);
        return device;
    }

    private Asset constructAsset(String name, String type) {
        Asset asset = new Asset();
        asset.setName(name);
        asset.setType(type);
        return asset;
    }

    private EdgeEvent createEdgeEvent() {
        EdgeEvent edgeEvent = new EdgeEvent();
        edgeEvent.setCreatedTime(System.currentTimeMillis());
        edgeEvent.setTenantId(tenantId);
        edgeEvent.setAction(EdgeEventActionType.ADDED);
        edgeEvent.setEntityId(tenantAdminUser.getUuidId());
        edgeEvent.setType(EdgeEventType.ALARM);
        try {
            edgeEventDao.saveAsync(edgeEvent).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        return edgeEvent;
    }

}

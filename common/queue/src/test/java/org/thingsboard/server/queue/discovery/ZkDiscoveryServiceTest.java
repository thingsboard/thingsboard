// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue.discovery;

import org.apache.curator.CuratorZookeeperClient;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CreateBuilder;
import org.apache.curator.framework.api.ExistsBuilder;
import org.apache.curator.framework.api.SetDataBuilder;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.listen.Listenable;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.discovery.event.OtherServiceShutdownEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import static org.apache.curator.framework.recipes.cache.CuratorCacheListener.Type.NODE_CREATED;
import static org.apache.curator.framework.recipes.cache.CuratorCacheListener.Type.NODE_DELETED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ZkDiscoveryServiceTest {

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private TbServiceInfoProvider serviceInfoProvider;

    @Mock
    private PartitionService partitionService;

    @Mock
    private CuratorFramework client;

    @Mock
    private CuratorCache cache;

    private ZkDiscoveryService zkDiscoveryService;
    private List<ChildData> dataList;

    private static final long RECALCULATE_DELAY = 100L;
    private static final long SESSION_ID = 0x100L;
    private static final long OTHER_SESSION_ID = 0x200L;

    final TransportProtos.ServiceInfo currentInfo = TransportProtos.ServiceInfo.newBuilder().setServiceId("tb-rule-engine-0").build();
    final ChildData currentData = new ChildData("/thingsboard/nodes/0000000010", null, currentInfo.toByteArray());
    final TransportProtos.ServiceInfo childInfo = TransportProtos.ServiceInfo.newBuilder().setServiceId("tb-rule-engine-1").build();
    final ChildData childData = new ChildData("/thingsboard/nodes/0000000020", null, childInfo.toByteArray());

    @BeforeEach
    public void setup() {
        zkDiscoveryService = Mockito.spy(new ZkDiscoveryService(applicationEventPublisher, serviceInfoProvider, partitionService));
        ScheduledExecutorService zkExecutorService = ThingsBoardExecutors.newSingleThreadScheduledExecutor("zk-discovery");
        lenient().when(client.getState()).thenReturn(CuratorFrameworkState.STARTED);
        ReflectionTestUtils.setField(zkDiscoveryService, "stopped", false);
        ReflectionTestUtils.setField(zkDiscoveryService, "client", client);
        ReflectionTestUtils.setField(zkDiscoveryService, "cache", cache);
        ReflectionTestUtils.setField(zkDiscoveryService, "nodePath", "/thingsboard/nodes/0000000010");
        ReflectionTestUtils.setField(zkDiscoveryService, "zkExecutorService", zkExecutorService);
        ReflectionTestUtils.setField(zkDiscoveryService, "recalculateDelay", RECALCULATE_DELAY);
        ReflectionTestUtils.setField(zkDiscoveryService, "zkDir", "/thingsboard");
        ReflectionTestUtils.setField(zkDiscoveryService, "zkNodesDir", "/thingsboard/nodes");

        lenient().when(serviceInfoProvider.getServiceInfo()).thenReturn(currentInfo);

        dataList = new ArrayList<>();
        dataList.add(currentData);
        lenient().when(cache.stream()).thenAnswer(inv -> dataList.stream());
    }

    @Test
    public void restartNodeInTimeTest() throws Exception {
        startNode(childData);

        verify(partitionService, times(1)).recalculatePartitions(eq(currentInfo), eq(List.of(childInfo)));

        reset(partitionService);

        stopNode(childData);

        assertEquals(1, zkDiscoveryService.delayedTasks.size());

        verify(partitionService, never()).recalculatePartitions(any(), any());

        startNode(childData);

        verify(partitionService, never()).recalculatePartitions(any(), any());

        Thread.sleep(RECALCULATE_DELAY * 2);

        verify(partitionService, never()).recalculatePartitions(any(), any());

        assertTrue(zkDiscoveryService.delayedTasks.isEmpty());
    }

    @Test
    public void restartNodeNotInTimeTest() throws Exception {
        startNode(childData);

        verify(partitionService, times(1)).recalculatePartitions(eq(currentInfo), eq(List.of(childInfo)));

        reset(partitionService);

        stopNode(childData);

        assertEquals(1, zkDiscoveryService.delayedTasks.size());

        Thread.sleep(RECALCULATE_DELAY * 2);

        assertTrue(zkDiscoveryService.delayedTasks.isEmpty());

        startNode(childData);

        verify(partitionService, times(1)).recalculatePartitions(eq(currentInfo), eq(Collections.emptyList()));

        verify(partitionService, times(1)).recalculatePartitions(eq(currentInfo), eq(List.of(childInfo)));

        reset(partitionService);
    }

    @Test
    public void startAnotherNodeDuringRestartTest() throws Exception {
        var anotherInfo = TransportProtos.ServiceInfo.newBuilder().setServiceId("tb-transport").build();
        var anotherData = new ChildData("/thingsboard/nodes/0000000030", null, anotherInfo.toByteArray());

        startNode(childData);

        verify(partitionService, times(1)).recalculatePartitions(eq(currentInfo), eq(List.of(childInfo)));

        reset(partitionService);

        stopNode(childData);

        assertEquals(1, zkDiscoveryService.delayedTasks.size());

        startNode(anotherData);

        assertTrue(zkDiscoveryService.delayedTasks.isEmpty());

        verify(partitionService, times(1)).recalculatePartitions(eq(currentInfo), eq(List.of(anotherInfo)));
        reset(partitionService);

        Thread.sleep(RECALCULATE_DELAY * 2);

        verify(partitionService, never()).recalculatePartitions(any(), any());

        startNode(childData);

        verify(partitionService, times(1)).recalculatePartitions(eq(currentInfo), eq(List.of(anotherInfo, childInfo)));
    }

    @Test
    public void publishCurrentServerUpdatesExistingNodeTest() throws Exception {
        ExistsBuilder existsBuilder = mockExistsBuilder();
        when(existsBuilder.forPath(currentData.getPath())).thenReturn(statOwnedBy(SESSION_ID));
        mockSessionId();
        SetDataBuilder setDataBuilder = mock(SetDataBuilder.class);
        when(client.setData()).thenReturn(setDataBuilder);
        when(serviceInfoProvider.generateNewServiceInfoWithCurrentSystemInfo()).thenReturn(currentInfo);

        zkDiscoveryService.publishCurrentServer();

        verify(setDataBuilder).forPath(currentData.getPath(), currentInfo.toByteArray());
        verify(client, never()).create();
    }

    @Test
    public void publishCurrentServerUpdatesNodeWithOutdatedDataTest() throws Exception {
        TransportProtos.ServiceInfo updatedInfo = currentInfo.toBuilder().setReady(true).build();
        when(serviceInfoProvider.getServiceInfo()).thenReturn(updatedInfo);
        ExistsBuilder existsBuilder = mockExistsBuilder();
        when(existsBuilder.forPath(currentData.getPath())).thenReturn(statOwnedBy(SESSION_ID));
        mockSessionId();
        SetDataBuilder setDataBuilder = mock(SetDataBuilder.class);
        when(client.setData()).thenReturn(setDataBuilder);
        when(serviceInfoProvider.generateNewServiceInfoWithCurrentSystemInfo()).thenReturn(updatedInfo);

        zkDiscoveryService.publishCurrentServer();

        verify(setDataBuilder).forPath(currentData.getPath(), updatedInfo.toByteArray());
        verify(client, never()).create();
    }

    @Test
    public void publishCurrentServerCreatesNodeWhenAbsentTest() throws Exception {
        String newPath = "/thingsboard/nodes/0000000011";
        ExistsBuilder existsBuilder = mockExistsBuilder();
        when(existsBuilder.forPath(currentData.getPath())).thenReturn(null);
        CreateBuilder createBuilder = mock(CreateBuilder.class, RETURNS_DEEP_STUBS);
        when(client.create()).thenReturn(createBuilder);
        when(createBuilder.creatingParentsIfNeeded().withProtection().withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath("/thingsboard/nodes/node-", currentInfo.toByteArray()))
                .thenReturn(newPath);
        when(client.getConnectionStateListenable()).thenReturn(mock(Listenable.class));

        zkDiscoveryService.publishCurrentServer();

        assertEquals(newPath, ReflectionTestUtils.getField(zkDiscoveryService, "nodePath"));
        verify(client, never()).setData();
    }

    @Test
    public void publishCurrentServerCreatesNodeWhenExistingNodeOwnedByAnotherSessionTest() throws Exception {
        String newPath = "/thingsboard/nodes/0000000011";
        ExistsBuilder existsBuilder = mockExistsBuilder();
        when(existsBuilder.forPath(currentData.getPath())).thenReturn(statOwnedBy(OTHER_SESSION_ID));
        mockSessionId();
        CreateBuilder createBuilder = mock(CreateBuilder.class, RETURNS_DEEP_STUBS);
        when(client.create()).thenReturn(createBuilder);
        when(createBuilder.creatingParentsIfNeeded().withProtection().withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath("/thingsboard/nodes/node-", currentInfo.toByteArray()))
                .thenReturn(newPath);
        when(client.getConnectionStateListenable()).thenReturn(mock(Listenable.class));

        zkDiscoveryService.publishCurrentServer();

        assertEquals(newPath, ReflectionTestUtils.getField(zkDiscoveryService, "nodePath"));
        verify(client, never()).setData();
    }

    @Test
    public void publishCurrentServerSkipsWhenNodeStateUnknownTest() throws Exception {
        ExistsBuilder existsBuilder = mockExistsBuilder();
        when(existsBuilder.forPath(currentData.getPath())).thenThrow(new InterruptedException());

        zkDiscoveryService.publishCurrentServer();

        assertTrue(Thread.interrupted());
        assertEquals(currentData.getPath(), ReflectionTestUtils.getField(zkDiscoveryService, "nodePath"));
        verify(client, never()).create();
        verify(client, never()).setData();
    }

    @Test
    public void getOtherServersIgnoresNodesWithCurrentServiceIdTest() {
        dataList.add(new ChildData("/thingsboard/nodes/0000000011", statWithMtime(1), currentInfo.toByteArray()));
        dataList.add(childData);

        assertEquals(List.of(childInfo), zkDiscoveryService.getOtherServers());
    }

    @Test
    public void getOtherServersKeepsLatestNodeForDuplicatedServiceIdTest() {
        TransportProtos.ServiceInfo latestInfo = childInfo.toBuilder().setReady(true).build();
        dataList.add(new ChildData("/thingsboard/nodes/0000000020", statWithMtime(1), childInfo.toByteArray()));
        dataList.add(new ChildData("/thingsboard/nodes/0000000021", statWithMtime(2), latestInfo.toByteArray()));

        assertEquals(List.of(latestInfo), zkDiscoveryService.getOtherServers());

        Collections.swap(dataList, 1, 2);

        assertEquals(List.of(latestInfo), zkDiscoveryService.getOtherServers());
    }

    @Test
    public void nodeWithCurrentServiceIdDoesNotTriggerRecalculationTest() {
        ChildData orphanData = new ChildData("/thingsboard/nodes/0000000011", statWithMtime(1), currentInfo.toByteArray());

        startNode(orphanData);
        stopNode(orphanData);

        assertTrue(zkDiscoveryService.delayedTasks.isEmpty());
        verify(partitionService, never()).recalculatePartitions(any(), any());
        verify(applicationEventPublisher, never()).publishEvent(any(OtherServiceShutdownEvent.class));
    }

    @Test
    public void removalOfDuplicatedNodeDoesNotPublishShutdownEventTest() {
        ChildData staleData = new ChildData("/thingsboard/nodes/0000000019", statWithMtime(1), childInfo.toByteArray());
        ChildData liveData = new ChildData("/thingsboard/nodes/0000000020", statWithMtime(2), childInfo.toByteArray());
        startNode(staleData);
        startNode(liveData);

        stopNode(staleData);

        assertTrue(zkDiscoveryService.delayedTasks.isEmpty());
        verify(applicationEventPublisher, never()).publishEvent(any(OtherServiceShutdownEvent.class));

        stopNode(liveData);

        assertEquals(1, zkDiscoveryService.delayedTasks.size());
    }

    private ExistsBuilder mockExistsBuilder() {
        ExistsBuilder existsBuilder = mock(ExistsBuilder.class);
        when(client.checkExists()).thenReturn(existsBuilder);
        return existsBuilder;
    }

    private void mockSessionId() throws Exception {
        CuratorZookeeperClient zookeeperClient = mock(CuratorZookeeperClient.class);
        ZooKeeper zooKeeper = mock(ZooKeeper.class);
        when(client.getZookeeperClient()).thenReturn(zookeeperClient);
        when(zookeeperClient.getZooKeeper()).thenReturn(zooKeeper);
        when(zooKeeper.getSessionId()).thenReturn(SESSION_ID);
    }

    private static Stat statOwnedBy(long sessionId) {
        Stat stat = new Stat();
        stat.setEphemeralOwner(sessionId);
        return stat;
    }

    private static Stat statWithMtime(long mtime) {
        Stat stat = new Stat();
        stat.setMtime(mtime);
        return stat;
    }

    private void startNode(ChildData data) {
        dataList.add(data);
        zkDiscoveryService.onCacheEvent(NODE_CREATED, null, data);
    }

    private void stopNode(ChildData data) {
        dataList.remove(data);
        zkDiscoveryService.onCacheEvent(NODE_DELETED, data, null);
    }

}

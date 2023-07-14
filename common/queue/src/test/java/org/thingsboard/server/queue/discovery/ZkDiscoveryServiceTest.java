/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.queue.discovery;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent.Type.CHILD_ADDED;
import static org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent.Type.CHILD_REMOVED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ZkDiscoveryServiceTest {

    @Mock
    private TbServiceInfoProvider serviceInfoProvider;

    @Mock
    private PartitionService partitionService;

    @Mock
    private CuratorFramework client;

    @Mock
    private PathChildrenCache cache;

    private ScheduledExecutorService zkExecutorService;

    @Mock
    private CuratorFramework curatorFramework;

    private ZkDiscoveryService zkDiscoveryService;

    @Before
    public void setup() {
        zkDiscoveryService = Mockito.spy(new ZkDiscoveryService(serviceInfoProvider, partitionService));
        zkExecutorService = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("zk-discovery"));
        when(client.getState()).thenReturn(CuratorFrameworkState.STARTED);
        ReflectionTestUtils.setField(zkDiscoveryService, "stopped", false);
        ReflectionTestUtils.setField(zkDiscoveryService, "client", client);
        ReflectionTestUtils.setField(zkDiscoveryService, "cache", cache);
        ReflectionTestUtils.setField(zkDiscoveryService, "nodePath", "/thingsboard/nodes/0000000010");
        ReflectionTestUtils.setField(zkDiscoveryService, "zkExecutorService", zkExecutorService);
        ReflectionTestUtils.setField(zkDiscoveryService, "recalculateDelay", 1000L);
        ReflectionTestUtils.setField(zkDiscoveryService, "zkDir", "/thingsboard");
    }

    @Test
    public void restartNodeTest() throws Exception {
        var currentInfo = TransportProtos.ServiceInfo.newBuilder().setServiceId("currentId").build();
        var currentData = new ChildData("/thingsboard/nodes/0000000010", null, currentInfo.toByteArray());
        var childInfo = TransportProtos.ServiceInfo.newBuilder().setServiceId("childId").build();
        var childData = new ChildData("/thingsboard/nodes/0000000020", null, childInfo.toByteArray());

        when(serviceInfoProvider.getServiceInfo()).thenReturn(currentInfo);
        List<ChildData> dataList = new ArrayList<>();
        dataList.add(currentData);
        when(cache.getCurrentData()).thenReturn(dataList);

        startNode(childData);

        verify(partitionService, times(1)).recalculatePartitions(eq(currentInfo), eq(List.of(childInfo)));

        reset(partitionService);

        //Restart in timeAssert.assertTrue(zkDiscoveryService.delayedTasks.isEmpty());
        stopNode(childData);

        assertEquals(1, zkDiscoveryService.delayedTasks.size());

        verify(partitionService, never()).recalculatePartitions(eq(currentInfo), any());

        startNode(childData);

        verify(partitionService, never()).recalculatePartitions(eq(currentInfo), any());

        Thread.sleep(2000);

        verify(partitionService, never()).recalculatePartitions(eq(currentInfo), any());

        assertTrue(zkDiscoveryService.delayedTasks.isEmpty());

        //Restart not in time
        stopNode(childData);

        assertEquals(1, zkDiscoveryService.delayedTasks.size());

        Thread.sleep(2000);

        assertTrue(zkDiscoveryService.delayedTasks.isEmpty());

        startNode(childData);

        verify(partitionService, times(1)).recalculatePartitions(eq(currentInfo), eq(Collections.emptyList()));

        verify(partitionService, times(1)).recalculatePartitions(eq(currentInfo), eq(List.of(childInfo)));

        reset(partitionService);

        //Start another node during restart
        var anotherInfo = TransportProtos.ServiceInfo.newBuilder().setServiceId("anotherId").build();
        var anotherData = new ChildData("/thingsboard/nodes/0000000030", null, anotherInfo.toByteArray());

        stopNode(childData);

        assertEquals(1, zkDiscoveryService.delayedTasks.size());

        startNode(anotherData);

        assertTrue(zkDiscoveryService.delayedTasks.isEmpty());

        verify(partitionService, times(1)).recalculatePartitions(eq(currentInfo), eq(List.of(anotherInfo)));
        reset(partitionService);

        Thread.sleep(2000);

        verify(partitionService, never()).recalculatePartitions(eq(currentInfo), any());

        startNode(childData);

        verify(partitionService, times(1)).recalculatePartitions(eq(currentInfo), eq(List.of(anotherInfo, childInfo)));
    }

    private void startNode(ChildData data) throws Exception {
        cache.getCurrentData().add(data);
        zkDiscoveryService.childEvent(curatorFramework, new PathChildrenCacheEvent(CHILD_ADDED, data));
    }

    private void stopNode(ChildData data) throws Exception {
        cache.getCurrentData().remove(data);
        zkDiscoveryService.childEvent(curatorFramework, new PathChildrenCacheEvent(CHILD_REMOVED, data));
    }

}

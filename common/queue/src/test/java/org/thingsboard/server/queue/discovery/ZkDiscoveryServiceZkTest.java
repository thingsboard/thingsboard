// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue.discovery;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryOneTime;
import org.apache.curator.test.TestingServer;
import org.apache.curator.utils.CloseableUtils;
import org.apache.zookeeper.data.Stat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.time.Duration;
import java.util.List;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class ZkDiscoveryServiceZkTest {

    private static final String ZK_NODES_DIR = "/thingsboard/nodes";
    private static final String PROTECTED_NODE_NAME_REGEX = "_c_[0-9a-f-]{36}-node-\\d{10}";

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private TbServiceInfoProvider serviceInfoProvider;
    @Mock
    private PartitionService partitionService;

    private final TransportProtos.ServiceInfo currentInfo = TransportProtos.ServiceInfo.newBuilder().setServiceId("tb-core-0").build();

    private TestingServer zkServer;
    private CuratorFramework observer;
    private ZkDiscoveryService zkDiscoveryService;

    @BeforeEach
    public void setup() throws Exception {
        zkServer = new TestingServer(true);
        observer = CuratorFrameworkFactory.newClient(zkServer.getConnectString(), new RetryOneTime(100));
        observer.start();

        lenient().when(serviceInfoProvider.getServiceInfo()).thenReturn(currentInfo);
        lenient().when(serviceInfoProvider.generateNewServiceInfoWithCurrentSystemInfo()).thenReturn(currentInfo);

        zkDiscoveryService = new ZkDiscoveryService(applicationEventPublisher, serviceInfoProvider, partitionService);
        ReflectionTestUtils.setField(zkDiscoveryService, "zkUrl", zkServer.getConnectString());
        ReflectionTestUtils.setField(zkDiscoveryService, "zkRetryInterval", 100);
        ReflectionTestUtils.setField(zkDiscoveryService, "zkConnectionTimeout", 3000);
        ReflectionTestUtils.setField(zkDiscoveryService, "zkSessionTimeout", 3000);
        ReflectionTestUtils.setField(zkDiscoveryService, "zkDir", "/thingsboard");
        ReflectionTestUtils.setField(zkDiscoveryService, "recalculateDelay", 0L);
        zkDiscoveryService.init();
        zkDiscoveryService.onApplicationEvent(null);
    }

    @AfterEach
    public void tearDown() throws Exception {
        ReflectionTestUtils.invokeMethod(zkDiscoveryService, "destroy");
        CloseableUtils.closeQuietly(observer);
        zkServer.close();
    }

    @Test
    public void publishCreatesSingleProtectedNodeAndExcludesItFromOtherServersTest() throws Exception {
        List<String> nodes = observer.getChildren().forPath(ZK_NODES_DIR);

        assertEquals(1, nodes.size());
        assertTrue(nodes.get(0).matches(PROTECTED_NODE_NAME_REGEX), nodes.get(0));
        assertEquals(ZK_NODES_DIR + "/" + nodes.get(0), ReflectionTestUtils.getField(zkDiscoveryService, "nodePath"));
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertTrue(zkDiscoveryService.getOtherServers().isEmpty()));
    }

    @Test
    public void sessionExpiryLeavesSingleNodeOwnedByCurrentSessionTest() throws Exception {
        String nodeBeforeExpiry = observer.getChildren().forPath(ZK_NODES_DIR).get(0);

        zkDiscoveryService.getClient().getZookeeperClient().getZooKeeper().getTestable().injectSessionExpiration();

        await().atMost(Duration.ofSeconds(30))
                .during(Duration.ofSeconds(2))
                .ignoreExceptions()
                .untilAsserted(() -> {
                    List<String> nodes = observer.getChildren().forPath(ZK_NODES_DIR);
                    assertEquals(1, nodes.size());
                    String nodePath = ZK_NODES_DIR + "/" + nodes.get(0);
                    assertNotEquals(nodeBeforeExpiry, nodes.get(0));
                    assertEquals(nodePath, ReflectionTestUtils.getField(zkDiscoveryService, "nodePath"));
                    Stat stat = observer.checkExists().forPath(nodePath);
                    long sessionId = zkDiscoveryService.getClient().getZookeeperClient().getZooKeeper().getSessionId();
                    assertEquals(sessionId, stat.getEphemeralOwner());
                    assertTrue(zkDiscoveryService.getOtherServers().isEmpty());
                });
    }

}

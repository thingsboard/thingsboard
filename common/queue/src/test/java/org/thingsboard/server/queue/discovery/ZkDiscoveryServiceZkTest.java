// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
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
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.TbTransportService;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

        zkDiscoveryService = createDiscoveryService(serviceInfoProvider);
        zkDiscoveryService.onApplicationEvent(null);
    }

    private ZkDiscoveryService createDiscoveryService(TbServiceInfoProvider provider) {
        ZkDiscoveryService discoveryService = new ZkDiscoveryService(applicationEventPublisher, provider, partitionService);
        ReflectionTestUtils.setField(discoveryService, "zkUrl", zkServer.getConnectString());
        ReflectionTestUtils.setField(discoveryService, "zkRetryInterval", 100);
        ReflectionTestUtils.setField(discoveryService, "zkConnectionTimeout", 3000);
        ReflectionTestUtils.setField(discoveryService, "zkSessionTimeout", 3000);
        ReflectionTestUtils.setField(discoveryService, "zkDir", "/thingsboard");
        ReflectionTestUtils.setField(discoveryService, "recalculateDelay", 0L);
        discoveryService.init();
        return discoveryService;
    }

    /**
     * A provider as a transport node has it once the context is ready: created, initialized and told its transports,
     * which is the state it is in when the discovery service publishes the node for the first time.
     */
    private static DefaultTbServiceInfoProvider transportNodeProvider(String serviceId, String transportName) {
        TbTransportService transport = mock(TbTransportService.class);
        when(transport.getName()).thenReturn(transportName);
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        when(applicationContext.getBeansOfType(TbTransportService.class)).thenReturn(Map.of("transportService", transport));

        DefaultTbServiceInfoProvider provider = new DefaultTbServiceInfoProvider();
        ReflectionTestUtils.setField(provider, "serviceId", serviceId);
        ReflectionTestUtils.setField(provider, "serviceType", "tb-transport");
        ReflectionTestUtils.setField(provider, "applicationContext", applicationContext);
        provider.init();
        provider.setTransports();
        return provider;
    }

    private TransportProtos.ServiceInfo publishedInfo(ZkDiscoveryService discoveryService) throws Exception {
        String nodePath = (String) ReflectionTestUtils.getField(discoveryService, "nodePath");
        return TransportProtos.ServiceInfo.parseFrom(observer.getData().forPath(nodePath));
    }

    private List<String> transportsOfOtherNode(String serviceId) {
        return zkDiscoveryService.getOtherServers().stream()
                .filter(info -> info.getServiceId().equals(serviceId))
                .findFirst()
                .map(info -> List.copyOf(info.getTransportsList()))
                .orElse(null);
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
    public void periodicRepublishKeepsTransportsOfTheNodeTest() throws Exception {
        DefaultTbServiceInfoProvider provider = transportNodeProvider("tb-snmp-transport-0", DataConstants.SNMP_TRANSPORT_NAME);
        ZkDiscoveryService transportNode = createDiscoveryService(provider);
        try {
            transportNode.onApplicationEvent(null);
            assertThat(publishedInfo(transportNode).getTransportsList()).containsExactly(DataConstants.SNMP_TRANSPORT_NAME);

            // what the discovery service does every minute
            transportNode.publishCurrentServer();

            assertThat(publishedInfo(transportNode).getTransportsList()).containsExactly(DataConstants.SNMP_TRANSPORT_NAME);
            assertThat(provider.getServiceInfo().getTransportsList()).containsExactly(DataConstants.SNMP_TRANSPORT_NAME);
            // and what the other nodes read to balance the transport
            await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                    assertThat(transportsOfOtherNode("tb-snmp-transport-0")).containsExactly(DataConstants.SNMP_TRANSPORT_NAME));
        } finally {
            ReflectionTestUtils.invokeMethod(transportNode, "destroy");
        }
    }

    @Test
    public void readyFlagChangeKeepsTransportsOfTheNodeTest() throws Exception {
        DefaultTbServiceInfoProvider provider = transportNodeProvider("tb-snmp-transport-0", DataConstants.SNMP_TRANSPORT_NAME);
        ZkDiscoveryService transportNode = createDiscoveryService(provider);
        try {
            transportNode.onApplicationEvent(null);

            transportNode.setReady(false);

            TransportProtos.ServiceInfo published = publishedInfo(transportNode);
            assertThat(published.getReady()).isFalse();
            assertThat(published.getTransportsList()).containsExactly(DataConstants.SNMP_TRANSPORT_NAME);
            assertThat(provider.getServiceInfo().getTransportsList()).containsExactly(DataConstants.SNMP_TRANSPORT_NAME);
        } finally {
            ReflectionTestUtils.invokeMethod(transportNode, "destroy");
        }
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

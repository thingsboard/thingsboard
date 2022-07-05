/**
 * Copyright © 2016-2022 The Thingsboard Authors
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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.mockito.Mockito.mock;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class HashPartitionServiceTest {

    public static final int ITERATIONS = 1000000;
    public static final int SERVER_COUNT = 3;
    private HashPartitionService clusterRoutingService;

    private TbServiceInfoProvider discoveryService;
    private TenantRoutingInfoService routingInfoService;
    private ApplicationEventPublisher applicationEventPublisher;
    private QueueRoutingInfoService queueRoutingInfoService;

    private String hashFunctionName = "sha256";

    @Before
    public void setup() throws Exception {
        discoveryService = mock(TbServiceInfoProvider.class);
        applicationEventPublisher = mock(ApplicationEventPublisher.class);
        routingInfoService = mock(TenantRoutingInfoService.class);
        queueRoutingInfoService = mock(QueueRoutingInfoService.class);
        clusterRoutingService = new HashPartitionService(discoveryService,
                routingInfoService,
                applicationEventPublisher,
                queueRoutingInfoService);
        ReflectionTestUtils.setField(clusterRoutingService, "coreTopic", "tb.core");
        ReflectionTestUtils.setField(clusterRoutingService, "corePartitions", 10);
        ReflectionTestUtils.setField(clusterRoutingService, "vcTopic", "tb.vc");
        ReflectionTestUtils.setField(clusterRoutingService, "vcPartitions", 10);
        ReflectionTestUtils.setField(clusterRoutingService, "hashFunctionName", hashFunctionName);
        TransportProtos.ServiceInfo currentServer = TransportProtos.ServiceInfo.newBuilder()
                .setServiceId("tb-core-0")
                .addAllServiceTypes(Collections.singletonList(ServiceType.TB_CORE.name()))
                .build();
//        when(queueService.resolve(Mockito.any(), Mockito.anyString())).thenAnswer(i -> i.getArguments()[1]);
//        when(discoveryService.getServiceInfo()).thenReturn(currentServer);
        List<TransportProtos.ServiceInfo> otherServers = new ArrayList<>();
        for (int i = 1; i < SERVER_COUNT; i++) {
            otherServers.add(TransportProtos.ServiceInfo.newBuilder()
                    .setServiceId("tb-rule-" + i)
                    .addAllServiceTypes(Collections.singletonList(ServiceType.TB_CORE.name()))
                    .build());
        }

        clusterRoutingService.init();
        clusterRoutingService.partitionsInit();
        clusterRoutingService.recalculatePartitions(currentServer, otherServers);
    }

    @Test
    public void testDispersionOnMillionDevices() {
        List<DeviceId> devices = new ArrayList<>();
        for (int i = 0; i < ITERATIONS; i++) {
            devices.add(new DeviceId(Uuids.timeBased()));
        }
        testDevicesDispersion(devices);
    }

    private void testDevicesDispersion(List<DeviceId> devices) {
        long start = System.currentTimeMillis();
        Map<Integer, Integer> map = new HashMap<>();
        for (DeviceId deviceId : devices) {
            TopicPartitionInfo address = clusterRoutingService.resolve(ServiceType.TB_CORE, TenantId.SYS_TENANT_ID, deviceId);
            Integer partition = address.getPartition().get();
            map.put(partition, map.getOrDefault(partition, 0) + 1);
        }

        printDispersion(start, map);
    }

    @Test
    public void testDispersionOnResolveByPartitionIdx() {
        int serverCount = 10;
        int queueCount = 1000;
        int partitionCount = 3;

        List<TransportProtos.ServiceInfo> services = new ArrayList<>();

        for (int i = 0; i < serverCount; i++) {
            services.add(TransportProtos.ServiceInfo.newBuilder().setServiceId("RE-" + i).build());
        }

        long start = System.currentTimeMillis();
        Map<String, Integer> map = new HashMap<>();
        services.forEach(s -> map.put(s.getServiceId(), 0));

        for (int queueIndex = 0; queueIndex < queueCount; queueIndex++) {
            for (int partition = 0; partition < partitionCount; partition++) {
                TopicPartitionInfo tpi = new TopicPartitionInfo("tb_rule_engine.queue_" + queueIndex, TenantId.SYS_TENANT_ID, partition, false);
                TransportProtos.ServiceInfo serviceInfo = clusterRoutingService.resolveByPartitionIdx(services, tpi);
                String serviceId = serviceInfo.getServiceId();
                map.put(serviceId, map.get(serviceId) + 1);
            }
        }

        printDispersion(start, map);
    }

    private <T> void printDispersion(long start, Map<T, Integer> map) {
        List<Map.Entry<T, Integer>> data = map.entrySet().stream().sorted(Comparator.comparingInt(Map.Entry::getValue)).collect(Collectors.toList());
        long end = System.currentTimeMillis();
        double diff = (data.get(data.size() - 1).getValue() - data.get(0).getValue());
        double diffPercent = (diff / ITERATIONS) * 100.0;
        System.out.println("Time: " + (end - start) + " Diff: " + diff + "(" + String.format("%f", diffPercent) + "%)");
        Assert.assertTrue(diffPercent < 0.5);
        for (Map.Entry<T, Integer> entry : data) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
    }

}

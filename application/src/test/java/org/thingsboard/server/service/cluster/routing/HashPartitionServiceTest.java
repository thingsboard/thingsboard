/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.service.cluster.routing;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceQueue;
import org.thingsboard.server.queue.discovery.HashPartitionService;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.discovery.TenantRoutingInfoService;
import org.thingsboard.server.queue.settings.TbQueueRuleEngineSettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class HashPartitionServiceTest {

    public static final int ITERATIONS = 1000000;
    public static final int SERVER_COUNT = 3;
    private HashPartitionService clusterRoutingService;

    private TbServiceInfoProvider discoveryService;
    private TenantRoutingInfoService routingInfoService;
    private ApplicationEventPublisher applicationEventPublisher;
    private TbQueueRuleEngineSettings ruleEngineSettings;

    private String hashFunctionName = "sha256";


    @Before
    public void setup() throws Exception {
        discoveryService = mock(TbServiceInfoProvider.class);
        applicationEventPublisher = mock(ApplicationEventPublisher.class);
        routingInfoService = mock(TenantRoutingInfoService.class);
        ruleEngineSettings = mock(TbQueueRuleEngineSettings.class);
        clusterRoutingService = new HashPartitionService(discoveryService,
                routingInfoService,
                applicationEventPublisher,
                ruleEngineSettings
        );
        when(ruleEngineSettings.getQueues()).thenReturn(Collections.emptyList());
        ReflectionTestUtils.setField(clusterRoutingService, "coreTopic", "tb.core");
        ReflectionTestUtils.setField(clusterRoutingService, "corePartitions", 10);
        ReflectionTestUtils.setField(clusterRoutingService, "hashFunctionName", hashFunctionName);
        TransportProtos.ServiceInfo currentServer = TransportProtos.ServiceInfo.newBuilder()
                .setServiceId("tb-core-0")
                .setTenantIdMSB(TenantId.NULL_UUID.getMostSignificantBits())
                .setTenantIdLSB(TenantId.NULL_UUID.getLeastSignificantBits())
                .addAllServiceTypes(Collections.singletonList(ServiceType.TB_CORE.name()))
                .build();
//        when(discoveryService.getServiceInfo()).thenReturn(currentServer);
        List<TransportProtos.ServiceInfo> otherServers = new ArrayList<>();
        for (int i = 1; i < SERVER_COUNT; i++) {
            otherServers.add(TransportProtos.ServiceInfo.newBuilder()
                    .setServiceId("tb-rule-" + i)
                    .setTenantIdMSB(TenantId.NULL_UUID.getMostSignificantBits())
                    .setTenantIdLSB(TenantId.NULL_UUID.getLeastSignificantBits())
                    .addAllServiceTypes(Collections.singletonList(ServiceType.TB_CORE.name()))
                    .build());
        }
        clusterRoutingService.init();
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

        List<Map.Entry<Integer, Integer>> data = map.entrySet().stream().sorted(Comparator.comparingInt(Map.Entry::getValue)).collect(Collectors.toList());
        long end = System.currentTimeMillis();
        double diff = (data.get(data.size() - 1).getValue() - data.get(0).getValue());
        double diffPercent = (diff / ITERATIONS) * 100.0;
        System.out.println("Time: " + (end - start) + " Diff: " + diff + "(" + String.format("%f", diffPercent) + "%)");
        Assert.assertTrue(diffPercent < 0.5);
        for (Map.Entry<Integer, Integer> entry : data) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
    }

}

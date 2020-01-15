/**
 * Copyright © 2016-2020 The Thingsboard Authors
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

import com.datastax.driver.core.utils.UUIDs;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.common.msg.cluster.ServerType;
import org.thingsboard.server.service.cluster.discovery.DiscoveryService;
import org.thingsboard.server.service.cluster.discovery.ServerInstance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class ConsistentClusterRoutingServiceTest {

    private ConsistentClusterRoutingService clusterRoutingService;

    private DiscoveryService discoveryService;

    private String hashFunctionName = "murmur3_128";
    private Integer virtualNodesSize = 1024*4;
    private ServerAddress currentServer = new ServerAddress(" 100.96.1.0", 9001, ServerType.CORE);


    @Before
    public void setup() throws Exception {
        discoveryService = mock(DiscoveryService.class);
        clusterRoutingService = new ConsistentClusterRoutingService();
        ReflectionTestUtils.setField(clusterRoutingService, "discoveryService", discoveryService);
        ReflectionTestUtils.setField(clusterRoutingService, "hashFunctionName", hashFunctionName);
        ReflectionTestUtils.setField(clusterRoutingService, "virtualNodesSize", virtualNodesSize);
        when(discoveryService.getCurrentServer()).thenReturn(new ServerInstance(currentServer));
        List<ServerInstance> otherServers = new ArrayList<>();
        for (int i = 1; i < 30; i++) {
            otherServers.add(new ServerInstance(new ServerAddress(" 100.96." + i*2 + "." + i, 9001, ServerType.CORE)));
        }
        when(discoveryService.getOtherServers()).thenReturn(otherServers);
        clusterRoutingService.init();
    }

    @Test
    public void testDispersionOnMillionDevices() {
        List<DeviceId> devices = new ArrayList<>();
        for (int i = 0; i < 1000000; i++) {
            devices.add(new DeviceId(UUIDs.timeBased()));
        }

        testDevicesDispersion(devices);
    }

    private void testDevicesDispersion(List<DeviceId> devices) {
        long start = System.currentTimeMillis();
        Map<ServerAddress, Integer> map = new HashMap<>();
        for (DeviceId deviceId : devices) {
            ServerAddress address = clusterRoutingService.resolveById(deviceId).orElse(currentServer);
            map.put(address, map.getOrDefault(address, 0) + 1);
        }

        List<Map.Entry<ServerAddress, Integer>> data = map.entrySet().stream().sorted(Comparator.comparingInt(Map.Entry::getValue)).collect(Collectors.toList());
        long end = System.currentTimeMillis();
        System.out.println("Size: " + virtualNodesSize + " Time: " + (end - start) + " Diff: " + (data.get(data.size() - 1).getValue() - data.get(0).getValue()));

        for (Map.Entry<ServerAddress, Integer> entry : data) {
//            System.out.println(entry.getKey().getHost() + ": " + entry.getValue());
        }

    }

}

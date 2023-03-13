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
package org.thingsboard.server.service.system;

import com.google.protobuf.ProtocolStringList;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.SystemInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@TbCoreComponent
@Service
@RequiredArgsConstructor
public class DefaultSystemInfoService implements SystemInfoService {

    private final TbServiceInfoProvider serviceInfoProvider;
    private final PartitionService partitionService;

    @Value("${zk.enabled:false}")
    private boolean zkEnabled;

    @Override
    @SneakyThrows
    public SystemInfo getSystemInfo() {
        SystemInfo systemInfo = new SystemInfo();

        TransportProtos.ServiceInfo serviceInfo = serviceInfoProvider.getServiceInfo();
        List<TransportProtos.ServiceInfo> currentOtherServices = partitionService.getCurrentOtherServices();

        if (zkEnabled) {
            Map<String, String> serviceInfos = new HashMap<>();
            addServiceInfo(serviceInfos, serviceInfo);
            currentOtherServices.forEach(otherInfo -> addServiceInfo(serviceInfos, otherInfo));
            systemInfo.setServiceInfos(serviceInfos);
        } else {
            systemInfo.setMonolith(true);
            systemInfo.setMemUsage(getMemoryUsage());

            systemInfo.setCpuUsage((int) (getCpuUsage() * 100) / 100.0);
            systemInfo.setFreeDiscSpace(getFreeDiscSpace());
        }

        return systemInfo;
    }

    private void addServiceInfo(Map<String, String> serviceInfos, TransportProtos.ServiceInfo serviceInfo) {
        ProtocolStringList serviceTypes = serviceInfo.getServiceTypesList();
        serviceInfos.put(serviceInfo.getServiceId(), serviceTypes.size() > 1 ? "MONOLITH" : serviceTypes.get(0));
    }

    private long getMemoryUsage() {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        return memoryMXBean.getHeapMemoryUsage().getUsed();
    }

    private double getCpuUsage() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        return osBean.getSystemLoadAverage();
    }

    private long getFreeDiscSpace() {
        File file = new File("/");
        return file.getFreeSpace();
    }
}

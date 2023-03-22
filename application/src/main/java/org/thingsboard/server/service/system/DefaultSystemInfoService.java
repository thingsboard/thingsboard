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

import com.google.common.util.concurrent.FutureCallback;
import com.google.protobuf.ProtocolStringList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.SystemInfo;
import org.thingsboard.server.common.data.SystemInfoData;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.stats.TbApiUsageStateClient;
import org.thingsboard.server.gen.transport.TransportProtos.ServiceInfo;
import org.thingsboard.server.queue.discovery.DiscoveryService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbApplicationEventListener;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import javax.annotation.Nullable;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.common.util.SystemUtil.getCpuUsage;
import static org.thingsboard.common.util.SystemUtil.getFreeDiscSpace;
import static org.thingsboard.common.util.SystemUtil.getFreeMemory;
import static org.thingsboard.common.util.SystemUtil.getMemoryUsage;
import static org.thingsboard.common.util.SystemUtil.getTotalCpuUsage;
import static org.thingsboard.common.util.SystemUtil.getTotalDiscSpace;
import static org.thingsboard.common.util.SystemUtil.getTotalMemory;

@TbCoreComponent
@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultSystemInfoService extends TbApplicationEventListener<PartitionChangeEvent> implements SystemInfoService {

    public static final FutureCallback<Integer> CALLBACK = new FutureCallback<>() {
        @Override
        public void onSuccess(@Nullable Integer result) {
        }

        @Override
        public void onFailure(Throwable t) {
            log.warn("Failed to persist system info", t);
        }
    };

    private final TbServiceInfoProvider serviceInfoProvider;
    private final PartitionService partitionService;
    private final DiscoveryService discoveryService;
    private final TelemetrySubscriptionService telemetryService;
    private final TbApiUsageStateClient apiUsageStateClient;
    private ScheduledExecutorService scheduler;

    @Value("${zk.enabled:false}")
    private boolean zkEnabled;

    @Override
    protected void onTbApplicationEvent(PartitionChangeEvent partitionChangeEvent) {
        if (ServiceType.TB_CORE.equals(partitionChangeEvent.getServiceType())) {
            if (scheduler == null && partitionService.resolve(ServiceType.TB_CORE, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID).isMyPartition()) {
                scheduler = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("tb-system-info-scheduler"));
                scheduler.scheduleAtFixedRate(this::saveCurrentSystemInfo, 0, 1, TimeUnit.MINUTES);
            } else {
                destroy();
            }
        }
    }

    @Override
    public SystemInfo getSystemInfo() {
        SystemInfo systemInfo = new SystemInfo();

        ServiceInfo serviceInfo = serviceInfoProvider.getServiceInfoWithCurrentSystemInfo();

        if (zkEnabled) {
            systemInfo.setSystemData(getSystemData(serviceInfo));
        } else {
            systemInfo.setMonolith(true);
            systemInfo.setSystemData(Collections.singletonList(createSystemInfoData(serviceInfo)));
        }

        return systemInfo;
    }

    protected void saveCurrentSystemInfo() {
        if (zkEnabled) {
            saveCurrentClusterSystemInfo();
        } else {
            saveCurrentMonolithSystemInfo();
        }
    }

    private void saveCurrentClusterSystemInfo() {
        long ts = System.currentTimeMillis();

        List<SystemInfoData> clusterSystemData = getSystemData(serviceInfoProvider.getServiceInfoWithCurrentSystemInfo());
        BasicTsKvEntry clusterDataKv = new BasicTsKvEntry(ts, new JsonDataEntry("clusterSystemData", JacksonUtil.toString(clusterSystemData)));
        doSave(Collections.singletonList(clusterDataKv));
    }

    private void saveCurrentMonolithSystemInfo() {
        long ts = System.currentTimeMillis();
        List<TsKvEntry> tsList = new ArrayList<>();

        Long memoryUsage = getMemoryUsage();
        if (memoryUsage != null) {
            tsList.add(new BasicTsKvEntry(ts, new LongDataEntry("memoryUsage", memoryUsage)));
        }
        Long totalMemory = getTotalMemory();
        if (totalMemory != null) {
            tsList.add(new BasicTsKvEntry(ts, new LongDataEntry("totalMemory", totalMemory)));
        }
        Long freeMemory = getFreeMemory();
        if (freeMemory != null) {
            tsList.add(new BasicTsKvEntry(ts, new LongDataEntry("freeMemory", freeMemory)));
        }
        Double cpuUsage = getCpuUsage();
        if (cpuUsage != null) {
            tsList.add(new BasicTsKvEntry(ts, new DoubleDataEntry("cpuUsage", cpuUsage)));
        }
        Double totalCpuUsage = getTotalCpuUsage();
        if (totalCpuUsage != null) {
            tsList.add(new BasicTsKvEntry(ts, new DoubleDataEntry("totalCpuUsage", totalCpuUsage)));
        }
        Long freeDiscSpace = getFreeDiscSpace();
        if (freeDiscSpace != null) {
            tsList.add(new BasicTsKvEntry(ts, new LongDataEntry("freeDiscSpace", freeDiscSpace)));
        }
        Long totalDiscSpace = getTotalDiscSpace();
        if (totalDiscSpace != null) {
            tsList.add(new BasicTsKvEntry(ts, new LongDataEntry("totalDiscSpace", totalDiscSpace)));
        }

        doSave(tsList);
    }

    private void doSave(List<TsKvEntry> telemetry) {
        ApiUsageState apiUsageState = apiUsageStateClient.getApiUsageState(TenantId.SYS_TENANT_ID);
        telemetryService.saveAndNotifyInternal(TenantId.SYS_TENANT_ID, apiUsageState.getId(), telemetry, CALLBACK);
    }

    private List<SystemInfoData> getSystemData(ServiceInfo serviceInfo) {
        List<SystemInfoData> clusterSystemData = new ArrayList<>();
        clusterSystemData.add(createSystemInfoData(serviceInfo));
        this.discoveryService.getOtherServers()
                .stream()
                .map(this::createSystemInfoData)
                .forEach(clusterSystemData::add);
        return clusterSystemData;
    }

    private SystemInfoData createSystemInfoData(ServiceInfo serviceInfo) {
        ProtocolStringList serviceTypes = serviceInfo.getServiceTypesList();
        SystemInfoData infoData = new SystemInfoData();
        infoData.setServiceId(serviceInfo.getServiceId());
        infoData.setServiceType(serviceTypes.size() > 1 ? "MONOLITH" : serviceTypes.get(0));
        infoData.setMemoryUsage(serviceInfo.getSystemInfo().getMemoryUsage());
        infoData.setTotalMemory(serviceInfo.getSystemInfo().getTotalMemory());
        infoData.setFreeMemory(serviceInfo.getSystemInfo().getFreeMemory());
        infoData.setCpuUsage(serviceInfo.getSystemInfo().getCpuUsage());
        infoData.setTotalCpuUsage(serviceInfo.getSystemInfo().getTotalCpuUsage());
        infoData.setFreeDiscSpace(serviceInfo.getSystemInfo().getFreeDiscSpace());
        infoData.setTotalDiscSpace(serviceInfo.getSystemInfo().getTotalDiscSpace());
        return infoData;
    }

    @PreDestroy
    private void destroy() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }
}

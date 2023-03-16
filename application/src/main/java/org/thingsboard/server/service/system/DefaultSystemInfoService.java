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
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.SystemInfo;
import org.thingsboard.server.common.data.SystemInfoData;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.gen.transport.TransportProtos.ServiceInfo;
import org.thingsboard.server.queue.discovery.DiscoveryService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.common.util.SystemUtil.getCpuUsage;
import static org.thingsboard.common.util.SystemUtil.getFreeDiscSpace;
import static org.thingsboard.common.util.SystemUtil.getMemoryUsage;

@TbCoreComponent
@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultSystemInfoService implements SystemInfoService {

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
    private final DiscoveryService discoveryService;
    private final TelemetrySubscriptionService telemetryService;
    private ScheduledExecutorService scheduler;

    @Value("${zk.enabled:false}")
    private boolean zkEnabled;

    @PostConstruct
    private void init() {
        if (!zkEnabled) {
            scheduler = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("tb-system-info-scheduler"));
            scheduler.scheduleAtFixedRate(this::saveCurrentSystemInfo, 0, 1, TimeUnit.MINUTES);
        }
    }

    @PreDestroy
    private void destroy() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    @Override
    public SystemInfo getSystemInfo() {
        SystemInfo systemInfo = new SystemInfo();

        ServiceInfo serviceInfo = serviceInfoProvider.getServiceInfoWithCurrentSystemInfo();

        if (zkEnabled) {
            List<SystemInfoData> clusterSystemData = new ArrayList<>();
            clusterSystemData.add(createSystemInfoData(serviceInfo));
            this.discoveryService.getOtherServers()
                    .stream()
                    .map(this::createSystemInfoData)
                    .forEach(clusterSystemData::add);
            systemInfo.setSystemData(clusterSystemData);
        } else {
            systemInfo.setMonolith(true);
            systemInfo.setSystemData(Collections.singletonList(createSystemInfoData(serviceInfo)));
        }

        return systemInfo;
    }

    protected void saveCurrentSystemInfo() {
        long ts = System.currentTimeMillis();
        List<TsKvEntry> tsList = new ArrayList<>();

        Long memoryUsage = getMemoryUsage();
        if (memoryUsage != null) {
            tsList.add(new BasicTsKvEntry(ts, new LongDataEntry("memoryUsage", memoryUsage)));
        }
        Double cpuUsage = getCpuUsage();
        if (cpuUsage != null) {
            tsList.add(new BasicTsKvEntry(ts, new DoubleDataEntry("cpuUsage", cpuUsage)));
        }
        Long freeDiscSpace = getFreeDiscSpace();
        if (freeDiscSpace != null) {
            tsList.add(new BasicTsKvEntry(ts, new LongDataEntry("freeDiscSpace", freeDiscSpace)));
        }

        telemetryService.saveAndNotifyInternal(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, tsList, CALLBACK);
    }

    private SystemInfoData createSystemInfoData(ServiceInfo serviceInfo) {
        ProtocolStringList serviceTypes = serviceInfo.getServiceTypesList();
        SystemInfoData infoData = new SystemInfoData();
        infoData.setServiceId(serviceInfo.getServiceId());
        infoData.setServiceType(serviceTypes.size() > 1 ? "MONOLITH" : serviceTypes.get(0));
        infoData.setMemUsage(serviceInfo.getSystemInfo().getMemoryUsage());
        infoData.setCpuUsage(serviceInfo.getSystemInfo().getCpuUsage());
        infoData.setFreeDiscSpace(serviceInfo.getSystemInfo().getFreeDiscSpace());
        return infoData;
    }

}

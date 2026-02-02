/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
import jakarta.annotation.Nullable;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.rule.engine.api.SmsService;
import org.thingsboard.rule.engine.api.TimeseriesSaveRequest;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.FeaturesInfo;
import org.thingsboard.server.common.data.SystemInfo;
import org.thingsboard.server.common.data.SystemInfoData;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.notification.rule.trigger.ResourcesShortageTrigger;
import org.thingsboard.server.common.data.notification.rule.trigger.ResourcesShortageTrigger.Resource;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.stats.TbApiUsageStateClient;
import org.thingsboard.server.dao.domain.DomainService;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.gen.transport.TransportProtos.ServiceInfo;
import org.thingsboard.server.queue.discovery.DiscoveryService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbApplicationEventListener;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.common.util.SystemUtil.getCpuCount;
import static org.thingsboard.common.util.SystemUtil.getCpuUsage;
import static org.thingsboard.common.util.SystemUtil.getDiscSpaceUsage;
import static org.thingsboard.common.util.SystemUtil.getMemoryUsage;
import static org.thingsboard.common.util.SystemUtil.getTotalDiscSpace;
import static org.thingsboard.common.util.SystemUtil.getTotalMemory;

@TbCoreComponent
@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultSystemInfoService extends TbApplicationEventListener<PartitionChangeEvent> implements SystemInfoService {

    public static final FutureCallback<Void> CALLBACK = new FutureCallback<>() {
        @Override
        public void onSuccess(@Nullable Void result) {
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
    private final AdminSettingsService adminSettingsService;
    private final DomainService domainService;
    private final MailService mailService;
    private final SmsService smsService;
    private final NotificationRuleProcessor notificationRuleProcessor;
    private volatile ScheduledExecutorService scheduler;

    @Value("${metrics.system_info.persist_frequency:60}")
    private int systemInfoPersistFrequencySeconds;
    @Value("#{${metrics.system_info.ttl:7} * 86400}")
    private int systemInfoTtlSeconds;

    @Override
    protected void onTbApplicationEvent(PartitionChangeEvent partitionChangeEvent) {
        if (ServiceType.TB_CORE.equals(partitionChangeEvent.getServiceType())) {
            boolean myPartition = partitionService.resolve(ServiceType.TB_CORE, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID).isMyPartition();
            synchronized (this) {
                if (myPartition) {
                    if (scheduler == null) {
                        scheduler = ThingsBoardExecutors.newSingleThreadScheduledExecutor("tb-system-info-scheduler");
                        scheduler.scheduleWithFixedDelay(this::saveCurrentSystemInfo, 0, systemInfoPersistFrequencySeconds, TimeUnit.SECONDS);
                    }
                } else {
                    destroy();
                }
            }
        }
    }

    @Override
    public SystemInfo getSystemInfo() {
        SystemInfo systemInfo = new SystemInfo();

        if (discoveryService.isMonolith()) {
            systemInfo.setMonolith(true);
            systemInfo.setSystemData(Collections.singletonList(createSystemInfoData(serviceInfoProvider.generateNewServiceInfoWithCurrentSystemInfo())));
        } else {
            systemInfo.setSystemData(getSystemData(serviceInfoProvider.getServiceInfo()));
        }

        return systemInfo;
    }

    protected void saveCurrentSystemInfo() {
        if (discoveryService.isMonolith()) {
            saveCurrentMonolithSystemInfo();
        } else {
            saveCurrentClusterSystemInfo();
        }
    }

    @Override
    public FeaturesInfo getFeaturesInfo() {
        FeaturesInfo featuresInfo = new FeaturesInfo();
        featuresInfo.setEmailEnabled(isEmailEnabled());
        featuresInfo.setSmsEnabled(smsService.isConfigured(TenantId.SYS_TENANT_ID));
        featuresInfo.setOauthEnabled(domainService.isOauth2Enabled(TenantId.SYS_TENANT_ID));
        featuresInfo.setTwoFaEnabled(isTwoFaEnabled());
        featuresInfo.setNotificationEnabled(isSlackEnabled());
        return featuresInfo;
    }

    private boolean isEmailEnabled() {
        try {
            mailService.testConnection(TenantId.SYS_TENANT_ID);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTwoFaEnabled() {
        AdminSettings twoFaSettings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "twoFaSettings");
        if (twoFaSettings != null) {
            var providers = twoFaSettings.getJsonValue().get("providers");
            if (providers != null) {
                return !providers.isEmpty();
            }
        }
        return false;
    }

    private boolean isSlackEnabled() {
        AdminSettings notifications = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "notifications");
        if (notifications != null) {
            return notifications.getJsonValue().get("deliveryMethodsConfigs").has("SLACK");
        }
        return false;
    }

    private void saveCurrentClusterSystemInfo() {
        long ts = System.currentTimeMillis();
        List<SystemInfoData> clusterSystemData = getSystemData(serviceInfoProvider.getServiceInfo());
        clusterSystemData.forEach(data -> {
            Arrays.stream(Resource.values()).forEach(resource -> {
                notificationRuleProcessor.process(ResourcesShortageTrigger.builder()
                        .resource(resource)
                        .serviceId(data.getServiceId())
                        .serviceType(data.getServiceType())
                        .usage(extractResourceUsage(data, resource))
                        .build());
            });
        });
        BasicTsKvEntry clusterDataKv = new BasicTsKvEntry(ts, new JsonDataEntry("clusterSystemData", JacksonUtil.toString(clusterSystemData)));
        doSave(Arrays.asList(new BasicTsKvEntry(ts, new BooleanDataEntry("clusterMode", true)), clusterDataKv));
    }

    private void saveCurrentMonolithSystemInfo() {
        long ts = System.currentTimeMillis();
        List<TsKvEntry> tsList = new ArrayList<>();
        tsList.add(new BasicTsKvEntry(ts, new BooleanDataEntry("clusterMode", false)));
        getCpuUsage().ifPresent(v -> {
            long value = (long) v;
            tsList.add(new BasicTsKvEntry(ts, new LongDataEntry("cpuUsage", value)));
            notificationRuleProcessor.process(ResourcesShortageTrigger.builder().resource(Resource.CPU).usage(value).serviceId(serviceInfoProvider.getServiceId()).serviceType(serviceInfoProvider.getServiceType()).build());
        });
        getMemoryUsage().ifPresent(v -> {
            long value = (long) v;
            tsList.add(new BasicTsKvEntry(ts, new LongDataEntry("memoryUsage", value)));
            notificationRuleProcessor.process(ResourcesShortageTrigger.builder().resource(Resource.RAM).usage(value).serviceId(serviceInfoProvider.getServiceId()).serviceType(serviceInfoProvider.getServiceType()).build());
        });
        getDiscSpaceUsage().ifPresent(v -> {
            long value = (long) v;
            tsList.add(new BasicTsKvEntry(ts, new LongDataEntry("discUsage", value)));
            notificationRuleProcessor.process(ResourcesShortageTrigger.builder().resource(Resource.STORAGE).usage(value).serviceId(serviceInfoProvider.getServiceId()).serviceType(serviceInfoProvider.getServiceType()).build());
        });

        getCpuCount().ifPresent(v -> tsList.add(new BasicTsKvEntry(ts, new LongDataEntry("cpuCount", (long) v))));
        getTotalMemory().ifPresent(v -> tsList.add(new BasicTsKvEntry(ts, new LongDataEntry("totalMemory", v))));
        getTotalDiscSpace().ifPresent(v -> tsList.add(new BasicTsKvEntry(ts, new LongDataEntry("totalDiscSpace", v))));

        doSave(tsList);
    }

    private void doSave(List<TsKvEntry> telemetry) {
        ApiUsageState apiUsageState = apiUsageStateClient.getApiUsageState(TenantId.SYS_TENANT_ID);
        telemetryService.saveTimeseriesInternal(TimeseriesSaveRequest.builder()
                .tenantId(TenantId.SYS_TENANT_ID)
                .entityId(apiUsageState.getId())
                .entries(telemetry)
                .ttl(systemInfoTtlSeconds)
                .callback(CALLBACK)
                .build());
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

        infoData.setCpuUsage(serviceInfo.getSystemInfo().getCpuUsage());
        infoData.setMemoryUsage(serviceInfo.getSystemInfo().getMemoryUsage());
        infoData.setDiscUsage(serviceInfo.getSystemInfo().getDiskUsage());

        infoData.setCpuCount(serviceInfo.getSystemInfo().getCpuCount());
        infoData.setTotalMemory(serviceInfo.getSystemInfo().getTotalMemory());
        infoData.setTotalDiscSpace(serviceInfo.getSystemInfo().getTotalDiscSpace());

        return infoData;
    }

    private Long extractResourceUsage(SystemInfoData info, Resource resource) {
        return switch (resource) {
            case CPU -> info.getCpuUsage();
            case RAM -> info.getMemoryUsage();
            case STORAGE -> info.getDiscUsage();
        };
    }

    @PreDestroy
    private void destroy() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

}

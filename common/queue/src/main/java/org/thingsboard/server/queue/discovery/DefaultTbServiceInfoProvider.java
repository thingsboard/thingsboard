/**
 * Copyright © 2016-2025 The Thingsboard Authors
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

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TbTransportService;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.util.CollectionsUtil;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ServiceInfo;
import org.thingsboard.server.queue.edqs.EdqsConfig;
import org.thingsboard.server.queue.task.TaskProcessor;
import org.thingsboard.server.queue.util.AfterContextReady;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.thingsboard.common.util.SystemUtil.getCpuCount;
import static org.thingsboard.common.util.SystemUtil.getCpuUsage;
import static org.thingsboard.common.util.SystemUtil.getDiscSpaceUsage;
import static org.thingsboard.common.util.SystemUtil.getMemoryUsage;
import static org.thingsboard.common.util.SystemUtil.getTotalDiscSpace;
import static org.thingsboard.common.util.SystemUtil.getTotalMemory;


@Component
@Slf4j
public class DefaultTbServiceInfoProvider implements TbServiceInfoProvider {

    @Getter
    @Value("${service.id:#{null}}")
    private String serviceId;

    @Getter
    @Value("${service.type:monolith}")
    private String serviceType;

    @Getter
    @Value("${service.rule_engine.assigned_tenant_profiles:}")
    private Set<UUID> assignedTenantProfiles;

    @Autowired(required = false)
    private EdqsConfig edqsConfig;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired(required = false)
    private List<TaskProcessor<?, ?>> availableTaskProcessors;

    private List<ServiceType> serviceTypes;
    private List<JobType> taskTypes;
    private ServiceInfo serviceInfo;

    private boolean ready = true;

    @PostConstruct
    public void init() {
        if (StringUtils.isEmpty(serviceId)) {
            try {
                serviceId = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                serviceId = StringUtils.randomAlphabetic(10);
            }
        }
        log.info("Current Service ID: {}", serviceId);
        if (serviceType.equalsIgnoreCase("monolith")) {
            serviceTypes = List.of(ServiceType.values());
        } else {
            serviceTypes = Collections.singletonList(ServiceType.of(serviceType));
        }
        if (!serviceTypes.contains(ServiceType.TB_RULE_ENGINE) || assignedTenantProfiles == null) {
            assignedTenantProfiles = Collections.emptySet();
        }
        if (serviceTypes.contains(ServiceType.EDQS)) {
            ready = false;
            if (StringUtils.isBlank(edqsConfig.getLabel())) {
                edqsConfig.setLabel(serviceId);
            }
        }
        if (CollectionsUtil.isNotEmpty(availableTaskProcessors)) {
            taskTypes = availableTaskProcessors.stream()
                    .map(TaskProcessor::getJobType)
                    .toList();
        } else {
            taskTypes = Collections.emptyList();
        }

        generateNewServiceInfoWithCurrentSystemInfo();
    }

    @AfterContextReady
    public void setTransports() {
        serviceInfo = ServiceInfo.newBuilder(serviceInfo)
                .addAllTransports(getTransportServices().stream()
                        .map(TbTransportService::getName)
                        .collect(Collectors.toSet()))
                .build();
    }

    private Collection<TbTransportService> getTransportServices() {
        return applicationContext.getBeansOfType(TbTransportService.class).values();
    }

    @Override
    public ServiceInfo getServiceInfo() {
        return serviceInfo;
    }

    @Override
    public boolean isService(ServiceType serviceType) {
        return serviceTypes.contains(serviceType);
    }

    @Override
    public ServiceInfo generateNewServiceInfoWithCurrentSystemInfo() {
        ServiceInfo.Builder builder = ServiceInfo.newBuilder()
                .setServiceId(serviceId)
                .addAllServiceTypes(serviceTypes.stream().map(ServiceType::name).collect(Collectors.toList()))
                .setSystemInfo(getCurrentSystemInfoProto());
        if (CollectionsUtil.isNotEmpty(assignedTenantProfiles)) {
            builder.addAllAssignedTenantProfiles(assignedTenantProfiles.stream().map(UUID::toString).collect(Collectors.toList()));
        }
        if (edqsConfig != null) {
            builder.setLabel(edqsConfig.getLabel());
        }
        builder.setReady(ready);
        builder.addAllTaskTypes(taskTypes.stream().map(JobType::name).toList());
        return serviceInfo = builder.build();
    }

    @Override
    public boolean setReady(boolean ready) {
        boolean changed = this.ready != ready;
        this.ready = ready;
        return changed;
    }

    private TransportProtos.SystemInfoProto getCurrentSystemInfoProto() {
        TransportProtos.SystemInfoProto.Builder builder = TransportProtos.SystemInfoProto.newBuilder();

        getCpuUsage().ifPresent(builder::setCpuUsage);
        getMemoryUsage().ifPresent(builder::setMemoryUsage);
        getDiscSpaceUsage().ifPresent(builder::setDiskUsage);

        getCpuCount().ifPresent(builder::setCpuCount);
        getTotalMemory().ifPresent(builder::setTotalMemory);
        getTotalDiscSpace().ifPresent(builder::setTotalDiscSpace);

        return builder.build();
    }

}

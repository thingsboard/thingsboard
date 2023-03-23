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
package org.thingsboard.server.queue.discovery;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TbTransportService;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ServiceInfo;
import org.thingsboard.server.queue.util.AfterContextReady;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.thingsboard.common.util.SystemUtil.getCpuUsage;
import static org.thingsboard.common.util.SystemUtil.getFreeDiscSpace;
import static org.thingsboard.common.util.SystemUtil.getFreeMemory;
import static org.thingsboard.common.util.SystemUtil.getMemoryUsage;
import static org.thingsboard.common.util.SystemUtil.getTotalCpuUsage;
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

    @Autowired
    private ApplicationContext applicationContext;

    private List<ServiceType> serviceTypes;
    private ServiceInfo serviceInfo;

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
            serviceTypes = Collections.unmodifiableList(Arrays.asList(ServiceType.values()));
        } else {
            serviceTypes = Collections.singletonList(ServiceType.of(serviceType));
        }

        serviceInfo = getServiceInfoWithCurrentSystemInfo();
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
    public ServiceInfo getServiceInfoWithCurrentSystemInfo() {
        ServiceInfo.Builder builder = ServiceInfo.newBuilder()
                .setServiceId(serviceId)
                .addAllServiceTypes(serviceTypes.stream().map(ServiceType::name).collect(Collectors.toList()))
                .setSystemInfo(getCurrentSystemInfoProto());

        return builder.build();
    }

    private TransportProtos.SystemInfoProto getCurrentSystemInfoProto() {
        TransportProtos.SystemInfoProto.Builder builder = TransportProtos.SystemInfoProto.newBuilder();

        getMemoryUsage().ifPresent(builder::setMemoryUsage);
        getTotalMemory().ifPresent(builder::setTotalMemory);
        getFreeMemory().ifPresent(builder::setFreeMemory);
        getCpuUsage().ifPresent(builder::setCpuUsage);
        getTotalCpuUsage().ifPresent(builder::setTotalCpuUsage);
        getFreeDiscSpace().ifPresent(builder::setFreeDiscSpace);
        getTotalDiscSpace().ifPresent(builder::setTotalDiscSpace);

        return builder.build();
    }

}

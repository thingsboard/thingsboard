// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue.discovery.event;

import lombok.Getter;
import org.thingsboard.server.common.msg.queue.ServiceType;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class OtherServiceShutdownEvent extends TbApplicationEvent {

    private static final long serialVersionUID = -2441739930040282254L;

    @Getter
    private final String serviceId;
    @Getter
    private final Set<ServiceType> serviceTypes;

    public OtherServiceShutdownEvent(Object source, String serviceId, List<String> serviceTypes) {
        super(source);
        this.serviceId = serviceId;
        this.serviceTypes = serviceTypes.stream().map(ServiceType::valueOf).collect(Collectors.toSet());
    }
}

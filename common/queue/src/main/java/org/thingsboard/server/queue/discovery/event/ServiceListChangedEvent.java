// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue.discovery.event;

import lombok.Getter;
import lombok.ToString;
import org.thingsboard.server.gen.transport.TransportProtos.ServiceInfo;

import java.util.List;

@Getter
@ToString
public class ServiceListChangedEvent extends TbApplicationEvent {
    private final List<ServiceInfo> otherServices;
    private final ServiceInfo currentService;

    public ServiceListChangedEvent(List<ServiceInfo> otherServices, ServiceInfo currentService) {
        super(otherServices);
        this.otherServices = otherServices;
        this.currentService = currentService;
    }
}

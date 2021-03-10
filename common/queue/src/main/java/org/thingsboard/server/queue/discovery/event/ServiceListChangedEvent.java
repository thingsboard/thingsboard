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
package org.thingsboard.server.queue.discovery.event;

import lombok.Getter;
import org.thingsboard.server.gen.transport.TransportProtos.ServiceInfo;

import java.util.List;

@Getter
public class ServiceListChangedEvent {
    private final List<ServiceInfo> serviceList;
    private final ServiceInfo currentService;
    private ServiceInfo changedService;

    public ServiceListChangedEvent(List<ServiceInfo> serviceList, ServiceInfo currentService) {
        this.serviceList = serviceList;
        this.currentService = currentService;
    }

    public ServiceListChangedEvent(List<ServiceInfo> serviceList, ServiceInfo currentService, ServiceInfo changedService) {
        this.serviceList = serviceList;
        this.currentService = currentService;
        this.changedService = changedService;
    }
}

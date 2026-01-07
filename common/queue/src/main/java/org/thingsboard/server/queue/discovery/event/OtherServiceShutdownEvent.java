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

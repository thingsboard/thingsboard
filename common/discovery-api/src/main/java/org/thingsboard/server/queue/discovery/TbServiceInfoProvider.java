// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue.discovery;

import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.gen.transport.TransportProtos.ServiceInfo;

import java.util.Set;
import java.util.UUID;

public interface TbServiceInfoProvider {

    String getServiceId();

    String getServiceType();

    ServiceInfo getServiceInfo();

    boolean isMonolith();

    boolean isService(ServiceType serviceType);

    ServiceInfo generateNewServiceInfoWithCurrentSystemInfo();

    Set<UUID> getAssignedTenantProfiles();

    boolean setReady(boolean ready);

}

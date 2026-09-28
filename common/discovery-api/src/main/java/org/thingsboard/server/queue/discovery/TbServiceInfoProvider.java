// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.queue.discovery;

import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.gen.transport.TransportProtos.ServiceInfo;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface TbServiceInfoProvider {

    String getServiceId();

    String getServiceType();

    ServiceInfo getServiceInfo();

    boolean isMonolith();

    boolean isService(ServiceType serviceType);

    ServiceInfo generateNewServiceInfoWithCurrentSystemInfo();

    List<IntegrationType> getSupportedIntegrationTypes();

    Set<UUID> getAssignedTenantProfiles();

    boolean setReady(boolean ready);

}

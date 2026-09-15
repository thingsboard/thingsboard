// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.transport;

import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Optional;

public interface TransportResourceCache {

    Optional<TbResource> get(TenantId tenantId, ResourceType resourceType, String resourceId);

    void update(TenantId tenantId, ResourceType resourceType, String resourceI);

    void evict(TenantId tenantId, ResourceType resourceType, String resourceId);
}

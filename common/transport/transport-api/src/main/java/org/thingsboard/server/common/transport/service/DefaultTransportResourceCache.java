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
package org.thingsboard.server.common.transport.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.Resource;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.transport.TransportResourceCache;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.util.DataDecodingEncodingService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbTransportComponent;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
@TbTransportComponent
public class DefaultTransportResourceCache implements TransportResourceCache {

    private final Lock resourceFetchLock = new ReentrantLock();
    private final ConcurrentMap<ResourceKey, Resource> resources = new ConcurrentHashMap<>();
    private final Set<ResourceKey> keys = ConcurrentHashMap.newKeySet();
    private final DataDecodingEncodingService dataDecodingEncodingService;
    private final TransportService transportService;

    public DefaultTransportResourceCache(DataDecodingEncodingService dataDecodingEncodingService, @Lazy TransportService transportService) {
        this.dataDecodingEncodingService = dataDecodingEncodingService;
        this.transportService = transportService;
    }

    @Override
    public Resource get(TenantId tenantId, ResourceType resourceType, String resourceId) {
        ResourceKey resourceKey = new ResourceKey(tenantId, resourceType, resourceId);
        Resource resource;

        if (keys.contains(resourceKey)) {
            resource = resources.get(resourceKey);
            if (resource == null) {
                resource = resources.get(resourceKey.getSystemKey());
            }
        } else {
            resourceFetchLock.lock();
            try {
                if (keys.contains(resourceKey)) {
                    resource = resources.get(resourceKey);
                    if (resource == null) {
                        resource = resources.get(resourceKey.getSystemKey());
                    }
                } else {
                    resource = fetchResource(resourceKey);
                    keys.add(resourceKey);
                }
            } finally {
                resourceFetchLock.unlock();
            }
        }

        return resource;
    }

    private Resource fetchResource(ResourceKey resourceKey) {
        UUID tenantId = resourceKey.getTenantId().getId();
        TransportProtos.GetResourceRequestMsg.Builder builder = TransportProtos.GetResourceRequestMsg.newBuilder();
        builder
                .setTenantIdLSB(tenantId.getLeastSignificantBits())
                .setTenantIdMSB(tenantId.getMostSignificantBits())
                .setResourceType(resourceKey.resourceType.name())
                .setResourceId(resourceKey.resourceId);
        TransportProtos.GetResourceResponseMsg responseMsg = transportService.getResource(builder.build());

        Optional<Resource> optionalResource = dataDecodingEncodingService.decode(responseMsg.getResource().toByteArray());
        if (optionalResource.isPresent()) {
            Resource resource = optionalResource.get();
            resources.put(new ResourceKey(resource.getTenantId(), resource.getResourceType(), resource.getResourceId()), resource);
            return resource;
        }

        return null;
    }

    @Override
    public void update(TenantId tenantId, ResourceType resourceType, String resourceId) {
        ResourceKey resourceKey = new ResourceKey(tenantId, resourceType, resourceId);
        if (keys.contains(resourceKey) || resources.containsKey(resourceKey)) {
            fetchResource(resourceKey);
        }
    }

    @Override
    public void evict(TenantId tenantId, ResourceType resourceType, String resourceId) {
        ResourceKey resourceKey = new ResourceKey(tenantId, resourceType, resourceId);
        keys.remove(resourceKey);
        resources.remove(resourceKey);
    }

    @Data
    private static class ResourceKey {
        private final TenantId tenantId;
        private final ResourceType resourceType;
        private final String resourceId;

        public ResourceKey getSystemKey() {
            return new ResourceKey(TenantId.SYS_TENANT_ID, resourceType, resourceId);
        }
    }
}

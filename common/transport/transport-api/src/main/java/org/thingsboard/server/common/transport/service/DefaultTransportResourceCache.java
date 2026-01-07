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
package org.thingsboard.server.common.transport.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.transport.TransportResourceCache;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.util.ProtoUtils;
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
@RequiredArgsConstructor
public class DefaultTransportResourceCache implements TransportResourceCache {

    private final Lock resourceFetchLock = new ReentrantLock();
    private final ConcurrentMap<ResourceCompositeKey, TbResource> resources = new ConcurrentHashMap<>();
    private final Set<ResourceCompositeKey> keys = ConcurrentHashMap.newKeySet();
    @Lazy
    private final TransportService transportService;

    @Override
    public Optional<TbResource> get(TenantId tenantId, ResourceType resourceType, String resourceKey) {
        ResourceCompositeKey compositeKey = new ResourceCompositeKey(tenantId, resourceType, resourceKey);
        TbResource resource;

        if (keys.contains(compositeKey)) {
            resource = resources.get(compositeKey);
            if (resource == null) {
                resource = resources.get(compositeKey.getSystemKey());
            }
        } else {
            resourceFetchLock.lock();
            try {
                if (keys.contains(compositeKey)) {
                    resource = resources.get(compositeKey);
                    if (resource == null) {
                        resource = resources.get(compositeKey.getSystemKey());
                    }
                } else {
                    resource = fetchResource(compositeKey);
                    keys.add(compositeKey);
                }
            } finally {
                resourceFetchLock.unlock();
            }
        }

        return Optional.ofNullable(resource);
    }

    private TbResource fetchResource(ResourceCompositeKey compositeKey) {
        UUID tenantId = compositeKey.getTenantId().getId();
        TransportProtos.GetResourceRequestMsg.Builder builder = TransportProtos.GetResourceRequestMsg.newBuilder();
        builder
                .setTenantIdLSB(tenantId.getLeastSignificantBits())
                .setTenantIdMSB(tenantId.getMostSignificantBits())
                .setResourceType(compositeKey.resourceType.name())
                .setResourceKey(compositeKey.resourceKey);
        TransportProtos.GetResourceResponseMsg responseMsg = transportService.getResource(builder.build());

        if (responseMsg.hasResource()) {
            TbResource resource = ProtoUtils.fromProto(responseMsg.getResource());
            resources.put(new ResourceCompositeKey(resource.getTenantId(), resource.getResourceType(), resource.getResourceKey()), resource);
            return resource;
        }

        return null;
    }

    @Override
    public void update(TenantId tenantId, ResourceType resourceType, String resourceKey) {
        ResourceCompositeKey compositeKey = new ResourceCompositeKey(tenantId, resourceType, resourceKey);
        if (keys.contains(compositeKey) || resources.containsKey(compositeKey)) {
            fetchResource(compositeKey);
        }
    }

    @Override
    public void evict(TenantId tenantId, ResourceType resourceType, String resourceKey) {
        ResourceCompositeKey compositeKey = new ResourceCompositeKey(tenantId, resourceType, resourceKey);
        keys.remove(compositeKey);
        resources.remove(compositeKey);
    }

    @Data
    private static class ResourceCompositeKey {
        private final TenantId tenantId;
        private final ResourceType resourceType;
        private final String resourceKey;

        public ResourceCompositeKey getSystemKey() {
            return new ResourceCompositeKey(TenantId.SYS_TENANT_ID, resourceType, resourceKey);
        }
    }
}

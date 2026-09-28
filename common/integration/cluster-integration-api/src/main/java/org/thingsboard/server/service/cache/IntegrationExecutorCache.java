// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.cache;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.gen.integration.IntegrationInfoProto;
import org.thingsboard.server.service.data.EntityCacheKey;
import org.thingsboard.server.service.data.EntityUplinkData;

import java.util.UUID;

public interface IntegrationExecutorCache<Entity extends HasName> {

    default ListenableFuture<Entity> getProfile(UUID id, EntityCacheKey key) {
        return Futures.immediateFuture(null);
    }

    default ListenableFuture<Entity> getEntity(IntegrationInfoProto proto, EntityUplinkData entityUplinkData) {
        return Futures.immediateFuture(null);
    }

    void put(EntityCacheKey key, Entity entity);

    void evict(UUID id);

}

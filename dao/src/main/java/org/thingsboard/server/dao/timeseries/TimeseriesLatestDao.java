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
package org.thingsboard.server.dao.timeseries;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.DeleteTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.kv.TsKvLatestRemovingResult;

import java.util.List;
import java.util.Optional;

public interface TimeseriesLatestDao {

    /**
     * Optional TsKvEntry if the value is present in the DB
     *
     */
    ListenableFuture<Optional<TsKvEntry>> findLatestOpt(TenantId tenantId, EntityId entityId, String key);

    /**
     * Returns new BasicTsKvEntry(System.currentTimeMillis(), new StringDataEntry(key, null)) if the value is NOT present in the DB
     *
     */
    ListenableFuture<TsKvEntry> findLatest(TenantId tenantId, EntityId entityId, String key);

    ListenableFuture<List<TsKvEntry>> findAllLatest(TenantId tenantId, EntityId entityId);

    ListenableFuture<Long> saveLatest(TenantId tenantId, EntityId entityId, TsKvEntry tsKvEntry);

    ListenableFuture<TsKvLatestRemovingResult> removeLatest(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query);

    List<String> findAllKeysByDeviceProfileId(TenantId tenantId, DeviceProfileId deviceProfileId);

    List<String> findAllKeysByEntityIds(TenantId tenantId, List<EntityId> entityIds);

    ListenableFuture<List<String>> findAllKeysByEntityIdsAsync(TenantId tenantId, List<EntityId> entityIds);

    /**
     * For each unique timeseries key across the given entities, returns the single most recent {@link TsKvEntry}
     * (i.e. the entry with the highest timestamp). If the same key exists on multiple entities,
     * only the freshest value is kept. Useful for discovering available keys together with a representative sample value.
     */
    List<TsKvEntry> findLatestByEntityIds(TenantId tenantId, List<EntityId> entityIds);

    ListenableFuture<List<TsKvEntry>> findLatestByEntityIdsAsync(TenantId tenantId, List<EntityId> entityIds);

}

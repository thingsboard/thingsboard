/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.rule.engine.util;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import org.apache.commons.collections.CollectionUtils;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.data.DeviceRelationsQuery;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.device.DeviceSearchQuery;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;
import org.thingsboard.server.dao.device.DeviceService;

import java.util.List;

public class EntitiesRelatedDeviceIdAsyncLoader {

    public static ListenableFuture<DeviceId> findDeviceAsync(TbContext ctx, EntityId originator,
                                                             DeviceRelationsQuery deviceRelationsQuery) {
        DeviceService deviceService = ctx.getDeviceService();
        DeviceSearchQuery query = buildQuery(originator, deviceRelationsQuery);

        ListenableFuture<List<Device>> asyncDevices = deviceService.findDevicesByQuery(ctx.getTenantId(), query);

        return Futures.transformAsync(asyncDevices, d -> CollectionUtils.isNotEmpty(d) ? Futures.immediateFuture(d.get(0).getId())
                : Futures.immediateFuture(null), MoreExecutors.directExecutor());
    }

    private static DeviceSearchQuery buildQuery(EntityId originator, DeviceRelationsQuery deviceRelationsQuery) {
        DeviceSearchQuery query = new DeviceSearchQuery();
        RelationsSearchParameters parameters = new RelationsSearchParameters(originator,
                deviceRelationsQuery.getDirection(), deviceRelationsQuery.getMaxLevel(), deviceRelationsQuery.isFetchLastLevelOnly());
        query.setParameters(parameters);
        query.setRelationType(deviceRelationsQuery.getRelationType());
        query.setDeviceTypes(deviceRelationsQuery.getDeviceTypes());
        return query;
    }
}

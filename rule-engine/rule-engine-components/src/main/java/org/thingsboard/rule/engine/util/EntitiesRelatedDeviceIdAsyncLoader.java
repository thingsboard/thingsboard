// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.util;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.commons.collections4.CollectionUtils;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.data.DeviceRelationsQuery;
import org.thingsboard.server.common.data.device.DeviceSearchQuery;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;

public class EntitiesRelatedDeviceIdAsyncLoader {

    public static ListenableFuture<DeviceId> findDeviceAsync(
            TbContext ctx,
            EntityId originator,
            DeviceRelationsQuery deviceRelationsQuery
    ) {
        var deviceService = ctx.getDeviceService();
        var query = buildQuery(originator, deviceRelationsQuery);
        var devicesListFuture = deviceService.findDevicesByQuery(ctx.getTenantId(), query);
        return Futures.transformAsync(devicesListFuture,
                deviceList -> CollectionUtils.isNotEmpty(deviceList) ?
                        Futures.immediateFuture(deviceList.get(0).getId())
                        : Futures.immediateFuture(null), ctx.getDbCallbackExecutor());
    }

    private static DeviceSearchQuery buildQuery(EntityId originator, DeviceRelationsQuery deviceRelationsQuery) {
        var query = new DeviceSearchQuery();
        var parameters = new RelationsSearchParameters(
                originator,
                deviceRelationsQuery.getDirection(),
                deviceRelationsQuery.getMaxLevel(),
                deviceRelationsQuery.isFetchLastLevelOnly()
        );
        query.setParameters(parameters);
        query.setRelationType(deviceRelationsQuery.getRelationType());
        query.setDeviceTypes(deviceRelationsQuery.getDeviceTypes());
        return query;
    }

}

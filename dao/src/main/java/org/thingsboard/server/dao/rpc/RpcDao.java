// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.rpc;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rpc.Rpc;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.dao.Dao;

public interface RpcDao extends Dao<Rpc> {

    /**
     * Idempotent by rpcId: a re-delivered command must not overwrite the row created by the first delivery.
     *
     * @return true if a row was inserted, false if a row with this id already existed
     */
    ListenableFuture<Boolean> createIfAbsentAsync(Rpc rpc);

    ListenableFuture<Boolean> updateAsync(Rpc rpc);

    PageData<Rpc> findAllByDeviceId(TenantId tenantId, DeviceId deviceId, PageLink pageLink);

    PageData<Rpc> findAllByDeviceIdAndStatus(TenantId tenantId, DeviceId deviceId, RpcStatus rpcStatus, PageLink pageLink);

    PageData<Rpc> findInFlightForReload(TenantId tenantId, DeviceId deviceId, PageLink pageLink);

    PageData<Rpc> findAllRpcByTenantId(TenantId tenantId, PageLink pageLink);

    int deleteOutdatedRpcByTenantIdBatch(TenantId tenantId, Long expirationTime, int batchSize);

}

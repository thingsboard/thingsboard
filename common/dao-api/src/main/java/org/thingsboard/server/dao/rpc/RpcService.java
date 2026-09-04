// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.rpc;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.RpcId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rpc.Rpc;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.dao.entity.EntityDaoService;

public interface RpcService extends EntityDaoService {

    Rpc save(Rpc rpc);

    void deleteRpc(TenantId tenantId, RpcId id);

    void deleteAllRpcByTenantId(TenantId tenantId);

    Rpc findById(TenantId tenantId, RpcId id);

    ListenableFuture<Rpc> findRpcByIdAsync(TenantId tenantId, RpcId id);

    PageData<Rpc> findAllByDeviceId(TenantId tenantId, DeviceId deviceId, PageLink pageLink);

    PageData<Rpc> findAllByDeviceIdAndStatus(TenantId tenantId, DeviceId deviceId, RpcStatus rpcStatus, PageLink pageLink);
}

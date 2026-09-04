// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.resource;

import com.google.common.util.concurrent.FluentFuture;
import org.thingsboard.server.common.data.TbResourceDataInfo;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;

public interface TbResourceDataCache {

    FluentFuture<TbResourceDataInfo> getResourceDataInfoAsync(TenantId tenantId, TbResourceId resourceId);

    void evictResourceData(TenantId tenantId, TbResourceId resourceId);
}

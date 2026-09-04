// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.vc.service;

import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.queue.discovery.TenantRoutingInfo;
import org.thingsboard.server.queue.discovery.TenantRoutingInfoService;

@Service
public class VersionControlTenantRoutingInfoService implements TenantRoutingInfoService {
    @Override
    public TenantRoutingInfo getRoutingInfo(TenantId tenantId) {
        //This dummy implementation is ok since Version Control service does not produce any rule engine messages.
        return new TenantRoutingInfo(tenantId, null, false);
    }
}

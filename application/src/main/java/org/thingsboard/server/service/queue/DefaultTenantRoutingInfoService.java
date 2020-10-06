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
package org.thingsboard.server.service.queue;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.tenant.TenantProfileService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.queue.discovery.TenantRoutingInfo;
import org.thingsboard.server.queue.discovery.TenantRoutingInfoService;

@Slf4j
@Service
@ConditionalOnExpression("'${service.type:null}'=='monolith' || '${service.type:null}'=='tb-core' || '${service.type:null}'=='tb-rule-engine'")
public class DefaultTenantRoutingInfoService implements TenantRoutingInfoService {

    private final TenantService tenantService;

    private final TenantProfileService tenantProfileService;

    public DefaultTenantRoutingInfoService(TenantService tenantService, TenantProfileService tenantProfileService) {
        this.tenantService = tenantService;
        this.tenantProfileService = tenantProfileService;
    }

    @Override
    public TenantRoutingInfo getRoutingInfo(TenantId tenantId) {
        Tenant tenant = tenantService.findTenantById(tenantId);
        if (tenant != null) {
            // TODO: Tenant Profile from cache
            TenantProfile tenantProfile = tenantProfileService.findTenantProfileById(tenantId, tenant.getTenantProfileId());
            return new TenantRoutingInfo(tenantId, tenantProfile.isIsolatedTbCore(), tenantProfile.isIsolatedTbRuleEngine());
        } else {
            throw new RuntimeException("Tenant not found!");
        }
    }
}

/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.dao.service.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.dao.asset.AssetProfileDao;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;

@Component
public class AssetProfileDataValidator extends DataValidator<AssetProfile> {

    @Autowired
    private AssetProfileDao assetProfileDao;
    @Autowired
    @Lazy
    private AssetProfileService assetProfileService;
    @Autowired
    private TenantService tenantService;
    @Lazy
    @Autowired
    private QueueService queueService;
    @Autowired
    private RuleChainService ruleChainService;
    @Autowired
    private DashboardService dashboardService;

    @Override
    protected void validateDataImpl(TenantId tenantId, AssetProfile assetProfile) {
        validateString("Asset profile name", assetProfile.getName());
        if (assetProfile.getTenantId() == null) {
            throw new DataValidationException("Asset profile should be assigned to tenant!");
        } else {
            if (!tenantService.tenantExists(assetProfile.getTenantId())) {
                throw new DataValidationException("Asset profile is referencing to non-existent tenant!");
            }
        }
        if (assetProfile.isDefault()) {
            AssetProfile defaultAssetProfile = assetProfileService.findDefaultAssetProfile(tenantId);
            if (defaultAssetProfile != null && !defaultAssetProfile.getId().equals(assetProfile.getId())) {
                throw new DataValidationException("Another default asset profile is present in scope of current tenant!");
            }
        }
        if (StringUtils.isNotEmpty(assetProfile.getDefaultQueueName())) {
            Queue queue = queueService.findQueueByTenantIdAndName(tenantId, assetProfile.getDefaultQueueName());
            if (queue == null) {
                throw new DataValidationException("Asset profile is referencing to non-existent queue!");
            }
        }

        if (assetProfile.getDefaultRuleChainId() != null) {
            RuleChain ruleChain = ruleChainService.findRuleChainById(tenantId, assetProfile.getDefaultRuleChainId());
            if (ruleChain == null) {
                throw new DataValidationException("Can't assign non-existent rule chain!");
            }
            if (!ruleChain.getTenantId().equals(assetProfile.getTenantId())) {
                throw new DataValidationException("Can't assign rule chain from different tenant!");
            }
        }

        if (assetProfile.getDefaultDashboardId() != null) {
            DashboardInfo dashboard = dashboardService.findDashboardInfoById(tenantId, assetProfile.getDefaultDashboardId());
            if (dashboard == null) {
                throw new DataValidationException("Can't assign non-existent dashboard!");
            }
            if (!dashboard.getTenantId().equals(assetProfile.getTenantId())) {
                throw new DataValidationException("Can't assign dashboard from different tenant!");
            }
        }
    }

    @Override
    protected AssetProfile validateUpdate(TenantId tenantId, AssetProfile assetProfile) {
        AssetProfile old = assetProfileDao.findById(assetProfile.getTenantId(), assetProfile.getId().getId());
        if (old == null) {
            throw new DataValidationException("Can't update non existing asset profile!");
        }
        return old;
    }

}

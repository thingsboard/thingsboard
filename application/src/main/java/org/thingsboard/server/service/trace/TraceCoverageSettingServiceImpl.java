/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2026 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.server.service.trace;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.trace.TraceCoverageSetting;
import org.thingsboard.server.dao.sql.tracecoverage.TraceCoverageSettingDao;
import org.thingsboard.server.exception.DataValidationException;

@Service
@Slf4j
@RequiredArgsConstructor
public class TraceCoverageSettingServiceImpl implements TraceCoverageSettingService {
    private final TraceCoverageSettingDao traceCoverageSettingDao;
    private final TbClusterService clusterService;

    @Override
    public TraceCoverageSetting findTraceCoverageSettingByTenantId(TenantId tenantId) {
        log.trace("Executing findTraceCoverageSettingByTenantId [{}]", tenantId);
        validateTenantId(tenantId);
        TraceCoverageSetting setting = traceCoverageSettingDao.findByTenantId(tenantId.getId());
        return setting != null ? setting : TraceCoverageSetting.defaultSetting(tenantId.getId());
    }

    @Override
    public TraceCoverageSetting saveTraceCoverageSetting(TenantId tenantId, TraceCoverageSetting setting) {
        log.trace("Executing saveTraceCoverageSetting [{}][{}]", tenantId, setting);
        validateTenantId(tenantId);
        validate(setting);

        // The id and created time are never taken from the request: the row is found by tenant.
        TraceCoverageSetting saved = traceCoverageSettingDao.upsertByTenantId(tenantId.getId(), setting);
        broadcastCacheInvalidation(ComponentLifecycleEvent.UPDATED);
        return saved;
    }

    private void broadcastCacheInvalidation(ComponentLifecycleEvent event) {
        clusterService.broadcastEntityStateChangeEvent(TenantId.SYS_TENANT_ID,
                new AssetId(TraceCoverageSetting.SETTINGS_ASSET_ID), event);
    }

    private void validateTenantId(TenantId tenantId) {
        if (tenantId == null || tenantId.getId() == null) {
            throw new DataValidationException("Tenant id should be specified for trace coverage setting!");
        }
    }

    private void validate(TraceCoverageSetting setting) {
        if (setting == null) {
            throw new DataValidationException("Trace coverage setting should be specified!");
        }
        if (setting.getIntervalSeconds() < 1) {
            throw new DataValidationException("Trace coverage setting intervalSeconds should be a positive integer!");
        }
        if (setting.getMessagePayloadRecording() == null) {
            throw new DataValidationException("Trace coverage setting messagePayloadRecording should be specified (NONE, FIRST_SPAN or ALL_SPANS)!");
        }
        if (setting.getTracesPerInterval() < 1) {
            throw new DataValidationException("Trace coverage setting tracesPerInterval should be a positive integer!");
        }
        if (setting.getTracesPerPack() < 0) {
            throw new DataValidationException("Trace coverage setting tracesPerPack should be zero (unlimited) or a positive integer!");
        }
        if (setting.getSwitchPeriodSeconds() < 1) {
            throw new DataValidationException("Trace coverage setting switchPeriodSeconds should be a positive integer!");
        }
        if (setting.getRelatedTraceSampleIntervalSeconds() < 1) {
            throw new DataValidationException("Trace coverage setting relatedTraceSampleIntervalSeconds should be a positive integer!");
        }
        if (setting.getMaxTraceGroupsPerDay() < 0) {
            throw new DataValidationException("Trace coverage setting maxTraceGroupsPerDay should be zero (unlimited) or a positive integer!");
        }
    }
}

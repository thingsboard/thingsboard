// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
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

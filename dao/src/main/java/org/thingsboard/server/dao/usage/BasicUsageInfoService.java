/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.dao.usage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.UsageInfo;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.dao.entity.EntityCountService;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.usagerecord.ApiUsageStateService;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
@RequiredArgsConstructor
public class BasicUsageInfoService implements UsageInfoService {

    private final EntityCountService countService;
    private final ApiUsageStateService apiUsageStateService;
    private final TimeseriesService tsService;
    @Lazy
    private final TbTenantProfileCache tenantProfileCache;

    @Override
    public UsageInfo getUsageInfo(TenantId tenantId) {
        DefaultTenantProfileConfiguration profileConfiguration =
                (DefaultTenantProfileConfiguration) tenantProfileCache.get(tenantId).getProfileData().getConfiguration();
        UsageInfo usageInfo = new UsageInfo();
        usageInfo.setDevices(countService.countByTenantIdAndEntityType(tenantId, EntityType.DEVICE));
        usageInfo.setMaxDevices(profileConfiguration.getMaxDevices());
        usageInfo.setAssets(countService.countByTenantIdAndEntityType(tenantId, EntityType.ASSET));
        usageInfo.setMaxAssets(profileConfiguration.getMaxAssets());
        usageInfo.setCustomers(countService.countByTenantIdAndEntityType(tenantId, EntityType.CUSTOMER));
        usageInfo.setMaxCustomers(profileConfiguration.getMaxCustomers());
        usageInfo.setUsers(countService.countByTenantIdAndEntityType(tenantId, EntityType.USER));
        usageInfo.setMaxUsers(profileConfiguration.getMaxUsers());
        usageInfo.setDashboards(countService.countByTenantIdAndEntityType(tenantId, EntityType.DASHBOARD));
        usageInfo.setMaxDashboards(profileConfiguration.getMaxDashboards());
        usageInfo.setEdges(countService.countByTenantIdAndEntityType(tenantId, EntityType.EDGE));
        usageInfo.setMaxEdges(profileConfiguration.getMaxEdges());

        usageInfo.setMaxAlarms(profileConfiguration.getMaxCreatedAlarms());
        usageInfo.setMaxTransportMessages(profileConfiguration.getMaxTransportMessages());
        usageInfo.setMaxJsExecutions(profileConfiguration.getMaxJSExecutions());
        usageInfo.setMaxTbelExecutions(profileConfiguration.getMaxTbelExecutions());
        usageInfo.setMaxEmails(profileConfiguration.getMaxEmails());
        usageInfo.setMaxSms(profileConfiguration.getMaxSms());
        usageInfo.setSmsEnabled(profileConfiguration.getSmsEnabled());
        ApiUsageState apiUsageState = apiUsageStateService.findTenantApiUsageState(tenantId);
        if (apiUsageState != null) {
            Collection<String> keys = Arrays.asList(
                    ApiUsageRecordKey.TRANSPORT_MSG_COUNT.getApiCountKey(),
                    ApiUsageRecordKey.JS_EXEC_COUNT.getApiCountKey(),
                    ApiUsageRecordKey.TBEL_EXEC_COUNT.getApiCountKey(),
                    ApiUsageRecordKey.EMAIL_EXEC_COUNT.getApiCountKey(),
                    ApiUsageRecordKey.SMS_EXEC_COUNT.getApiCountKey(),
                    ApiUsageRecordKey.CREATED_ALARMS_COUNT.getApiCountKey());
            try {
                List<TsKvEntry> entries = tsService.findLatest(tenantId, apiUsageState.getId(), keys).get();
                usageInfo.setTransportMessages(getLongValueFromTsEntries(entries, ApiUsageRecordKey.TRANSPORT_MSG_COUNT.getApiCountKey()));
                usageInfo.setJsExecutions(getLongValueFromTsEntries(entries, ApiUsageRecordKey.JS_EXEC_COUNT.getApiCountKey()));
                usageInfo.setTbelExecutions(getLongValueFromTsEntries(entries, ApiUsageRecordKey.TBEL_EXEC_COUNT.getApiCountKey()));
                usageInfo.setEmails(getLongValueFromTsEntries(entries, ApiUsageRecordKey.EMAIL_EXEC_COUNT.getApiCountKey()));
                usageInfo.setSms(getLongValueFromTsEntries(entries, ApiUsageRecordKey.SMS_EXEC_COUNT.getApiCountKey()));
                usageInfo.setAlarms(getLongValueFromTsEntries(entries, ApiUsageRecordKey.CREATED_ALARMS_COUNT.getApiCountKey()));
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException("Failed to fetch api usage values from timeseries!");
            }
        }
        return usageInfo;
    }

    private long getLongValueFromTsEntries(List<TsKvEntry> entries, String key) {
        Optional<TsKvEntry> entryOpt = entries.stream().filter(e -> e.getKey().equals(key)).findFirst();
        return entryOpt.map(entry -> entry.getLongValue().orElse(0L)).orElse(0L);
    }
}

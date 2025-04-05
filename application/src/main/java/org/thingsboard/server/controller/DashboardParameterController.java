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
package org.thingsboard.server.controller;


import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.DeviceInfo;
import org.thingsboard.server.common.data.DeviceInfoFilter;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQueryV2;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.controller.cusomize.AssetStatistical;
import org.thingsboard.server.controller.cusomize.StatisticalResponse;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
@TbCoreComponent
@RequestMapping("/api/dashboard-parameter")
@Slf4j
@RequiredArgsConstructor
public class DashboardParameterController extends BaseController {

    // get statistical
    @RequestMapping(value = "/statistical")
    @PreAuthorize("hasAnyAuthority('CUSTOMER_USER')")
    public StatisticalResponse getStatistical(@AuthenticationPrincipal SecurityUser user) throws Exception {
        StatisticalResponse statisticalResponse = new StatisticalResponse();

        // get device of customer
        DeviceInfoFilter.DeviceInfoFilterBuilder filterBuilder = DeviceInfoFilter.builder();
        filterBuilder.tenantId(user.getTenantId());
        filterBuilder.customerId(user.getCustomerId());
        PageData<DeviceInfo> deviceInfoPageData = deviceService.findDeviceInfosByFilter(filterBuilder.build(), new PageLink(100, 0));
        statisticalResponse.setNumberDevice(deviceInfoPageData.getTotalElements());
        statisticalResponse.setNumberDeviceActive(deviceInfoPageData.getData().stream().filter(DeviceInfo::isActive).count());

        // get device type map
        Map<String, Long> deviceTypeMap = deviceInfoPageData.getData().stream().collect(Collectors.groupingBy(DeviceInfo::getDeviceProfileName, Collectors.counting()));
        statisticalResponse.setDeviceTypeMap(deviceTypeMap);

        // get asset of customer
        PageData<Asset> assetPageData = assetService.findAssetsByTenantIdAndCustomerId(user.getTenantId(), user.getCustomerId(), new PageLink(100, 0));
        statisticalResponse.setNumberGarden(assetPageData.getTotalElements());
        Map<String, AssetStatistical> assetStatisticalMap = assetPageData.getData().stream().map(asset -> {
            AssetStatistical assetStatistical = new AssetStatistical();
            assetStatistical.setId(asset.getId().getId().toString());
            assetStatistical.setName(asset.getName());
            assetStatistical.setAddress("");
            assetStatistical.setStatus("");
            assetStatistical.setNumberDevice(0L);
            return assetStatistical;
        }).collect(Collectors.toMap(AssetStatistical::getName, assetStatistical -> assetStatistical));
        statisticalResponse.setAssets(assetStatisticalMap);

        // get nunber alarm
        PageData<AlarmInfo> alarmInfoPageData = alarmService.findCustomerAlarmsV2(user.getTenantId(), user.getCustomerId(), new AlarmQueryV2(null, new TimePageLink(100, 0), null, null, null, null));
        statisticalResponse.setNumberAlarm(alarmInfoPageData.getTotalElements());
        statisticalResponse.setAlarmInfos(alarmInfoPageData.getData());

        return statisticalResponse;
    }
}

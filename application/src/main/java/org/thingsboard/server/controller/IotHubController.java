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
package org.thingsboard.server.controller;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.iot_hub.IotHubInstalledItem;
import org.thingsboard.server.common.data.iot_hub.IotHubInstalledItemInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.List;
import java.util.UUID;
import org.thingsboard.server.dao.iot_hub.IotHubInstalledItemService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.iot_hub.InstallItemVersionResult;
import org.thingsboard.server.service.iot_hub.UpdateItemVersionResult;
import org.thingsboard.server.service.iot_hub.IotHubService;

@Hidden
@RestController
@TbCoreComponent
@RequestMapping("/api/iot-hub")
@RequiredArgsConstructor
@Slf4j
public class IotHubController extends BaseController {

    private final IotHubService iotHubService;
    private final IotHubInstalledItemService iotHubInstalledItemService;

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/versions/{versionId}/install")
    @ResponseBody
    public InstallItemVersionResult installItemVersion(@PathVariable String versionId) throws ThingsboardException {
        return iotHubService.installItemVersion(getCurrentUser(), versionId);
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/installedItems/{itemId}/update/{versionId}")
    @ResponseBody
    public UpdateItemVersionResult updateItemVersion(@PathVariable UUID itemId,
                                                     @PathVariable String versionId) throws ThingsboardException {
        return iotHubService.updateItemVersion(getCurrentUser(), itemId, versionId);
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/installedItems")
    @ResponseBody
    public PageData<IotHubInstalledItem> getInstalledItems(@RequestParam int pageSize,
                                                           @RequestParam int page,
                                                           @RequestParam(required = false) String textSearch,
                                                           @RequestParam(required = false) String sortProperty,
                                                           @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return iotHubInstalledItemService.findByTenantId(getTenantId(), pageLink);
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/installedItems/byItemId/{itemId}")
    @ResponseBody
    public IotHubInstalledItem getInstalledItemByItemId(@PathVariable UUID itemId) throws ThingsboardException {
        return iotHubInstalledItemService.findByTenantIdAndItemId(getTenantId(), itemId).orElse(null);
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/installedItems/info")
    @ResponseBody
    public List<IotHubInstalledItemInfo> getInstalledItemInfos() throws ThingsboardException {
        return iotHubInstalledItemService.findInstalledItemInfosByTenantId(getTenantId());
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping("/installedItems/{itemId}")
    @ResponseBody
    public void deleteInstalledItem(@PathVariable UUID itemId) throws ThingsboardException {
        iotHubService.deleteInstalledItem(getCurrentUser(), itemId);
    }
}

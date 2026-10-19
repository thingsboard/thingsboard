// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.controller;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.iot_hub.DeviceInstalledItemDescriptor;
import org.thingsboard.server.common.data.iot_hub.IotHubInstalledItem;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.thingsboard.server.common.data.id.IotHubInstalledItemId;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.dao.device.DeviceConnectivityService;
import org.thingsboard.server.dao.iot_hub.IotHubInstalledItemService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.system.SystemSecurityService;
import org.thingsboard.server.service.iot_hub.InstallItemVersionResult;
import org.thingsboard.server.service.iot_hub.InstallPlan;
import org.thingsboard.server.service.iot_hub.InstallPlanResult;
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
    private final DeviceConnectivityService deviceConnectivityService;
    private final SystemSecurityService systemSecurityService;

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/versions/{versionId}/install")
    @ResponseBody
    public InstallItemVersionResult installItemVersion(@PathVariable String versionId,
                                                         @RequestBody(required = false) JsonNode data,
                                                         HttpServletRequest request) throws ThingsboardException {
        checkAllPermissions();
        return iotHubService.installItemVersion(getCurrentUser(), versionId, data, request);
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/versions/{versionId}/installPlan")
    @ResponseBody
    public InstallPlan resolveInstallPlan(@PathVariable String versionId) throws ThingsboardException {
        checkAllPermissions();
        return iotHubService.resolveInstallPlan(getCurrentUser(), versionId);
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/installPlan")
    @ResponseBody
    public InstallPlanResult installPlan(@RequestBody InstallPlanRequest body,
                                         HttpServletRequest request) throws ThingsboardException {
        checkAllPermissions();
        return iotHubService.installPlan(getCurrentUser(), body.plan(), body.data(), request);
    }

    public record InstallPlanRequest(InstallPlan plan, JsonNode data) {}

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/device/register")
    @ResponseBody
    public InstallItemVersionResult registerDeviceInstall(
            @RequestParam String versionId,
            @RequestBody JsonNode body) throws ThingsboardException {
        checkAllPermissions();
        DeviceInstalledItemDescriptor descriptor = JacksonUtil.treeToValue(body, DeviceInstalledItemDescriptor.class);
        return iotHubService.registerDeviceInstall(getCurrentUser(), versionId, descriptor);
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/installedItems/{installedItemId}/update/{versionId}")
    @ResponseBody
    public UpdateItemVersionResult updateItemVersion(@PathVariable UUID installedItemId,
                                                     @PathVariable String versionId,
                                                     @RequestParam(required = false, defaultValue = "false") boolean force,
                                                     HttpServletRequest request) throws ThingsboardException {
        checkAllPermissions();
        return iotHubService.updateItemVersion(getCurrentUser(), new IotHubInstalledItemId(installedItemId), versionId, force, request);
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/installedItems")
    @ResponseBody
    public PageData<IotHubInstalledItem> getInstalledItems(@RequestParam int pageSize,
                                                           @RequestParam int page,
                                                           @RequestParam(required = false) String textSearch,
                                                           @RequestParam(required = false) String sortProperty,
                                                           @RequestParam(required = false) String sortOrder,
                                                           @RequestParam(required = false) List<String> itemTypes,
                                                           @RequestParam(required = false) UUID itemId) throws ThingsboardException {
        checkAllPermissions();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return iotHubInstalledItemService.findByTenantId(getTenantId(), itemTypes, itemId, pageLink);
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/installedItems/count")
    @ResponseBody
    public long getInstalledItemsCount(@RequestParam(required = false) String itemType) throws ThingsboardException {
        checkAllPermissions();
        return iotHubInstalledItemService.countByTenantId(getTenantId(), itemType);
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/installedItems/itemIds")
    @ResponseBody
    public List<UUID> getInstalledItemIds() throws ThingsboardException {
        checkAllPermissions();
        return iotHubInstalledItemService.findInstalledItemIdsByTenantId(getTenantId());
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/installedItems/counts")
    @ResponseBody
    public Map<UUID, Long> getInstalledItemCounts(@RequestParam String itemType) throws ThingsboardException {
        checkAllPermissions();
        return iotHubInstalledItemService.findInstalledItemCounts(getTenantId(), itemType);
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping("/installedItems/{installedItemId}")
    @ResponseBody
    public void deleteInstalledItem(@PathVariable UUID installedItemId) throws ThingsboardException {
        checkAllPermissions();
        iotHubService.deleteInstalledItem(getCurrentUser(), new IotHubInstalledItemId(installedItemId));
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/connectivity")
    @ResponseBody
    public JsonNode getConnectivitySettings(HttpServletRequest request) throws Exception {
        checkAllPermissions();
        String baseUrl = systemSecurityService.getBaseUrl(getTenantId(), getCurrentUser().getCustomerId(), request);
        return deviceConnectivityService.getConnectivityInfo(baseUrl);
    }

    private void checkAllPermissions() throws ThingsboardException {
        if (!getCurrentUser().getUserPermissions().hasGenericPermission(Resource.ALL, Operation.ALL)) {
            throw new ThingsboardException("You don't have permissions to use solution templates!",
                    ThingsboardErrorCode.PERMISSION_DENIED);
        }
    }
}

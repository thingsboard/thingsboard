// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.ota.DeviceGroupOtaPackage;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.UUID;

@Slf4j
@RestController
@TbCoreComponent
@RequestMapping("/api")
public class DeviceGroupOtaPackageController extends BaseController {

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/deviceGroupOtaPackage/{groupId}/{firmwareType}")
    public DeviceGroupOtaPackage getFirmwareById(@PathVariable("groupId") String strGroupId,
                                                 @PathVariable("firmwareType") String strFirmwareType) throws ThingsboardException {
        checkParameter("groupId", strGroupId);
        checkParameter("firmwareType", strFirmwareType);
        EntityGroupId groupId = new EntityGroupId(toUUID(strGroupId));
        checkEntityGroupId(groupId, Operation.READ);
        return deviceGroupOtaPackageService.findDeviceGroupOtaPackageByGroupIdAndType(groupId, OtaPackageType.valueOf(strFirmwareType));
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping(value = "/deviceGroupOtaPackage")
    public DeviceGroupOtaPackage saveDeviceGroupOtaPackage(@RequestBody DeviceGroupOtaPackage deviceGroupOtaPackage) throws Exception {
        checkEntityGroupId(deviceGroupOtaPackage.getGroupId(), Operation.WRITE);
        return tbDeviceGroupOtaPackageService.saveDeviceGroupOtaPackage(getTenantId(), deviceGroupOtaPackage, getCurrentUser());
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @DeleteMapping(value = "/deviceGroupOtaPackage/{id}")
    public void deleteDeviceGroupOtaPackage(@PathVariable("id") String strId) throws ThingsboardException {
        checkParameter("deviceGroupOtaPackageId", strId);
        UUID id = toUUID(strId);
        DeviceGroupOtaPackage deviceGroupOtaPackage = deviceGroupOtaPackageService.findDeviceGroupOtaPackageById(id);
        checkEntityGroupId(deviceGroupOtaPackage.getGroupId(), Operation.WRITE);
        tbDeviceGroupOtaPackageService.deleteDeviceGroupOtaPackage(getTenantId(), deviceGroupOtaPackage);
    }

}

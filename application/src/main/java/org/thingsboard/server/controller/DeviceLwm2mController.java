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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.rule.engine.api.msg.DeviceNameOrTypeUpdateMsg;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.lwm2m.LwM2mObject;
import org.thingsboard.server.common.data.lwm2m.ServerSecurityConfig;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@TbCoreComponent
@RequestMapping("/api")
public class DeviceLwm2mController extends BaseController {

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/lwm2m/deviceProfile",  params = {"sortOrder", "sortProperty"},  method = RequestMethod.GET)
    @ResponseBody
    public List<LwM2mObject> getLwm2mListObjects(@RequestParam String sortOrder,
                                                 @RequestParam String sortProperty,
                                                 @RequestParam(required = false) int[] objectIds,
                                                 @RequestParam(required = false) String searchText)
                                                 throws ThingsboardException {
        try {
            return lwM2MModelsRepository.getLwm2mObjects(objectIds, searchText, sortProperty, sortOrder);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/lwm2m/deviceProfile/objects", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<LwM2mObject> getLwm2mListObjects(@RequestParam int pageSize,
                                                     @RequestParam int page,
                                                     @RequestParam(required = false) String searchText,
                                                     @RequestParam(required = false) String sortProperty,
                                                     @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        try {
            PageLink pageLink = createPageLink(pageSize, page, searchText, sortProperty, sortOrder);
            return checkNotNull(lwM2MModelsRepository.findDeviceLwm2mObjects(getTenantId(), pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/lwm2m/deviceProfile/bootstrap/{securityMode}/{bootstrapServerIs}", method = RequestMethod.GET)
    @ResponseBody
    public ServerSecurityConfig getLwm2mBootstrapSecurityInfo(@PathVariable("securityMode") String securityMode,
                                                              @PathVariable("bootstrapServerIs") boolean bootstrapServerIs) throws ThingsboardException {
        try {
            return lwM2MModelsRepository.getBootstrapSecurityInfo(securityMode, bootstrapServerIs);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/lwm2m/device-credentials", method = RequestMethod.POST)
    @ResponseBody
    public Device saveDeviceWithCredentials(@RequestBody (required=false) Map<Class<?>, Object> deviceWithDeviceCredentials) throws ThingsboardException {
        ObjectMapper mapper = new ObjectMapper();
        Device device = checkNotNull(mapper.convertValue(deviceWithDeviceCredentials.get(Device.class), Device.class));
        DeviceCredentials credentials = checkNotNull(mapper.convertValue( deviceWithDeviceCredentials.get(DeviceCredentials.class), DeviceCredentials.class));
        try {
            device.setTenantId(getCurrentUser().getTenantId());
            checkEntity(device.getId(), device, Resource.DEVICE);
            Device savedDevice = deviceService.saveDeviceWithCredentials(device, credentials);
            checkNotNull(savedDevice);

            tbClusterService.onDeviceChange(savedDevice, null);
            tbClusterService.pushMsgToCore(new DeviceNameOrTypeUpdateMsg(savedDevice.getTenantId(),
                    savedDevice.getId(), savedDevice.getName(), savedDevice.getType()), null);
            tbClusterService.onEntityStateChange(savedDevice.getTenantId(), savedDevice.getId(),
                    device.getId() == null ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);

            logEntityAction(savedDevice.getId(), savedDevice,
                    savedDevice.getCustomerId(),
                    device.getId() == null ? ActionType.ADDED : ActionType.UPDATED, null);

            if (device.getId() == null) {
                deviceStateService.onDeviceAdded(savedDevice);
            } else {
                deviceStateService.onDeviceUpdated(savedDevice);
            }
            return savedDevice;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.DEVICE), device,
                    null, device.getId() == null ? ActionType.ADDED : ActionType.UPDATED, e);
            throw handleException(e);
        }
    }
}

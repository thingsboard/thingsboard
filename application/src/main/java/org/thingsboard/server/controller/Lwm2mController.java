/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.lwm2m.ServerSecurityConfig;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.Map;

import static org.thingsboard.server.controller.ControllerConstants.DEVICE_WITH_DEVICE_CREDENTIALS_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.IS_BOOTSTRAP_SERVER_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;

@Slf4j
@RestController
@TbCoreComponent
@RequestMapping("/api")
public class Lwm2mController extends BaseController {
    public static final String IS_BOOTSTRAP_SERVER = "isBootstrapServer";


    @ApiOperation(value = "Get Lwm2m Bootstrap SecurityInfo (getLwm2mBootstrapSecurityInfo)",
            notes = "Get the Lwm2m Bootstrap SecurityInfo object (of the current server) based on the provided isBootstrapServer parameter. If isBootstrapServer == true, get the parameters of the current Bootstrap Server. If isBootstrapServer == false, get the parameters of the current Lwm2m Server. Used for client settings when starting the client in Bootstrap mode. " +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/lwm2m/deviceProfile/bootstrap/{isBootstrapServer}", method = RequestMethod.GET)
    @ResponseBody
    public ServerSecurityConfig getLwm2mBootstrapSecurityInfo(
            @ApiParam(value = IS_BOOTSTRAP_SERVER_PARAM_DESCRIPTION)
            @PathVariable(IS_BOOTSTRAP_SERVER) boolean bootstrapServer) throws ThingsboardException {
        try {
            return lwM2MServerSecurityInfoRepository.getServerSecurityInfo(bootstrapServer);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Create Device (saveDevice) with credentials ",
            notes = "\nCreate new Device  with credentials (example with security mode: RPK\n" +
                    "\nRequestBody is the Map<Class<?>, Object>:\n" +
                    "\nThe first param of this map: Device\n"+
                    "\n-- key1 = \"class org.thingsboard.server.common.data.Device\" - value1 = \"new Device()\"\n" +
                    "\nThe second param of this map: Device credentials\n" +
                    "\n-- key2 = \"class org.thingsboard.server.common.data.security.DeviceCredentials\" - value2 = \"new DeviceCredentials()\"\n" +
                    "\n- Example of the RequestBody with security mode: RPK:\n" +
                    "\n- " + DEVICE_WITH_DEVICE_CREDENTIALS_PARAM_DESCRIPTION + "\n" +
                    "\nWhen creating new device, platform generates Device Id as [time-based UUID](https://en.wikipedia.org/wiki/Universally_unique_identifier#Version_1_(date-time_and_MAC_address).\n" +
                    "\nAfter creating new device Device DeviceCredentials is added to new Device."
                    + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/lwm2m/device-credentials", method = RequestMethod.POST)
    @ResponseBody
    public Device saveDeviceWithCredentials(@ApiParam(value = DEVICE_WITH_DEVICE_CREDENTIALS_PARAM_DESCRIPTION)
                                            @RequestBody(required = false) Map<Class<?>, Object> deviceWithDeviceCredentials) throws ThingsboardException {
        ObjectMapper mapper = new ObjectMapper();
        Device device = checkNotNull(mapper.convertValue(deviceWithDeviceCredentials.get(Device.class), Device.class));
        DeviceCredentials credentials = checkNotNull(mapper.convertValue(deviceWithDeviceCredentials.get(DeviceCredentials.class), DeviceCredentials.class));
        try {
            device.setTenantId(getCurrentUser().getTenantId());
            checkEntity(device.getId(), device, Resource.DEVICE);
            Device savedDevice = deviceService.saveDeviceWithCredentials(device, credentials);
            checkNotNull(savedDevice);
            tbClusterService.onDeviceUpdated(savedDevice, device);
            logEntityAction(savedDevice.getId(), savedDevice,
                    savedDevice.getCustomerId(),
                    device.getId() == null ? ActionType.ADDED : ActionType.UPDATED, null);

            return savedDevice;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.DEVICE), device,
                    null, device.getId() == null ? ActionType.ADDED : ActionType.UPDATED, e);
            throw handleException(e);
        }
    }
}

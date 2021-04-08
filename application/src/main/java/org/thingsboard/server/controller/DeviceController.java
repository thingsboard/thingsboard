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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.rule.engine.api.msg.DeviceCredentialsUpdateNotificationMsg;
import org.thingsboard.rule.engine.api.msg.DeviceNameOrTypeUpdateMsg;
import org.thingsboard.server.common.data.ClaimRequest;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceInfo;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.device.DeviceSearchQuery;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.device.claim.ClaimResponse;
import org.thingsboard.server.dao.device.claim.ClaimResult;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@TbCoreComponent
@RequestMapping("/api")
public class DeviceController extends BaseController {

    private static final String DEVICE_ID = "deviceId";
    private static final String DEVICE_NAME = "deviceName";
    private static final String TENANT_ID = "tenantId";

    @PreAuthorize("hasAnyAuthority('ROOT', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/device/{deviceId}", method = RequestMethod.GET)
    @ResponseBody
    public Device getDeviceById(@PathVariable(DEVICE_ID) String strDeviceId) throws ThingsboardException {
        checkParameter(DEVICE_ID, strDeviceId);
        try {
            DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
            return checkDeviceId(deviceId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('ROOT', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/device/info/{deviceId}", method = RequestMethod.GET)
    @ResponseBody
    public DeviceInfo getDeviceInfoById(@PathVariable(DEVICE_ID) String strDeviceId) throws ThingsboardException {
        checkParameter(DEVICE_ID, strDeviceId);
        try {
            DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
            return checkDeviceInfoId(deviceId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('ROOT', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/device", method = RequestMethod.POST)
    @ResponseBody
    public Device saveDevice(@RequestBody Device device,
                             @RequestParam(name = "accessToken", required = false) String accessToken,
                             @RequestParam(name = "tenantId", required = false) TenantId tenantId) throws ThingsboardException {
        try {
            TenantId currentTenantId =
                getAuthority() == Authority.ROOT && tenantId != null
                    ? tenantId
                    : getTenantId();

            device.setTenantId(currentTenantId);

            checkEntity(device.getId(), device, Resource.DEVICE);

            Device savedDevice = checkNotNull(deviceService.saveDeviceWithAccessToken(device, accessToken));

            tbClusterService.onDeviceChange(savedDevice, null);
            tbClusterService.pushMsgToCore(new DeviceNameOrTypeUpdateMsg(currentTenantId,
                    savedDevice.getId(), savedDevice.getName(), savedDevice.getType()), null);
            tbClusterService.onEntityStateChange(currentTenantId, savedDevice.getId(),
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

    @PreAuthorize("hasAnyAuthority('ROOT', 'TENANT_ADMIN')")
    @RequestMapping(value = "/device/{deviceId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteDevice(@PathVariable(DEVICE_ID) String strDeviceId, @RequestParam(name = "tenantId", required = false) TenantId tenantId) throws ThingsboardException {
        checkParameter(DEVICE_ID, strDeviceId);
        try {
            TenantId currentTenantId =
                getAuthority() == Authority.ROOT && tenantId != null
                    ? tenantId
                    : getTenantId();

            DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
            Device device = checkDeviceId(deviceId, Operation.DELETE);
            deviceService.deleteDevice(currentTenantId, deviceId);

            tbClusterService.onDeviceDeleted(device, null);
            tbClusterService.onEntityStateChange(currentTenantId, deviceId, ComponentLifecycleEvent.DELETED);

            logEntityAction(deviceId, device,
                    device.getCustomerId(),
                    ActionType.DELETED, null, strDeviceId);

            deviceStateService.onDeviceDeleted(device);
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.DEVICE),
                    null,
                    null,
                    ActionType.DELETED, e, strDeviceId);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('ROOT', 'TENANT_ADMIN')")
    @RequestMapping(value = "/customer/{customerId}/device/{deviceId}", method = RequestMethod.POST)
    @ResponseBody
    public Device assignDeviceToCustomer(@PathVariable("customerId") String strCustomerId,
                                         @PathVariable(DEVICE_ID) String strDeviceId, @RequestParam(name = "tenantId", required = false) TenantId tenantId) throws ThingsboardException {
        checkParameter("customerId", strCustomerId);
        checkParameter(DEVICE_ID, strDeviceId);
        try {
            TenantId currentTenantId =
                getAuthority() == Authority.ROOT && tenantId != null
                    ? tenantId
                    : getTenantId();

            CustomerId customerId = new CustomerId(toUUID(strCustomerId));
            Customer customer = checkCustomerId(customerId, Operation.READ);

            DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
            checkDeviceId(deviceId, Operation.ASSIGN_TO_CUSTOMER);

            Device savedDevice = checkNotNull(deviceService.assignDeviceToCustomer(currentTenantId, deviceId, customerId));

            logEntityAction(deviceId, savedDevice,
                    savedDevice.getCustomerId(),
                    ActionType.ASSIGNED_TO_CUSTOMER, null, strDeviceId, strCustomerId, customer.getName());

            return savedDevice;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.DEVICE), null,
                    null,
                    ActionType.ASSIGNED_TO_CUSTOMER, e, strDeviceId, strCustomerId);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('ROOT', 'TENANT_ADMIN')")
    @RequestMapping(value = "/customer/device/{deviceId}", method = RequestMethod.DELETE)
    @ResponseBody
    public Device unassignDeviceFromCustomer(@PathVariable(DEVICE_ID) String strDeviceId, @RequestParam(name = "tenantId", required = false) TenantId tenantId) throws ThingsboardException {
        checkParameter(DEVICE_ID, strDeviceId);
        try {

            TenantId currentTenantId =
                getAuthority() == Authority.ROOT && tenantId != null
                    ? tenantId
                    : getTenantId();

            DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
            Device device = checkDeviceId(deviceId, Operation.UNASSIGN_FROM_CUSTOMER);
            if (device.getCustomerId() == null || device.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
                throw new IncorrectParameterException("Device isn't assigned to any customer!");
            }
            Customer customer = checkCustomerId(device.getCustomerId(), Operation.READ);

            Device savedDevice = checkNotNull(deviceService.unassignDeviceFromCustomer(currentTenantId, deviceId));

            logEntityAction(deviceId, device,
                    device.getCustomerId(),
                    ActionType.UNASSIGNED_FROM_CUSTOMER, null, strDeviceId, customer.getId().toString(), customer.getName());

            return savedDevice;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.DEVICE), null,
                    null,
                    ActionType.UNASSIGNED_FROM_CUSTOMER, e, strDeviceId);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('ROOT', 'TENANT_ADMIN')")
    @RequestMapping(value = "/customer/public/device/{deviceId}", method = RequestMethod.POST)
    @ResponseBody
    public Device assignDeviceToPublicCustomer(@PathVariable(DEVICE_ID) String strDeviceId, @RequestParam(name = "tenantId", required = false) TenantId tenantId) throws ThingsboardException {
        checkParameter(DEVICE_ID, strDeviceId);
        try {
            TenantId currentTenantId =
                getAuthority() == Authority.ROOT && tenantId != null
                    ? tenantId
                    : getTenantId();

            DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
            Device device = checkDeviceId(deviceId, Operation.ASSIGN_TO_CUSTOMER);
            Customer publicCustomer = customerService.findOrCreatePublicCustomer(device.getTenantId());
            Device savedDevice = checkNotNull(deviceService.assignDeviceToCustomer(currentTenantId, deviceId, publicCustomer.getId()));

            logEntityAction(deviceId, savedDevice,
                    savedDevice.getCustomerId(),
                    ActionType.ASSIGNED_TO_CUSTOMER, null, strDeviceId, publicCustomer.getId().toString(), publicCustomer.getName());

            return savedDevice;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.DEVICE), null,
                    null,
                    ActionType.ASSIGNED_TO_CUSTOMER, e, strDeviceId);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('ROOT', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/device/{deviceId}/credentials", method = RequestMethod.GET)
    @ResponseBody
    public DeviceCredentials getDeviceCredentialsByDeviceId(@PathVariable(DEVICE_ID) String strDeviceId, @RequestParam(name = "tenantId", required = false) TenantId tenantId) throws ThingsboardException {
        checkParameter(DEVICE_ID, strDeviceId);
        try {
            TenantId currentTenantId =
                getAuthority() == Authority.ROOT && tenantId != null
                    ? tenantId
                    : getTenantId();

            DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
            Device device = checkDeviceId(deviceId, Operation.READ_CREDENTIALS);
            DeviceCredentials deviceCredentials = checkNotNull(deviceCredentialsService.findDeviceCredentialsByDeviceId(currentTenantId, deviceId));
            logEntityAction(deviceId, device,
                    device.getCustomerId(),
                    ActionType.CREDENTIALS_READ, null, strDeviceId);
            return deviceCredentials;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.DEVICE), null,
                    null,
                    ActionType.CREDENTIALS_READ, e, strDeviceId);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('ROOT', 'TENANT_ADMIN')")
    @RequestMapping(value = "/device/credentials", method = RequestMethod.POST)
    @ResponseBody
    public DeviceCredentials saveDeviceCredentials(@RequestBody DeviceCredentials deviceCredentials, @RequestParam(name = "tenantId", required = false) TenantId tenantId) throws ThingsboardException {
        checkNotNull(deviceCredentials);
        try {
            TenantId currentTenantId =
                getAuthority() == Authority.ROOT && tenantId != null
                    ? tenantId
                    : getTenantId();

            Device device = checkDeviceId(deviceCredentials.getDeviceId(), Operation.WRITE_CREDENTIALS);
            DeviceCredentials result = checkNotNull(deviceCredentialsService.updateDeviceCredentials(currentTenantId, deviceCredentials));

            tbClusterService.pushMsgToCore(new DeviceCredentialsUpdateNotificationMsg(currentTenantId, deviceCredentials.getDeviceId()), null);

            logEntityAction(device.getId(), device,
                    device.getCustomerId(),
                    ActionType.CREDENTIALS_UPDATED, null, deviceCredentials);
            return result;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.DEVICE), null,
                    null,
                    ActionType.CREDENTIALS_UPDATED, e, deviceCredentials);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('ROOT', 'TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/devices", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<Device> getTenantDevices(
            @RequestParam int pageSize,
            @RequestParam int page,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String sortProperty,
            @RequestParam(required = false) String sortOrder,
            @RequestParam(name = "tenantId", required = false) TenantId tenantId) throws ThingsboardException {
        try {
            TenantId currentTenantId =
                getAuthority() == Authority.ROOT && tenantId != null
                    ? tenantId
                    : getTenantId();
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            if (type != null && type.trim().length() > 0) {
                return checkNotNull(deviceService.findDevicesByTenantIdAndType(currentTenantId, type, pageLink));
            } else {
                return checkNotNull(deviceService.findDevicesByTenantId(currentTenantId, pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('ROOT', 'TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/deviceInfos", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<DeviceInfo> getTenantDeviceInfos(
            @RequestParam int pageSize,
            @RequestParam int page,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String deviceProfileId,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String sortProperty,
            @RequestParam(required = false) String sortOrder,
            @RequestParam(name = "tenantId", required = false) TenantId tenantId) throws ThingsboardException {
        try {
            TenantId currentTenantId =
                getAuthority() == Authority.ROOT && tenantId != null
                    ? tenantId
                    : getTenantId();
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            if (type != null && type.trim().length() > 0) {
                return checkNotNull(deviceService.findDeviceInfosByTenantIdAndType(currentTenantId, type, pageLink));
            } else if (deviceProfileId != null && deviceProfileId.length() > 0) {
                DeviceProfileId profileId = new DeviceProfileId(toUUID(deviceProfileId));
                return checkNotNull(deviceService.findDeviceInfosByTenantIdAndDeviceProfileId(currentTenantId, profileId, pageLink));
            } else {
                return checkNotNull(deviceService.findDeviceInfosByTenantId(currentTenantId, pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('ROOT', 'TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/devices", params = {"deviceName"}, method = RequestMethod.GET)
    @ResponseBody
    public Device getTenantDevice(
            @RequestParam String deviceName,
            @RequestParam(name = "tenantId", required = false) TenantId tenantId) throws ThingsboardException {
        try {
            TenantId currentTenantId =
                getAuthority() == Authority.ROOT && tenantId != null
                    ? tenantId
                    : getTenantId();
            return checkNotNull(deviceService.findDeviceByTenantIdAndName(currentTenantId, deviceName));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('ROOT', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/{customerId}/devices", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<Device> getCustomerDevices(
            @PathVariable("customerId") String strCustomerId,
            @RequestParam int pageSize,
            @RequestParam int page,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String sortProperty,
            @RequestParam(required = false) String sortOrder,
            @RequestParam(name = "tenantId", required = false) TenantId tenantId) throws ThingsboardException {
        checkParameter("customerId", strCustomerId);
        try {
            TenantId currentTenantId =
                getAuthority() == Authority.ROOT && tenantId != null
                    ? tenantId
                    : getTenantId();
            CustomerId customerId = new CustomerId(toUUID(strCustomerId));
            checkCustomerId(customerId, Operation.READ);
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            if (type != null && type.trim().length() > 0) {
                return checkNotNull(deviceService.findDevicesByTenantIdAndCustomerIdAndType(currentTenantId, customerId, type, pageLink));
            } else {
                return checkNotNull(deviceService.findDevicesByTenantIdAndCustomerId(currentTenantId, customerId, pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('ROOT', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/{customerId}/deviceInfos", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<DeviceInfo> getCustomerDeviceInfos(
            @PathVariable("customerId") String strCustomerId,
            @RequestParam int pageSize,
            @RequestParam int page,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String deviceProfileId,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String sortProperty,
            @RequestParam(required = false) String sortOrder,
            @RequestParam(name = "tenantId", required = false) TenantId tenantId) throws ThingsboardException {
        checkParameter("customerId", strCustomerId);
        try {
            TenantId currentTenantId =
                getAuthority() == Authority.ROOT && tenantId != null
                    ? tenantId
                    : getTenantId();
            CustomerId customerId = new CustomerId(toUUID(strCustomerId));
            checkCustomerId(customerId, Operation.READ);
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            if (type != null && type.trim().length() > 0) {
                return checkNotNull(deviceService.findDeviceInfosByTenantIdAndCustomerIdAndType(currentTenantId, customerId, type, pageLink));
            } else if (deviceProfileId != null && deviceProfileId.length() > 0) {
                DeviceProfileId profileId = new DeviceProfileId(toUUID(deviceProfileId));
                return checkNotNull(deviceService.findDeviceInfosByTenantIdAndCustomerIdAndDeviceProfileId(currentTenantId, customerId, profileId, pageLink));
            } else {
                return checkNotNull(deviceService.findDeviceInfosByTenantIdAndCustomerId(currentTenantId, customerId, pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('ROOT', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/devices", params = {"deviceIds"}, method = RequestMethod.GET)
    @ResponseBody
    public List<Device> getDevicesByIds(
            @RequestParam("deviceIds") String[] strDeviceIds,
            @RequestParam(name = "tenantId", required = false) TenantId tenantId) throws ThingsboardException {
        checkArrayParameter("deviceIds", strDeviceIds);
        try {
            SecurityUser user = getCurrentUser();
            TenantId currentTenantId =
                getAuthority() == Authority.ROOT && tenantId != null
                    ? tenantId
                    : getTenantId();
            CustomerId customerId = user.getCustomerId();
            List<DeviceId> deviceIds = new ArrayList<>();
            for (String strDeviceId : strDeviceIds) {
                deviceIds.add(new DeviceId(toUUID(strDeviceId)));
            }
            ListenableFuture<List<Device>> devices;
            if (customerId == null || customerId.isNullUid()) {
                devices = deviceService.findDevicesByTenantIdAndIdsAsync(currentTenantId, deviceIds);
            } else {
                devices = deviceService.findDevicesByTenantIdCustomerIdAndIdsAsync(currentTenantId, customerId, deviceIds);
            }
            return checkNotNull(devices.get());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('ROOT', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/devices", method = RequestMethod.POST)
    @ResponseBody
    public List<Device> findByQuery(@RequestBody DeviceSearchQuery query, @RequestParam(name = "tenantId", required = false) TenantId tenantId) throws ThingsboardException {
        checkNotNull(query);
        checkNotNull(query.getParameters());
        checkNotNull(query.getDeviceTypes());
        checkEntityId(query.getParameters().getEntityId(), Operation.READ);
        try {
            TenantId currentTenantId =
                getAuthority() == Authority.ROOT && tenantId != null
                    ? tenantId
                    : getTenantId();

            List<Device> devices = checkNotNull(deviceService.findDevicesByQuery(currentTenantId, query).get());
            devices = devices.stream().filter(device -> {
                try {
                    accessControlService.checkPermission(getCurrentUser(), Resource.DEVICE, Operation.READ, device.getId(), device);
                    return true;
                } catch (ThingsboardException e) {
                    return false;
                }
            }).collect(Collectors.toList());
            return devices;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('ROOT', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/device/types", method = RequestMethod.GET)
    @ResponseBody
    public List<EntitySubtype> getDeviceTypes(@RequestParam(name = "tenantId", required = false) TenantId tenantId) throws ThingsboardException {
        try {
            TenantId currentTenantId =
                getAuthority() == Authority.ROOT && tenantId != null
                    ? tenantId
                    : getTenantId();
            ListenableFuture<List<EntitySubtype>> deviceTypes = deviceService.findDeviceTypesByTenantId(currentTenantId);
            return checkNotNull(deviceTypes.get());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('ROOT', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/device/{deviceName}/claim", method = RequestMethod.POST)
    @ResponseBody
    public DeferredResult<ResponseEntity> claimDevice(@PathVariable(DEVICE_NAME) String deviceName,
                                                      @RequestBody(required = false) ClaimRequest claimRequest,
                                                      @RequestParam(name = "tenantId", required = false) TenantId tenantId) throws ThingsboardException {
        checkParameter(DEVICE_NAME, deviceName);
        try {
            final DeferredResult<ResponseEntity> deferredResult = new DeferredResult<>();

            SecurityUser user = getCurrentUser();
            TenantId currentTenantId =
                getAuthority() == Authority.ROOT && tenantId != null
                    ? tenantId
                    : getTenantId();
            CustomerId customerId = user.getCustomerId();

            Device device = checkNotNull(deviceService.findDeviceByTenantIdAndName(currentTenantId, deviceName));
            accessControlService.checkPermission(user, Resource.DEVICE, Operation.CLAIM_DEVICES,
                    device.getId(), device);
            String secretKey = getSecretKey(claimRequest);

            ListenableFuture<ClaimResult> future = claimDevicesService.claimDevice(device, customerId, secretKey);
            Futures.addCallback(future, new FutureCallback<ClaimResult>() {
                @Override
                public void onSuccess(@Nullable ClaimResult result) {
                    HttpStatus status;
                    if (result != null) {
                        if (result.getResponse().equals(ClaimResponse.SUCCESS)) {
                            status = HttpStatus.OK;
                            deferredResult.setResult(new ResponseEntity<>(result, status));
                        } else {
                            status = HttpStatus.BAD_REQUEST;
                            deferredResult.setResult(new ResponseEntity<>(result.getResponse(), status));
                        }
                    } else {
                        deferredResult.setResult(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    deferredResult.setErrorResult(t);
                }
            }, MoreExecutors.directExecutor());
            return deferredResult;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('ROOT', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/device/{deviceName}/claim", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public DeferredResult<ResponseEntity> reClaimDevice(@PathVariable(DEVICE_NAME) String deviceName,
                                                        @RequestParam(name = "tenantId", required = false) TenantId tenantId) throws ThingsboardException {
        checkParameter(DEVICE_NAME, deviceName);
        try {
            final DeferredResult<ResponseEntity> deferredResult = new DeferredResult<>();

            SecurityUser user = getCurrentUser();
            TenantId currentTenantId =
                getAuthority() == Authority.ROOT && tenantId != null
                    ? tenantId
                    : getTenantId();

            Device device = checkNotNull(deviceService.findDeviceByTenantIdAndName(currentTenantId, deviceName));
            accessControlService.checkPermission(user, Resource.DEVICE, Operation.CLAIM_DEVICES,
                    device.getId(), device);

            ListenableFuture<List<Void>> future = claimDevicesService.reClaimDevice(currentTenantId, device);
            Futures.addCallback(future, new FutureCallback<List<Void>>() {
                @Override
                public void onSuccess(@Nullable List<Void> result) {
                    if (result != null) {
                        deferredResult.setResult(new ResponseEntity(HttpStatus.OK));
                    } else {
                        deferredResult.setResult(new ResponseEntity(HttpStatus.BAD_REQUEST));
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    deferredResult.setErrorResult(t);
                }
            }, MoreExecutors.directExecutor());
            return deferredResult;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private String getSecretKey(ClaimRequest claimRequest) throws IOException {
        String secretKey = claimRequest.getSecretKey();
        if (secretKey != null) {
            return secretKey;
        }
        return DataConstants.DEFAULT_SECRET_KEY;
    }

    @PreAuthorize("hasAnyAuthority('ROOT', 'TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/{tenantId}/device/{deviceId}", method = RequestMethod.POST)
    @ResponseBody
    public Device assignDeviceToTenant(@PathVariable(TENANT_ID) String strTenantId,
                                       @PathVariable(DEVICE_ID) String strDeviceId, 
                                       @RequestParam(name = "tenantId", required = false) TenantId tenantId) throws ThingsboardException {
        checkParameter(TENANT_ID, strTenantId);
        checkParameter(DEVICE_ID, strDeviceId);
        try {
            TenantId currentTenantId =
                getAuthority() == Authority.ROOT && tenantId != null
                    ? tenantId
                    : getTenantId();
            DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
            Device device = checkDeviceId(deviceId, Operation.ASSIGN_TO_TENANT);

            TenantId newTenantId = new TenantId(toUUID(strTenantId));
            Tenant newTenant = tenantService.findTenantById(newTenantId);
            if (newTenant == null) {
                throw new ThingsboardException("Could not find the specified Tenant!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }

            Device assignedDevice = deviceService.assignDeviceToTenant(newTenantId, device);

            logEntityAction(getCurrentUser(), deviceId, assignedDevice,
                    assignedDevice.getCustomerId(),
                    ActionType.ASSIGNED_TO_TENANT, null, strTenantId, newTenant.getName());

            Tenant currentTenant = tenantService.findTenantById(currentTenantId);
            pushAssignedFromNotification(currentTenant, newTenantId, assignedDevice);

            return assignedDevice;
        } catch (Exception e) {
            logEntityAction(getCurrentUser(), emptyId(EntityType.DEVICE), null,
                    null,
                    ActionType.ASSIGNED_TO_TENANT, e, strTenantId);
            throw handleException(e);
        }
    }

    private void pushAssignedFromNotification(Tenant currentTenant, TenantId newTenantId, Device assignedDevice) {
        String data = entityToStr(assignedDevice);
        if (data != null) {
            TbMsg tbMsg = TbMsg.newMsg(DataConstants.ENTITY_ASSIGNED_FROM_TENANT, assignedDevice.getId(), getMetaDataForAssignedFrom(currentTenant), TbMsgDataType.JSON, data);
            tbClusterService.pushMsgToRuleEngine(newTenantId, assignedDevice.getId(), tbMsg, null);
        }
    }

    private TbMsgMetaData getMetaDataForAssignedFrom(Tenant tenant) {
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("assignedFromTenantId", tenant.getId().getId().toString());
        metaData.putValue("assignedFromTenantName", tenant.getName());
        return metaData;
    }
}

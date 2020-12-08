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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.AccessValidator;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@Slf4j
public class DeviceProfileController extends BaseController {

    private static final String DEVICE_PROFILE_ID = "deviceProfileId";

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private TimeseriesService timeseriesService;

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/deviceProfile/{deviceProfileId}", method = RequestMethod.GET)
    @ResponseBody
    public DeviceProfile getDeviceProfileById(@PathVariable(DEVICE_PROFILE_ID) String strDeviceProfileId) throws ThingsboardException {
        checkParameter(DEVICE_PROFILE_ID, strDeviceProfileId);
        try {
            DeviceProfileId deviceProfileId = new DeviceProfileId(toUUID(strDeviceProfileId));
            return checkDeviceProfileId(deviceProfileId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/deviceProfileInfo/{deviceProfileId}", method = RequestMethod.GET)
    @ResponseBody
    public DeviceProfileInfo getDeviceProfileInfoById(@PathVariable(DEVICE_PROFILE_ID) String strDeviceProfileId) throws ThingsboardException {
        checkParameter(DEVICE_PROFILE_ID, strDeviceProfileId);
        try {
            DeviceProfileId deviceProfileId = new DeviceProfileId(toUUID(strDeviceProfileId));
            return checkNotNull(deviceProfileService.findDeviceProfileInfoById(getTenantId(), deviceProfileId));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/deviceProfileInfo/default", method = RequestMethod.GET)
    @ResponseBody
    public DeviceProfileInfo getDefaultDeviceProfileInfo() throws ThingsboardException {
        try {
            return checkNotNull(deviceProfileService.findDefaultDeviceProfileInfo(getTenantId()));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/deviceProfile/devices/keys/timeseries", method = RequestMethod.GET)
    @ResponseBody
    public DeferredResult<ResponseEntity> getTimeseriesKeys(
            @RequestParam(name = DEVICE_PROFILE_ID, required = false) String deviceProfileIdStr) throws ThingsboardException {

        List<Device> devices;
        TenantId tenantId = getTenantId();
        devices = getDevices(tenantId, deviceProfileIdStr);

        try {
            final DeferredResult<ResponseEntity> response = new DeferredResult<>();
            if (devices.size() > 0) {
                ListenableFuture<Set<String>> keysFuture = Futures.transform(
                        timeseriesService.findAllLatestByEntityIds(getTenantId(), EntityType.DEVICE, devices.stream().map(Device::getId).collect(Collectors.toList())), list -> {
                            if (!CollectionUtils.isEmpty(list)) {
                                return list.stream().map(TsKvEntry::getKey).sorted().collect(Collectors.toCollection(LinkedHashSet::new));
                            }
                            return Collections.emptySet();
                        }, MoreExecutors.directExecutor());

                Futures.addCallback(keysFuture, new FutureCallback<Set<String>>() {
                    @Override
                    public void onSuccess(Set<String> keys) {
                        response.setResult(new ResponseEntity<>(keys, HttpStatus.OK));
                    }

                    @Override
                    public void onFailure(Throwable e) {
                        log.error("Failed to fetch timeseries keys by deviceProfileId [{}]", deviceProfileIdStr, e);
                        AccessValidator.handleError(e, response, HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }, MoreExecutors.directExecutor());
            } else {
                response.setResult(new ResponseEntity<>(Collections.EMPTY_LIST, HttpStatus.OK));
            }
            return response;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/deviceProfile/devices/keys/attributes", method = RequestMethod.GET)
    @ResponseBody
    public DeferredResult<ResponseEntity> getAttributesKeys(
            @RequestParam(name = DEVICE_PROFILE_ID, required = false) String deviceProfileIdStr) throws ThingsboardException {

        List<Device> devices;
        TenantId tenantId = getTenantId();
        devices = getDevices(tenantId, deviceProfileIdStr);

        try {
            final DeferredResult<ResponseEntity> response = new DeferredResult<>();
            if (devices.size() > 0) {
                List<EntityId> ids = devices.stream().map(Device::getId).collect(Collectors.toList());

                List<ListenableFuture<List<AttributeKvEntry>>> futures = new ArrayList<>(ids.size());

                for (String scope : DataConstants.allScopes()) {
                    futures.add(attributesService.findAll(tenantId, EntityType.DEVICE, ids, scope));
                }

                ListenableFuture<Set<String>> futureKeys = Futures.transform(Futures.allAsList(futures), lists -> {
                    if (!CollectionUtils.isEmpty(lists)) {
                        return lists.stream()
                                .filter(list -> !CollectionUtils.isEmpty(list))
                                .flatMap(List::stream)
                                .map(AttributeKvEntry::getKey)
                                .sorted()
                                .collect(Collectors.toCollection(LinkedHashSet::new));
                    }
                    return Collections.emptySet();
                }, MoreExecutors.directExecutor());

                Futures.addCallback(futureKeys, new FutureCallback<Set<String>>() {
                    @Override
                    public void onSuccess(@Nullable Set<String> attributesKeys) {
                        response.setResult(new ResponseEntity<>(attributesKeys, HttpStatus.OK));
                    }

                    @Override
                    public void onFailure(Throwable e) {
                        log.error("Failed to fetch attributes keys by deviceProfileId [{}]", deviceProfileIdStr, e);
                        AccessValidator.handleError(e, response, HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }, MoreExecutors.directExecutor());
            } else {
                response.setResult(new ResponseEntity<>(Collections.EMPTY_LIST, HttpStatus.OK));
            }
            return response;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private List<Device> getDevices(TenantId tenantId, String deviceProfileIdStr) throws ThingsboardException {
        List<Device> devices;
        PageLink pageLink = new PageLink(100, 0);
        if (StringUtils.isNotEmpty(deviceProfileIdStr)) {
            DeviceProfileId deviceProfileId = new DeviceProfileId(UUID.fromString(deviceProfileIdStr));
            DeviceProfile deviceProfile = checkDeviceProfileId(deviceProfileId, Operation.READ);
            devices = deviceService.findDevicesByTenantIdAndType(tenantId, deviceProfile.getName(), pageLink).getData();
        } else {
            devices = deviceService.findDevicesByTenantId(tenantId, pageLink).getData();
        }
        return devices;
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/deviceProfile", method = RequestMethod.POST)
    @ResponseBody
    public DeviceProfile saveDeviceProfile(@RequestBody DeviceProfile deviceProfile) throws ThingsboardException {
        try {
            boolean created = deviceProfile.getId() == null;
            deviceProfile.setTenantId(getTenantId());

            checkEntity(deviceProfile.getId(), deviceProfile, Resource.DEVICE_PROFILE);

            DeviceProfile savedDeviceProfile = checkNotNull(deviceProfileService.saveDeviceProfile(deviceProfile));

            tbClusterService.onDeviceProfileChange(savedDeviceProfile, null);
            tbClusterService.onEntityStateChange(deviceProfile.getTenantId(), savedDeviceProfile.getId(),
                    created ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);

            logEntityAction(savedDeviceProfile.getId(), savedDeviceProfile,
                    null,
                    savedDeviceProfile.getId() == null ? ActionType.ADDED : ActionType.UPDATED, null);

            return savedDeviceProfile;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.DEVICE_PROFILE), deviceProfile,
                    null, deviceProfile.getId() == null ? ActionType.ADDED : ActionType.UPDATED, e);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/deviceProfile/{deviceProfileId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteDeviceProfile(@PathVariable(DEVICE_PROFILE_ID) String strDeviceProfileId) throws ThingsboardException {
        checkParameter(DEVICE_PROFILE_ID, strDeviceProfileId);
        try {
            DeviceProfileId deviceProfileId = new DeviceProfileId(toUUID(strDeviceProfileId));
            DeviceProfile deviceProfile = checkDeviceProfileId(deviceProfileId, Operation.DELETE);
            deviceProfileService.deleteDeviceProfile(getTenantId(), deviceProfileId);

            tbClusterService.onDeviceProfileDelete(deviceProfile, null);
            tbClusterService.onEntityStateChange(deviceProfile.getTenantId(), deviceProfile.getId(), ComponentLifecycleEvent.DELETED);

            logEntityAction(deviceProfileId, deviceProfile,
                    null,
                    ActionType.DELETED, null, strDeviceProfileId);

        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.DEVICE_PROFILE),
                    null,
                    null,
                    ActionType.DELETED, e, strDeviceProfileId);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/deviceProfile/{deviceProfileId}/default", method = RequestMethod.POST)
    @ResponseBody
    public DeviceProfile setDefaultDeviceProfile(@PathVariable(DEVICE_PROFILE_ID) String strDeviceProfileId) throws ThingsboardException {
        checkParameter(DEVICE_PROFILE_ID, strDeviceProfileId);
        try {
            DeviceProfileId deviceProfileId = new DeviceProfileId(toUUID(strDeviceProfileId));
            DeviceProfile deviceProfile = checkDeviceProfileId(deviceProfileId, Operation.WRITE);
            DeviceProfile previousDefaultDeviceProfile = deviceProfileService.findDefaultDeviceProfile(getTenantId());
            if (deviceProfileService.setDefaultDeviceProfile(getTenantId(), deviceProfileId)) {
                if (previousDefaultDeviceProfile != null) {
                    previousDefaultDeviceProfile = deviceProfileService.findDeviceProfileById(getTenantId(), previousDefaultDeviceProfile.getId());

                    logEntityAction(previousDefaultDeviceProfile.getId(), previousDefaultDeviceProfile,
                            null, ActionType.UPDATED, null);
                }
                deviceProfile = deviceProfileService.findDeviceProfileById(getTenantId(), deviceProfileId);

                logEntityAction(deviceProfile.getId(), deviceProfile,
                        null, ActionType.UPDATED, null);
            }
            return deviceProfile;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.DEVICE_PROFILE),
                    null,
                    null,
                    ActionType.UPDATED, e, strDeviceProfileId);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/deviceProfiles", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<DeviceProfile> getDeviceProfiles(@RequestParam int pageSize,
                                                     @RequestParam int page,
                                                     @RequestParam(required = false) String textSearch,
                                                     @RequestParam(required = false) String sortProperty,
                                                     @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        try {
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            return checkNotNull(deviceProfileService.findDeviceProfiles(getTenantId(), pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/deviceProfileInfos", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<DeviceProfileInfo> getDeviceProfileInfos(@RequestParam int pageSize,
                                                             @RequestParam int page,
                                                             @RequestParam(required = false) String textSearch,
                                                             @RequestParam(required = false) String sortProperty,
                                                             @RequestParam(required = false) String sortOrder,
                                                             @RequestParam(required = false) String transportType) throws ThingsboardException {
        try {
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            return checkNotNull(deviceProfileService.findDeviceProfileInfos(getTenantId(), pageLink, transportType));
        } catch (Exception e) {
            throw handleException(e);
        }
    }
}

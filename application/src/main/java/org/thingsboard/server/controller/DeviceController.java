// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.controller;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.server.common.data.ClaimRequest;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceInfo;
import org.thingsboard.server.common.data.DeviceInfoFilter;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.NameConflictPolicy;
import org.thingsboard.server.common.data.NameConflictStrategy;
import org.thingsboard.server.common.data.SaveDeviceWithCredentialsRequest;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.UniquifyStrategy;
import org.thingsboard.server.common.data.device.DeviceSearchQuery;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.sync.ie.importing.csv.BulkImportRequest;
import org.thingsboard.server.common.data.sync.ie.importing.csv.BulkImportResult;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.device.claim.ClaimResponse;
import org.thingsboard.server.dao.device.claim.ClaimResult;
import org.thingsboard.server.dao.device.claim.ReclaimResult;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.device.DeviceBulkImportService;
import org.thingsboard.server.service.entitiy.device.TbDeviceService;
import org.thingsboard.server.service.gateway_device.GatewayNotificationsService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.thingsboard.server.controller.ControllerConstants.CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.CUSTOMER_ID;
import static org.thingsboard.server.controller.ControllerConstants.CUSTOMER_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_ACTIVE_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_ID;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_INFO_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_NAME_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_PROFILE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_TYPE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_UPDATE_CREDENTIALS_PARAM_ACCESS_TOKEN_DESCRIPTION_MARKDOWN;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_UPDATE_CREDENTIALS_PARAM_LVM2M_RPK_DESCRIPTION_MARKDOWN;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_UPDATE_CREDENTIALS_PARAM_MQTT_BASIC_DESCRIPTION_MARKDOWN;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_UPDATE_CREDENTIALS_PARAM_X509_CERTIFICATE_DESCRIPTION_MARKDOWN;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_WITH_DEVICE_CREDENTIALS_PARAM_ACCESS_TOKEN_DEFAULT_DESCRIPTION_MARKDOWN;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_WITH_DEVICE_CREDENTIALS_PARAM_ACCESS_TOKEN_DESCRIPTION_MARKDOWN;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_WITH_DEVICE_CREDENTIALS_PARAM_LVM2M_RPK_DESCRIPTION_MARKDOWN;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_WITH_DEVICE_CREDENTIALS_PARAM_MQTT_BASIC_DESCRIPTION_MARKDOWN;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_WITH_DEVICE_CREDENTIALS_PARAM_X509_CERTIFICATE_DESCRIPTION_MARKDOWN;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_GROUP_ID;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_GROUP_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.INCLUDE_CUSTOMERS_OR_SUB_CUSTOMERS;
import static org.thingsboard.server.controller.ControllerConstants.NAME_CONFLICT_POLICY_DESC;
import static org.thingsboard.server.controller.ControllerConstants.UNIQUIFY_SEPARATOR_DESC;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_DELETE_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_GROUP_READ_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_READ_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_WRITE_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_ID;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.UNIQUIFY_STRATEGY_DESC;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;

import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.device.DevicePingResponse;
import org.thingsboard.server.service.device.DevicePingService;


@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class DeviceController extends BaseController {

    protected static final String DEVICE_NAME = "deviceName";

    protected static final String RBAC_READ_CREDENTIALS_CHECK = " Security check is performed to verify that the user has 'READ_CREDENTIALS' permission for the entity (entities).";
    protected static final String RBAC_WRITE_CREDENTIALS_CHECK = " Security check is performed to verify that the user has 'WRITE_CREDENTIALS' permission for the entity (entities).";
    protected static final String RBAC_CLAIM_CHECK = " Security check is performed to verify that the user has 'CLAIM_DEVICES' permission for the entity (entities).";
    protected static final String RBAC_ASSIGN_TO_TENANT_CHECK = " Security check is performed to verify that the user has 'ASSIGN_TO_TENANT' permission for the entity (entities).";

    private final DeviceBulkImportService deviceBulkImportService;
    private final GatewayNotificationsService gatewayNotificationsService;
    private final TbDeviceService tbDeviceService;
    
    private final DevicePingService devicePingService;

    @ApiOperation(value = "Get Device (getDeviceById)",
            notes = "Fetch the Device object based on the provided Device Id. "
                    + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/device/{deviceId}", method = RequestMethod.GET)
    @ResponseBody
    public Device getDeviceById(@Parameter(description = DEVICE_ID_PARAM_DESCRIPTION)
                                @PathVariable(DEVICE_ID) String strDeviceId) throws ThingsboardException {
        checkParameter(DEVICE_ID, strDeviceId);
        DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
        return checkDeviceId(deviceId, Operation.READ);
    }

    @ApiOperation(value = "Get Device (getDeviceInfoById)",
            notes = "Fetch the Device info object based on the provided Device Id. "
                    + DEVICE_INFO_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/device/info/{deviceId}", method = RequestMethod.GET)
    @ResponseBody
    public DeviceInfo getDeviceInfoById(@Parameter(description = DEVICE_ID_PARAM_DESCRIPTION)
                                        @PathVariable(DEVICE_ID) String strDeviceId) throws ThingsboardException {
        checkParameter(DEVICE_ID, strDeviceId);
        DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
        return checkDeviceInfoId(deviceId, Operation.READ);
    }

    @ApiOperation(value = "Create Or Update Device (saveDevice)",
            notes = "Create or update the Device. When creating device, platform generates Device Id as " + UUID_WIKI_LINK +
                    "Device credentials are also generated if not provided in the 'accessToken' request parameter. " +
                    "The newly created device id will be present in the response. " +
                    "Specify existing Device id to update the device. " +
                    "Referencing non-existing device Id will cause 'Not Found' error." +
                    "\n\nDevice name is unique in the scope of tenant. Use unique identifiers like MAC or IMEI for the device names and non-unique 'label' field for user-friendly visualization purposes." +
                    "Remove 'id', 'tenantId' and optionally 'customerId' from the request body example (below) to create new Device entity. " +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_WRITE_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/device", method = RequestMethod.POST)
    @ResponseBody
    public Device saveDevice(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "A JSON value representing the device.", required = true) @RequestBody Device device,
                             @Parameter(description = "Optional value of the device credentials to be used during device creation. " +
                                     "If omitted, access token will be auto-generated.") @RequestParam(name = "accessToken", required = false) String accessToken,
                             @RequestParam(name = "entityGroupId", required = false) String strEntityGroupId,
                             @Parameter(description = "A list of entity group ids, separated by comma ','", array = @ArraySchema(schema = @Schema(type = "string")))
                             @RequestParam(name = "entityGroupIds", required = false) String[] strEntityGroupIds,
                             @Parameter(description = NAME_CONFLICT_POLICY_DESC)
                             @RequestParam(name = "nameConflictPolicy", defaultValue = "FAIL") NameConflictPolicy nameConflictPolicy,
                             @Parameter(description = UNIQUIFY_SEPARATOR_DESC)
                             @RequestParam(name = "uniquifySeparator", defaultValue = "_") String uniquifySeparator,
                             @Parameter(description = UNIQUIFY_STRATEGY_DESC)
                             @RequestParam(name = "uniquifyStrategy", defaultValue = "RANDOM") UniquifyStrategy uniquifyStrategy) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        return saveGroupEntity(device, strEntityGroupId, strEntityGroupIds,
                (device1, entityGroups) -> {
                    try {
                        return tbDeviceService.save(device1, accessToken, entityGroups, new NameConflictStrategy(nameConflictPolicy, uniquifySeparator, uniquifyStrategy), user);
                    } catch (Exception e) {
                        throw handleException(e);
                    }
                });
    }

    @ApiOperation(value = "Create Device (saveDevice) with credentials ",
            notes = "Create or update the Device. When creating device, platform generates Device Id as " + UUID_WIKI_LINK +
                    "Requires to provide the Device Credentials object as well as an existing device profile ID or use \"default\".\n" +
                    "You may find the example of device with different type of credentials below: \n\n" +
                    "- Credentials type: **\"Access token\"** with **device profile ID** below: \n\n" +
                    DEVICE_WITH_DEVICE_CREDENTIALS_PARAM_ACCESS_TOKEN_DESCRIPTION_MARKDOWN + "\n\n" +
                    "- Credentials type: **\"Access token\"** with  **device profile default** below: \n\n" +
                    DEVICE_WITH_DEVICE_CREDENTIALS_PARAM_ACCESS_TOKEN_DEFAULT_DESCRIPTION_MARKDOWN + "\n\n" +
                    "- Credentials type: **\"X509\"** with **device profile ID** below: \n\n" +
                    "Note: **credentialsId** -  format **Sha3Hash**, **certificateValue** - format **PEM** (with \"--BEGIN CERTIFICATE----\" and  -\"----END CERTIFICATE-\").\n\n" +
                    DEVICE_WITH_DEVICE_CREDENTIALS_PARAM_X509_CERTIFICATE_DESCRIPTION_MARKDOWN + "\n\n" +
                    "- Credentials type: **\"MQTT_BASIC\"** with **device profile ID** below: \n\n" +
                    DEVICE_WITH_DEVICE_CREDENTIALS_PARAM_MQTT_BASIC_DESCRIPTION_MARKDOWN + "\n\n" +
                    "- You may find the example of **LwM2M** device and **RPK** credentials below: \n\n" +
                    "Note: LwM2M device - only existing device profile ID (Transport configuration -> Transport type: \"LWM2M\".\n\n" +
                    DEVICE_WITH_DEVICE_CREDENTIALS_PARAM_LVM2M_RPK_DESCRIPTION_MARKDOWN + "\n\n" +
                    "Remove 'id', 'tenantId' and optionally 'customerId' from the request body example (below) to create new Device entity. " +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_WRITE_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/device-with-credentials", method = RequestMethod.POST)
    @ResponseBody
    public Device saveDeviceWithCredentials(@Parameter(description = "The JSON object with device and credentials. See method description above for example.")
                                            @Valid @RequestBody SaveDeviceWithCredentialsRequest deviceAndCredentials,
                                            @RequestParam(name = "entityGroupId", required = false) String strEntityGroupId,
                                            @Parameter(description = "A list of entity group ids, separated by comma ','", array = @ArraySchema(schema = @Schema(type = "string")))
                                            @RequestParam(name = "entityGroupIds", required = false) String[] strEntityGroupIds,
                                            @Parameter(description = NAME_CONFLICT_POLICY_DESC)
                                            @RequestParam(name = "nameConflictPolicy", defaultValue = "FAIL") NameConflictPolicy nameConflictPolicy,
                                            @Parameter(description = UNIQUIFY_SEPARATOR_DESC)
                                            @RequestParam(name = "uniquifySeparator", defaultValue = "_") String uniquifySeparator,
                                            @Parameter(description = UNIQUIFY_STRATEGY_DESC)
                                            @RequestParam(name = "uniquifyStrategy", defaultValue = "RANDOM") UniquifyStrategy uniquifyStrategy) throws ThingsboardException {
        Device device = deviceAndCredentials.getDevice();
        DeviceCredentials credentials = deviceAndCredentials.getCredentials();
        SecurityUser user = getCurrentUser();
        return saveGroupEntity(device, strEntityGroupId, strEntityGroupIds,
                (device1, entityGroup) -> tbDeviceService.saveDeviceWithCredentials(device1, credentials, entityGroup, new NameConflictStrategy(nameConflictPolicy, uniquifySeparator, uniquifyStrategy), user));
    }

    @ApiOperation(value = "Delete device (deleteDevice)",
            notes = "Deletes the device, it's credentials and all the relations (from and to the device). Referencing non-existing device Id will cause an error."
                    + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_DELETE_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/device/{deviceId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteDevice(@Parameter(description = DEVICE_ID_PARAM_DESCRIPTION)
                             @PathVariable(DEVICE_ID) String strDeviceId) throws Exception {
        checkParameter(DEVICE_ID, strDeviceId);
        DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
        Device device = checkDeviceId(deviceId, Operation.DELETE);
        tbDeviceService.delete(device, getCurrentUser());
    }

    @ApiOperation(value = "Get Device Credentials (getDeviceCredentialsByDeviceId)",
            notes = "If during device creation there wasn't specified any credentials, platform generates random 'ACCESS_TOKEN' credentials."
                    + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CREDENTIALS_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/device/{deviceId}/credentials", method = RequestMethod.GET)
    @ResponseBody
    public DeviceCredentials getDeviceCredentialsByDeviceId(@Parameter(description = DEVICE_ID_PARAM_DESCRIPTION)
                                                            @PathVariable(DEVICE_ID) String strDeviceId) throws ThingsboardException {
        checkParameter(DEVICE_ID, strDeviceId);
        DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
        Device device = checkDeviceId(deviceId, Operation.READ_CREDENTIALS);
        return tbDeviceService.getDeviceCredentialsByDeviceId(device, getCurrentUser());
    }

    @ApiOperation(value = "Update device credentials (updateDeviceCredentials)",
            notes = "During device creation, platform generates random 'ACCESS_TOKEN' credentials.\n" +
                    "Use this method to update the device credentials. First use 'getDeviceCredentialsByDeviceId' to get the credentials id and value.\n" +
                    "Then use current method to update the credentials type and value. It is not possible to create multiple device credentials for the same device.\n" +
                    "The structure of device credentials id and value is simple for the 'ACCESS_TOKEN' but is much more complex for the 'MQTT_BASIC' or 'LWM2M_CREDENTIALS'.\n" +
                    "You may find the example of device with different type of credentials below: \n\n" +
                    "- Credentials type: **\"Access token\"** with **device ID** and with **device ID** below: \n\n" +
                    DEVICE_UPDATE_CREDENTIALS_PARAM_ACCESS_TOKEN_DESCRIPTION_MARKDOWN + "\n\n" +
                    "- Credentials type: **\"X509\"** with **device profile ID** below: \n\n" +
                    "Note: **credentialsId** -  format **Sha3Hash**, **certificateValue** - format **PEM** (with \"--BEGIN CERTIFICATE----\" and  -\"----END CERTIFICATE-\").\n\n" +
                    DEVICE_UPDATE_CREDENTIALS_PARAM_X509_CERTIFICATE_DESCRIPTION_MARKDOWN + "\n\n" +
                    "- Credentials type: **\"MQTT_BASIC\"** with **device profile ID** below: \n\n" +
                    DEVICE_UPDATE_CREDENTIALS_PARAM_MQTT_BASIC_DESCRIPTION_MARKDOWN + "\n\n" +
                    "- You may find the example of **LwM2M** device and **RPK** credentials below: \n\n" +
                    "Note: LwM2M device - only existing device profile ID (Transport configuration -> Transport type: \"LWM2M\".\n\n" +
                    DEVICE_UPDATE_CREDENTIALS_PARAM_LVM2M_RPK_DESCRIPTION_MARKDOWN + "\n\n" +
                    "Update to real value:\n" +
                    " - 'id' (this is id of Device Credentials ->  \"Get Device Credentials (getDeviceCredentialsByDeviceId)\",\n" +
                    " - 'deviceId.id' (this is id of Device).\n" +
                    "Remove 'tenantId' and optionally 'customerId' from the request body example (below) to create new Device entity." +
                    TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/device/credentials", method = RequestMethod.POST)
    @ResponseBody
    public DeviceCredentials updateDeviceCredentials(
            @Parameter(description = "A JSON value representing the device credentials.")
            @RequestBody DeviceCredentials deviceCredentials) throws ThingsboardException {
        checkNotNull(deviceCredentials);
        Device device = checkDeviceId(deviceCredentials.getDeviceId(), Operation.WRITE_CREDENTIALS);
        return tbDeviceService.updateDeviceCredentials(device, deviceCredentials, getCurrentUser());
    }

    @ApiOperation(value = "Get Tenant Devices (getTenantDevices)",
            notes = "Returns a page of devices owned by tenant. " +
                    PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/tenant/devices")
    public PageData<Device> getTenantDevices(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = DEVICE_TYPE_DESCRIPTION)
            @RequestParam(required = false) String type,
            @Parameter(description = DEVICE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name", "deviceProfileName", "label", "customerTitle"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.DEVICE, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        if (type != null && type.trim().length() > 0) {
            return checkNotNull(deviceService.findDevicesByTenantIdAndType(tenantId, type, pageLink));
        } else {
            return checkNotNull(deviceService.findDevicesByTenantId(tenantId, pageLink));
        }
    }

    @Hidden
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/tenant/devices", params = {"deviceName"})
    public Device getTenantDevice(
            @Parameter(description = DEVICE_NAME_DESCRIPTION)
            @RequestParam String deviceName) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.DEVICE, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        return checkNotNull(deviceService.findDeviceByTenantIdAndName(tenantId, deviceName));
    }

    @ApiOperation(value = "Get Tenant Device (getTenantDevice)",
            notes = "Requested device must be owned by tenant that the user belongs to. " +
                    "Device name is an unique property of device. So it can be used to identify the device."
                    + TENANT_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/tenant/device")
    public Device getTenantDeviceByName(
            @Parameter(description = DEVICE_NAME_DESCRIPTION)
            @RequestParam String deviceName) throws ThingsboardException {
        return getTenantDevice(deviceName);
    }

    @ApiOperation(value = "Get Customer Devices (getCustomerDevices)",
            notes = "Returns a page of devices objects assigned to customer. " +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/customer/{customerId}/devices")
    public PageData<Device> getCustomerDevices(
            @Parameter(description = CUSTOMER_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(CUSTOMER_ID) String strCustomerId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = DEVICE_TYPE_DESCRIPTION)
            @RequestParam(required = false) String type,
            @Parameter(description = DEVICE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name", "deviceProfileName", "label", "customerTitle"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter("customerId", strCustomerId);
        TenantId tenantId = getCurrentUser().getTenantId();
        CustomerId customerId = new CustomerId(toUUID(strCustomerId));
        checkCustomerId(customerId, Operation.READ);
        accessControlService.checkPermission(getCurrentUser(), Resource.DEVICE, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        if (type != null && type.trim().length() > 0) {
            return checkNotNull(deviceService.findDevicesByTenantIdAndCustomerIdAndType(tenantId, customerId, type, pageLink));
        } else {
            return checkNotNull(deviceService.findDevicesByTenantIdAndCustomerId(tenantId, customerId, pageLink));
        }
    }

    @ApiOperation(value = "Get Devices (getUserDevices)",
            notes = "Returns a page of devices that are available for the current user. " +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/user/devices", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<Device> getUserDevices(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true, schema = @Schema(minimum = "1"))
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true, schema = @Schema(minimum = "0"))
            @RequestParam int page,
            @Parameter(description = DEVICE_TYPE_DESCRIPTION)
            @RequestParam(required = false) String type,
            @Parameter(description = DEVICE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name", "deviceProfileName", "label", "customerTitle"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        SecurityUser currentUser = getCurrentUser();
        MergedUserPermissions mergedUserPermissions = currentUser.getUserPermissions();
        return entityService.findUserEntities(currentUser.getTenantId(), currentUser.getCustomerId(), mergedUserPermissions, EntityType.DEVICE,
                Operation.READ, type, pageLink);
    }

    @ApiOperation(value = "Get All Device Infos for current user (getAllDeviceInfos)",
            notes = "Returns a page of device info objects owned by the tenant or the customer of a current user. "
                    + DEVICE_INFO_DESCRIPTION + " " + PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/deviceInfos/all", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<DeviceInfo> getAllDeviceInfos(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = INCLUDE_CUSTOMERS_OR_SUB_CUSTOMERS)
            @RequestParam(required = false) Boolean includeCustomers,
            @Parameter(description = DEVICE_PROFILE_ID_PARAM_DESCRIPTION)
            @RequestParam(required = false) String deviceProfileId,
            @Parameter(description = DEVICE_ACTIVE_PARAM_DESCRIPTION)
            @RequestParam(required = false) Boolean active,
            @Parameter(description = DEVICE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name", "deviceProfileName", "label", "customerTitle"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.DEVICE, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        DeviceInfoFilter.DeviceInfoFilterBuilder filter = DeviceInfoFilter.builder();
        filter.tenantId(tenantId);
        filter.active(active);
        filter.includeCustomers(includeCustomers != null && includeCustomers);
        if (deviceProfileId != null && deviceProfileId.length() > 0) {
            filter.deviceProfileId(new DeviceProfileId(toUUID(deviceProfileId)));
        }
        if (Authority.CUSTOMER_USER.equals(getCurrentUser().getAuthority())) {
            filter.customerId(getCurrentUser().getCustomerId());
        }
        return checkNotNull(deviceService.findDeviceInfosByFilter(filter.build(), pageLink));
    }

    @ApiOperation(value = "Get Customer Device Infos (getCustomerDeviceInfos)",
            notes = "Returns a page of device info objects owned by the specified customer. "
                    + DEVICE_INFO_DESCRIPTION + " " + PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/customer/{customerId}/deviceInfos")
    public PageData<DeviceInfo> getCustomerDeviceInfos(
            @Parameter(description = CUSTOMER_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(CUSTOMER_ID) String strCustomerId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = INCLUDE_CUSTOMERS_OR_SUB_CUSTOMERS)
            @RequestParam(required = false) Boolean includeCustomers,
            @Parameter(description = DEVICE_PROFILE_ID_PARAM_DESCRIPTION)
            @RequestParam(required = false) String deviceProfileId,
            @Parameter(description = DEVICE_ACTIVE_PARAM_DESCRIPTION)
            @RequestParam(required = false) Boolean active,
            @Parameter(description = DEVICE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name", "deviceProfileName", "label", "customerTitle"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter(CUSTOMER_ID, strCustomerId);
        accessControlService.checkPermission(getCurrentUser(), Resource.DEVICE, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        CustomerId customerId = new CustomerId(toUUID(strCustomerId));
        checkCustomerId(customerId, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        DeviceInfoFilter.DeviceInfoFilterBuilder filter = DeviceInfoFilter.builder();
        filter.tenantId(tenantId);
        filter.active(active);
        filter.includeCustomers(includeCustomers != null && includeCustomers);
        filter.customerId(customerId);
        if (deviceProfileId != null && deviceProfileId.length() > 0) {
            filter.deviceProfileId(new DeviceProfileId(toUUID(deviceProfileId)));
        }
        return checkNotNull(deviceService.findDeviceInfosByFilter(filter.build(), pageLink));
    }

    @ApiOperation(value = "Get Devices By Ids (getDevicesByIds)",
            notes = "Requested devices must be owned by tenant or assigned to customer which user is performing the request. "
                    + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/devices")
    public List<Device> getDevicesByIds(
            @Parameter(description = "A list of devices ids, separated by comma ','",  array = @ArraySchema(schema = @Schema(type = "string")))
            @RequestParam("deviceIds") String[] strDeviceIds) throws ThingsboardException, ExecutionException, InterruptedException {
        checkArrayParameter("deviceIds", strDeviceIds);
        SecurityUser user = getCurrentUser();
        TenantId tenantId = user.getTenantId();
        List<DeviceId> deviceIds = new ArrayList<>();
        for (String strDeviceId : strDeviceIds) {
            deviceIds.add(new DeviceId(toUUID(strDeviceId)));
        }
        List<Device> devices = checkNotNull(deviceService.findDevicesByTenantIdAndIdsAsync(tenantId, deviceIds).get());
        return filterDevicesByReadPermission(devices);
    }

    @ApiOperation(value = "Find related devices (findDevicesByQuery)",
            notes = "Returns all devices that are related to the specific entity. " +
                    "The entity id, relation type, device types, depth of the search, and other query parameters defined using complex 'DeviceSearchQuery' object. " +
                    "See 'Model' tab of the Parameters for more info." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/devices", method = RequestMethod.POST)
    @ResponseBody
    public List<Device> findDevicesByQuery(
            @Parameter(description = "The device search query JSON")
            @RequestBody DeviceSearchQuery query) throws ThingsboardException, ExecutionException, InterruptedException {
        checkNotNull(query);
        checkNotNull(query.getParameters());
        checkNotNull(query.getDeviceTypes());
        checkEntityId(query.getParameters().getEntityId(), Operation.READ);
        List<Device> devices = checkNotNull(deviceService.findDevicesByQuery(getCurrentUser().getTenantId(), query).get());
        return filterDevicesByReadPermission(devices);
    }

    @ApiOperation(value = "Get devices by Entity Group Id (getDevicesByEntityGroupId)",
            notes = "Returns a page of Device objects that belongs to specified Entity Group Id. " +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_GROUP_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityGroup/{entityGroupId}/devices", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<Device> getDevicesByEntityGroupId(
            @Parameter(description = ENTITY_GROUP_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(ENTITY_GROUP_ID) String strEntityGroupId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true, schema = @Schema(minimum = "1"))
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true, schema = @Schema(minimum = "0"))
            @RequestParam int page,
            @Parameter(description = DEVICE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name", "deviceProfileName", "label", "customerTitle"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder
    ) throws ThingsboardException {
        checkParameter(ENTITY_GROUP_ID, strEntityGroupId);
        EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
        EntityGroup entityGroup = checkEntityGroupId(entityGroupId, Operation.READ);
        checkEntityGroupType(EntityType.DEVICE, entityGroup.getType());
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(deviceService.findDevicesByEntityGroupId(entityGroupId, pageLink));
    }

    private List<Device> filterDevicesByReadPermission(List<Device> devices) {
        return devices.stream().filter(device -> {
            try {
                return accessControlService.hasPermission(getCurrentUser(), Resource.DEVICE, Operation.READ, device.getId(), device);
            } catch (ThingsboardException e) {
                return false;
            }
        }).toList();
    }

    @ApiOperation(value = "Get Device Types (getDeviceTypes)",
            notes = "Deprecated. See 'getDeviceProfileNames' API from Device Profile Controller instead." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/device/types", method = RequestMethod.GET)
    @ResponseBody
    @Deprecated(since = "3.6.2")
    public List<EntitySubtype> getDeviceTypes() throws ThingsboardException, ExecutionException, InterruptedException {
        SecurityUser user = getCurrentUser();
        TenantId tenantId = user.getTenantId();
        ListenableFuture<List<EntitySubtype>> deviceTypes = deviceService.findDeviceTypesByTenantId(tenantId);
        return checkNotNull(deviceTypes.get());
    }

    @ApiOperation(value = "Claim device (claimDevice)",
            notes = "Claiming makes it possible to assign a device to the specific customer using device/server side claiming data (in the form of secret key)." +
                    "To make this happen you have to provide unique device name and optional claiming data (it is needed only for device-side claiming)." +
                    "Once device is claimed, the customer becomes its owner and customer users may access device data as well as control the device. \n" +
                    "In order to enable claiming devices feature a system parameter security.claim.allowClaimingByDefault should be set to true, " +
                    "otherwise a server-side claimingAllowed attribute with the value true is obligatory for provisioned devices. \n" +
                    "See official documentation for more details regarding claiming." + CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_CLAIM_CHECK)
    @PreAuthorize("hasAuthority('CUSTOMER_USER')")
    @RequestMapping(value = "/customer/device/{deviceName}/claim", method = RequestMethod.POST)
    @ResponseBody
    public DeferredResult<ResponseEntity> claimDevice(@Parameter(description = "Unique name of the device which is going to be claimed")
                                                      @PathVariable(DEVICE_NAME) String deviceName,
                                                      @Parameter(description = "Claiming request which can optionally contain secret key")
                                                      @RequestBody(required = false) ClaimRequest claimRequest,
                                                      @RequestParam(required = false) String subCustomerId) throws ThingsboardException {
        checkParameter(DEVICE_NAME, deviceName);
        final DeferredResult<ResponseEntity> deferredResult = new DeferredResult<>();

        SecurityUser user = getCurrentUser();
        TenantId tenantId = user.getTenantId();
        CustomerId parentCustomerId = user.getCustomerId();
        CustomerId customerId;
        Device device = checkNotNull(deviceService.findDeviceByTenantIdAndName(tenantId, deviceName));
        accessControlService.checkPermission(user, Resource.DEVICE, Operation.CLAIM_DEVICES,
                device.getId(), device);
        String secretKey = getSecretKey(claimRequest);

        if (StringUtils.isEmpty(subCustomerId)) {
            customerId = parentCustomerId;
        } else {
            Customer subCustomer = checkNotNull(customerService.findCustomerById(tenantId, new CustomerId(UUID.fromString(subCustomerId))));
            customerId = subCustomer.getId();
            if (!ownersCacheService.isChildOwner(tenantId, parentCustomerId, customerId)) {
                throw new ThingsboardException("Requested sub-customer wasn't found!", ThingsboardErrorCode.ITEM_NOT_FOUND);
            }
        }
        ListenableFuture<ClaimResult> future = tbDeviceService.claimDevice(tenantId, device, customerId, secretKey, user);
        Futures.addCallback(future, new FutureCallback<>() {
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
    }

    @ApiOperation(value = "Reclaim device (reClaimDevice)",
            notes = "Reclaiming means the device will be unassigned from the customer and the device will be available for claiming again."
                    + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_CLAIM_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/device/{deviceName}/claim", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public DeferredResult<ResponseEntity> reClaimDevice(@Parameter(description = "Unique name of the device which is going to be reclaimed")
                                                        @PathVariable(DEVICE_NAME) String deviceName) throws ThingsboardException {
        checkParameter(DEVICE_NAME, deviceName);
        final DeferredResult<ResponseEntity> deferredResult = new DeferredResult<>();

        SecurityUser user = getCurrentUser();
        TenantId tenantId = user.getTenantId();

        Device device = checkNotNull(deviceService.findDeviceByTenantIdAndName(tenantId, deviceName));
        accessControlService.checkPermission(user, Resource.DEVICE, Operation.CLAIM_DEVICES,
                device.getId(), device);

        ListenableFuture<ReclaimResult> result = tbDeviceService.reclaimDevice(tenantId, device, user);
        Futures.addCallback(result, new FutureCallback<>() {
            @Override
            public void onSuccess(ReclaimResult reclaimResult) {
                deferredResult.setResult(new ResponseEntity(HttpStatus.OK));
            }

            @Override
            public void onFailure(Throwable t) {
                deferredResult.setErrorResult(t);
            }
        }, MoreExecutors.directExecutor());
        return deferredResult;
    }

    private String getSecretKey(ClaimRequest claimRequest) {
        String secretKey = claimRequest.getSecretKey();
        if (secretKey != null) {
            return secretKey;
        }
        return DataConstants.DEFAULT_SECRET_KEY;
    }

    @ApiOperation(value = "Assign device to tenant (assignDeviceToTenant)",
            notes = "Creates assignment of the device to tenant. Thereafter tenant will be able to reassign the device to a customer."
                    + TENANT_AUTHORITY_PARAGRAPH + RBAC_ASSIGN_TO_TENANT_CHECK)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/{tenantId}/device/{deviceId}", method = RequestMethod.POST)
    @ResponseBody
    public Device assignDeviceToTenant(@Parameter(description = TENANT_ID_PARAM_DESCRIPTION)
                                       @PathVariable(TENANT_ID) String strTenantId,
                                       @Parameter(description = DEVICE_ID_PARAM_DESCRIPTION)
                                       @PathVariable(DEVICE_ID) String strDeviceId) throws ThingsboardException {
        checkParameter(TENANT_ID, strTenantId);
        checkParameter(DEVICE_ID, strDeviceId);
        DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
        Device device = checkDeviceId(deviceId, Operation.ASSIGN_TO_TENANT);

        TenantId newTenantId = TenantId.fromUUID(toUUID(strTenantId));
        Tenant newTenant = tenantService.findTenantById(newTenantId);
        if (newTenant == null) {
            throw new ThingsboardException("Could not find the specified Tenant!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        return tbDeviceService.assignDeviceToTenant(device, newTenant, getCurrentUser());
    }

    @ApiOperation(value = "Count devices by device profile  (countByDeviceProfileAndEmptyOtaPackage)",
            notes = "The platform gives an ability to load OTA (over-the-air) packages to devices. " +
                    "It can be done in two different ways: device scope or device profile scope." +
                    "In the response you will find the number of devices with specified device profile, but without previously defined device scope OTA package. " +
                    "It can be useful when you want to define number of devices that will be affected with future OTA package"
                    + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/devices/count/{otaPackageType}/{deviceProfileId}", method = RequestMethod.GET)
    @ResponseBody
    public Long countByDeviceProfileAndEmptyOtaPackage(@Parameter(description = "OTA package type", schema = @Schema(allowableValues = {"FIRMWARE", "SOFTWARE"}))
                                                       @PathVariable("otaPackageType") String otaPackageType,
                                                       @Parameter(description = "Device Profile Id. I.g. '784f394c-42b6-435a-983c-b7beff2784f9'")
                                                       @PathVariable("deviceProfileId") String deviceProfileId) throws ThingsboardException {
        checkParameter("OtaPackageType", otaPackageType);
        checkParameter("DeviceProfileId", deviceProfileId);
        return deviceService.countByDeviceProfileAndEmptyOtaPackage(
                getTenantId(),
                new DeviceProfileId(UUID.fromString(deviceProfileId)),
                OtaPackageType.valueOf(otaPackageType));
    }

    @ApiOperation(value = "Count devices by device profile  (countByDeviceProfileAndEmptyOtaPackage)",
            notes = "The platform gives an ability to load OTA (over-the-air) packages to devices. " +
                    "It can be done in two different ways: device scope or device profile scope." +
                    "In the response you will find the number of devices with specified device profile, but without previously defined device scope OTA package. " +
                    "It can be useful when you want to define number of devices that will be affected with future OTA package" + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/devices/count/{otaPackageType}/{otaPackageId}/{entityGroupId}", method = RequestMethod.GET)
    @ResponseBody
    public Long countByDeviceGroupAndEmptyOtaPackage(
            @Parameter(description = "OTA package type", schema = @Schema(allowableValues = {"FIRMWARE", "SOFTWARE"}))
            @PathVariable("otaPackageType") String otaPackageType,
            @PathVariable("otaPackageId") String otaPackageId,
            @PathVariable("entityGroupId") String deviceGroupId) throws ThingsboardException {
        checkParameter("OtaPackageType", otaPackageType);
        checkParameter("OtaPackageId", otaPackageId);
        checkParameter("EntityGroupId", deviceGroupId);
        checkParameter("DeviceGroupId", deviceGroupId);
        return deviceService.countByEntityGroupAndEmptyOtaPackage(
                new EntityGroupId(UUID.fromString(deviceGroupId)),
                new OtaPackageId(UUID.fromString(otaPackageId)),
                OtaPackageType.valueOf(otaPackageType));
    }

    @ApiOperation(value = "Import the bulk of devices (processDevicesBulkImport)",
            notes = "There's an ability to import the bulk of devices using the only .csv file." + RBAC_WRITE_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping("/device/bulk_import")
    public BulkImportResult<Device> processDevicesBulkImport(@RequestBody BulkImportRequest request) throws Exception {
        return deviceBulkImportService.processBulkImport(request, getCurrentUser(), (device, savingFunction) -> {
            try {
                saveGroupEntity(device, request.getEntityGroupId(), savingFunction);
            } catch (ThingsboardException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Ping a device to check its reachability status.
     *
     * @param strDeviceId the device ID
     * @return DevicePingResponse containing reachability status and last seen timestamp
     */
    @ApiOperation(value = "Ping Device", notes = "Checks if a device is reachable based on its last activity timestamp")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/device/ping/{deviceId}", method = RequestMethod.GET)
    @ResponseBody
    public DevicePingResponse pingDevice(
            @Parameter(description = "Device ID") @PathVariable("deviceId") String strDeviceId) throws ThingsboardException {
        checkParameter(DEVICE_ID, strDeviceId);

        try {
            DeviceId deviceId = new DeviceId(toUUID(strDeviceId));

            // Check device exists and user has access
            checkDeviceId(deviceId, Operation.READ);

            DevicePingResponse response = devicePingService.pingDevice(getTenantId(), deviceId);

            // Log the action
            SecurityUser user = getCurrentUser();
            logEntityActionService.logEntityAction(user.getTenantId(), deviceId,
                    ActionType.ATTRIBUTES_READ, user, null, "lastActivityTime");

            return response;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

}

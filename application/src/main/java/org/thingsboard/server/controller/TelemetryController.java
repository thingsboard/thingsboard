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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Function;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.rule.engine.api.AttributesDeleteRequest;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.rule.engine.api.TimeseriesDeleteRequest;
import org.thingsboard.rule.engine.api.TimeseriesSaveRequest;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseDeleteTsKvQuery;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.data.kv.DeleteTsKvQuery;
import org.thingsboard.server.common.data.kv.IntervalType;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.msg.rule.engine.DeviceAttributesEventNotificationMsg;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.AccessValidator;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.telemetry.AttributeData;
import org.thingsboard.server.service.telemetry.TbTelemetryService;
import org.thingsboard.server.service.telemetry.TsData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.thingsboard.server.controller.ControllerConstants.ATTRIBUTES_JSON_REQUEST_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ATTRIBUTES_KEYS_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ATTRIBUTES_SCOPE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ATTRIBUTE_DATA_EXAMPLE;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_ID;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_GET_ATTRIBUTE_SCOPES;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_SAVE_ATTRIBUTE_SCOPES;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_TYPE_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.INVALID_STRUCTURE_OF_THE_REQUEST;
import static org.thingsboard.server.controller.ControllerConstants.LATEST_TS_NON_STRICT_DATA_EXAMPLE;
import static org.thingsboard.server.controller.ControllerConstants.LATEST_TS_STRICT_DATA_EXAMPLE;
import static org.thingsboard.server.controller.ControllerConstants.MARKDOWN_CODE_BLOCK_END;
import static org.thingsboard.server.controller.ControllerConstants.MARKDOWN_CODE_BLOCK_START;
import static org.thingsboard.server.controller.ControllerConstants.SAVE_ATTIRIBUTES_STATUS_BAD_REQUEST;
import static org.thingsboard.server.controller.ControllerConstants.SAVE_ATTIRIBUTES_STATUS_OK;
import static org.thingsboard.server.controller.ControllerConstants.SAVE_ATTRIBUTES_REQUEST_PAYLOAD;
import static org.thingsboard.server.controller.ControllerConstants.SAVE_ENTITY_ATTRIBUTES_STATUS_INTERNAL_SERVER_ERROR;
import static org.thingsboard.server.controller.ControllerConstants.SAVE_ENTITY_ATTRIBUTES_STATUS_OK;
import static org.thingsboard.server.controller.ControllerConstants.SAVE_ENTITY_ATTRIBUTES_STATUS_UNAUTHORIZED;
import static org.thingsboard.server.controller.ControllerConstants.SAVE_ENTITY_TIMESERIES_STATUS_INTERNAL_SERVER_ERROR;
import static org.thingsboard.server.controller.ControllerConstants.SAVE_ENTITY_TIMESERIES_STATUS_OK;
import static org.thingsboard.server.controller.ControllerConstants.SAVE_ENTITY_TIMESERIES_STATUS_UNAUTHORIZED;
import static org.thingsboard.server.controller.ControllerConstants.SAVE_TIMESERIES_REQUEST_PAYLOAD;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.STRICT_DATA_TYPES_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TELEMETRY_JSON_REQUEST_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TELEMETRY_KEYS_BASE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TELEMETRY_KEYS_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TELEMETRY_SCOPE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TS_STRICT_DATA_EXAMPLE;

@RestController
@TbCoreComponent
@RequestMapping(TbUrlConstants.TELEMETRY_URL_PREFIX)
@Slf4j
public class TelemetryController extends BaseController {

    @Autowired
    private TimeseriesService tsService;

    @Autowired
    private AccessValidator accessValidator;

    @Autowired
    private TbTelemetryService tbTelemetryService;

    private ExecutorService executor;

    @PostConstruct
    public void initExecutor() {
        executor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("telemetry-controller"));
    }

    @PreDestroy
    public void shutdownExecutor() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @ApiOperation(value = "Get all attribute keys (getAttributeKeys)",
            notes = "Returns a set of unique attribute key names for the selected entity. " +
                    "The response will include merged key names set for all attribute scopes:" +
                    "\n\n * SERVER_SCOPE - supported for all entity types;" +
                    "\n * CLIENT_SCOPE - supported for devices;" +
                    "\n * SHARED_SCOPE - supported for devices. "
                    + "\n\n" + INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/{entityType}/{entityId}/keys/attributes")
    public DeferredResult<ResponseEntity> getAttributeKeys(
            @Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION, required = true, schema = @Schema(defaultValue = "DEVICE")) @PathVariable("entityType") String entityType,
            @Parameter(description = ENTITY_ID_PARAM_DESCRIPTION, required = true) @PathVariable("entityId") String entityIdStr) throws ThingsboardException {
        return accessValidator.validateEntityAndCallback(getCurrentUser(), Operation.READ_ATTRIBUTES, entityType, entityIdStr, this::getAttributeKeysCallback);
    }

    @ApiOperation(value = "Get all attribute keys by scope (getAttributeKeysByScope)",
            notes = "Returns a set of unique attribute key names for the selected entity and attributes scope: " +
                    "\n\n * SERVER_SCOPE - supported for all entity types;" +
                    "\n * CLIENT_SCOPE - supported for devices;" +
                    "\n * SHARED_SCOPE - supported for devices. "
                    + "\n\n" + INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/{entityType}/{entityId}/keys/attributes/{scope}")
    public DeferredResult<ResponseEntity> getAttributeKeysByScope(
            @Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION, required = true, schema = @Schema(defaultValue = "DEVICE")) @PathVariable("entityType") String entityType,
            @Parameter(description = ENTITY_ID_PARAM_DESCRIPTION, required = true) @PathVariable("entityId") String entityIdStr,
            @Parameter(description = ATTRIBUTES_SCOPE_DESCRIPTION, required = true, schema = @Schema(allowableValues = {"SERVER_SCOPE", "SHARED_SCOPE", "CLIENT_SCOPE"})) @PathVariable("scope") AttributeScope scope) throws ThingsboardException {
        return accessValidator.validateEntityAndCallback(getCurrentUser(), Operation.READ_ATTRIBUTES, entityType, entityIdStr,
                (result, tenantId, entityId) -> getAttributeKeysCallback(result, tenantId, entityId, scope));
    }

    @ApiOperation(value = "Get attributes (getAttributes)",
            notes = "Returns all attributes that belong to specified entity. Use optional 'keys' parameter to return specific attributes."
                    + "\n Example of the result: \n\n"
                    + MARKDOWN_CODE_BLOCK_START
                    + ATTRIBUTE_DATA_EXAMPLE
                    + MARKDOWN_CODE_BLOCK_END
                    + "\n\n " + INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/{entityType}/{entityId}/values/attributes")
    public DeferredResult<ResponseEntity> getAttributes(
            @Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION, required = true, schema = @Schema(defaultValue = "DEVICE")) @PathVariable("entityType") String entityType,
            @Parameter(description = ENTITY_ID_PARAM_DESCRIPTION, required = true) @PathVariable("entityId") String entityIdStr,
            @Parameter(description = ATTRIBUTES_KEYS_DESCRIPTION) @RequestParam(name = "keys", required = false) String keysStr,
            @RequestParam MultiValueMap<String, String> params) throws ThingsboardException {
        List<String> keys = getKeys(keysStr, params);
        SecurityUser user = getCurrentUser();
        return accessValidator.validateEntityAndCallback(getCurrentUser(), Operation.READ_ATTRIBUTES, entityType, entityIdStr,
                (result, tenantId, entityId) -> getAttributeValuesCallback(result, user, entityId, null, keys));
    }


    @ApiOperation(value = "Get attributes by scope (getAttributesByScope)",
            notes = "Returns all attributes of a specified scope that belong to specified entity." +
                    ENTITY_GET_ATTRIBUTE_SCOPES +
                    "Use optional 'keys' parameter to return specific attributes."
                    + "\n Example of the result: \n\n"
                    + MARKDOWN_CODE_BLOCK_START
                    + ATTRIBUTE_DATA_EXAMPLE
                    + MARKDOWN_CODE_BLOCK_END
                    + "\n\n " + INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/{entityType}/{entityId}/values/attributes/{scope}")
    public DeferredResult<ResponseEntity> getAttributesByScope(
            @Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION, required = true, schema = @Schema(defaultValue = "DEVICE")) @PathVariable("entityType") String entityType,
            @Parameter(description = ENTITY_ID_PARAM_DESCRIPTION, required = true) @PathVariable("entityId") String entityIdStr,
            @Parameter(description = ATTRIBUTES_SCOPE_DESCRIPTION, schema = @Schema(allowableValues = {"SERVER_SCOPE", "SHARED_SCOPE", "CLIENT_SCOPE"}, requiredMode = Schema.RequiredMode.REQUIRED)) @PathVariable("scope") AttributeScope scope,
            @Parameter(description = ATTRIBUTES_KEYS_DESCRIPTION) @RequestParam(name = "keys", required = false) String keysStr,
            @RequestParam MultiValueMap<String, String> params) throws ThingsboardException {
        List<String> keys = getKeys(keysStr, params);
        SecurityUser user = getCurrentUser();
        return accessValidator.validateEntityAndCallback(getCurrentUser(), Operation.READ_ATTRIBUTES, entityType, entityIdStr,
                (result, tenantId, entityId) -> getAttributeValuesCallback(result, user, entityId, scope, keys));
    }

    @ApiOperation(value = "Get time series keys (getTimeseriesKeys)",
            notes = "Returns a set of unique time series key names for the selected entity. " +
                    "\n\n" + INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/{entityType}/{entityId}/keys/timeseries")
    public DeferredResult<ResponseEntity> getTimeseriesKeys(
            @Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION, required = true, schema = @Schema(defaultValue = "DEVICE")) @PathVariable("entityType") String entityType,
            @Parameter(description = ENTITY_ID_PARAM_DESCRIPTION, required = true) @PathVariable("entityId") String entityIdStr) throws ThingsboardException {
        return accessValidator.validateEntityAndCallback(getCurrentUser(), Operation.READ_TELEMETRY, entityType, entityIdStr,
                (result, tenantId, entityId) -> Futures.addCallback(tsService.findAllLatest(tenantId, entityId), getTsKeysToResponseCallback(result), MoreExecutors.directExecutor()));
    }

    @ApiOperation(value = "Get latest time series value (getLatestTimeseries)",
            notes = "Returns all time series that belong to specified entity. Use optional 'keys' parameter to return specific time series." +
                    " The result is a JSON object. The format of the values depends on the 'useStrictDataTypes' parameter." +
                    " By default, all time series values are converted to strings: \n\n"
                    + MARKDOWN_CODE_BLOCK_START
                    + LATEST_TS_NON_STRICT_DATA_EXAMPLE
                    + MARKDOWN_CODE_BLOCK_END
                    + "\n\n However, it is possible to request the values without conversion ('useStrictDataTypes'=true): \n\n"
                    + MARKDOWN_CODE_BLOCK_START
                    + LATEST_TS_STRICT_DATA_EXAMPLE
                    + MARKDOWN_CODE_BLOCK_END
                    + "\n\n " + INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/{entityType}/{entityId}/values/timeseries")
    public DeferredResult<ResponseEntity> getLatestTimeseries(
            @Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION, required = true, schema = @Schema(defaultValue = "DEVICE")) @PathVariable("entityType") String entityType,
            @Parameter(description = ENTITY_ID_PARAM_DESCRIPTION, required = true) @PathVariable("entityId") String entityIdStr,
            @Parameter(description = TELEMETRY_KEYS_DESCRIPTION) @RequestParam(name = "keys", required = false) String keysStr,
            @Parameter(description = STRICT_DATA_TYPES_DESCRIPTION)
            @RequestParam(name = "useStrictDataTypes", required = false, defaultValue = "false") Boolean useStrictDataTypes,
            @RequestParam MultiValueMap<String, String> params) throws ThingsboardException {
        List<String> keys = getKeys(keysStr, params);
        SecurityUser user = getCurrentUser();
        return accessValidator.validateEntityAndCallback(getCurrentUser(), Operation.READ_TELEMETRY, entityType, entityIdStr,
                (result, tenantId, entityId) -> getLatestTimeseriesValuesCallback(result, user, entityId, keys, useStrictDataTypes));
    }

    @ApiOperation(value = "Get time series data (getTimeseries)",
            notes = "Returns a range of time series values for specified entity. " +
                    "Returns not aggregated data by default. " +
                    "Use aggregation function ('agg') and aggregation interval ('interval') to enable aggregation of the results on the database / server side. " +
                    "The aggregation is generally more efficient then fetching all records. \n\n"
                    + MARKDOWN_CODE_BLOCK_START
                    + TS_STRICT_DATA_EXAMPLE
                    + MARKDOWN_CODE_BLOCK_END
                    + "\n\n" + INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/{entityType}/{entityId}/values/timeseries", params = {"keys", "startTs", "endTs"})
    public DeferredResult<ResponseEntity> getTimeseries(
            @Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION, required = true, schema = @Schema(defaultValue = "DEVICE")) @PathVariable("entityType") String entityType,
            @Parameter(description = ENTITY_ID_PARAM_DESCRIPTION, required = true) @PathVariable("entityId") String entityIdStr,
            @Parameter(description = TELEMETRY_KEYS_BASE_DESCRIPTION) @RequestParam(name = "keys", required = false) String keysStr,
            @Parameter(description = "A long value representing the start timestamp of the time range in milliseconds, UTC.")
            @RequestParam(name = "startTs") Long startTs,
            @Parameter(description = "A long value representing the end timestamp of the time range in milliseconds, UTC.")
            @RequestParam(name = "endTs") Long endTs,
            @Parameter(description = "A string value representing the type fo the interval.", schema = @Schema(allowableValues = {"MILLISECONDS", "WEEK", "WEEK_ISO", "MONTH", "QUARTER"}))
            @RequestParam(name = "intervalType", required = false) IntervalType intervalType,
            @Parameter(description = "A long value representing the aggregation interval range in milliseconds.")
            @RequestParam(name = "interval", defaultValue = "0") Long interval,
            @Parameter(description = "A string value representing the timezone that will be used to calculate exact timestamps for 'WEEK', 'WEEK_ISO', 'MONTH' and 'QUARTER' interval types.")
            @RequestParam(name = "timeZone", required = false) String timeZone,
            @Parameter(description = "An integer value that represents a max number of time series data points to fetch." +
                    " This parameter is used only in the case if 'agg' parameter is set to 'NONE'.", schema = @Schema(defaultValue = "100"))
            @RequestParam(name = "limit", defaultValue = "100") Integer limit,
            @Parameter(description = "A string value representing the aggregation function. " +
                    "If the interval is not specified, 'agg' parameter will use 'NONE' value.",
                    schema = @Schema(allowableValues = {"MIN", "MAX", "AVG", "SUM", "COUNT", "NONE"}))
            @RequestParam(name = "agg", defaultValue = "NONE") String aggStr,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(name = "orderBy", defaultValue = "DESC") String orderBy,
            @Parameter(description = STRICT_DATA_TYPES_DESCRIPTION)
            @RequestParam(name = "useStrictDataTypes", required = false, defaultValue = "false") Boolean useStrictDataTypes,
            @RequestParam MultiValueMap<String, String> params) throws ThingsboardException {
        List<String> keys = getKeys(keysStr, params);
        DeferredResult<ResponseEntity> response = new DeferredResult<>();
        Futures.addCallback(tbTelemetryService.getTimeseries(EntityIdFactory.getByTypeAndId(entityType, entityIdStr), keys, startTs, endTs,
                        intervalType, interval, timeZone, limit, Aggregation.valueOf(aggStr), orderBy, useStrictDataTypes, getCurrentUser()),
                getTsKvListCallback(response, useStrictDataTypes), MoreExecutors.directExecutor());
        return response;
    }

    @ApiOperation(value = "Save device attributes (saveDeviceAttributes)",
            notes = "Creates or updates the device attributes based on device id and specified attribute scope. " +
                    SAVE_ATTRIBUTES_REQUEST_PAYLOAD
                    + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = SAVE_ATTIRIBUTES_STATUS_OK +
                    "Platform creates an audit log event about device attributes updates with action type 'ATTRIBUTES_UPDATED', " +
                    "and also sends event msg to the rule engine with msg type 'ATTRIBUTES_UPDATED'."),
            @ApiResponse(responseCode = "400", description = SAVE_ATTIRIBUTES_STATUS_BAD_REQUEST),
            @ApiResponse(responseCode = "401", description = "User is not authorized to save device attributes for selected device. Most likely, User belongs to different Customer or Tenant."),
            @ApiResponse(responseCode = "500", description = "The exception was thrown during processing the request. " +
                    "Platform creates an audit log event about device attributes updates with action type 'ATTRIBUTES_UPDATED' that includes an error stacktrace."),
    })
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping(value = "/{deviceId}/{scope}")
    public DeferredResult<ResponseEntity> saveDeviceAttributes(@Parameter(description = DEVICE_ID_PARAM_DESCRIPTION, required = true)
                                                               @PathVariable("deviceId") String deviceIdStr,
                                                               @Parameter(description = ATTRIBUTES_SCOPE_DESCRIPTION, schema = @Schema(allowableValues = {"SERVER_SCOPE", "SHARED_SCOPE"}, requiredMode = Schema.RequiredMode.REQUIRED))
                                                               @PathVariable("scope") AttributeScope scope,
                                                               @io.swagger.v3.oas.annotations.parameters.RequestBody(description = ATTRIBUTES_JSON_REQUEST_DESCRIPTION, required = true)
                                                               @RequestBody String request) throws ThingsboardException {
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(EntityType.DEVICE, deviceIdStr);
        return saveAttributes(getTenantId(), entityId, scope, request);
    }

    @ApiOperation(value = "Save entity attributes (saveEntityAttributesV1)",
            notes = "Creates or updates the entity attributes based on Entity Id and the specified attribute scope. " +
                    ENTITY_SAVE_ATTRIBUTE_SCOPES +
                    SAVE_ATTRIBUTES_REQUEST_PAYLOAD
                    + INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = SAVE_ATTIRIBUTES_STATUS_OK + SAVE_ENTITY_ATTRIBUTES_STATUS_OK),
            @ApiResponse(responseCode = "400", description = SAVE_ATTIRIBUTES_STATUS_BAD_REQUEST),
            @ApiResponse(responseCode = "401", description = SAVE_ENTITY_ATTRIBUTES_STATUS_UNAUTHORIZED),
            @ApiResponse(responseCode = "500", description = SAVE_ENTITY_ATTRIBUTES_STATUS_INTERNAL_SERVER_ERROR),
    })
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping(value = "/{entityType}/{entityId}/{scope}")
    public DeferredResult<ResponseEntity> saveEntityAttributesV1(@Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION, required = true, schema = @Schema(defaultValue = "DEVICE"))
                                                                 @PathVariable("entityType") String entityType,
                                                                 @Parameter(description = ENTITY_ID_PARAM_DESCRIPTION, required = true)
                                                                 @PathVariable("entityId") String entityIdStr,
                                                                 @Parameter(description = ATTRIBUTES_SCOPE_DESCRIPTION, schema = @Schema(allowableValues = {"SERVER_SCOPE", "SHARED_SCOPE"}))
                                                                 @PathVariable("scope") AttributeScope scope,
                                                                 @io.swagger.v3.oas.annotations.parameters.RequestBody(description = ATTRIBUTES_JSON_REQUEST_DESCRIPTION, required = true)
                                                                 @RequestBody String request) throws ThingsboardException {
        EntityId entityId = EntityIdFactory.getByTypeAndId(entityType, entityIdStr);
        return saveAttributes(getTenantId(), entityId, scope, request);
    }

    @ApiOperation(value = "Save entity attributes (saveEntityAttributesV2)",
            notes = "Creates or updates the entity attributes based on Entity Id and the specified attribute scope. " +
                    ENTITY_SAVE_ATTRIBUTE_SCOPES +
                    SAVE_ATTRIBUTES_REQUEST_PAYLOAD
                    + INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = SAVE_ATTIRIBUTES_STATUS_OK + SAVE_ENTITY_ATTRIBUTES_STATUS_OK),
            @ApiResponse(responseCode = "400", description = SAVE_ATTIRIBUTES_STATUS_BAD_REQUEST),
            @ApiResponse(responseCode = "401", description = SAVE_ENTITY_ATTRIBUTES_STATUS_UNAUTHORIZED),
            @ApiResponse(responseCode = "500", description = SAVE_ENTITY_ATTRIBUTES_STATUS_INTERNAL_SERVER_ERROR),
    })
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping(value = "/{entityType}/{entityId}/attributes/{scope}")
    public DeferredResult<ResponseEntity> saveEntityAttributesV2(@Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION, required = true, schema = @Schema(defaultValue = "DEVICE"))
                                                                 @PathVariable("entityType") String entityType,
                                                                 @Parameter(description = ENTITY_ID_PARAM_DESCRIPTION, required = true)
                                                                 @PathVariable("entityId") String entityIdStr,
                                                                 @Parameter(description = ATTRIBUTES_SCOPE_DESCRIPTION, schema = @Schema(allowableValues = {"SERVER_SCOPE", "SHARED_SCOPE"}, requiredMode = Schema.RequiredMode.REQUIRED))
                                                                 @PathVariable("scope") AttributeScope scope,
                                                                 @io.swagger.v3.oas.annotations.parameters.RequestBody(description = ATTRIBUTES_JSON_REQUEST_DESCRIPTION, required = true)
                                                                 @RequestBody String request) throws ThingsboardException {
        EntityId entityId = EntityIdFactory.getByTypeAndId(entityType, entityIdStr);
        return saveAttributes(getTenantId(), entityId, scope, request);
    }


    @ApiOperation(value = "Save or update time series data (saveEntityTelemetry)",
            notes = "Creates or updates the entity time series data based on the Entity Id and request payload." +
                    SAVE_TIMESERIES_REQUEST_PAYLOAD +
                    "\n\n The scope parameter is not used in the API call implementation but should be specified whatever value because it is used as a path variable. "
                    + INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = SAVE_ENTITY_TIMESERIES_STATUS_OK),
            @ApiResponse(responseCode = "400", description = INVALID_STRUCTURE_OF_THE_REQUEST),
            @ApiResponse(responseCode = "401", description = SAVE_ENTITY_TIMESERIES_STATUS_UNAUTHORIZED),
            @ApiResponse(responseCode = "500", description = SAVE_ENTITY_TIMESERIES_STATUS_INTERNAL_SERVER_ERROR),
    })
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping(value = "/{entityType}/{entityId}/timeseries/{scope}")
    public DeferredResult<ResponseEntity> saveEntityTelemetry(
            @Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION, required = true, schema = @Schema(defaultValue = "DEVICE")) @PathVariable("entityType") String entityType,
            @Parameter(description = ENTITY_ID_PARAM_DESCRIPTION, required = true) @PathVariable("entityId") String entityIdStr,
            @Parameter(description = TELEMETRY_SCOPE_DESCRIPTION, required = true, schema = @Schema(allowableValues = "ANY")) @PathVariable("scope") String scope,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = TELEMETRY_JSON_REQUEST_DESCRIPTION, required = true) @RequestBody String requestBody) throws ThingsboardException {
        EntityId entityId = EntityIdFactory.getByTypeAndId(entityType, entityIdStr);
        return saveTelemetry(getTenantId(), entityId, requestBody, 0L);
    }

    @ApiOperation(value = "Save or update time series data with TTL (saveEntityTelemetryWithTTL)",
            notes = "Creates or updates the entity time series data based on the Entity Id and request payload." +
                    SAVE_TIMESERIES_REQUEST_PAYLOAD +
                    "\n\n The scope parameter is not used in the API call implementation but should be specified whatever value because it is used as a path variable. "
                    + "\n\nThe ttl parameter takes affect only in case of Cassandra DB."
                    + INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = SAVE_ENTITY_TIMESERIES_STATUS_OK),
            @ApiResponse(responseCode = "400", description = INVALID_STRUCTURE_OF_THE_REQUEST),
            @ApiResponse(responseCode = "401", description = SAVE_ENTITY_TIMESERIES_STATUS_UNAUTHORIZED),
            @ApiResponse(responseCode = "500", description = SAVE_ENTITY_TIMESERIES_STATUS_INTERNAL_SERVER_ERROR),
    })
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping(value = "/{entityType}/{entityId}/timeseries/{scope}/{ttl}")
    public DeferredResult<ResponseEntity> saveEntityTelemetryWithTTL(
            @Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION, required = true, schema = @Schema(defaultValue = "DEVICE")) @PathVariable("entityType") String entityType,
            @Parameter(description = ENTITY_ID_PARAM_DESCRIPTION, required = true) @PathVariable("entityId") String entityIdStr,
            @Parameter(description = TELEMETRY_SCOPE_DESCRIPTION, required = true, schema = @Schema(allowableValues = "ANY")) @PathVariable("scope") String scope,
            @Parameter(description = "A long value representing TTL (Time to Live) parameter.", required = true) @PathVariable("ttl") Long ttl,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = TELEMETRY_JSON_REQUEST_DESCRIPTION, required = true) @RequestBody String requestBody) throws ThingsboardException {
        EntityId entityId = EntityIdFactory.getByTypeAndId(entityType, entityIdStr);
        return saveTelemetry(getTenantId(), entityId, requestBody, ttl);
    }

    @ApiOperation(value = "Delete entity time series data (deleteEntityTimeseries)",
            notes = "Delete time series for selected entity based on entity id, entity type and keys." +
                    " Use 'deleteAllDataForKeys' to delete all time series data." +
                    " Use 'startTs' and 'endTs' to specify time-range instead. " +
                    " Use 'deleteLatest' to delete latest value (stored in separate table for performance) if the value's timestamp matches the time-range. " +
                    " Use 'rewriteLatestIfDeleted' to rewrite latest value (stored in separate table for performance) if the value's timestamp matches the time-range and 'deleteLatest' param is true." +
                    " The replacement value will be fetched from the 'time series' table, and its timestamp will be the most recent one before the defined time-range. " +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Time series for the selected keys in the request was removed. " +
                    "Platform creates an audit log event about entity time series removal with action type 'TIMESERIES_DELETED'."),
            @ApiResponse(responseCode = "400", description = "Platform returns a bad request in case if keys list is empty or start and end timestamp values is empty when deleteAllDataForKeys is set to false."),
            @ApiResponse(responseCode = "401", description = "User is not authorized to delete entity time series for selected entity. Most likely, User belongs to different Customer or Tenant."),
            @ApiResponse(responseCode = "500", description = "The exception was thrown during processing the request. " +
                    "Platform creates an audit log event about entity time series removal with action type 'TIMESERIES_DELETED' that includes an error stacktrace."),
    })
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @DeleteMapping(value = "/{entityType}/{entityId}/timeseries/delete")
    public DeferredResult<ResponseEntity> deleteEntityTimeseries(
            @Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION, required = true, schema = @Schema(defaultValue = "DEVICE")) @PathVariable("entityType") String entityType,
            @Parameter(description = ENTITY_ID_PARAM_DESCRIPTION, required = true) @PathVariable("entityId") String entityIdStr,
            @Parameter(description = TELEMETRY_KEYS_DESCRIPTION) @RequestParam(name = "keys", required = false) String keysStr,
            @Parameter(description = "A boolean value to specify if should be deleted all data for selected keys or only data that are in the selected time range.")
            @RequestParam(name = "deleteAllDataForKeys", defaultValue = "false") boolean deleteAllDataForKeys,
            @Parameter(description = "A long value representing the start timestamp of removal time range in milliseconds.")
            @RequestParam(name = "startTs", required = false) Long startTs,
            @Parameter(description = "A long value representing the end timestamp of removal time range in milliseconds.")
            @RequestParam(name = "endTs", required = false) Long endTs,
            @Parameter(description = "If the parameter is set to true, the latest telemetry can be removed, otherwise, in case that parameter is set to false the latest value will not removed.")
            @RequestParam(name = "deleteLatest", required = false, defaultValue = "true") boolean deleteLatest,
            @Parameter(description = "If the parameter is set to true, the latest telemetry will be rewritten in case that current latest value was removed, otherwise, in case that parameter is set to false the new latest value will not set.")
            @RequestParam(name = "rewriteLatestIfDeleted", defaultValue = "false") boolean rewriteLatestIfDeleted,
            @RequestParam MultiValueMap<String, String> params) throws ThingsboardException {
        List<String> keys = getKeys(keysStr, params);
        EntityId entityId = EntityIdFactory.getByTypeAndId(entityType, entityIdStr);
        return deleteTimeseries(entityId, keys, deleteAllDataForKeys, startTs, endTs, rewriteLatestIfDeleted, deleteLatest);
    }

    private DeferredResult<ResponseEntity> deleteTimeseries(EntityId entityIdStr, List<String> keys, boolean deleteAllDataForKeys,
                                                            Long startTs, Long endTs, boolean rewriteLatestIfDeleted, boolean deleteLatest) throws ThingsboardException {
        if (keys.isEmpty()) {
            return getImmediateDeferredResult("Empty keys: " + keys, HttpStatus.BAD_REQUEST);
        }
        SecurityUser user = getCurrentUser();

        long deleteFromTs;
        long deleteToTs;
        if (deleteAllDataForKeys) {
            deleteFromTs = 0L;
            deleteToTs = System.currentTimeMillis();
        } else {
            if (startTs == null || endTs == null) {
                return getImmediateDeferredResult("When deleteAllDataForKeys is false, start and end timestamp values shouldn't be empty", HttpStatus.BAD_REQUEST);
            } else {
                deleteFromTs = startTs;
                deleteToTs = endTs;
            }
        }

        return accessValidator.validateEntityAndCallback(user, Operation.WRITE_TELEMETRY, entityIdStr, (result, tenantId, entityId) -> {
            List<DeleteTsKvQuery> deleteTsKvQueries = new ArrayList<>();
            for (String key : keys) {
                deleteTsKvQueries.add(new BaseDeleteTsKvQuery(key, deleteFromTs, deleteToTs, rewriteLatestIfDeleted, deleteLatest));
            }
            tsSubService.deleteTimeseries(TimeseriesDeleteRequest.builder()
                    .tenantId(tenantId)
                    .entityId(entityId)
                    .keys(keys)
                    .deleteHistoryQueries(deleteTsKvQueries)
                    .callback(new FutureCallback<>() {
                        @Override
                        public void onSuccess(@Nullable List<String> tmp) {
                            logTimeseriesDeleted(user, entityId, keys, deleteFromTs, deleteToTs, null);
                            result.setResult(new ResponseEntity<>(HttpStatus.OK));
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            logTimeseriesDeleted(user, entityId, keys, deleteFromTs, deleteToTs, t);
                            result.setResult(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
                        }
                    })
                    .build());
        });
    }

    @ApiOperation(value = "Delete device attributes (deleteDeviceAttributes)",
            notes = "Delete device attributes using provided Device Id, scope and a list of keys. " +
                    "Referencing a non-existing Device Id will cause an error" + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Device attributes was removed for the selected keys in the request. " +
                    "Platform creates an audit log event about device attributes removal with action type 'ATTRIBUTES_DELETED'."),
            @ApiResponse(responseCode = "400", description = "Platform returns a bad request in case if keys or scope are not specified."),
            @ApiResponse(responseCode = "401", description = "User is not authorized to delete device attributes for selected entity. Most likely, User belongs to different Customer or Tenant."),
            @ApiResponse(responseCode = "500", description = "The exception was thrown during processing the request. " +
                    "Platform creates an audit log event about device attributes removal with action type 'ATTRIBUTES_DELETED' that includes an error stacktrace."),
    })
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @DeleteMapping(value = "/{deviceId}/{scope}")
    public DeferredResult<ResponseEntity> deleteDeviceAttributes(
            @Parameter(description = DEVICE_ID_PARAM_DESCRIPTION, required = true) @PathVariable(DEVICE_ID) String deviceIdStr,
            @Parameter(description = ATTRIBUTES_SCOPE_DESCRIPTION, schema = @Schema(allowableValues = {"SERVER_SCOPE", "SHARED_SCOPE", "CLIENT_SCOPE"}, requiredMode = Schema.RequiredMode.REQUIRED)) @PathVariable("scope") AttributeScope scope,
            @Parameter(description = ATTRIBUTES_KEYS_DESCRIPTION) @RequestParam(name = "keys", required = false) String keysStr,
            @RequestParam MultiValueMap<String, String> params) throws ThingsboardException {
        List<String> keys = getKeys(keysStr, params);
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(EntityType.DEVICE, deviceIdStr);
        return deleteAttributes(entityId, scope, keys);
    }

    @ApiOperation(value = "Delete entity attributes (deleteEntityAttributes)",
            notes = "Delete entity attributes using provided Entity Id, scope and a list of keys. " +
                    INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Entity attributes was removed for the selected keys in the request. " +
                    "Platform creates an audit log event about entity attributes removal with action type 'ATTRIBUTES_DELETED'."),
            @ApiResponse(responseCode = "400", description = "Platform returns a bad request in case if keys or scope are not specified."),
            @ApiResponse(responseCode = "401", description = "User is not authorized to delete entity attributes for selected entity. Most likely, User belongs to different Customer or Tenant."),
            @ApiResponse(responseCode = "500", description = "The exception was thrown during processing the request. " +
                    "Platform creates an audit log event about entity attributes removal with action type 'ATTRIBUTES_DELETED' that includes an error stacktrace."),
    })
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @DeleteMapping(value = "/{entityType}/{entityId}/{scope}")
    public DeferredResult<ResponseEntity> deleteEntityAttributes(
            @Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION, required = true, schema = @Schema(defaultValue = "DEVICE")) @PathVariable("entityType") String entityType,
            @Parameter(description = ENTITY_ID_PARAM_DESCRIPTION, required = true) @PathVariable("entityId") String entityIdStr,
            @Parameter(description = ATTRIBUTES_SCOPE_DESCRIPTION, required = true, schema = @Schema(allowableValues = {"SERVER_SCOPE", "SHARED_SCOPE", "CLIENT_SCOPE"})) @PathVariable("scope") AttributeScope scope,
            @Parameter(description = ATTRIBUTES_KEYS_DESCRIPTION) @RequestParam(name = "keys", required = false) String keysStr,
            @RequestParam MultiValueMap<String, String> params) throws ThingsboardException {
        List<String> keys = getKeys(keysStr, params);
        EntityId entityId = EntityIdFactory.getByTypeAndId(entityType, entityIdStr);
        return deleteAttributes(entityId, scope, keys);
    }

    private List<String> getKeys(String keysStr, MultiValueMap<String, String> params) {
        return params.get("key") != null ? params.get("key") : toKeysList(keysStr);
    }

    private DeferredResult<ResponseEntity> deleteAttributes(EntityId entityIdSrc, AttributeScope scope, List<String> keys) throws ThingsboardException {
        if (keys.isEmpty()) {
            return getImmediateDeferredResult("Empty keys: " + keys, HttpStatus.BAD_REQUEST);
        }
        SecurityUser user = getCurrentUser();

        return accessValidator.validateEntityAndCallback(getCurrentUser(), Operation.WRITE_ATTRIBUTES, entityIdSrc, (result, tenantId, entityId) -> {
            tsSubService.deleteAttributes(AttributesDeleteRequest.builder()
                    .tenantId(tenantId)
                    .entityId(entityId)
                    .scope(scope)
                    .keys(keys)
                    .callback(new FutureCallback<>() {
                        @Override
                        public void onSuccess(@Nullable Void tmp) {
                            logAttributesDeleted(user, entityId, scope, keys, null);
                            if (entityIdSrc.getEntityType().equals(EntityType.DEVICE)) {
                                DeviceId deviceId = new DeviceId(entityId.getId());
                                tbClusterService.pushMsgToCore(DeviceAttributesEventNotificationMsg.onDelete(
                                        user.getTenantId(), deviceId, scope.name(), keys), null);
                            }
                            result.setResult(new ResponseEntity<>(HttpStatus.OK));
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            logAttributesDeleted(user, entityId, scope, keys, t);
                            result.setResult(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
                        }
                    })
                    .build());
        });
    }

    private DeferredResult<ResponseEntity> saveAttributes(TenantId srcTenantId, EntityId entityIdSrc, AttributeScope scope, String jsonStr) throws ThingsboardException {
        if (AttributeScope.SERVER_SCOPE != scope && AttributeScope.SHARED_SCOPE != scope) {
            return getImmediateDeferredResult("Invalid scope: " + scope, HttpStatus.BAD_REQUEST);
        }
        JsonElement json;
        try {
            json = JsonParser.parseString(jsonStr);
        } catch (Exception e) {
            return getImmediateDeferredResult("Invalid JSON", HttpStatus.BAD_REQUEST);
        }
        if (json.isJsonObject()) {
            List<AttributeKvEntry> attributes = JsonConverter.convertToAttributes(json);
            if (attributes.isEmpty()) {
                return getImmediateDeferredResult("No attributes data found in request body!", HttpStatus.BAD_REQUEST);
            }
            for (AttributeKvEntry attributeKvEntry : attributes) {
                if (attributeKvEntry.getKey().isBlank()) {
                    return getImmediateDeferredResult("Key cannot be blank", HttpStatus.BAD_REQUEST);
                }
            }
            SecurityUser user = getCurrentUser();
            return accessValidator.validateEntityAndCallback(getCurrentUser(), Operation.WRITE_ATTRIBUTES, entityIdSrc, (result, tenantId, entityId) -> {
                tsSubService.saveAttributes(AttributesSaveRequest.builder()
                        .tenantId(tenantId)
                        .entityId(entityId)
                        .scope(scope)
                        .entries(attributes)
                        .callback(new FutureCallback<>() {
                            @Override
                            public void onSuccess(@Nullable Void tmp) {
                                logAttributesUpdated(user, entityId, scope, attributes, null);
                                result.setResult(new ResponseEntity(HttpStatus.OK));
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                logAttributesUpdated(user, entityId, scope, attributes, t);
                                AccessValidator.handleError(t, result, HttpStatus.INTERNAL_SERVER_ERROR);
                            }
                        })
                        .build());
            });
        } else {
            return getImmediateDeferredResult("Request is not a JSON object", HttpStatus.BAD_REQUEST);
        }
    }

    private DeferredResult<ResponseEntity> saveTelemetry(TenantId curTenantId, EntityId entityIdSrc, String requestBody, long ttl) throws ThingsboardException {
        Map<Long, List<KvEntry>> telemetryRequest;
        JsonElement telemetryJson;
        try {
            telemetryJson = JsonParser.parseString(requestBody);
        } catch (Exception e) {
            return getImmediateDeferredResult("Unable to parse time series payload: Invalid JSON body!", HttpStatus.BAD_REQUEST);
        }
        try {
            telemetryRequest = JsonConverter.convertToTelemetry(telemetryJson, System.currentTimeMillis());
        } catch (Exception e) {
            return getImmediateDeferredResult("Unable to parse time series payload. Invalid JSON body: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        List<TsKvEntry> entries = new ArrayList<>();
        for (Map.Entry<Long, List<KvEntry>> entry : telemetryRequest.entrySet()) {
            for (KvEntry kv : entry.getValue()) {
                entries.add(new BasicTsKvEntry(entry.getKey(), kv));
            }
        }
        if (entries.isEmpty()) {
            return getImmediateDeferredResult("No time series data found in request body!", HttpStatus.BAD_REQUEST);
        }
        SecurityUser user = getCurrentUser();
        return accessValidator.validateEntityAndCallback(getCurrentUser(), Operation.WRITE_TELEMETRY, entityIdSrc, (result, tenantId, entityId) -> {
            long tenantTtl = ttl;
            if (!TenantId.SYS_TENANT_ID.equals(tenantId) && tenantTtl == 0) {
                TenantProfile tenantProfile = tenantProfileCache.get(tenantId);
                tenantTtl = TimeUnit.DAYS.toSeconds(((DefaultTenantProfileConfiguration) tenantProfile.getProfileData().getConfiguration()).getDefaultStorageTtlDays());
            }
            tsSubService.saveTimeseries(TimeseriesSaveRequest.builder()
                    .tenantId(tenantId)
                    .customerId(user.getCustomerId())
                    .entityId(entityId)
                    .entries(entries)
                    .ttl(tenantTtl)
                    .callback(new FutureCallback<Void>() {
                        @Override
                        public void onSuccess(@Nullable Void tmp) {
                            logTelemetryUpdated(user, entityId, entries, null);
                            result.setResult(new ResponseEntity(HttpStatus.OK));
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            logTelemetryUpdated(user, entityId, entries, t);
                            AccessValidator.handleError(t, result, HttpStatus.INTERNAL_SERVER_ERROR);
                        }
                    })
                    .build());
        });
    }

    private void getLatestTimeseriesValuesCallback(@Nullable DeferredResult<ResponseEntity> result, SecurityUser user, EntityId entityId, List<String> keys, Boolean useStrictDataTypes) {
        ListenableFuture<List<TsKvEntry>> future;
        if (keys.isEmpty()) {
            future = tsService.findAllLatest(user.getTenantId(), entityId);
        } else {
            future = tsService.findLatest(user.getTenantId(), entityId, keys);
        }
        Futures.addCallback(future, getTsKvListCallback(result, useStrictDataTypes), MoreExecutors.directExecutor());
    }

    private void getAttributeValuesCallback(@Nullable DeferredResult<ResponseEntity> result, SecurityUser user, EntityId entityId, AttributeScope scope, List<String> keys) {
        FutureCallback<List<AttributeKvEntry>> callback = getAttributeValuesToResponseCallback(result, user, scope, entityId, keys);
        if (scope != null) {
            if (keys != null && !keys.isEmpty()) {
                Futures.addCallback(attributesService.find(user.getTenantId(), entityId, scope, keys), callback, MoreExecutors.directExecutor());
            } else {
                Futures.addCallback(attributesService.findAll(user.getTenantId(), entityId, scope), callback, MoreExecutors.directExecutor());
            }
        } else {
            List<ListenableFuture<List<AttributeKvEntry>>> futures = new ArrayList<>();
            for (AttributeScope tmpScope : AttributeScope.values()) {
                if (keys != null && !keys.isEmpty()) {
                    futures.add(attributesService.find(user.getTenantId(), entityId, tmpScope, keys));
                } else {
                    futures.add(attributesService.findAll(user.getTenantId(), entityId, tmpScope));
                }
            }

            ListenableFuture<List<AttributeKvEntry>> future = mergeAllAttributesFutures(futures);

            Futures.addCallback(future, callback, MoreExecutors.directExecutor());
        }
    }

    private void getAttributeKeysCallback(@Nullable DeferredResult<ResponseEntity> result, TenantId tenantId, EntityId entityId, AttributeScope scope) {
        Futures.addCallback(attributesService.findAll(tenantId, entityId, scope), getAttributeKeysToResponseCallback(result), MoreExecutors.directExecutor());
    }

    private void getAttributeKeysCallback(@Nullable DeferredResult<ResponseEntity> result, TenantId tenantId, EntityId entityId) {
        List<ListenableFuture<List<AttributeKvEntry>>> futures = new ArrayList<>();
        for (AttributeScope scope : AttributeScope.values()) {
            futures.add(attributesService.findAll(tenantId, entityId, scope));
        }

        ListenableFuture<List<AttributeKvEntry>> future = mergeAllAttributesFutures(futures);

        Futures.addCallback(future, getAttributeKeysToResponseCallback(result), MoreExecutors.directExecutor());
    }

    private FutureCallback<List<TsKvEntry>> getTsKeysToResponseCallback(final DeferredResult<ResponseEntity> response) {
        return new FutureCallback<>() {
            @Override
            public void onSuccess(List<TsKvEntry> values) {
                List<String> keys = values.stream().map(KvEntry::getKey).collect(Collectors.toList());
                response.setResult(new ResponseEntity<>(keys, HttpStatus.OK));
            }

            @Override
            public void onFailure(Throwable e) {
                log.error("Failed to fetch attributes", e);
                AccessValidator.handleError(e, response, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        };
    }

    private FutureCallback<List<AttributeKvEntry>> getAttributeKeysToResponseCallback(final DeferredResult<ResponseEntity> response) {
        return new FutureCallback<List<AttributeKvEntry>>() {

            @Override
            public void onSuccess(List<AttributeKvEntry> attributes) {
                List<String> keys = attributes.stream().map(KvEntry::getKey).collect(Collectors.toList());
                response.setResult(new ResponseEntity<>(keys, HttpStatus.OK));
            }

            @Override
            public void onFailure(Throwable e) {
                log.error("Failed to fetch attributes", e);
                AccessValidator.handleError(e, response, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        };
    }

    private FutureCallback<List<AttributeKvEntry>> getAttributeValuesToResponseCallback(final DeferredResult<ResponseEntity> response,
                                                                                        final SecurityUser user, final AttributeScope scope,
                                                                                        final EntityId entityId, final List<String> keyList) {
        return new FutureCallback<>() {
            @Override
            public void onSuccess(List<AttributeKvEntry> attributes) {
                List<AttributeData> values = attributes.stream().map(attribute ->
                        new AttributeData(attribute.getLastUpdateTs(), attribute.getKey(), getKvValue(attribute))
                ).collect(Collectors.toList());
                logAttributesRead(user, entityId, scope, keyList, null);
                response.setResult(new ResponseEntity<>(values, HttpStatus.OK));
            }

            @Override
            public void onFailure(Throwable e) {
                log.error("Failed to fetch attributes", e);
                logAttributesRead(user, entityId, scope, keyList, e);
                AccessValidator.handleError(e, response, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        };
    }

    private FutureCallback<List<TsKvEntry>> getTsKvListCallback(final DeferredResult<ResponseEntity> response, Boolean useStrictDataTypes) {
        return new FutureCallback<>() {
            @Override
            public void onSuccess(List<TsKvEntry> data) {
                Map<String, List<TsData>> result = new LinkedHashMap<>();
                for (TsKvEntry entry : data) {
                    Object value = useStrictDataTypes ? getKvValue(entry) : entry.getValueAsString();
                    result.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(new TsData(entry.getTs(), value));
                }
                response.setResult(new ResponseEntity<>(result, HttpStatus.OK));
            }

            @Override
            public void onFailure(Throwable e) {
                log.error("Failed to fetch historical data", e);
                AccessValidator.handleError(e, response, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        };
    }

    private void logTimeseriesDeleted(SecurityUser user, EntityId entityId, List<String> keys, long startTs, long endTs, Throwable e) {
        logEntityActionService.logEntityAction(user.getTenantId(), entityId, ActionType.TIMESERIES_DELETED, user,
                toException(e), keys, startTs, endTs);
    }

    private void logTelemetryUpdated(SecurityUser user, EntityId entityId, List<TsKvEntry> telemetry, Throwable e) {
        logEntityActionService.logEntityAction(user.getTenantId(), entityId, ActionType.TIMESERIES_UPDATED, user,
                toException(e), telemetry);
    }

    private void logAttributesDeleted(SecurityUser user, EntityId entityId, AttributeScope scope, List<String> keys, Throwable e) {
        logEntityActionService.logEntityAction(user.getTenantId(), (UUIDBased & EntityId) entityId,
                ActionType.ATTRIBUTES_DELETED, user, toException(e), scope, keys);
    }

    private void logAttributesUpdated(SecurityUser user, EntityId entityId, AttributeScope scope, List<AttributeKvEntry> attributes, Throwable e) {
        logEntityActionService.logEntityAction(user.getTenantId(), entityId, ActionType.ATTRIBUTES_UPDATED, user,
                toException(e), scope, attributes);
    }


    private void logAttributesRead(SecurityUser user, EntityId entityId, AttributeScope scope, List<String> keys, Throwable e) {
        logEntityActionService.logEntityAction(user.getTenantId(), entityId, ActionType.ATTRIBUTES_READ, user,
                toException(e), scope, keys);
    }

    private ListenableFuture<List<AttributeKvEntry>> mergeAllAttributesFutures(List<ListenableFuture<List<AttributeKvEntry>>> futures) {
        return Futures.transform(Futures.successfulAsList(futures),
                (Function<? super List<List<AttributeKvEntry>>, ? extends List<AttributeKvEntry>>) input -> {
                    List<AttributeKvEntry> tmp = new ArrayList<>();
                    if (input != null) {
                        input.forEach(tmp::addAll);
                    }
                    return tmp;
                }, executor);
    }

    private List<String> toKeysList(String keys) {
        if (!StringUtils.isEmpty(keys)) {
            return Arrays.asList(keys.split(","));
        } else {
            return Collections.emptyList();
        }
    }

    private DeferredResult<ResponseEntity> getImmediateDeferredResult(String message, HttpStatus status) {
        DeferredResult<ResponseEntity> result = new DeferredResult<>();
        result.setResult(new ResponseEntity<>(message, status));
        return result;
    }

    private JsonNode toJsonNode(String value) {
        try {
            return JacksonUtil.toJsonNode(value);
        } catch (IllegalArgumentException e) {
            throw new JsonParseException("Can't parse jsonValue: " + value, e);
        }
    }

    private Object getKvValue(KvEntry entry) {
        if (entry.getDataType() == DataType.JSON) {
            return toJsonNode(entry.getJsonValue().get());
        }
        return entry.getValue();
    }

}
